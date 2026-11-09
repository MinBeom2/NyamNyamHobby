package com.ssafy.nyamnyam.domain.report.repository;

import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportAction;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);

    /** 대상의 처리 전 신고(사유별 개수·상세 사유를 보여 줄 때) */
    List<Report> findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(ReportTargetType targetType, Long targetId, ReportStatus status);

    /** 아직 Mattermost로 보내지 않은 신고(오래된 순) */
    @Query("SELECT r FROM Report r WHERE r.notifiedAt IS NULL ORDER BY r.id ASC")
    List<Report> findUnnotified(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Report r SET r.notifiedAt = :now WHERE r.id IN :ids AND r.notifiedAt IS NULL")
    int markNotified(@Param("ids") Collection<Long> ids, @Param("now") LocalDateTime now);

    /**
     * 대상의 처리 전(RECEIVED) 신고를 한 번에 처리한다. 버튼을 두 사람이 동시에 눌러도 한쪽만 바뀐다.
     * @return 처리한 신고 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Report r SET r.status = :status, r.action = :action, r.adminNote = :note, r.processedAt = :now " +
           "WHERE r.targetType = :targetType AND r.targetId = :targetId " +
           "AND r.status = com.ssafy.nyamnyam.domain.report.entity.ReportStatus.RECEIVED")
    int resolveReceived(@Param("targetType") ReportTargetType targetType,
                        @Param("targetId") Long targetId,
                        @Param("status") ReportStatus status,
                        @Param("action") ReportAction action,
                        @Param("note") String note,
                        @Param("now") LocalDateTime now);
}
