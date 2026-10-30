package com.ssafy.nyamnyam.domain.plan.entity;

import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "user_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_hobby_id", nullable = false)
    private UserHobby userHobby;
    @Column(name = "template_id", nullable = false) private Long templateId;
    @Column(name = "template_version", nullable = false) private int templateVersion;
    @Column(nullable = false, length = 50) private String title;
    @Column(name = "total_weeks", nullable = false) private short totalWeeks;
    @Column(nullable = false, length = 10) private String status;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;

    @ElementCollection
    @CollectionTable(name = "user_plan_weeks", joinColumns = @JoinColumn(name = "user_plan_id"))
    @MapKeyColumn(name = "week_no")
    @Column(name = "title", nullable = false, length = 50)
    private Map<Short, String> weekTitles = new TreeMap<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    @OrderBy("weekNo, seq, id")
    private Set<UserPlanItem> items = new LinkedHashSet<>();

    public UserPlan(UserHobby userHobby, PlanTemplate template, LocalDateTime now) {
        this.userHobby = userHobby;
        this.templateId = template.getId();
        this.templateVersion = template.getVersion();
        this.title = template.getTitle();
        this.totalWeeks = template.getTotalWeeks();
        this.status = "ACTIVE";
        this.startedAt = now;
        this.weekTitles.putAll(template.getWeekTitles());
        template.getItems().forEach(item -> items.add(UserPlanItem.copy(this, item, now)));
    }

    public List<UserPlanItem> visibleItems() {
        return items.stream().filter(i -> i.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(UserPlanItem::getWeekNo).thenComparingInt(UserPlanItem::getSeq)
                        .thenComparing(UserPlanItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public void addItem(UserPlanItem item) { items.add(item); }
}
