package com.ssafy.nyamnyam.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 12)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "interest_reset_at")
    private LocalDateTime interestResetAt;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    public User(String email, String passwordHash, String nickname, String profileImageUrl, Role role, UserStatus status) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role != null ? role : Role.USER;
        this.status = status != null ? status : UserStatus.PENDING;
        this.loginFailCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementLoginFail(LocalDateTime now) {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.lockedUntil = now.plusMinutes(5);
        }
        this.updatedAt = now;
    }

    public void resetLoginFail(LocalDateTime now) {
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public boolean isLocked(LocalDateTime now) {
        return this.lockedUntil != null && now.isBefore(this.lockedUntil);
    }

    public void activate(LocalDateTime now) {
        this.status = UserStatus.ACTIVE;
        this.emailVerifiedAt = now;
        this.updatedAt = now;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = LocalDateTime.now();
    }

    public void withdraw(LocalDateTime now) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
