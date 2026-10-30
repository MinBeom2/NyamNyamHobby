package com.ssafy.nyamnyam.domain.plan.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "plan_template_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanTemplateItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id", nullable = false)
    private PlanTemplate template;
    @Column(name = "week_no", nullable = false) private short weekNo;
    @Column(nullable = false) private short seq;
    @Column(nullable = false, length = 50) private String title;
    @Column(length = 500) private String description;
    @Column(name = "expected_minutes", nullable = false) private short expectedMinutes;
    @Column(length = 200) private String supplies;
    @Column(name = "done_criteria", nullable = false, length = 200) private String doneCriteria;

    public PlanTemplateItem(PlanTemplate template, short weekNo, short seq, String title,
                            String description, short expectedMinutes, String supplies, String doneCriteria) {
        this.template = template;
        this.weekNo = weekNo;
        this.seq = seq;
        this.title = title;
        this.description = description;
        this.expectedMinutes = expectedMinutes;
        this.supplies = supplies;
        this.doneCriteria = doneCriteria;
    }
}
