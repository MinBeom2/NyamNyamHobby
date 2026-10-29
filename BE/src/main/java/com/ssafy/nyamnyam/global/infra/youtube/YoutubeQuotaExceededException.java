package com.ssafy.nyamnyam.global.infra.youtube;

/**
 * 하루 할당량 초과(403 quotaExceeded · dailyLimitExceeded).
 * 할당량은 미국 태평양 시간 자정(한국 16~17시)에 초기화되므로, 받으면 그 회차 수집을 멈춘다.
 */
public class YoutubeQuotaExceededException extends YoutubeApiException {

    public YoutubeQuotaExceededException(String reason) {
        super("YouTube 할당량 초과: " + reason);
    }
}
