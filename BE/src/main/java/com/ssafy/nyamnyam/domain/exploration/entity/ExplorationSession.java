package com.ssafy.nyamnyam.domain.exploration.entity;

import com.ssafy.nyamnyam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "exploration_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplorationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "guest_key", length = 36, columnDefinition = "char(36)")
    private String guestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SessionStatus status;

    @Column(name = "max_count", nullable = false, columnDefinition = "smallint")
    private java.lang.Short maxCount;

    @Column(name = "viewed_count", nullable = false, columnDefinition = "smallint")
    private java.lang.Short viewedCount;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder
    public ExplorationSession(User user, String guestKey, java.lang.Short maxCount) {
        this.user = user;
        this.guestKey = guestKey;
        this.status = SessionStatus.IN_PROGRESS;
        this.maxCount = maxCount != null ? maxCount : (short) 12;
        this.viewedCount = 0;
        this.startedAt = LocalDateTime.now();
    }

    public void incrementViewedCount() {
        this.viewedCount = (short) (this.viewedCount + 1);
    }

    public void complete() {
        if (this.status != SessionStatus.IN_PROGRESS) return;
        this.status = SessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
    }

    public void abandon() {
        this.status = SessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
    }
}
