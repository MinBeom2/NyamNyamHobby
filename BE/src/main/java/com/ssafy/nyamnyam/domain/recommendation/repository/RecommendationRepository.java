package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.entity.Recommendation;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<Recommendation> findBySessionId(Long sessionId);
    boolean existsBySessionId(Long sessionId);
    Optional<Recommendation> findByIdAndUserId(Long id, Long userId);
    /** 홈 '맞춤 추천 취미'용: 가장 최근에 만든 추천(같은 시각이면 나중에 저장된 것) */
    Optional<Recommendation> findTopByUserIdOrderByIdDesc(Long userId);

    /** 비회원 이관용: guestKey로 연결된 추천 목록 */
    List<Recommendation> findByGuestKey(String guestKey);
}
