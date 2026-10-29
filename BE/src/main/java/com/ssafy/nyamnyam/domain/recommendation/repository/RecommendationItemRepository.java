package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.entity.Recommendation;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationItemRepository extends JpaRepository<RecommendationItem, Long> {
    List<RecommendationItem> findByRecommendationOrderByRankNoAsc(Recommendation recommendation);
    List<RecommendationItem> findByRecommendationIdOrderByRankNoAsc(Long recommendationId);
}
