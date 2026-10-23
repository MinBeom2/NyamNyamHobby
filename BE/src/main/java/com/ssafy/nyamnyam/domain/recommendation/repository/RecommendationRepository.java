package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.entity.Recommendation;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<Recommendation> findBySessionId(Long sessionId);
}
