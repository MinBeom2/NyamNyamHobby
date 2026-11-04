package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.publicdata.KopisClient;
import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import com.ssafy.nyamnyam.global.infra.publicdata.TourApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FEED-B02 행사·공연·전시 수집(매일 05:00). 출처별로 받아 제목을 취미 행사 검색어와 대조하고,
 * 걸린 항목만 feed_items에 저장한다(걸리지 않은 항목의 GMS 분류는 FEED-B04).
 * <ul>
 *   <li>TourAPI(축제·행사): 서울에서 진행 중·예정인 행사 목록을 받아 제목으로 취미를 찾는다. 새 항목만 소개·홈페이지를 더 받는다</li>
 *   <li>KOPIS(공연): 취미 검색어로 공연명을 검색한다(장르가 넓어 전체를 받으면 대부분 취미와 무관)</li>
 * </ul>
 * 이미 받은 항목은 (origin, source_ref)로 알아보고 기간·제목·이미지만 고친다. 한 출처가 실패해도 다른 출처는 계속 받는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCollectService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final String TOUR_SOURCE = "한국관광공사 TourAPI";
    static final String KOPIS_SOURCE = "KOPIS 공연예술통합전산망";
    static final String TOUR_SEARCH_URL = "https://korean.visitkorea.or.kr/search/search_list.do?keyword=";

    private final TourApiClient tourApiClient;
    private final KopisClient kopisClient;
    private final HobbySourceMappingRepository mappingRepository;
    private final FeedWriter feedWriter;
    private final BatchRunRepository batchRunRepository;
    private final FeedCollectProperties properties;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    /** 수집 1회. 이미 실행 중이면 건너뛴다 */
    public BatchRun collect() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[FEED_COLLECT] previous run still in progress, skipped");
            return null;
        }
        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.FEED_COLLECT));
        Stats stats = new Stats();
        try {
            List<HobbySourceMapping> mappings = mappingRepository.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED);
            FeedHobbyMatcher matcher = new FeedHobbyMatcher(mappings);
            if (matcher.isEmpty()) {
                stats.fatal = "행사 검색어(EVENT_KEYWORD)가 없습니다";
            } else {
                LocalDate today = LocalDate.now(KST);
                runSource("tour", stats, tourApiClient.isConfigured(), () -> collectTour(matcher, today, stats));
                runSource("kopis", stats, kopisClient.isConfigured(), () -> collectKopis(mappings, matcher, today, stats));
            }
        } catch (RuntimeException e) {
            log.error("[FEED_COLLECT] unexpected failure", e);
            stats.fatal = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            run.finish(stats.status(), stats.processed, stats.accepted, stats.rejected, stats.calls, stats.message());
            batchRunRepository.save(run);
            collecting.set(false);
            log.info("[FEED_COLLECT] {} processed={} accepted={} rejected={} calls={} {}",
                    run.getStatus(), stats.processed, stats.accepted, stats.rejected, stats.calls, stats.message());
        }
        return run;
    }

    private void runSource(String name, Stats stats, boolean configured, Runnable task) {
        if (!configured) {
            stats.note(name + " 키 없음");
            return;
        }
        try {
            task.run();
            stats.succeeded++;
        } catch (PublicDataApiException e) {
            log.warn("[FEED_COLLECT] {} failed: {}", name, e.getMessage());
            stats.failed++;
            stats.note(name + " 실패(" + e.getMessage() + ")");
        }
    }

    void collectTour(FeedHobbyMatcher matcher, LocalDate today, Stats stats) {
        int fetched = 0;
        for (int page = 1; page <= properties.tourMaxPages(); page++) {
            stats.calls++;
            TourApiClient.FestivalPage result = tourApiClient.searchFestivals(
                    properties.tourRegionCode(), today, page, properties.tourPageSize());
            for (TourApiClient.Festival festival : result.items()) {
                stats.processed++;
                if (festival.contentId() == null || festival.title() == null
                        || (festival.end() != null && festival.end().isBefore(today))) {
                    stats.rejected++;
                    continue;
                }
                Optional<Hobby> hobby = matcher.match(festival.title());
                if (hobby.isEmpty()) {
                    stats.rejected++;
                    continue;
                }
                if (feedWriter.refreshIfExists(FeedOrigin.TOUR_API, festival.contentId(), festival.title(),
                        festival.imageUrl(), festival.start(), festival.end())) {
                    stats.updated++;
                    continue;
                }
                TourApiClient.FestivalDetail detail = tourDetail(festival.contentId(), stats);
                String url = detail != null && detail.homepage() != null ? detail.homepage()
                        : TOUR_SEARCH_URL + URLEncoder.encode(festival.title(), StandardCharsets.UTF_8);
                save(new FeedCandidate(FeedOrigin.TOUR_API, festival.contentId(), FeedType.EVENT, festival.title(),
                        detail == null ? null : detail.overview(), TOUR_SOURCE, url, festival.imageUrl(),
                        festival.regionCode(), festival.start(), festival.end()), hobby.get(), stats);
            }
            fetched += result.items().size();
            if (result.items().isEmpty() || fetched >= result.totalCount()) break;
        }
    }

    /** 소개·홈페이지는 있으면 좋은 정보라, 실패해도 목록 정보만으로 저장한다 */
    private TourApiClient.FestivalDetail tourDetail(String contentId, Stats stats) {
        try {
            stats.calls++;
            return tourApiClient.detail(contentId).orElse(null);
        } catch (PublicDataApiException e) {
            log.debug("[FEED_COLLECT] tour detail {} skipped: {}", contentId, e.getMessage());
            return null;
        }
    }

    void collectKopis(List<HobbySourceMapping> mappings, FeedHobbyMatcher matcher, LocalDate today, Stats stats) {
        LocalDate to = today.plusDays(properties.lookaheadDays());
        Set<String> keywords = new LinkedHashSet<>();
        mappings.forEach(m -> keywords.add(m.getKeyword()));
        Set<String> seen = new HashSet<>();
        for (String keyword : keywords) {
            stats.calls++;
            for (KopisClient.Performance show : kopisClient.search(keyword, properties.kopisRegionCode(), today, to, 1,
                    properties.kopisRows())) {
                if (!seen.add(show.id())) continue; // 다른 검색어로 이미 본 공연
                stats.processed++;
                // 공연명 부분 검색이라 검색어가 다른 뜻으로 걸릴 수 있어, 저장 전에 같은 규칙으로 한 번 더 대조한다
                Optional<Hobby> hobby = matcher.match(show.title());
                if (show.title() == null || hobby.isEmpty()) {
                    stats.rejected++;
                    continue;
                }
                if (feedWriter.refreshIfExists(FeedOrigin.KOPIS, show.id(), show.title(), show.posterUrl(),
                        show.start(), show.end())) {
                    stats.updated++;
                    continue;
                }
                String summary = joinNonBlank(show.genre(), show.venue());
                save(new FeedCandidate(FeedOrigin.KOPIS, show.id(), FeedType.SHOW, show.title(), summary,
                        KOPIS_SOURCE, KopisClient.DETAIL_URL + show.id(), show.posterUrl(), null,
                        show.start(), show.end()), hobby.get(), stats);
            }
        }
    }

    private void save(FeedCandidate candidate, Hobby hobby, Stats stats) {
        FeedWriter.Result result = feedWriter.create(candidate, hobby, properties.titleSimilarity());
        if (result == FeedWriter.Result.CREATED) stats.accepted++;
        else stats.rejected++;
    }

    private static String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) if (value != null && !value.isBlank()) parts.add(value);
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    static final class Stats {
        int processed;
        int accepted;
        int updated;
        int rejected;
        int calls;
        int succeeded;
        int failed;
        String fatal;
        final List<String> notes = new ArrayList<>();

        void note(String note) {
            notes.add(note);
        }

        BatchRunStatus status() {
            if (fatal != null) return BatchRunStatus.FAILED;
            if (failed > 0) return succeeded == 0 ? BatchRunStatus.FAILED : BatchRunStatus.PARTIAL;
            return BatchRunStatus.SUCCESS;
        }

        String message() {
            List<String> parts = new ArrayList<>();
            if (fatal != null) parts.add(fatal);
            parts.add("갱신 " + updated + "건");
            parts.addAll(notes);
            return String.join(", ", parts);
        }
    }
}
