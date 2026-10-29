package com.ssafy.nyamnyam.domain.recommendation.repository;

import com.ssafy.nyamnyam.domain.recommendation.model.HobbyMatch;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface HobbyRecommendationRepository {
    Optional<String> findPublishedHobbyVector(long hobbyId);
    List<HobbyMatch> findNearest(String vector, int limit, Set<Long> excludedHobbyIds);
}
