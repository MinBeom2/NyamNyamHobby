package com.ssafy.nyamnyam.domain.feed.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "feed_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private FeedType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 300)
    private String summary;

    @Column(name = "source_name", nullable = false, length = 50)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "event_start")
    private LocalDate eventStart;

    @Column(name = "event_end")
    private LocalDate eventEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FeedStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public FeedItem(Hobby hobby, FeedType type, String title, String summary,
                    String sourceName, String sourceUrl, String sourceRef,
                    String thumbnailUrl, Region region, LocalDateTime publishedAt,
                    LocalDate eventStart, LocalDate eventEnd, FeedOrigin origin,
                    FeedStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.hobby = hobby;
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.sourceRef = sourceRef;
        this.thumbnailUrl = thumbnailUrl;
        this.region = region;
        this.publishedAt = publishedAt != null ? publishedAt : LocalDateTime.now();
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.origin = origin != null ? origin : FeedOrigin.ADMIN;
        this.status = status != null ? status : FeedStatus.PENDING_REVIEW;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    /** 행사 종료 여부 판단 (eventEnd가 있고 오늘보다 이전이면 종료됨) */
    public boolean computeIsEnded() {
        return eventEnd != null && eventEnd.isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
    }
}

