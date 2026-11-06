package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoLocalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 장소 자동 수집 서비스(PLACE-B01).
 * 서울 25개 구 x 취미별 카카오 키워드를 결합(예: "마포구 클라이밍")하여 카카오 로컬 검색 API를 호출하고
 * places 테이블에 저장(UPSERT)한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCollectService {

    private final KakaoLocalClient kakaoLocalClient;
    private final RegionRepository regionRepository;
    private final HobbyRepository hobbyRepository;
    private final HobbySourceMappingRepository mappingRepository;
    private final PlaceWriter placeWriter;
    private final BatchRunRepository batchRunRepository;
    private final PlaceCollectProperties properties;

    private final AtomicBoolean collecting = new AtomicBoolean(false);
    private final Set<String> onDemandCollectedCache = ConcurrentHashMap.newKeySet();

    /**
     * 사용자가 장소 찾기 화면에서 특정 지역과 취미를 선택했을 때 온디맨드로 카카오 장소를 실시간 수집한다.
     * 한 번 수집된 (regionId, hobbyId) 조합은 캐시되어 중복 호출을 방지한다.
     */
    public void collectOnDemand(Long regionId, Long hobbyId) {
        if (!kakaoLocalClient.isConfigured() || regionId == null || hobbyId == null) {
            return;
        }

        String cacheKey = regionId + "-" + hobbyId;
        if (onDemandCollectedCache.contains(cacheKey)) {
            return;
        }

        Optional<Region> regionOpt = regionRepository.findById(regionId);
        Optional<Hobby> hobbyOpt = hobbyRepository.findById(hobbyId);
        if (regionOpt.isEmpty() || hobbyOpt.isEmpty()) {
            return;
        }

        Region region = regionOpt.get();
        Hobby hobby = hobbyOpt.get();

        List<HobbySourceMapping> mappings = mappingRepository.findByHobbyIdAndTarget(
                hobby.getId(), SourceTarget.KAKAO_KEYWORD);
        List<String> keywords = mappings.stream()
                .map(HobbySourceMapping::getKeyword)
                .filter(k -> !k.isBlank())
                .distinct()
                .toList();

        if (keywords.isEmpty()) {
            keywords = List.of(hobby.getName());
        }

        log.info("[PLACE_ON_DEMAND] Fetching Kakao places for region='{}', hobby='{}', keywords={}",
                region.getSigungu(), hobby.getName(), keywords);

        for (String keyword : keywords) {
            String query = region.getSigungu() + " " + keyword;

            for (int page = 1; page <= properties.maxPages(); page++) {
                try {
                    KakaoKeywordSearchResponse response = kakaoLocalClient.searchByKeyword(query, page, 15);
                    if (response == null || response.documents() == null || response.documents().isEmpty()) {
                        break;
                    }

                    for (KakaoKeywordSearchResponse.KakaoPlaceDocument doc : response.documents()) {
                        placeWriter.write(doc, hobby);
                    }

                    if (response.meta() != null && response.meta().isEnd()) {
                        break;
                    }
                } catch (Exception e) {
                    log.warn("[PLACE_ON_DEMAND] Query failed: query='{}', page={}: {}", query, page, e.getMessage());
                    break;
                }
            }
        }

        onDemandCollectedCache.add(cacheKey);
    }

    /**
     * 전체 장소 자동 수집 1회 실행 (스케줄러 또는 수동 호출)
     */
    public BatchRun collect() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[PLACE_COLLECT] Previous run is still in progress, skipped");
            return null;
        }

        BatchRun run = batchRunRepository.save(BatchRun.start(BatchJob.PLACE_KAKAO));
        int processed = 0;
        int accepted = 0;
        int rejected = 0;
        int calls = 0;
        int queryFailures = 0;
        String errorMessage = null;

        try {
            if (!kakaoLocalClient.isConfigured()) {
                errorMessage = "카카오 로컬 REST API 키가 설정되지 않았습니다 (app.public-data.kakao-local.key)";
                log.info("[PLACE_COLLECT] {}", errorMessage);
                return run;
            }

            List<Region> regions = regionRepository.findBySidoOrderBySigunguAsc(properties.targetSido());
            List<HobbySourceMapping> mappings = mappingRepository.findAllWithHobby(
                    SourceTarget.KAKAO_KEYWORD, HobbyStatus.PUBLISHED);

            if (regions.isEmpty() || mappings.isEmpty()) {
                errorMessage = String.format("수집 대상 지역(%d) 또는 검색어 매핑(%d)이 없습니다", regions.size(), mappings.size());
                log.warn("[PLACE_COLLECT] {}", errorMessage);
                return run;
            }

            log.info("[PLACE_COLLECT] Starting collection for {} regions x {} keywords", regions.size(), mappings.size());

            for (Region region : regions) {
                for (HobbySourceMapping mapping : mappings) {
                    String query = region.getSigungu() + " " + mapping.getKeyword();

                    for (int page = 1; page <= properties.maxPages(); page++) {
                        try {
                            KakaoKeywordSearchResponse response = kakaoLocalClient.searchByKeyword(query, page, 15);
                            calls++;

                            if (response == null || response.documents() == null || response.documents().isEmpty()) {
                                break;
                            }

                            for (KakaoKeywordSearchResponse.KakaoPlaceDocument doc : response.documents()) {
                                processed++;
                                PlaceWriter.WriteResult result = placeWriter.write(doc, mapping.getHobby());
                                if (result == PlaceWriter.WriteResult.CREATED || result == PlaceWriter.WriteResult.UPDATED) {
                                    accepted++;
                                } else {
                                    rejected++;
                                }
                            }

                            if (response.meta() != null && response.meta().isEnd()) {
                                break;
                            }

                            sleepInterval();
                        } catch (Exception e) {
                            queryFailures++;
                            log.warn("[PLACE_COLLECT] Query failed: query='{}', page={}: {}", query, page, e.getMessage());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[PLACE_COLLECT] Unexpected collection failure", e);
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            BatchRunStatus status = determineStatus(errorMessage, queryFailures, calls);
            String message = buildMessage(errorMessage, queryFailures, calls);

            run.finish(status, processed, accepted, rejected, calls, message);
            batchRunRepository.save(run);
            collecting.set(false);

            log.info("[PLACE_COLLECT] Completed: status={}, processed={}, accepted={}, rejected={}, calls={}, queryFailures={}",
                    status, processed, accepted, rejected, calls, queryFailures);
        }

        return run;
    }

    private void sleepInterval() {
        if (properties.intervalMillis() <= 0) return;
        try {
            Thread.sleep(properties.intervalMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private BatchRunStatus determineStatus(String errorMessage, int queryFailures, int calls) {
        if (errorMessage != null && calls == 0) {
            return BatchRunStatus.FAILED;
        }
        if (queryFailures > 0 && queryFailures >= calls) {
            return BatchRunStatus.FAILED;
        }
        return BatchRunStatus.SUCCESS;
    }

    private String buildMessage(String errorMessage, int queryFailures, int calls) {
        if (errorMessage != null) {
            return errorMessage;
        }
        if (queryFailures > 0) {
            return String.format("총 %d회 호출 중 %d회 실패", calls, queryFailures);
        }
        return "정상 완료";
    }
}
