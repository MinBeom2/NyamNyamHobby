package com.ssafy.nyamnyam.domain.place.collect;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 장소 자동 수집 스케줄러(PLACE-B03).
 * app.place-collect.enabled=true일 때만 스케줄이 활성화된다 (로컬/테스트에서 외부 API 호출 방지).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.place-collect", name = "enabled", havingValue = "true")
public class PlaceCollectScheduler {

    private final PlaceCollectService placeCollectService;

    /** 매주 일요일 03:00 (Asia/Seoul) */
    @Scheduled(cron = "${app.place-collect.cron:0 0 3 * * SUN}", zone = "Asia/Seoul")
    public void collect() {
        placeCollectService.collect();
    }
}

