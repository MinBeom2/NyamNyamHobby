package com.ssafy.nyamnyam.domain.shortform.scheduler;

import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 숏폼 수집·주간 반영 스케줄. 실행 시점만 정하고 처리는 {@link ShortCollectService}가 한다.
 * app.short-collect.enabled=true일 때만 등록된다(로컬·테스트에서 할당량을 쓰지 않도록 기본은 꺼 둠).
 * 서버가 한 대라 분산 락은 두지 않았다. 여러 대로 늘리면 Redis 락 등으로 한 대에서만 돌게 해야 한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.short-collect", name = "enabled", havingValue = "true")
public class ShortScheduler {

    private final ShortCollectService shortCollectService;

    /** 6시간마다 7분(00·06·12·18시): 키워드 2개씩 돌아가며 수집(하루 약 800단위) */
    @Scheduled(cron = "${app.short-collect.cron:0 7 */6 * * *}", zone = "Asia/Seoul")
    public void collect() {
        shortCollectService.collect();
    }

    /** 매주 목 06:00: CANDIDATE → ACTIVE 주간 반영 */
    @Scheduled(cron = "${app.short-collect.release-cron:0 0 6 * * THU}", zone = "Asia/Seoul")
    public void release() {
        shortCollectService.release();
    }
}
