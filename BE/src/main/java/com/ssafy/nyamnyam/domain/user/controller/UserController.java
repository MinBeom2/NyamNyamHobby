package com.ssafy.nyamnyam.domain.user.controller;

import com.ssafy.nyamnyam.domain.user.dto.request.ChangePasswordRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateConditionsRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateProfileRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.WithdrawRequest;
import com.ssafy.nyamnyam.domain.user.dto.response.UserConditionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserProfileResponse;
import com.ssafy.nyamnyam.domain.user.service.UserService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1) 내 프로필 및 환경 정보 조회 (200 OK)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserProfileResponse response = userService.getUserProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 2) 프로필 기본 정보 수정 (200 OK)
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse response = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3) 이용 상황 및 선호 지역 수정 (200 OK)
     */
    @PutMapping("/conditions")
    public ResponseEntity<ApiResponse<UserConditionsResponse>> updateConditions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateConditionsRequest request
    ) {
        UserConditionsResponse response = userService.updateConditions(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 4) 비밀번호 변경 (200 OK)
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "비밀번호가 성공적으로 변경되었습니다.")));
    }

    /**
     * 5) 회원 탈퇴 (204 No Content)
     */
    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) WithdrawRequest request
    ) {
        userService.withdraw(principal.getUserId(), request != null ? request : new WithdrawRequest(null));
        return ResponseEntity.noContent().build();
    }

    /**
     * 6) 내 취미 목록 조회 (200 OK)
     */
    @GetMapping("/hobbies")
    public ResponseEntity<ApiResponse<List<UserHobbyResponse>>> getUserHobbies(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "status", required = false) String status
    ) {
        List<UserHobbyResponse> response = userService.getUserHobbies(principal.getUserId(), status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 7) 관심 취미 저장 (200 OK)
     */
    @PutMapping("/hobbies/{hobbyId}/save")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> saveHobby(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("hobbyId") Long hobbyId
    ) {
        userService.saveHobby(principal.getUserId(), hobbyId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("saved", true)));
    }

    /**
     * 8) 관심 취미 해제 (200 OK)
     */
    @DeleteMapping("/hobbies/{hobbyId}/save")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> unsaveHobby(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("hobbyId") Long hobbyId
    ) {
        userService.unsaveHobby(principal.getUserId(), hobbyId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("saved", false)));
    }
}

