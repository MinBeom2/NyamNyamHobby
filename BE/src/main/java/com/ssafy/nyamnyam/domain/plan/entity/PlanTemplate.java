package com.ssafy.nyamnyam.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "plan_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hobby_id", nullable = false) private Long hobbyId;
    @Column(name = "skill_level", nullable = false, length = 12) private String skillLevel;
    @Column(nullable = false, length = 50) private String title;
    @Column(name = "total_weeks", nullable = false) private short totalWeeks;
    @Column(nullable = false) private int version;
    @Column(nullable = false, length = 10) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "plan_template_weeks", joinColumns = @JoinColumn(name = "template_id"))
    @MapKeyColumn(name = "week_no")
    @Column(name = "title", nullable = false, length = 50)
    private Map<Short, String> weekTitles = new TreeMap<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    @OrderBy("weekNo, seq, id")
    private Set<PlanTemplateItem> items = new LinkedHashSet<>();

    public PlanTemplate(Long hobbyId, String skillLevel, String title, short totalWeeks,
                        int version, String status, LocalDateTime now) {
        this.hobbyId = hobbyId;
        this.skillLevel = skillLevel;
        this.title = title;
        this.totalWeeks = totalWeeks;
        this.version = version;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addWeek(short week, String title) { weekTitles.put(week, title); }
    public void addItem(PlanTemplateItem item) { items.add(item); }
}
