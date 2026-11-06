package com.ssafy.nyamnyam.domain.place.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 장소 자동 수집 설정(app.place-collect.*)
 *
 * @param enabled        스케줄러 활성화 여부 (.env에서 PLACE_COLLECT_ENABLED=true 설정)
 * @param maxPages       검색어당 최대 수집 페이지 수 (1~3, 페이지당 15건)
 * @param intervalMillis API 호출 사이 딜레이(ms)
 * @param targetSido     수집 대상 시도 (기본 "서울특별시")
 */
@ConfigurationProperties(prefix = "app.place-collect")
public record PlaceCollectProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("2") int maxPages,
        @DefaultValue("200") long intervalMillis,
        @DefaultValue("서울특별시") String targetSido
) {
}

