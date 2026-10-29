package com.ssafy.nyamnyam.domain.user.repository;

import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {
    boolean existsByUserId(Long userId);

    @Query("SELECT uc FROM UserCondition uc LEFT JOIN FETCH uc.region WHERE uc.userId = :userId")
    Optional<UserCondition> findByUserIdWithRegion(@Param("userId") Long userId);
}
