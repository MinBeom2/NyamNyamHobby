package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.recommendation.model.HobbyMatch;
import com.ssafy.nyamnyam.domain.recommendation.model.TagFeature;
import com.ssafy.nyamnyam.domain.recommendation.repository.HobbyRecommendationRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class HobbyRecommendationService {
    private final HobbyRecommendationRepository repository;

    public HobbyRecommendationService(HobbyRecommendationRepository repository) {
        this.repository = repository;
    }

    /** 생략한 태그는 0점. 음수는 비선호로 유지하고 양수 선호가 없으면 빈 결과. */
    public List<HobbyMatch> recommendByPreference(Map<TagFeature, Double> preferenceScores,
                                                int limit, Set<Long> excludedHobbyIds) {
        validateOptions(limit, excludedHobbyIds);
        if (preferenceScores == null || preferenceScores.entrySet().stream().anyMatch(e ->
                e.getKey() == null || e.getValue() == null || !Double.isFinite(e.getValue()))) {
            throw invalid("태그 선호 점수는 유한한 숫자여야 합니다.");
        }
        double max = preferenceScores.values().stream().mapToDouble(Math::abs).max().orElse(0);
        if (preferenceScores.values().stream().noneMatch(v -> v > 0)) return List.of();
        // 공통 배율은 코사인에 영향이 없다. 먼저 축소해 극단적인 입력의 overflow를 방지한다.
        String vector = Arrays.stream(TagFeature.values())
                .map(tag -> Double.toString(preferenceScores.getOrDefault(tag, 0.0) / max * tag.weight()))
                .collect(Collectors.joining(",", "[", "]"));
        return repository.findNearest(vector, limit, Set.copyOf(excludedHobbyIds));
    }

    public List<HobbyMatch> recommendSimilarToHobby(long hobbyId, int limit, Set<Long> excludedHobbyIds) {
        validateOptions(limit, excludedHobbyIds);
        if (hobbyId <= 0) throw invalid("취미 ID는 양수여야 합니다.");
        String vector = repository.findPublishedHobbyVector(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_RECOMMENDATION_UNAVAILABLE));
        Set<Long> exclusions = new HashSet<>(excludedHobbyIds);
        exclusions.add(hobbyId);
        return repository.findNearest(vector, limit, exclusions);
    }

    private void validateOptions(int limit, Set<Long> excluded) {
        if (limit < 1 || limit > 100 || excluded == null
                || excluded.stream().anyMatch(id -> id == null || id <= 0)) {
            throw invalid("추천 개수는 1~100, 제외 취미 ID는 양수여야 합니다.");
        }
    }

    private CustomException invalid(String message) {
        return new CustomException(ErrorCode.COMMON_INVALID_INPUT, message);
    }
}
