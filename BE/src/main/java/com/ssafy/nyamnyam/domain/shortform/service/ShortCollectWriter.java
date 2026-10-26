package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.entity.YoutubeChannel;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.YoutubeChannelRepository;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 수집 결과 저장 전용 빈. 외부 API 호출은 {@link ShortCollectService}가 트랜잭션 밖에서 끝내고,
 * 여기서는 키워드 하나 분량만 짧은 트랜잭션으로 저장한다.
 * (같은 클래스 안에서 @Transactional 메서드를 부르면 프록시를 거치지 않아 트랜잭션이 걸리지 않으므로 빈을 나눴다.)
 */
@Component
@RequiredArgsConstructor
public class ShortCollectWriter {

    static final String ASSIGN_METHOD_KEYWORD = "KEYWORD";
    static final String CHANNEL_TYPE_MULTI = "MULTI";
    static final String CHANNEL_STATUS_CANDIDATE = "CANDIDATE";
    static final String CHANNEL_DISCOVERED_BY_SEARCH = "SEARCH";

    private static final int TITLE_MAX = 200;
    private static final int THUMBNAIL_MAX = 500;
    private static final int CHANNEL_TITLE_MAX = 100;

    private final ShortRepository shortRepository;
    private final YoutubeChannelRepository channelRepository;
    private final HobbySourceMappingRepository mappingRepository;
    private final HobbyRepository hobbyRepository;

    /** 저장할 영상 한 건과 판별 결과 */
    public record ShortDraft(YoutubeVideo video, ShortFilter.Decision decision) {
    }

    public record SaveResult(int candidates, int needsReview, int rejected) {
        public int accepted() {
            return candidates + needsReview;
        }
    }

    /**
     * 키워드 하나의 수집 결과를 저장하고 키워드의 다음 수집 위치를 기록한다.
     * REJECTED 영상은 취미·채널 없이 사유만 남긴다(다시 수집하지 않기 위한 기록).
     */
    @Transactional
    public SaveResult save(Long mappingId, Long hobbyId, List<ShortDraft> drafts, String nextPageToken) {
        Hobby hobby = hobbyRepository.getReferenceById(hobbyId);
        Map<String, YoutubeChannel> channels = upsertChannels(drafts);

        int candidates = 0;
        int needsReview = 0;
        int rejected = 0;
        List<Short> shorts = new ArrayList<>(drafts.size());

        for (ShortDraft draft : drafts) {
            YoutubeVideo video = draft.video();
            ShortFilter.Decision decision = draft.decision();
            boolean isRejected = decision.status() == ShortStatus.REJECTED;

            Short entity = Short.builder()
                    .hobby(isRejected ? null : hobby)
                    .channel(isRejected ? null : channels.get(video.channelId()))
                    .youtubeVideoId(video.videoId())
                    .title(truncate(video.title() == null || video.title().isBlank() ? "(제목 없음)" : video.title(), TITLE_MAX))
                    .description(video.description())
                    .thumbnailUrl(truncate(video.thumbnailUrl(), THUMBNAIL_MAX))
                    .durationSeconds(toSmallint(video.durationSeconds()))
                    .publishedAt(video.publishedAt())
                    .status(isRejected ? ShortStatus.CANDIDATE : decision.status())
                    .assignMethod(isRejected ? null : ASSIGN_METHOD_KEYWORD)
                    .assignConfidence(toConfidence(decision.confidence()))
                    .build();

            if (isRejected) {
                entity.reject(decision.rejectReason());
                rejected++;
            } else if (decision.status() == ShortStatus.CANDIDATE) {
                candidates++;
            } else {
                needsReview++;
            }
            shorts.add(entity);
        }
        shortRepository.saveAll(shorts);

        HobbySourceMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new IllegalStateException("hobby_source_mappings not found: " + mappingId));
        mapping.markCollected(nextPageToken, LocalDateTime.now());

        return new SaveResult(candidates, needsReview, rejected);
    }

    /** 주간 반영: CANDIDATE 영상을 모두 ACTIVE로 올린다. 반영한 건수를 돌려준다. */
    @Transactional
    public int releaseCandidates() {
        return shortRepository.bulkRelease(ShortStatus.CANDIDATE, ShortStatus.ACTIVE, LocalDateTime.now());
    }

    /** 노출 대상(REJECTED 아님) 영상의 채널을 찾고, 없으면 검색으로 발견한 후보 채널로 만든다. */
    private Map<String, YoutubeChannel> upsertChannels(List<ShortDraft> drafts) {
        Map<String, String> titles = drafts.stream()
                .filter(d -> d.decision().status() != ShortStatus.REJECTED)
                .map(ShortDraft::video)
                .filter(v -> v.channelId() != null)
                .collect(Collectors.toMap(YoutubeVideo::channelId,
                        v -> Objects.requireNonNullElse(v.channelTitle(), v.channelId()),
                        (a, b) -> a));
        if (titles.isEmpty()) {
            return Map.of();
        }

        Map<String, YoutubeChannel> channels = channelRepository.findByYoutubeChannelIdIn(titles.keySet()).stream()
                .collect(Collectors.toMap(YoutubeChannel::getYoutubeChannelId, Function.identity(),
                        (a, b) -> a, HashMap::new));

        List<YoutubeChannel> created = titles.entrySet().stream()
                .filter(e -> !channels.containsKey(e.getKey()))
                .map(e -> YoutubeChannel.builder()
                        .youtubeChannelId(e.getKey())
                        .title(truncate(e.getValue(), CHANNEL_TITLE_MAX))
                        .type(CHANNEL_TYPE_MULTI)
                        .status(CHANNEL_STATUS_CANDIDATE)
                        .discoveredBy(CHANNEL_DISCOVERED_BY_SEARCH)
                        .build())
                .toList();
        channelRepository.saveAll(created).forEach(c -> channels.put(c.getYoutubeChannelId(), c));
        return channels;
    }

    private static java.lang.Short toSmallint(int seconds) {
        return (short) Math.max(0, Math.min(seconds, java.lang.Short.MAX_VALUE));
    }

    private static BigDecimal toConfidence(Double confidence) {
        return confidence == null ? null : BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
