package com.ssafy.nyamnyam.global.config;

import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectProperties;
import com.ssafy.nyamnyam.domain.feed.collect.FeedClassifyProperties;
import com.ssafy.nyamnyam.domain.feed.collect.FeedCollectProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 작업 활성화. 기본 스케줄러는 스레드 1개라 수집과 주간 반영이 동시에 돌지 않는다.
 * 각 작업의 등록 여부는 작업별 enabled 설정(@ConditionalOnProperty)으로 정한다.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({ShortCollectProperties.class, FeedCollectProperties.class, FeedClassifyProperties.class})
public class SchedulingConfig {
}
