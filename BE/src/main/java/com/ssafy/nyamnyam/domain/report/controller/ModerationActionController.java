package com.ssafy.nyamnyam.domain.report.controller;

import com.ssafy.nyamnyam.domain.report.dto.MattermostActionRequest;
import com.ssafy.nyamnyam.domain.report.dto.MattermostActionResponse;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationMessageFactory;
import com.ssafy.nyamnyam.domain.report.service.ModerationActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mattermost 신고 메시지의 [숨기기]·[유지] 버튼 콜백. 관리자 화면 대신 쓴다.
 * 로그인 대신 버튼 context의 HMAC 서명으로 확인하고, 응답은 Mattermost 형식 그대로 보낸다(ApiResponse로 감싸지 않음).
 */
@RestController
@RequiredArgsConstructor
public class ModerationActionController {

    private final ModerationActionService moderationActionService;

    @PostMapping(ModerationMessageFactory.ACTION_PATH)
    public ResponseEntity<MattermostActionResponse> handle(@RequestBody MattermostActionRequest request) {
        return ResponseEntity.ok(moderationActionService.handle(request));
    }
}
