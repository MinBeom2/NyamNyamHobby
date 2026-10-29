package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecoItemType;
import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecommendationPlanner {
    public record Choice(Hobby hobby, RecoItemType type, double score, boolean warning, String fallbackReason) {}
    public record Plan(boolean exploratory, List<Choice> choices) {}

    public Plan plan(List<Hobby> hobbies, PreferenceProfileService.Profile profile, Set<Long> excluded,
                     Set<Long> inProgress, UserCondition condition) {
        List<Hobby> eligible = hobbies.stream().filter(h -> !excluded.contains(h.getId())).toList();
        if (profile.likeCount() < 3) {
            Set<Long> categories = new HashSet<>();
            List<Choice> exploratory = new ArrayList<>();
            for (Hobby h : eligible.stream().filter(h -> !inProgress.contains(h.getId()))
                    .sorted(Comparator.<Hobby>comparingDouble(h -> choice(h, RecoItemType.INTEREST,
                            Math.max(1, h.getSaveCount()), condition, "").score()).reversed().thenComparing(Hobby::getId)).toList()) {
                if (h.getCategory() == null || !categories.add(h.getCategory().getId())) continue;
                exploratory.add(choice(h, exploratory.size() < 3 ? RecoItemType.INTEREST : RecoItemType.RELATED,
                        Math.max(1, h.getSaveCount()), condition,
                        "아직 관심사를 알아가는 중이라 인기 있는 " + h.getName() + " 취미를 제안해요"));
                if (exploratory.size() == 6) break;
            }
            return new Plan(true, exploratory);
        }
        List<Choice> interests = eligible.stream().filter(h -> profile.hobbyScores().getOrDefault(h.getId(), 0.0) > 0)
                .map(h -> choice(h, RecoItemType.INTEREST, profile.hobbyScores().get(h.getId()), condition,
                        profile.likes().getOrDefault(h.getId(), 0L) > 0
                                ? h.getName() + " 영상에 좋아요 " + profile.likes().get(h.getId()) + "회 남겼어요"
                                : h.getName() + " 영상을 관심 있게 시청했어요"))
                .sorted(order()).limit(3).toList();
        Set<Long> interestIds = interests.stream().map(c -> c.hobby().getId()).collect(Collectors.toSet());
        double maxTagScore = Math.max(1e-9, profile.tagScores().values().stream().mapToDouble(Double::doubleValue).max().orElse(1));
        List<Choice> related = eligible.stream().filter(h -> !interestIds.contains(h.getId()) && !inProgress.contains(h.getId()))
                .map(h -> {
                    double similarity = interests.stream().mapToDouble(i -> calculateTagSimilarity(i.hobby(), h)).max().orElse(0);
                    double tagSum = h.getHobbyTags().stream().mapToDouble(t -> profile.tagScores().getOrDefault(t.getTag().getName(), 0.0)).sum();
                    return choice(h, RecoItemType.RELATED, similarity * 0.7 + Math.max(0, tagSum / maxTagScore) * 0.3,
                            condition, "관심 취미와 비슷한 특성을 가진 " + h.getName() + " 취미예요");
                }).sorted(order()).limit(3).toList();
        List<Choice> result = new ArrayList<>(interests);
        result.addAll(related);
        return new Plan(false, result);
    }

    private Comparator<Choice> order() {
        return Comparator.comparingDouble(Choice::score).reversed().thenComparing(c -> c.hobby().getId());
    }
    private Choice choice(Hobby h, RecoItemType type, double score, UserCondition c, String fallback) {
        boolean warning = c != null && ((c.getMonthlyBudget() != null && h.getMonthlyCostMin() > c.getMonthlyBudget())
                || (c.getWeeklyHours() != null && h.getSessionMinMinutes() > c.getWeeklyHours() * 60));
        if (warning) score *= 0.8;
        if (c != null && c.getPlacePref() != null && !"ANY".equals(c.getPlacePref())
                && (c.getPlacePref().equals(h.getPlaceType().name()) || h.getPlaceType() == PlaceType.BOTH)) score *= 1.1;
        if (c != null && c.getSocialPref() != null && !"ANY".equals(c.getSocialPref())
                && (c.getSocialPref().equals(h.getSocialType().name()) || h.getSocialType() == SocialType.BOTH)) score *= 1.1;
        return new Choice(h, type, score, warning, fallback);
    }

    /** 기존 탐색 종료 추천의 유사도 공식을 유지한다. */
    double calculateTagSimilarity(Hobby h1, Hobby h2) {
        Set<String> tendency = new HashSet<>(), subject = new HashSet<>();
        for (HobbyTag ht : h1.getHobbyTags()) {
            Tag t = ht.getTag();
            (t.getTagGroup() == TagGroup.SUBJECT || t.getTagGroup() == TagGroup.GAIN ? subject : tendency).add(t.getName());
        }
        int tendencyOverlap = 0, subjectOverlap = 0;
        for (HobbyTag ht : h2.getHobbyTags()) {
            Tag t = ht.getTag();
            if (t.getTagGroup() == TagGroup.SUBJECT || t.getTagGroup() == TagGroup.GAIN) {
                if (subject.contains(t.getName())) subjectOverlap++;
            } else if (tendency.contains(t.getName())) tendencyOverlap++;
        }
        return (tendencyOverlap + 4.0 * subjectOverlap) / 11.0;
    }
}
