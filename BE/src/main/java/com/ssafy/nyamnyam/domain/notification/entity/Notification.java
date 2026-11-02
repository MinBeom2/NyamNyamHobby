package com.ssafy.nyamnyam.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 웹 알림 1건(notifications, V1). 생성은 중복 방지 키(dedup_key)가 있어 네이티브 INSERT로만 하고,
 * 엔티티는 조회와 읽음 처리에 쓴다. 시각은 UTC로 저장한다.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 200)
    private String body;

    @Column(name = "link_path", nullable = false, length = 200)
    private String linkPath;

    @Column(name = "dedup_key", nullable = false, length = 100)
    private String dedupKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isRead() {
        return readAt != null;
    }

    /** 이미 읽은 알림은 처음 읽은 시각을 유지한다 */
    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }
}
