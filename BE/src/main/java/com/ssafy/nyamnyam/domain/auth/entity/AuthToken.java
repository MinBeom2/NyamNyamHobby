package com.ssafy.nyamnyam.domain.auth.entity;

import com.ssafy.nyamnyam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens", indexes = {
        @Index(name = "idx_token_user", columnList = "user_id, type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "token_hash", columnDefinition = "char(64)", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AuthToken(User user, TokenType type, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.type = type;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }

    public boolean isUsed() {
        return this.usedAt != null;
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
