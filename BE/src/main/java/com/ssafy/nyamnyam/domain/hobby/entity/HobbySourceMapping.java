package com.ssafy.nyamnyam.domain.hobby.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터 매핑표: 취미별 외부 검색어(카카오 검색어 · 공공데이터 종목명 · 행사 검색어 · 숏폼 키워드).
 * MVP는 시드 SQL로 넣는다(관리자 편집 화면은 P2).
 */
@Entity
@Table(name = "hobby_source_mappings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HobbySourceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceTarget target;

    @Column(nullable = false, length = 50)
    private String keyword;

    /** SHORT_KEYWORD 전용: 다음에 이어서 볼 검색 결과 페이지. null이면 처음부터 */
    @Column(name = "next_page_token", length = 100)
    private String nextPageToken;

    /** SHORT_KEYWORD 전용: 마지막 수집 시각 */
    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public HobbySourceMapping(Hobby hobby, SourceTarget target, String keyword) {
        this.hobby = hobby;
        this.target = target;
        this.keyword = keyword;
        this.createdAt = LocalDateTime.now();
    }

    /** 이번 수집을 마치고 다음 위치를 기록한다. 마지막 페이지였으면 토큰이 null이 되어 다음엔 처음부터 본다. */
    public void markCollected(String nextPageToken, LocalDateTime collectedAt) {
        this.nextPageToken = nextPageToken;
        this.lastCollectedAt = collectedAt;
    }
}
