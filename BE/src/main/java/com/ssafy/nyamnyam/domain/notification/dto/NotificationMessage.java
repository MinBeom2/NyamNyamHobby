package com.ssafy.nyamnyam.domain.notification.dto;

import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;

/**
 * 알림 생성 요청. RabbitMQ notification.create 큐로 보내는 메시지이자 저장 단위다.
 * dedupKey는 (userId, dedupKey) UNIQUE라 같은 메시지가 두 번 와도 한 번만 저장된다.
 */
public record NotificationMessage(
        Long userId,
        NotificationType type,
        String title,
        String body,
        String linkPath,
        String dedupKey
) {
    public static final int TITLE_MAX = 100;
    public static final int BODY_MAX = 200;

    /** 컬럼 길이를 넘는 제목·본문은 말줄임표로 자른다 */
    public NotificationMessage {
        title = cut(title, TITLE_MAX);
        body = cut(body, BODY_MAX);
    }

    private static String cut(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
