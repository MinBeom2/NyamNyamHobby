package com.ssafy.nyamnyam.global.batchrun;

/** batch_runs.job 값. 정해진 시각에 도는 스케줄 작업의 종류(Spring Batch Job이 아님). */
public enum BatchJob {
    SHORT_COLLECT,
    SHORT_CHECK,
    SHORT_RELEASE,
    CHANNEL_DISCOVER,
    PLACE_PUBLIC,
    PLACE_KAKAO,
    FEED_COLLECT,
    CLASS_COLLECT
}
