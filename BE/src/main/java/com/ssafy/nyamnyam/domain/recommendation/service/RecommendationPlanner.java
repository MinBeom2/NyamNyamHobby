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
        // 점수가 양수인 취미가 먼저, 3개가 안 되면 좋아요를 받은 취미로 점수 순으로 채운다.
        // 좋아요는 확실한 호감 표시라 다른 영상을 넘기느라 점수가 깎였어도 추천 후보로 남긴다.
        List<Choice> interests = eligible.stream()
                .filter(h -> profile.hobbyScores().getOrDefault(h.getId(), 0.0) > 0
                        || profile.likes().getOrDefault(h.getId(), 0L) > 0)
                .map(h -> choice(h, RecoItemType.INTEREST, profile.hobbyScores().getOrDefault(h.getId(), 0.0), condition,
                        profile.likes().getOrDefault(h.getId(), 0L) > 0
                                ? h.getName() + " 영상에 좋아요 " + profile.likes().get(h.getId()) + "회 남겼어요"
                                : h.getName() + " 영상을 관심 있게 시청했어요"))
                .sorted(Comparator.comparing((Choice c) -> profile.hobbyScores().getOrDefault(c.hobby().getId(), 0.0) <= 0)
                        .thenComparing(order()))
                .limit(3).toList();
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

    /**
     * 직접 선택 추천. 고른 취미는 이미 관심이 확인됐으니 '취향'이 아니라 '지금 이용 상황으로 시작하기 좋은지'로 순서를 매겨 3개,
     * 고른 취미 전체와 태그가 많이 겹치는(고르지 않은) 취미 3개를 붙인다. 동점이면 먼저 고른 취미가 앞선다.
     *
     * @param selected 사용자가 고른 순서 그대로의 공개 취미
     * @param excluded 유사 취미 후보에서 뺄 취미(싫어요·진행 중). 사용자가 직접 고른 취미에는 적용하지 않는다
     */
    public Plan planSelection(List<Hobby> selected, List<Hobby> hobbies, Set<Long> excluded, UserCondition condition) {
        Map<Long, Integer> pickOrder = new HashMap<>();
        for (int i = 0; i < selected.size(); i++) pickOrder.putIfAbsent(selected.get(i).getId(), i);
        List<Choice> interests = selected.stream()
                .map(h -> choice(h, RecoItemType.INTEREST, 1.0, condition, fitReason(h, condition)))
                .sorted(Comparator.comparingDouble(Choice::score).reversed()
                        .thenComparing(c -> pickOrder.get(c.hobby().getId())))
                .limit(3).toList();
        Set<Long> selectedIds = pickOrder.keySet();
        List<Choice> related = hobbies.stream()
                .filter(h -> !selectedIds.contains(h.getId()) && !excluded.contains(h.getId()))
                .map(h -> {
                    Hobby closest = selected.stream().max(Comparator.comparingDouble(s -> calculateTagSimilarity(s, h))).orElseThrow();
                    double similarity = selected.stream().mapToDouble(s -> calculateTagSimilarity(s, h)).average().orElse(0) * 0.5
                            + calculateTagSimilarity(closest, h) * 0.5;
                    return choice(h, RecoItemType.RELATED, similarity, condition,
                            "직접 고른 " + closest.getName() + "와(과) 성향이 비슷한 취미예요");
                })
                .filter(c -> c.score() > 0)
                .sorted(order()).limit(3).toList();
        List<Choice> result = new ArrayList<>(interests);
        result.addAll(related);
        return new Plan(false, result);
    }

    /** 직접 고른 취미가 이용 상황과 맞는지 한 문장으로. 예산·시간을 넘으면 그 사실을 먼저 알린다 */
    static String fitReason(Hobby h, UserCondition c) {
        if (c == null) return "직접 고른 취미예요";
        List<String> fits = new ArrayList<>();
        if (c.getMonthlyBudget() != null) {
            if (h.getMonthlyCostMin() > c.getMonthlyBudget())
                return "월 예산보다 비용이 들 수 있어요 (월 " + won(h.getMonthlyCostMin()) + "부터)";
            fits.add("월 예산 " + won(c.getMonthlyBudget()) + " 안에서 시작할 수 있어요");
        }
        if (c.getWeeklyHours() != null) {
            if (h.getSessionMinMinutes() > c.getWeeklyHours() * 60)
                return "한 번에 " + h.getSessionMinMinutes() + "분 이상 필요해 주당 시간보다 길어요";
            if (fits.isEmpty()) fits.add("주 " + c.getWeeklyHours() + "시간 안에서 즐길 수 있어요");
        }
        if (c.getPlacePref() != null && !"ANY".equals(c.getPlacePref())
                && (c.getPlacePref().equals(h.getPlaceType().name()) || h.getPlaceType() == PlaceType.BOTH))
            fits.add(("INDOOR".equals(c.getPlacePref()) ? "실내" : "실외") + " 선호와 맞아요");
        return fits.isEmpty() ? "직접 고른 취미예요" : String.join(" · ", fits.subList(0, Math.min(2, fits.size())));
    }
    private static String won(int amount) {
        return amount % 10000 == 0 ? (amount / 10000) + "만 원" : String.format("%,d원", amount);
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
