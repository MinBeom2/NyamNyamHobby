package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.infra.publicdata.HtmlText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 수집한 행사를 feed_items에 저장한다. 출처와 상관없이 제목·요약의 HTML 문자 참조(&lt; &middot; 등)는 여기서 푼다. 항목마다 트랜잭션을 따로 써서 한 건이 실패해도 나머지는 저장된다.
 * MVP는 관리자 검토(P2)가 없어 취미가 정해진 항목은 바로 PUBLISHED로 공개한다.
 */
@Component
@RequiredArgsConstructor
public class FeedWriter {

    static final int TITLE_MAX = 150;
    static final int SUMMARY_MAX = 300;
    static final int URL_MAX = 500;

    public enum Result { CREATED, UPDATED, DUPLICATE }

    private final FeedItemRepository feedItemRepository;
    private final RegionRepository regionRepository;

    /** 같은 출처에서 이미 받은 항목이면 바뀔 수 있는 값만 고치고 true. 처음 보는 항목이면 false */
    @Transactional
    public boolean refreshIfExists(FeedOrigin origin, String sourceRef, String title, String thumbnailUrl,
                                   LocalDate start, LocalDate end) {
        return feedItemRepository.findByOriginAndSourceRef(origin, sourceRef)
                .map(item -> {
                    item.refreshFromSource(cut(HtmlText.unescape(title), TITLE_MAX), cut(thumbnailUrl, URL_MAX), start, end);
                    return true;
                })
                .orElse(false);
    }

    /** 위와 같고 요약도 새 값으로 바꾼다(강좌 요약에는 매일 바뀌는 접수 상태가 들어간다) */
    @Transactional
    public boolean refreshIfExists(FeedOrigin origin, String sourceRef, String title, String thumbnailUrl,
                                   LocalDate start, LocalDate end, String summary) {
        return feedItemRepository.findByOriginAndSourceRef(origin, sourceRef)
                .map(item -> {
                    item.refreshFromSource(cut(HtmlText.unescape(title), TITLE_MAX), cut(thumbnailUrl, URL_MAX), start, end);
                    item.refreshSummary(cut(HtmlText.plain(summary), SUMMARY_MAX));
                    return true;
                })
                .orElse(false);
    }

    /** 새 항목 저장. 다른 출처에 같은 행사(시작일 같고 제목이 비슷)가 있으면 저장하지 않는다 */
    @Transactional
    public Result create(FeedCandidate candidate, Hobby hobby, double titleSimilarity) {
        if (candidate.eventStart() != null) {
            boolean duplicate = feedItemRepository.findByEventStart(candidate.eventStart()).stream()
                    .anyMatch(other -> other.getOrigin() != candidate.origin()
                            && similarity(other.getTitle(), HtmlText.unescape(candidate.title())) >= titleSimilarity);
            if (duplicate) return Result.DUPLICATE;
        }
        LocalDateTime now = LocalDateTime.now();
        feedItemRepository.save(FeedItem.builder()
                .hobby(hobby)
                .type(candidate.type())
                .title(cut(HtmlText.unescape(candidate.title()), TITLE_MAX))
                .summary(cut(HtmlText.plain(candidate.summary()), SUMMARY_MAX))
                .sourceName(candidate.sourceName())
                .sourceUrl(cut(candidate.sourceUrl(), URL_MAX))
                .sourceRef(candidate.sourceRef())
                .thumbnailUrl(cut(candidate.thumbnailUrl(), URL_MAX))
                .region(candidate.regionCode() == null ? null : regionRepository.findByCode(candidate.regionCode()).orElse(null))
                .publishedAt(now)
                .eventStart(candidate.eventStart())
                .eventEnd(candidate.eventEnd())
                .origin(candidate.origin())
                .status(FeedStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return Result.CREATED;
    }

    /** 두 제목의 글자 2개씩 묶음(바이그램) 겹침 정도(다이스 계수, 0~1). 공백·대소문자는 무시한다 */
    static double similarity(String a, String b) {
        Set<String> x = bigrams(a), y = bigrams(b);
        if (x.isEmpty() || y.isEmpty()) return 0;
        Set<String> common = new HashSet<>(x);
        common.retainAll(y);
        return 2.0 * common.size() / (x.size() + y.size());
    }

    private static Set<String> bigrams(String value) {
        Set<String> set = new HashSet<>();
        if (value == null) return set;
        String text = FeedHobbyMatcher.normalize(value);
        for (int i = 0; i + 1 < text.length(); i++) set.add(text.substring(i, i + 2));
        return set;
    }

    private static String cut(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
