package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.entity.Recommendation;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM RecommendationItem i WHERE i.id = :id")
    java.util.Optional<RecommendationItem> findByIdForUpdate(@Param("id") Long id);
    List<RecommendationItem> findByRecommendationOrderByRankNoAsc(Recommendation recommendation);
    List<RecommendationItem> findByRecommendationIdOrderByRankNoAsc(Long recommendationId);
}
