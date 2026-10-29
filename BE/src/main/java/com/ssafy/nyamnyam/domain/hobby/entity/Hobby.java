package com.ssafy.nyamnyam.domain.hobby.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(length = 100)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", nullable = false, length = 10)
    private PlaceType placeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 10)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(name = "session_min_minutes", nullable = false, columnDefinition = "smallint")
    private Short sessionMinMinutes;

    @Column(name = "session_max_minutes", nullable = false, columnDefinition = "smallint")
    private Short sessionMaxMinutes;

    @Column(name = "initial_cost_min", nullable = false)
    private Integer initialCostMin;

    @Column(name = "initial_cost_max", nullable = false)
    private Integer initialCostMax;

    @Column(name = "monthly_cost_min", nullable = false)
    private Integer monthlyCostMin;

    @Column(name = "monthly_cost_max", nullable = false)
    private Integer monthlyCostMax;

    @Column(columnDefinition = "text")
    private String supplies;

    @Column(columnDefinition = "text")
    private String environment;

    @Column(name = "first_activity", columnDefinition = "text")
    private String firstActivity;

    @Column(columnDefinition = "text")
    private String cautions;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HobbyStatus status;

    @Column(name = "save_count", nullable = false)
    private int saveCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hobby", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HobbyTag> hobbyTags = new ArrayList<>();

    @Builder
    public Hobby(Category category, String name, String summary, String description,
                 PlaceType placeType, SocialType socialType, Difficulty difficulty,
                 Short sessionMinMinutes, Short sessionMaxMinutes,
                 Integer initialCostMin, Integer initialCostMax,
                 Integer monthlyCostMin, Integer monthlyCostMax,
                 String supplies, String environment, String firstActivity, String cautions,
                 String thumbnailUrl, HobbyStatus status, int saveCount) {
        this.category = category;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.placeType = placeType;
        this.socialType = socialType;
        this.difficulty = difficulty;
        this.sessionMinMinutes = sessionMinMinutes;
        this.sessionMaxMinutes = sessionMaxMinutes;
        this.initialCostMin = initialCostMin != null ? initialCostMin : 0;
        this.initialCostMax = initialCostMax != null ? initialCostMax : 0;
        this.monthlyCostMin = monthlyCostMin != null ? monthlyCostMin : 0;
        this.monthlyCostMax = monthlyCostMax != null ? monthlyCostMax : 0;
        this.supplies = supplies;
        this.environment = environment;
        this.firstActivity = firstActivity;
        this.cautions = cautions;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status != null ? status : HobbyStatus.DRAFT;
        this.saveCount = saveCount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
