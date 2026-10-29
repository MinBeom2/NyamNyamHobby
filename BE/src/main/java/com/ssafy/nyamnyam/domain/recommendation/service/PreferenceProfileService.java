package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.*;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenceProfileService {
    private final ExplorationViewRepository views;
    private final ShortReactionRepository reactions;
    public record Profile(Map<Long, Double> hobbyScores, Map<String, Double> tagScores,
                          Map<Long, Long> likes, Map<Long, Long> categoryViews, int reactionCount) {
        public static Profile empty() { return new Profile(Map.of(), Map.of(), Map.of(), Map.of(), 0); }
        public long likeCount() { return likes.values().stream().mapToLong(Long::longValue).sum(); }
    }
    public Profile load(User user) { return load(user, null); }
    public Profile load(User user, Long excludedSessionId) {
        Map<Long, ShortReaction> reactionMap = new HashMap<>();
        for (ShortReaction r : reactions.findByUser(user)) {
            if (user.getInterestResetAt() == null || !r.getUpdatedAt().isBefore(user.getInterestResetAt()))
                reactionMap.put(r.getShortform().getId(), r);
        }
        Map<Long, ExplorationView> latest = new LinkedHashMap<>();
        for (ExplorationView v : views.findHistoryByUserId(user.getId())) {
            if (excludedSessionId != null && excludedSessionId.equals(v.getSession().getId())) continue;
            if (user.getInterestResetAt() != null && v.getViewedAt().isBefore(user.getInterestResetAt())) continue;
            latest.putIfAbsent(v.getShortform().getId(), v);
        }
        Map<Long, Double> hobbyScores = new HashMap<>();
        Map<String, Double> tagScores = new HashMap<>();
        Map<Long, Long> likes = new HashMap<>();
        Map<Long, Long> categoryViews = new HashMap<>();
        int count = 0;
        for (ExplorationView v : latest.values()) {
            Hobby hobby = v.getShortform().getHobby();
            if (hobby == null || v.isPlayFailed()) continue;
            ShortReaction r = reactionMap.get(v.getShortform().getId());
            ReactionType type = r == null ? null : r.getType();
            if (type == ReactionType.IRRELEVANT) continue;
            if (r != null) count++;
            if (type == ReactionType.LIKE) likes.merge(hobby.getId(), 1L, Long::sum);
            double score = (type == ReactionType.LIKE ? 3 : type == ReactionType.DISLIKE ? -3 : 0)
                    + (v.getWatchRatio() == null ? 0 : v.getWatchRatio().doubleValue()) * 2 - 1;
            hobbyScores.merge(hobby.getId(), score, Double::sum);
            for (HobbyTag tag : hobby.getHobbyTags()) tagScores.merge(tag.getTag().getName(), score * 0.3, Double::sum);
            if (hobby.getCategory() != null) categoryViews.merge(hobby.getCategory().getId(), 1L, Long::sum);
        }
        return new Profile(hobbyScores, tagScores, likes, categoryViews, count);
    }
}
