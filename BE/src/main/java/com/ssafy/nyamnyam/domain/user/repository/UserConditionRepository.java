package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {
    boolean existsByUserId(Long userId);
}
