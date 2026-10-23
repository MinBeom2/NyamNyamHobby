package com.ssafy.nyamnyam.domain.shortform.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shorts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Short {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private YoutubeChannel channel;

    @Column(name = "youtube_video_id", nullable = false, unique = true, length = 20)
    private String youtubeVideoId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds", nullable = false, columnDefinition = "smallint")
    private java.lang.Short durationSeconds;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ShortStatus status;

    @Column(name = "assign_method", length = 10)
    private String assignMethod;

    @Column(name = "assign_confidence", precision = 3, scale = 2)
    private BigDecimal assignConfidence;

    @Column(name = "reject_reason", length = 20)
    private String rejectReason;

    @Column(name = "view_user_count", nullable = false)
    private int viewUserCount;

    @Column(name = "irrelevant_count", nullable = false)
    private int irrelevantCount;

    @Column(name = "fail_user_count", nullable = false)
    private int failUserCount;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Short(Hobby hobby, YoutubeChannel channel, String youtubeVideoId, String title,
                 String description, String thumbnailUrl, java.lang.Short durationSeconds,
                 LocalDateTime publishedAt, ShortStatus status, String assignMethod,
                 BigDecimal assignConfidence) {
        this.hobby = hobby;
        this.channel = channel;
        this.youtubeVideoId = youtubeVideoId;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds != null ? durationSeconds : 60;
        this.publishedAt = publishedAt;
        this.status = status != null ? status : ShortStatus.CANDIDATE;
        this.assignMethod = assignMethod;
        this.assignConfidence = assignConfidence;
        this.viewUserCount = 0;
        this.irrelevantCount = 0;
        this.failUserCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementIrrelevantCount(int delta) {
        this.irrelevantCount = Math.max(0, this.irrelevantCount + delta);
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewUserCount() {
        this.viewUserCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementFailUserCount() {
        this.failUserCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
