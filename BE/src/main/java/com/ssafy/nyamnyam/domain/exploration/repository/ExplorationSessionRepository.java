package com.ssafy.nyamnyam.domain.exploration.repository;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExplorationSessionRepository extends JpaRepository<ExplorationSession, Long> {
    List<ExplorationSession> findByUserOrderByStartedAtDesc(User user);
    Optional<ExplorationSession> findByIdAndUser(Long id, User user);
}
