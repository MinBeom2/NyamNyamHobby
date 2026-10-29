package com.ssafy.nyamnyam.domain.recommendation.entity;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "guest_key", length = 36, columnDefinition = "char(36)")
    private String guestKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExplorationSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RecoBasis basis;

    @Column(name = "is_exploratory", nullable = false)
    private boolean isExploratory;

    @Column(name = "reaction_count", nullable = false, columnDefinition = "smallint")
    private java.lang.Short reactionCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_snapshot", columnDefinition = "jsonb")
    private String conditionSnapshot;

    @Column(name = "basis_text", length = 500)
    private String basisText;

    public void initializeSnapshot(String basisText, String conditions) {
        this.basisText = basisText;
        this.conditionSnapshot = conditions;
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendationItem> items = new ArrayList<>();

    @Builder
    public Recommendation(User user, String guestKey, ExplorationSession session,
                          RecoBasis basis, boolean isExploratory, java.lang.Short reactionCount,
                          String conditionSnapshot) {
        this.user = user;
        this.guestKey = guestKey;
        this.session = session;
        this.basis = basis != null ? basis : RecoBasis.REACTION;
        this.isExploratory = isExploratory;
        this.reactionCount = reactionCount != null ? reactionCount : 0;
        this.conditionSnapshot = conditionSnapshot;
        this.createdAt = LocalDateTime.now();
    }
}
