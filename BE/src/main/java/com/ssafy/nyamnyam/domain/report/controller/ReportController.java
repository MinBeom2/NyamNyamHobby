package com.ssafy.nyamnyam.domain.report.controller;

import com.ssafy.nyamnyam.domain.report.dto.ReportCreateRequest;
import com.ssafy.nyamnyam.domain.report.dto.ReportCreateResponse;
import com.ssafy.nyamnyam.domain.report.service.ReportService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시글·댓글 신고(로그인 필요) */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportCreateResponse>> create(
            @Valid @RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        ReportCreateResponse response = reportService.create(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
