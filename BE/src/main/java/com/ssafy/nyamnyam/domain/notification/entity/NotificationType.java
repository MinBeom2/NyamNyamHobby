package com.ssafy.nyamnyam.domain.notification.entity;

/**
 * 알림 종류. notifications.type 컬럼(VARCHAR 20) 값과 같다.
 * V1 주석의 나머지 값(INQUIRY_ANSWERED, REPORT_DONE, NOTICE)은 해당 기능을 만들 때 추가한다.
 */
public enum NotificationType {
    /** 내 글에 댓글 */
    COMMENT,
    /** 내 댓글에 답글 */
    REPLY,
    /** 진행 중 계획의 주차가 바뀜 */
    WEEKLY_TODO,
    /** 내 취미·관심 취미의 새 정보 피드(하루 1건 묶음) */
    NEW_FEED
}
