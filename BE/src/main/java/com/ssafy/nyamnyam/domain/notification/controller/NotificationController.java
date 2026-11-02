package com.ssafy.nyamnyam.domain.notification.controller;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationResponse;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 웹 알림 API(명세 3.9-1·2). 모두 회원 전용이고 본인 알림만 다룬다.
 * 프론트는 화면 포커스 시와 60초마다 unread-count를 부른다(FR-NOTI-01).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    public record UnreadCountResponse(long unreadCount) {
    }

    public record ReadAllResponse(int updatedCount) {
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(notificationService.list(requireLogin(principal), unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(new UnreadCountResponse(notificationService.unreadCount(requireLogin(principal))));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> read(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(notificationService.read(requireLogin(principal), id));
    }

    @PatchMapping("/read-all")
    public ApiResponse<ReadAllResponse> readAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(new ReadAllResponse(notificationService.readAll(requireLogin(principal))));
    }

    private static Long requireLogin(UserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal.getUserId();
    }
}
