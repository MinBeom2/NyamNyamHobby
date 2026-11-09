package com.ssafy.nyamnyam.domain.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매시 정각(한국 시간) 신고 알림. 웹훅 URL이 비어 있으면 아무것도 하지 않는다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDigestScheduler {

    private final ReportDigestService digestService;

    @Scheduled(cron = "${app.report.digest-cron:0 0 * * * *}", zone = "Asia/Seoul")
    public void run() {
        try {
            digestService.sendDigest();
        } catch (RuntimeException e) {
            log.error("Report digest failed", e);
        }
    }
}
