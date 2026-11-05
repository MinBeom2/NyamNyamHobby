package com.ssafy.nyamnyam.domain.feed.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 정보 피드 GMS 취미 분류 설정(app.feed-classify.*). 실행 시각(cron)은 스케줄러가 직접 읽고,
 * 켜고 끄는 것은 수집과 같은 app.feed-collect.enabled를 따른다.
 *
 * @param dailyLimit  한 번 실행(하루 한 번)에 분류할 최대 건수. GMS 토큰 예산을 지키려고 둔다(남은 건 다음 날)
 * @param chunkSize   GMS 한 번에 묶어 보낼 건수
 * @param model       분류에 쓸 모델. 30종 중 하나를 고르는 단순한 판단이라 싼 모델로 충분하다
 * @param maxAttempts GMS가 답을 못 준 횟수가 이만큼 쌓이면 후보 취미(없으면 '그 외')로 확정한다
 */
@ConfigurationProperties(prefix = "app.feed-classify")
public record FeedClassifyProperties(
        @DefaultValue("60") int dailyLimit,
        @DefaultValue("20") int chunkSize,
        @DefaultValue("gpt-4o-mini") String model,
        @DefaultValue("3") int maxAttempts
) {
}
