package com.ssafy.nyamnyam.domain.exploration.repository;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExplorationSessionRepository extends JpaRepository<ExplorationSession, Long> {
    List<ExplorationSession> findByUserOrderByStartedAtDesc(User user);
    Optional<ExplorationSession> findByIdAndUser(Long id, User user);

    /** 시청·반응 변경과 추천 생성을 같은 세션 잠금으로 직렬화한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ExplorationSession s WHERE s.id = :id AND s.user = :user")
    Optional<ExplorationSession> findByIdAndUserForUpdate(@Param("id") Long id, @Param("user") User user);
}
