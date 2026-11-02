package com.ssafy.nyamnyam.domain.notification.repository;

import com.ssafy.nyamnyam.domain.plan.entity.UserPlan;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 주차 알림 대상 조회 전용(읽기만). plan 도메인의 저장소는 건드리지 않는다 */
public interface WeeklyTodoTargetRepository extends Repository<UserPlan, Long> {

    /** 진행 중(IN_PROGRESS) 취미의 현재(ACTIVE) 계획. 일시중지·종료한 취미는 주차가 멈춰 있으므로 제외한다 */
    @EntityGraph(attributePaths = {"userHobby", "userHobby.hobby", "weekTitles", "items"})
    @Query("SELECT p FROM UserPlan p WHERE p.status = 'ACTIVE' AND p.userHobby.status = :status")
    List<UserPlan> findActivePlans(@Param("status") UserHobbyStatus status);
}
