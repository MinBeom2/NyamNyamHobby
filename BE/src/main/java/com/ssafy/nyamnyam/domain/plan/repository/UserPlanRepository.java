package com.ssafy.nyamnyam.domain.plan.repository;

import com.ssafy.nyamnyam.domain.plan.entity.UserPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {
    @Override
    @EntityGraph(attributePaths = {"userHobby", "userHobby.user", "userHobby.hobby", "items", "weekTitles"})
    Optional<UserPlan> findById(Long id);

    @EntityGraph(attributePaths = {"userHobby", "userHobby.hobby", "items", "weekTitles"})
    List<UserPlan> findByUserHobbyUserIdAndStatusOrderByStartedAtDescIdDesc(Long userId, String status);
}
