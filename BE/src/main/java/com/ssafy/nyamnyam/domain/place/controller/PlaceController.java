package com.ssafy.nyamnyam.domain.place.controller;

import com.ssafy.nyamnyam.domain.place.dto.response.PlaceResponse;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.service.PlaceService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /**
     * 활동 장소 검색 (목록 + 지도 좌표) [공개]
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> searchPlaces(
            @RequestParam(value = "regionId", required = false) Long regionId,
            @RequestParam(value = "hobbyId", required = false) Long hobbyId,
            @RequestParam(value = "category", required = false) PlaceCategory category
    ) {
        List<PlaceResponse> response = placeService.searchPlaces(regionId, hobbyId, category);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

