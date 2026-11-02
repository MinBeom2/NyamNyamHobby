package com.ssafy.nyamnyam.domain.notification.dto;

import com.ssafy.nyamnyam.domain.notification.entity.Notification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 알림 목록 항목(API 명세 3.9-1). 시각은 UTC 저장값을 +09:00으로 내려 준다 */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String linkPath,
        boolean isRead,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(), n.getBody(), n.getLinkPath(),
                n.isRead(), kst(n.getReadAt()), kst(n.getCreatedAt()));
    }

    private static OffsetDateTime kst(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).withOffsetSameInstant(KST);
    }
}
