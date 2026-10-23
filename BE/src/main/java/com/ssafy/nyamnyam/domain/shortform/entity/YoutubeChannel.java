package com.ssafy.nyamnyam.domain.shortform.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YoutubeChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "youtube_channel_id", nullable = false, unique = true, length = 30)
    private String youtubeChannelId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "uploads_playlist_id", length = 40)
    private String uploadsPlaylistId;

    @Column(nullable = false, length = 10)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id")
    private Hobby hobby;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "discovered_by", nullable = false, length = 10)
    private String discoveredBy;

    @Column(name = "hidden_short_count", nullable = false)
    private int hiddenShortCount;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public YoutubeChannel(String youtubeChannelId, String title, String uploadsPlaylistId,
                          String type, Hobby hobby, String status, String discoveredBy) {
        this.youtubeChannelId = youtubeChannelId;
        this.title = title;
        this.uploadsPlaylistId = uploadsPlaylistId;
        this.type = type != null ? type : "SINGLE";
        this.hobby = hobby;
        this.status = status != null ? status : "CANDIDATE";
        this.discoveredBy = discoveredBy != null ? discoveredBy : "ADMIN";
        this.hiddenShortCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
