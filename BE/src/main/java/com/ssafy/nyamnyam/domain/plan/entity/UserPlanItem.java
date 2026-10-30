package com.ssafy.nyamnyam.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_plan_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPlanItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_plan_id", nullable = false)
    private UserPlan plan;
    @Column(name = "template_item_id") private Long templateItemId;
    @Column(name = "week_no", nullable = false) private short weekNo;
    @Column(nullable = false) private short seq;
    @Column(nullable = false, length = 50) private String title;
    @Column(length = 500) private String description;
    @Column(name = "expected_minutes", nullable = false) private short expectedMinutes;
    @Column(length = 200) private String supplies;
    @Column(name = "done_criteria", length = 200) private String doneCriteria;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ItemStatus status;
    @Column(length = 500) private String memo;
    @Column(name = "is_custom", nullable = false) private boolean custom;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public UserPlanItem(UserPlan plan, short weekNo, short seq, String title, String description,
                        short expectedMinutes, String supplies, String doneCriteria, LocalDateTime now) {
        this.plan = plan;
        this.weekNo = weekNo;
        this.seq = seq;
        this.title = title;
        this.description = description;
        this.expectedMinutes = expectedMinutes;
        this.supplies = supplies;
        this.doneCriteria = doneCriteria;
        this.status = ItemStatus.TODO;
        this.custom = true;
        this.updatedAt = now;
    }

    public static UserPlanItem copy(UserPlan plan, PlanTemplateItem source, LocalDateTime now) {
        var item = new UserPlanItem(plan, source.getWeekNo(), source.getSeq(), source.getTitle(),
                source.getDescription(), source.getExpectedMinutes(), source.getSupplies(), source.getDoneCriteria(), now);
        item.templateItemId = source.getId();
        item.custom = false;
        return item;
    }

    public void changeStatus(ItemStatus next, LocalDateTime now) {
        if (next == status) return;
        status = next;
        completedAt = next == ItemStatus.DONE ? now : null;
        updatedAt = now;
    }
    public void changeMemo(String memo, LocalDateTime now) { this.memo = memo; updatedAt = now; }
    public void move(short week, short seq, LocalDateTime now) { weekNo = week; this.seq = seq; updatedAt = now; }
    public void changeExpectedMinutes(short minutes, LocalDateTime now) { expectedMinutes = minutes; updatedAt = now; }
    public void delete(LocalDateTime now) { deletedAt = now; updatedAt = now; }
}
