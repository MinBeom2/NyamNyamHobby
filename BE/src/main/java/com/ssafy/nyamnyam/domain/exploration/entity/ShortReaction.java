package com.ssafy.nyamnyam.domain.exploration.entity;

import com.ssafy.nyamnyam.domain.shortform.entity.Short;
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
@Table(name = "short_reactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reaction_user", columnNames = {"user_id", "short_id"}),
        @UniqueConstraint(name = "uk_reaction_guest", columnNames = {"guest_key", "short_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShortReaction {

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
    @JoinColumn(name = "short_id", nullable = false)
    private Short shortform;

    @Column(name = "session_id")
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ShortReaction(User user, String guestKey, Short shortform, Long sessionId, ReactionType type) {
        this.user = user;
        this.guestKey = guestKey;
        this.shortform = shortform;
        this.sessionId = sessionId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateType(ReactionType type, Long sessionId) {
        this.type = type;
        this.sessionId = sessionId;
        this.updatedAt = LocalDateTime.now();
    }
}
