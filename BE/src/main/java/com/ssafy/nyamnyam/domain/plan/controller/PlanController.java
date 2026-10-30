package com.ssafy.nyamnyam.domain.plan.controller;

import com.ssafy.nyamnyam.domain.plan.dto.*;
import com.ssafy.nyamnyam.domain.plan.dto.PlanResponses.*;
import com.ssafy.nyamnyam.domain.plan.service.*;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PlanController {
    private final PlanService service;
    private final PlanQueryService query;

    @GetMapping("/users/me/todos/summary")
    public ApiResponse<Summary> summary(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.success(query.summary(user.getUserId()));
    }

    @PostMapping("/users/me/hobbies/{hobbyId}/start")
    public ResponseEntity<ApiResponse<Started>> start(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long hobbyId, @Valid @RequestBody StartPlanRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(service.start(user.getUserId(), hobbyId, request)));
    }

    @PatchMapping("/users/me/hobbies/{hobbyId}/status")
    public ApiResponse<HobbyState> status(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long hobbyId, @Valid @RequestBody HobbyStatusRequest request) {
        return ApiResponse.success(service.changeStatus(user.getUserId(), hobbyId, request));
    }

    @GetMapping("/plans/{planId}")
    public ApiResponse<View> week(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long planId, @RequestParam(required = false) Integer week) {
        return ApiResponse.success(query.get(user.getUserId(), planId, week, false));
    }

    @GetMapping("/plans/{planId}/overview")
    public ApiResponse<View> overview(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long planId) {
        return ApiResponse.success(query.get(user.getUserId(), planId, null, true));
    }

    @PatchMapping("/plans/{planId}/items/{itemId}")
    public ApiResponse<Mutation> update(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long planId,
            @PathVariable Long itemId, @Valid @RequestBody UpdatePlanItemRequest request) {
        return ApiResponse.success(service.update(user.getUserId(), planId, itemId, request));
    }

    @PostMapping("/plans/{planId}/items")
    public ResponseEntity<ApiResponse<Mutation>> create(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long planId, @Valid @RequestBody CreatePlanItemRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(service.create(user.getUserId(), planId, request)));
    }

    @DeleteMapping("/plans/{planId}/items/{itemId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long planId, @PathVariable Long itemId) {
        service.delete(user.getUserId(), planId, itemId);
        return ResponseEntity.noContent().build();
    }
}
