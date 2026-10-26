package com.ssafy.nyamnyam.global.batchrun;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 스케줄 작업 실행 기록(batch_runs). Spring Batch가 아니라 @Scheduled 작업이 직접 남기는 로그 테이블이다.
 * 수집 현황 화면(P2)과 YouTube 하루 할당량 계산에 쓴다.
 */
@Entity
@Table(name = "batch_runs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchRun {

    private static final int MESSAGE_MAX = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BatchRunStatus status;

    /** 조회한 건수 */
    @Column(nullable = false)
    private int processed;

    /** 저장·반영한 건수 */
    @Column(nullable = false)
    private int accepted;

    /** 필터로 제외한 건수 */
    @Column(nullable = false)
    private int rejected;

    /** 외부 API 사용량(YouTube는 할당량 단위) */
    @Column(name = "quota_used", nullable = false)
    private int quotaUsed;

    @Column(length = MESSAGE_MAX)
    private String message;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public static BatchRun start(BatchJob job) {
        BatchRun run = new BatchRun();
        run.job = job;
        run.status = BatchRunStatus.RUNNING;
        run.startedAt = LocalDateTime.now();
        return run;
    }

    public void finish(BatchRunStatus status, int processed, int accepted, int rejected, int quotaUsed, String message) {
        this.status = status;
        this.processed = processed;
        this.accepted = accepted;
        this.rejected = rejected;
        this.quotaUsed = quotaUsed;
        this.message = truncate(message);
        this.finishedAt = LocalDateTime.now();
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MESSAGE_MAX) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX - 3) + "...";
    }
}
