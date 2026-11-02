package com.ssafy.nyamnyam.domain.place.controller;

import com.ssafy.nyamnyam.domain.place.service.RegionService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    /**
     * 행정구역 지역 목록 조회 (공개)
     * - sido 생략 시 시·도 목록 반환 (MVP: 서울특별시 활성, 타 지역 enabled: false)
     * - sido 전달 시 해당 시·도의 시·군·구 목록 반환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getRegions(
            @RequestParam(value = "sido", required = false) String sido
    ) {
        if (sido == null || sido.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(regionService.getSidoList()));
        }
        return ResponseEntity.ok(ApiResponse.success(regionService.getSigunguList(sido.trim())));
    }
}

