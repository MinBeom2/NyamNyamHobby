package com.ssafy.nyamnyam.domain.feed.collect;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 정보 피드 수집 설정(app.feed-collect.*). 실행 시각(cron)은 스케줄러가 직접 읽는다.
 * MVP는 서울 [기본안]만 받는다. 지역을 넓히려면 코드 값만 바꾸면 된다.
 *
 * @param enabled          스케줄러 사용 여부. 로컬·테스트는 false, 서버 .env에서만 켠다
 * @param tourRegionCode   TourAPI 법정동 시도 코드(서울 11)
 * @param kopisRegionCode  KOPIS 시도 코드(서울 11)
 * @param lookaheadDays    오늘부터 며칠 안에 열리는 공연·전시까지 받을지(KOPIS·문화포털 검색 기간)
 * @param tourPageSize     TourAPI 한 번에 받을 행사 수
 * @param tourMaxPages     TourAPI 최대 페이지 수(서울 진행·예정 행사는 보통 수백 건 이하)
 * @param kopisRows        KOPIS 검색어 하나당 받을 공연 수
 * @param titleSimilarity  출처가 달라도 시작일이 같고 제목이 이만큼 비슷하면 같은 행사로 보고 먼저 들어온 쪽만 남긴다
 * @param cultureSido      문화포털 지역 이름(서울)
 * @param regionSido       regions.sido 이름. 문화포털의 시군구 이름을 지역 코드로 바꿀 때 쓴다
 * @param culturePageSize  문화포털 한 번에 받을 항목 수
 * @param cultureMaxPages  문화포털 최대 페이지 수(서울 기간 내 항목은 보통 수백 건)
 */
@ConfigurationProperties(prefix = "app.feed-collect")
public record FeedCollectProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("11") String tourRegionCode,
        @DefaultValue("11") String kopisRegionCode,
        @DefaultValue("60") int lookaheadDays,
        @DefaultValue("100") int tourPageSize,
        @DefaultValue("5") int tourMaxPages,
        @DefaultValue("50") int kopisRows,
        @DefaultValue("0.8") double titleSimilarity,
        @DefaultValue("서울") String cultureSido,
        @DefaultValue("서울특별시") String regionSido,
        @DefaultValue("100") int culturePageSize,
        @DefaultValue("10") int cultureMaxPages
) {
}
