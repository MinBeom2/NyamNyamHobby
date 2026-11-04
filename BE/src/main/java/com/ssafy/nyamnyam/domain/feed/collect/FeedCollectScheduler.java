package com.ssafy.nyamnyam.domain.feed.collect;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정보 피드 수집 스케줄. app.feed-collect.enabled=true일 때만 등록된다(로컬·테스트에서 외부 API를 부르지 않도록).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.feed-collect", name = "enabled", havingValue = "true")
public class FeedCollectScheduler {

    private final FeedCollectService feedCollectService;
    private final FeedClassCollectService feedClassCollectService;

    /** 매일 05:00 축제·행사·공연·전시(FR-FEED-02) */
    @Scheduled(cron = "${app.feed-collect.cron:0 0 5 * * *}", zone = "Asia/Seoul")
    public void collect() {
        feedCollectService.collect();
    }

    /** 매일 05:30 공공 강좌·클래스(FR-FEED-02). 05:00 수집과 겹치지 않게 30분 뒤 */
    @Scheduled(cron = "${app.feed-collect.class-cron:0 30 5 * * *}", zone = "Asia/Seoul")
    public void collectClasses() {
        feedClassCollectService.collect();
    }
}
