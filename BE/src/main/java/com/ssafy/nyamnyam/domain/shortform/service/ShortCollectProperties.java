package com.ssafy.nyamnyam.domain.shortform.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 숏폼 수집 설정(app.short-collect.*). 실행 주기(cron)는 스케줄러가 직접 읽는다.
 *
 * @param enabled             수집·주간 반영 스케줄러 사용 여부. 로컬·테스트는 false, 서버 .env에서만 켠다
 * @param keywordsPerRun      한 번 실행에 검색할 키워드 수(키워드당 101단위)
 * @param targetPerHobby      취미별 목표 영상 수(CANDIDATE+ACTIVE). 채우면 그 취미는 건너뛴다
 * @param maxPerChannel       한 취미에서 같은 채널 영상 최대 수(다양성 확보)
 * @param minViewCount        조회수 하한. 미달 영상은 저장하지 않아 나중에 다시 볼 수 있다
 * @param maxDurationSeconds  쇼츠로 인정할 최대 길이(초)
 * @param dailyQuotaLimit     하루 YouTube 할당량 중 이 작업이 쓸 상한(기본 한도 10,000)
 * @param confidenceThreshold GPT 판별 신뢰도 기준. 미만이면 NEEDS_REVIEW
 * @param classifyChunkSize   GPT 한 번에 보낼 영상 수
 */
@ConfigurationProperties(prefix = "app.short-collect")
public record ShortCollectProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("2") int keywordsPerRun,
        @DefaultValue("40") int targetPerHobby,
        @DefaultValue("3") int maxPerChannel,
        @DefaultValue("1000") long minViewCount,
        @DefaultValue("180") int maxDurationSeconds,
        @DefaultValue("9000") int dailyQuotaLimit,
        @DefaultValue("0.7") double confidenceThreshold,
        @DefaultValue("20") int classifyChunkSize
) {
}
