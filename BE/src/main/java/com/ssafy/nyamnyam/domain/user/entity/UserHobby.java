package com.ssafy.nyamnyam.domain.user.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "user_hobbies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_hobby", columnNames = {"user_id", "hobby_id"})
        },
        indexes = {
                @Index(name = "idx_user_hobby_status", columnList = "user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private UserHobbyStatus status;

    @Column(name = "skill_level", nullable = false, length = 12)
    private String skillLevel;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "paused_days", nullable = false)
    private int pausedDays;

    @Column(name = "paused_seconds", nullable = false)
    private long pausedSeconds;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder
    public UserHobby(User user, Hobby hobby, UserHobbyStatus status, String skillLevel, LocalDateTime savedAt) {
        this.user = user;
        this.hobby = hobby;
        this.status = status != null ? status : UserHobbyStatus.SAVED;
        this.skillLevel = skillLevel != null ? skillLevel : "BEGINNER";
        this.savedAt = savedAt != null ? savedAt : LocalDateTime.now();
        this.pausedDays = 0;
    }

    public void updateStatus(UserHobbyStatus status) {
        this.status = status;
    }

    public void start(String skillLevel, LocalDateTime now) {
        this.status = UserHobbyStatus.IN_PROGRESS;
        if (skillLevel != null && !skillLevel.isBlank()) {
            this.skillLevel = skillLevel;
        }
        this.startedAt = now;
        this.pausedAt = null;
        this.pausedDays = 0;
        this.pausedSeconds = 0;
        this.endedAt = null;
    }

    public void pause(LocalDateTime now) {
        if (status == UserHobbyStatus.PAUSED) return;
        status = UserHobbyStatus.PAUSED;
        pausedAt = now;
    }

    public void resume(LocalDateTime now) {
        if (status == UserHobbyStatus.IN_PROGRESS) return;
        pausedSeconds += Math.max(0, Duration.between(pausedAt, now).getSeconds());
        pausedDays = (int) (pausedSeconds / 86400);
        pausedAt = null;
        status = UserHobbyStatus.IN_PROGRESS;
    }

    public void stop(LocalDateTime now) {
        if (status == UserHobbyStatus.STOPPED) return;
        // Preserve the frozen timeline when a paused hobby is stopped.
        if (pausedAt != null) {
            pausedSeconds += Math.max(0, Duration.between(pausedAt, now).getSeconds());
            pausedDays = (int) (pausedSeconds / 86400);
            pausedAt = null;
        }
        status = UserHobbyStatus.STOPPED;
        endedAt = now;
    }

    /** UTC storage; week boundaries use Seoul calendar dates, excluding exact paused durations. */
    public int currentWeek(int totalWeeks, LocalDateTime now) {
        if (startedAt == null) return 1;
        var zone = ZoneId.of("Asia/Seoul");
        LocalDateTime reference = pausedAt != null ? pausedAt : endedAt != null ? endedAt : now;
        var startDate = startedAt.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
        var effectiveDate = reference.minusSeconds(pausedSeconds)
                .atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDate();
        long days = Math.max(0, ChronoUnit.DAYS.between(startDate, effectiveDate));
        return (int) Math.min(totalWeeks, days / 7 + 1);
    }
}

