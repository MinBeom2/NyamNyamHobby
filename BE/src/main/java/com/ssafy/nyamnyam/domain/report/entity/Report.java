package com.ssafy.nyamnyam.domain.report.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고(reports). 한 사용자는 같은 대상을 한 번만 신고할 수 있다(uk_report).
 * 처리는 관리자 화면 대신 Mattermost 버튼으로 하므로 admin_id는 비우고 admin_note에 처리한 Mattermost 사용자를 남긴다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportReason reason;

    @Column(length = 300)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ReportAction action;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_note", length = 300)
    private String adminNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Report(Long reporterId, ReportTargetType targetType, Long targetId, ReportReason reason, String detail) {
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.RECEIVED;
        this.createdAt = LocalDateTime.now();
    }
}
