package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.report.service.ReportCreatedEvent;
import com.ssafy.nyamnyam.domain.report.service.ReportDigestService;
import com.ssafy.nyamnyam.domain.report.service.ReportImmediateNotifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportImmediateNotifierTest {

    @Mock private ReportDigestService digestService;

    private ReportImmediateNotifier notifier(boolean immediately) {
        return new ReportImmediateNotifier(
                new ReportProperties(3, 30, immediately, "https://mm/hooks/x", "s", "https://a", "https://b"), digestService);
    }

    @Test
    @DisplayName("즉시 알림을 켜면 신고가 커밋된 뒤 바로 보낸다")
    void sendsWhenEnabled() {
        notifier(true).onReportCreated(new ReportCreatedEvent(1L));

        verify(digestService).sendDigest();
    }

    @Test
    @DisplayName("기본(꺼짐)은 매시 정각 알림만 쓴다")
    void skipsWhenDisabled() {
        notifier(false).onReportCreated(new ReportCreatedEvent(1L));

        verify(digestService, never()).sendDigest();
    }

    @Test
    @DisplayName("전송 중 오류가 나도 신고 요청에는 영향을 주지 않는다")
    void swallowsFailure() {
        given(digestService.sendDigest()).willThrow(new IllegalStateException("mattermost down"));

        assertThatCode(() -> notifier(true).onReportCreated(new ReportCreatedEvent(1L))).doesNotThrowAnyException();
    }
}
