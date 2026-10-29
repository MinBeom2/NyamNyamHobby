package com.ssafy.nyamnyam.domain.exploration.entity;

import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exploration_views", uniqueConstraints = {
        @UniqueConstraint(name = "uk_view", columnNames = {"session_id", "short_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExplorationView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExplorationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_id", nullable = false)
    private Short shortform;

    @Column(nullable = false, columnDefinition = "smallint")
    private java.lang.Short seq;

    @Column(name = "play_failed", nullable = false)
    private boolean playFailed;

    @Column(name = "watched_seconds", nullable = false, columnDefinition = "smallint")
    private java.lang.Short watchedSeconds;

    @Column(name = "watch_ratio", precision = 4, scale = 3)
    private BigDecimal watchRatio;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Builder
    public ExplorationView(ExplorationSession session, Short shortform, java.lang.Short seq,
                           boolean playFailed, java.lang.Short watchedSeconds, BigDecimal watchRatio) {
        this.session = session;
        this.shortform = shortform;
        this.seq = seq;
        this.playFailed = playFailed;
        this.watchedSeconds = watchedSeconds != null ? watchedSeconds : 0;
        this.watchRatio = watchRatio;
        this.viewedAt = LocalDateTime.now();
    }

    public void updateWatchTime(java.lang.Short watchedSeconds, BigDecimal watchRatio, boolean playFailed) {
        this.watchedSeconds = watchedSeconds;
        this.watchRatio = watchRatio;
        this.playFailed = playFailed;
        this.viewedAt = LocalDateTime.now();
    }
}
