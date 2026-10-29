package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService.Profile;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class ExplorationSelectionPolicy {
    public List<Short> order(List<Short> pool, Profile profile, boolean disperse, Set<Long> seenCategories, double preferRatio) {
        if (pool.isEmpty()) return new ArrayList<>();
        Set<Long> preferredHobbies = profile.hobbyScores().entrySet().stream().filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(3).map(Map.Entry::getKey).collect(Collectors.toSet());
        List<Short> preferred = pool.stream().filter(s -> s.getHobby() != null && preferredHobbies.contains(s.getHobby().getId())).toList();
        List<Short> focus;
        if (disperse) {
            focus = pool.stream().filter(s -> !seenCategories.contains(category(s))).toList();
            if (focus.isEmpty()) focus = pool;
        } else if (!preferred.isEmpty() && prefer(preferRatio)) {
            focus = preferred;
        } else {
            long leastViewed = pool.stream().mapToLong(s -> profile.categoryViews().getOrDefault(category(s), 0L)).min().orElse(0);
            focus = pool.stream().filter(s -> profile.categoryViews().getOrDefault(category(s), 0L) == leastViewed).toList();
        }
        Map<Long, List<Short>> groups = focus.stream().collect(Collectors.groupingBy(this::category, LinkedHashMap::new, Collectors.toList()));
        List<List<Short>> categories = new ArrayList<>(groups.values());
        List<Short> group = categories.get(index(categories.size()));
        Short first = group.get(index(group.size()));
        List<Short> result = new ArrayList<>(pool);
        result.remove(first);
        Collections.shuffle(result);
        result.add(0, first);
        return result;
    }
    protected boolean prefer(double ratio) { return ThreadLocalRandom.current().nextDouble() < ratio; }
    protected int index(int bound) { return ThreadLocalRandom.current().nextInt(bound); }
    private Long category(Short s) { return s.getHobby() == null || s.getHobby().getCategory() == null ? -1L : s.getHobby().getCategory().getId(); }
}
