package com.ssafy.nyamnyam.domain.recommendation.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "recommendation_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reco_item", columnNames = {"recommendation_id", "hobby_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecoItemType type;

    @Column(name = "rank_no", nullable = false, columnDefinition = "smallint")
    private java.lang.Short rankNo;

    @Column(nullable = false, precision = 7, scale = 3)
    private BigDecimal score;

    @Column(length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_status", nullable = false, length = 10)
    private ReasonStatus reasonStatus;

    @Column(name = "condition_warning", nullable = false)
    private boolean conditionWarning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hobby_snapshot", columnDefinition = "jsonb")
    private String hobbySnapshot;

    @Column(name = "fallback_reason", length = 200)
    private String fallbackReason;

    @Column(nullable = false)
    private boolean saved;

    public void initializeSnapshot(String hobbySnapshot, String fallbackReason, boolean saved) {
        this.hobbySnapshot = hobbySnapshot;
        this.fallbackReason = fallbackReason;
        this.saved = saved;
    }

    @Builder
    public RecommendationItem(Recommendation recommendation, Hobby hobby, RecoItemType type,
                              java.lang.Short rankNo, BigDecimal score, String reason,
                              ReasonStatus reasonStatus, boolean conditionWarning) {
        this.recommendation = recommendation;
        this.hobby = hobby;
        this.type = type;
        this.rankNo = rankNo;
        this.score = score;
        this.reason = reason;
        this.reasonStatus = reasonStatus != null ? reasonStatus : ReasonStatus.PENDING;
        this.conditionWarning = conditionWarning;
    }

    public void updateReason(String reason, ReasonStatus reasonStatus) {
        this.reason = reason;
        this.reasonStatus = reasonStatus;
    }
}
