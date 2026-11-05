package com.ssafy.nyamnyam.domain.place.controller;

import com.ssafy.nyamnyam.domain.place.collect.PlaceCollectService;
import com.ssafy.nyamnyam.domain.place.dto.response.PlaceResponse;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.service.PlaceService;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceCollectService placeCollectService;

    /**
     * 활동 장소 검색 (목록 + 지도 좌표) [공개]
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> searchPlaces(
            @RequestParam(value = "regionId", required = false) Long regionId,
            @RequestParam(value = "hobbyId", required = false) Long hobbyId,
            @RequestParam(value = "category", required = false) PlaceCategory category) {
        List<PlaceResponse> response = placeService.searchPlaces(regionId, hobbyId, category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 장소 자동 수집 수동 실행 트리거 [공개/관리자]
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<String>> triggerCollect() {
        BatchRun run = placeCollectService.collect();
        String message = run != null
                ? "장소 수집이 완료되었습니다 (" + run.getStatus() + ", 저장/갱신: " + run.getAccepted() + "건)"
                : "장소 수집이 이미 진행 중입니다.";
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
