package com.ssafy.nyamnyam.global.batchrun;

public enum BatchRunStatus {
    RUNNING,
    SUCCESS,
    /** 일부 대상만 처리(할당량 초과·일부 실패). 남은 대상은 다음 실행에서 이어서 한다 */
    PARTIAL,
    FAILED
}
