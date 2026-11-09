package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.report.ReportProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * app.report.notify-immediately=true면 신고가 커밋되자마자 알림을 보낸다(로컬 확인용).
 * 커밋 뒤에 보내므로 롤백된 신고는 알리지 않고, 전송이 실패해도 신고 응답에는 영향이 없다.
 * 아직 안 보낸 신고를 모두 보내는 정기 알림과 같은 로직을 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportImmediateNotifier {

    private final ReportProperties properties;
    private final ReportDigestService digestService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(ReportCreatedEvent event) {
        if (!properties.notifyImmediately()) {
            return;
        }
        try {
            digestService.sendDigest();
        } catch (RuntimeException e) {
            log.warn("Immediate report notify failed: reportId={}, error={}", event.reportId(), e.getClass().getSimpleName());
        }
    }
}
