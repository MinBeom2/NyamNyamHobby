package com.ssafy.nyamnyam.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_conditions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCondition {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "weekly_hours", columnDefinition = "smallint")
    private Short weeklyHours;

    @Column(name = "monthly_budget")
    private Integer monthlyBudget;

    @Column(name = "place_pref", length = 10)
    private String placePref;

    @Column(name = "social_pref", length = 10)
    private String socialPref;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserCondition(User user, Short weeklyHours, Integer monthlyBudget, String placePref, String socialPref, Long regionId) {
        this.user = user;
        this.userId = user != null ? user.getId() : null;
        this.weeklyHours = weeklyHours;
        this.monthlyBudget = monthlyBudget;
        this.placePref = placePref;
        this.socialPref = socialPref;
        this.regionId = regionId;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
