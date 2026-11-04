package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import com.ssafy.nyamnyam.global.infra.publicdata.SeoulReservationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FEED-B03 공공 강좌·클래스 수집(매일 05:30). 서울시 교육 공공서비스예약 강좌를 받아 제목을 취미 행사 검색어와
 * 대조하고, 걸린 강좌만 CLASS 피드로 저장한다(걸리지 않은 강좌의 GMS 분류는 FEED-B04).
 * 요약에는 '접수중 · 무료 · 장소'처럼 매일 바뀌는 접수 상태를 넣고, 다시 받을 때 요약도 새로 고친다.
 * 운영 기간이 끝난 강좌는 저장하지 않는다(이미 저장된 강좌는 종료 날짜로 목록에서 숨겨진다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedClassCollectService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final String SOURCE = "서울 공공서비스예약";

    private final SeoulReservationClient seoulReservationClient;
    private final HobbySourceMappingRepository mappingRepository;
    private final RegionRepository regionRepository;
    private final FeedWriter feedWriter;
    private final BatchRunRepository batchRunRepository;
    private final FeedCollectProperties properties;

    private final AtomicBoolean collecting = new AtomicBoolean(false);

    /** 수집 1회. 이미 실행 중이면 건너뛴다 */
    public BatchRun collect() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[CLASS_COLLECT] previous run still in progress, skipped");
            return null;
        }
        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.CLASS_COLLECT));
        FeedCollectService.Stats stats = new FeedCollectService.Stats();
        try {
            FeedHobbyMatcher matcher = new FeedHobbyMatcher(
                    mappingRepository.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED));
            if (matcher.isEmpty()) {
                stats.fatal = "행사 검색어(EVENT_KEYWORD)가 없습니다";
            } else if (!seoulReservationClient.isConfigured()) {
                stats.fatal = "서울 열린데이터광장 키 없음";
            } else {
                collectCourses(matcher, LocalDate.now(KST), stats);
                stats.succeeded++;
            }
        } catch (PublicDataApiException e) {
            log.warn("[CLASS_COLLECT] failed: {}", e.getMessage());
            stats.fatal = e.getMessage();
        } catch (RuntimeException e) {
            log.error("[CLASS_COLLECT] unexpected failure", e);
            stats.fatal = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            run.finish(stats.status(), stats.processed, stats.accepted, stats.rejected, stats.calls, stats.message());
            batchRunRepository.save(run);
            collecting.set(false);
            log.info("[CLASS_COLLECT] {} processed={} accepted={} rejected={} calls={} {}",
                    run.getStatus(), stats.processed, stats.accepted, stats.rejected, stats.calls, stats.message());
        }
        return run;
    }

    void collectCourses(FeedHobbyMatcher matcher, LocalDate today, FeedCollectService.Stats stats) {
        Map<String, String> regionCodes = new HashMap<>();
        regionRepository.findBySidoOrderBySigunguAsc(properties.regionSido())
                .forEach(region -> regionCodes.put(region.getSigungu(), region.getCode()));
        int size = Math.min(properties.classPageSize(), SeoulReservationClient.MAX_ROWS);
        for (int page = 0; page < properties.classMaxPages(); page++) {
            int start = page * size + 1;
            stats.calls++;
            SeoulReservationClient.CoursePage result = seoulReservationClient.list(start, start + size - 1);
            for (SeoulReservationClient.Course course : result.items()) {
                stats.processed++;
                if (course.id() == null || course.title() == null || course.url() == null
                        || (course.end() != null && course.end().isBefore(today))) {
                    stats.rejected++;
                    continue;
                }
                Optional<Hobby> hobby = matcher.match(course.title());
                if (hobby.isEmpty()) {
                    stats.rejected++;
                    continue;
                }
                String summary = FeedCollectService.joinNonBlank(course.status(), course.fee(), course.place());
                if (feedWriter.refreshIfExists(FeedOrigin.SEOUL_RESERVATION, course.id(), course.title(),
                        course.imageUrl(), course.start(), course.end(), summary)) {
                    stats.updated++;
                    continue;
                }
                FeedWriter.Result saved = feedWriter.create(new FeedCandidate(FeedOrigin.SEOUL_RESERVATION, course.id(),
                        FeedType.CLASS, course.title(), summary, SOURCE, course.url(), course.imageUrl(),
                        course.district() == null ? null : regionCodes.get(course.district()),
                        course.start(), course.end()), hobby.get(), properties.titleSimilarity());
                if (saved == FeedWriter.Result.CREATED) stats.accepted++;
                else stats.rejected++;
            }
            if (result.items().size() < size || start + size - 1 >= result.totalCount()) break;
        }
    }
}
