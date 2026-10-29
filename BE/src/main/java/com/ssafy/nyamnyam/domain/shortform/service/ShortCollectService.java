package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.YoutubeChannelRepository;
import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectWriter.SaveResult;
import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectWriter.ShortDraft;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFilter.Judgement;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFilter.Outcome;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeClient;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeQuotaExceededException;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeSearchPage;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 숏폼 수집(6시간마다)과 주간 반영(목 06:00). 스케줄러가 부르고, 결과는 batch_runs에 남긴다.
 *
 * <p>수집 한 번 = 목표에 못 미친 취미의 키워드 N개(기본 2개)를 오래된 순으로 골라
 * 검색(100단위) → 새 영상 상세 조회(1단위) → 기계적 필터 → GPT 판별 → 키워드 단위 저장.
 * 목표를 채운 취미도 refresh-interval-days마다 키워드를 다시 검색해, 최근 refresh-window-days 안에 올라온
 * 영상 중 검색 관련도 순으로 refresh-max-per-keyword개까지 새로 받는다(갱신). 이미 저장된 영상은
 * 상태와 관계없이(REJECTED 포함) 건너뛰므로 같은 영상을 다시 받거나 다시 판별하지 않는다.
 * 이 클래스에는 트랜잭션을 걸지 않는다. 외부 API를 기다리는 동안 DB 커넥션을 잡지 않도록
 * 저장만 {@link ShortCollectWriter}에서 짧게 처리한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortCollectService {

    /** YouTube 할당량을 쓰는 작업. 하루 사용량 합계에 넣는다 */
    static final List<BatchJob> YOUTUBE_JOBS = List.of(BatchJob.SHORT_COLLECT, BatchJob.SHORT_CHECK, BatchJob.CHANNEL_DISCOVER);
    /** 취미별 목표 개수에 세는 상태 */
    static final List<ShortStatus> TARGET_STATUSES = List.of(ShortStatus.CANDIDATE, ShortStatus.ACTIVE);
    /** 채널 상한에 세는 상태 */
    static final List<ShortStatus> CHANNEL_LIMIT_STATUSES = List.of(ShortStatus.CANDIDATE, ShortStatus.NEEDS_REVIEW, ShortStatus.ACTIVE);
    /** 키워드 하나에 드는 최대 할당량(검색 1회 + 상세 1회) */
    static final int COST_PER_KEYWORD = YoutubeClient.SEARCH_COST + YoutubeClient.VIDEOS_COST;
    /** YouTube 할당량 초기화 기준 시간대 */
    private static final ZoneId QUOTA_ZONE = ZoneId.of("America/Los_Angeles");
    private static final String CHANNEL_BLOCKED = "REJECTED";

    private final ShortCollectProperties properties;
    private final YoutubeClient youtubeClient;
    private final ShortFilter shortFilter;
    private final ShortContentClassifier classifier;
    private final ShortCollectWriter writer;
    private final ShortRepository shortRepository;
    private final YoutubeChannelRepository channelRepository;
    private final HobbySourceMappingRepository mappingRepository;
    private final BatchRunRepository batchRunRepository;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    /** FILL: 목표 개수까지 채우기(검색 결과를 페이지 순서대로) · REFRESH: 목표를 채운 취미에 최근 영상 조금씩 더하기 */
    enum Mode { FILL, REFRESH }

    record CollectTarget(HobbySourceMapping mapping, Mode mode) {
    }

    /** 수집 1회. 이미 실행 중이면 건너뛴다(직전 회차가 다음 실행 시각까지 끝나지 않은 경우). */
    public BatchRun collect() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[SHORT_COLLECT] previous run still in progress, skipped");
            return null;
        }
        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.SHORT_COLLECT));
        CollectStats stats = new CollectStats();
        try {
            doCollect(stats);
        } catch (RuntimeException e) {
            log.error("[SHORT_COLLECT] unexpected failure", e);
            stats.fatal = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            run.finish(stats.status(), stats.processed, stats.accepted, stats.rejected, stats.quotaUsed, stats.message());
            batchRunRepository.save(run);
            collecting.set(false);
            log.info("[SHORT_COLLECT] {} processed={} accepted={} rejected={} quota={} {}",
                    run.getStatus(), stats.processed, stats.accepted, stats.rejected, stats.quotaUsed, stats.message());
        }
        return run;
    }

    /** 주간 반영 1회: CANDIDATE → ACTIVE. */
    public BatchRun release() {
        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.SHORT_RELEASE));
        try {
            int released = writer.releaseCandidates();
            run.finish(BatchRunStatus.SUCCESS, released, released, 0, 0, null);
        } catch (RuntimeException e) {
            log.error("[SHORT_RELEASE] failed", e);
            run.finish(BatchRunStatus.FAILED, 0, 0, 0, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        log.info("[SHORT_RELEASE] {} released={}", run.getStatus(), run.getAccepted());
        return batchRunRepository.save(run);
    }

    private void doCollect(CollectStats stats) {
        if (!youtubeClient.isConfigured()) {
            stats.fatal = "YOUTUBE_API_KEY 미설정";
            return;
        }

        long usedToday = quotaUsedToday();
        List<CollectTarget> targets = pickTargets();
        if (targets.isEmpty()) {
            stats.note("수집할 키워드 없음(모든 취미가 목표 도달, 갱신 주기 전)");
            return;
        }

        for (CollectTarget collectTarget : targets) {
            HobbySourceMapping target = collectTarget.mapping();
            if (usedToday + stats.quotaUsed + COST_PER_KEYWORD > properties.dailyQuotaLimit()) {
                stats.note("하루 할당량 상한(" + properties.dailyQuotaLimit() + ") 도달, 오늘 사용 " + (usedToday + stats.quotaUsed));
                stats.stoppedByQuota = true;
                break;
            }
            try {
                collectKeyword(collectTarget, stats);
                stats.succeeded++;
            } catch (YoutubeQuotaExceededException e) {
                stats.fail(target.getKeyword(), e.getMessage());
                stats.stoppedByQuota = true;
                break;
            } catch (RuntimeException e) {
                log.warn("[SHORT_COLLECT] keyword '{}' failed: {}", target.getKeyword(), e.toString());
                stats.fail(target.getKeyword(), e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void collectKeyword(CollectTarget collectTarget, CollectStats stats) {
        HobbySourceMapping target = collectTarget.mapping();
        boolean refresh = collectTarget.mode() == Mode.REFRESH;
        Long hobbyId = target.getHobby().getId();
        String hobbyName = target.getHobby().getName();

        stats.quotaUsed += YoutubeClient.SEARCH_COST;
        // 갱신은 최근 업로드만 첫 페이지부터 본다. 채우기용 페이지 위치(nextPageToken)는 건드리지 않는다
        YoutubeSearchPage page = refresh
                ? youtubeClient.searchShorts(target.getKeyword(), null,
                        Instant.now().minus(Duration.ofDays(properties.refreshWindowDays())))
                : youtubeClient.searchShorts(target.getKeyword(), target.getNextPageToken(), null);
        stats.processed += page.videoIds().size();

        List<ShortDraft> drafts = new ArrayList<>();
        int skipped = 0;
        List<String> newIds = newVideoIds(page.videoIds());
        if (!newIds.isEmpty()) {
            stats.quotaUsed += YoutubeClient.videosCost(newIds.size());
            List<YoutubeVideo> videos = youtubeClient.getVideos(newIds);

            List<Judgement> judgements = shortFilter.judge(
                    videos, blockedChannels(videos), channelCounts(hobbyId), policy());

            List<YoutubeVideo> passed = judgements.stream()
                    .filter(j -> j.outcome() == Outcome.PASS)
                    .map(Judgement::video)
                    .toList();
            if (refresh) {
                // 갱신은 검색 관련도 순(YouTube 검색 결과 순서)으로 일부만 판별·저장한다.
                // 조회수 순으로 고르면 뉴스·바이럴 영상이 앞을 차지해 취미 영상이 밀려난다.
                // 나머지는 저장하지 않아 다음 갱신 때 다시 후보가 된다
                Map<String, Integer> searchRank = new HashMap<>();
                for (int i = 0; i < page.videoIds().size(); i++) {
                    searchRank.putIfAbsent(page.videoIds().get(i), i);
                }
                passed = passed.stream()
                        .sorted(Comparator.comparingInt(v -> searchRank.getOrDefault(v.videoId(), Integer.MAX_VALUE)))
                        .limit(Math.max(0, properties.refreshMaxPerKeyword()))
                        .toList();
            }
            Set<String> passedIds = new HashSet<>();
            passed.forEach(v -> passedIds.add(v.videoId()));
            Map<String, ShortContentClassifier.Verdict> verdicts = classifier.classify(hobbyName, passed);

            for (Judgement judgement : judgements) {
                switch (judgement.outcome()) {
                    case REJECT -> drafts.add(new ShortDraft(judgement.video(),
                            new ShortFilter.Decision(ShortStatus.REJECTED, judgement.reason(), null)));
                    case PASS -> {
                        if (passedIds.contains(judgement.video().videoId())) {
                            drafts.add(new ShortDraft(judgement.video(),
                                    ShortFilter.decide(verdicts.get(judgement.video().videoId()),
                                            properties.confidenceThreshold(), properties.rejectThreshold())));
                        } else {
                            skipped++;
                        }
                    }
                    case SKIP -> skipped++;
                }
            }
        }

        String nextPageToken = refresh ? target.getNextPageToken() : page.nextPageToken();
        SaveResult saved = writer.save(target.getId(), hobbyId, drafts, nextPageToken);
        stats.accepted += saved.accepted();
        stats.rejected += saved.rejected() + skipped;
        stats.note(target.getKeyword() + (refresh ? "(갱신)" : "") + " +" + saved.candidates()
                + (saved.needsReview() > 0 ? "(검토 " + saved.needsReview() + ")" : ""));
    }

    /**
     * 이번에 검색할 키워드를 고른다. 모두 오래전에 수집한 순(처음이면 맨 앞), 한 번 실행에서 같은 취미는 한 번만.
     * <ol>
     *   <li>채우기(FILL): 목표 개수에 못 미친 취미의 키워드를 먼저 고른다.</li>
     *   <li>갱신(REFRESH): 자리가 남으면, 목표를 채운 취미 중 마지막 수집 뒤 refresh-interval-days가 지난 키워드를 고른다.</li>
     * </ol>
     */
    List<CollectTarget> pickTargets() {
        List<HobbySourceMapping> mappings = mappingRepository.findAllWithHobby(SourceTarget.SHORT_KEYWORD, HobbyStatus.PUBLISHED);
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : shortRepository.countGroupByHobby(TARGET_STATUSES)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        List<HobbySourceMapping> sorted = mappings.stream()
                .sorted(Comparator.comparing(HobbySourceMapping::getLastCollectedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(HobbySourceMapping::getId))
                .toList();

        Set<Long> pickedHobbies = new HashSet<>();
        List<CollectTarget> picked = new ArrayList<>();

        for (HobbySourceMapping mapping : sorted) {
            if (picked.size() >= properties.keywordsPerRun()) {
                return picked;
            }
            Long hobbyId = mapping.getHobby().getId();
            if (counts.getOrDefault(hobbyId, 0L) < properties.targetPerHobby() && pickedHobbies.add(hobbyId)) {
                picked.add(new CollectTarget(mapping, Mode.FILL));
            }
        }

        LocalDateTime refreshBefore = LocalDateTime.now().minusDays(properties.refreshIntervalDays());
        for (HobbySourceMapping mapping : sorted) {
            if (picked.size() >= properties.keywordsPerRun()) {
                break;
            }
            Long hobbyId = mapping.getHobby().getId();
            boolean full = counts.getOrDefault(hobbyId, 0L) >= properties.targetPerHobby();
            boolean due = mapping.getLastCollectedAt() == null || !mapping.getLastCollectedAt().isAfter(refreshBefore);
            if (full && due && pickedHobbies.add(hobbyId)) {
                picked.add(new CollectTarget(mapping, Mode.REFRESH));
            }
        }
        return picked;
    }

    /** 미국 태평양 시간 자정 이후 YouTube 작업들이 쓴 할당량 */
    long quotaUsedToday() {
        LocalDateTime since = ZonedDateTime.now(QUOTA_ZONE)
                .truncatedTo(ChronoUnit.DAYS)
                .withZoneSameInstant(ZoneId.systemDefault())
                .toLocalDateTime();
        Number used = batchRunRepository.sumQuotaUsedSince(YOUTUBE_JOBS, since);
        return used != null ? used.longValue() : 0L;
    }

    private List<String> newVideoIds(List<String> videoIds) {
        if (videoIds.isEmpty()) {
            return List.of();
        }
        Set<String> existing = new HashSet<>(shortRepository.findExistingVideoIds(videoIds));
        return videoIds.stream()
                .filter(id -> !existing.contains(id))
                .distinct()
                .toList();
    }

    private Set<String> blockedChannels(List<YoutubeVideo> videos) {
        Set<String> channelIds = new LinkedHashSet<>();
        videos.stream().map(YoutubeVideo::channelId).filter(Objects::nonNull).forEach(channelIds::add);
        if (channelIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(channelRepository.findChannelIdsByStatus(channelIds, CHANNEL_BLOCKED));
    }

    private Map<String, Long> channelCounts(Long hobbyId) {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : shortRepository.countGroupByChannelForHobby(hobbyId, CHANNEL_LIMIT_STATUSES)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private ShortFilter.Policy policy() {
        return new ShortFilter.Policy(properties.maxDurationSeconds(), properties.minViewCount(), properties.maxPerChannel());
    }

    /** 한 번 실행의 집계 */
    private static final class CollectStats {
        int processed;
        int accepted;
        int rejected;
        int quotaUsed;
        int succeeded;
        int failed;
        boolean stoppedByQuota;
        String fatal;
        final List<String> notes = new ArrayList<>();

        void note(String note) {
            notes.add(note);
        }

        void fail(String keyword, String reason) {
            failed++;
            notes.add(keyword + " 실패(" + reason + ")");
        }

        BatchRunStatus status() {
            if (fatal != null) {
                return BatchRunStatus.FAILED;
            }
            if (stoppedByQuota) {
                return BatchRunStatus.PARTIAL; // 남은 키워드는 할당량이 초기화된 뒤 이어서 수집
            }
            if (failed > 0) {
                return succeeded == 0 ? BatchRunStatus.FAILED : BatchRunStatus.PARTIAL;
            }
            return BatchRunStatus.SUCCESS;
        }

        String message() {
            List<String> parts = new ArrayList<>(notes);
            if (fatal != null) {
                parts.add(0, fatal);
            }
            return parts.isEmpty() ? null : String.join("; ", parts);
        }
    }
}
