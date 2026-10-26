package com.ssafy.nyamnyam.global.batchrun;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface BatchRunRepository extends JpaRepository<BatchRun, Long> {

    /** since 이후 시작한 작업들의 외부 API 사용량 합계. 합계 타입이 Long/Integer로 갈릴 수 있어 Number로 받는다 */
    @Query("SELECT COALESCE(SUM(b.quotaUsed), 0) FROM BatchRun b " +
           "WHERE b.job IN :jobs AND b.startedAt >= :since")
    Number sumQuotaUsedSince(@Param("jobs") Collection<BatchJob> jobs,
                             @Param("since") LocalDateTime since);
}
