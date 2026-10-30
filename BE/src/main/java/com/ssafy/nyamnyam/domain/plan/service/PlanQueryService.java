package com.ssafy.nyamnyam.domain.plan.service;

import com.ssafy.nyamnyam.domain.plan.dto.PlanResponses.*;
import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.plan.repository.*;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class PlanQueryService {
    public static final int MAX_IN_PROGRESS = 3;
    private final UserPlanRepository plans;
    private final PlanTemplateRepository templates;
    private final UserHobbyRepository hobbies;
    private final Clock planClock;

    public LocalDateTime now() { return LocalDateTime.now(planClock); }

    public UserPlan owned(Long userId, Long planId) {
        var plan = plans.findById(planId).orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        if (!plan.getUserHobby().getUser().getId().equals(userId))
            throw new CustomException(ErrorCode.COMMON_FORBIDDEN);
        return plan;
    }

    public View get(Long userId, Long planId, Integer week, boolean overview) {
        var plan = owned(userId, planId);
        return view(plan, week, overview, now());
    }

    public static Progress progress(List<UserPlanItem> items) {
        int done = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.DONE).count();
        int skipped = (int) items.stream().filter(i -> i.getStatus() == ItemStatus.SKIPPED).count();
        return new Progress(done, skipped, items.size(), items.isEmpty() ? 0.0 : (double) done / items.size());
    }

    public static void validateWeek(UserPlan plan, int week) {
        if (week < 1 || week > plan.getTotalWeeks())
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "계획의 주차 범위를 확인해 주세요.");
    }

    public static boolean editable(UserPlan plan) {
        return "ACTIVE".equals(plan.getStatus()) && plan.getUserHobby().getStatus() == UserHobbyStatus.IN_PROGRESS;
    }

    public View view(UserPlan plan, Integer requested, boolean overview, LocalDateTime now) {
        int current = plan.getUserHobby().currentWeek(plan.getTotalWeeks(), now);
        int selected = requested == null ? current : requested;
        validateWeek(plan, selected);
        var all = plan.visibleItems();
        var selectedItems = all.stream().filter(i -> i.getWeekNo() == selected).toList();
        var overdue = all.stream().filter(i -> i.getWeekNo() < current && i.getStatus() == ItemStatus.TODO).toList();
        var skipped = all.stream().filter(i -> i.getStatus() == ItemStatus.SKIPPED).map(Item::from).toList();
        var planProgress = progress(all);
        var weekProgress = progress(selectedItems);
        var weeks = new ArrayList<Week>();
        for (int week = 1; week <= plan.getTotalWeeks(); week++) {
            final int number = week;
            var entries = all.stream().filter(i -> i.getWeekNo() == number).toList();
            var stats = progress(entries);
            int late = week < current ? (int) entries.stream().filter(i -> i.getStatus() == ItemStatus.TODO).count() : 0;
            weeks.add(new Week(week, plan.getWeekTitles().get((short) week),
                    week < current ? "PAST" : week == current ? "CURRENT" : "FUTURE", week == current,
                    stats, minutes(entries), late, stats.completed(), editable(plan) && week <= current, editable(plan)));
        }
        // Strings are display labels only. Check state stays in the frontend; no fabricated persistent IDs.
        var supplies = selectedItems.stream().map(UserPlanItem::getSupplies).filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split("[,，\\n]"))).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().toList();
        var hobby = plan.getUserHobby();
        return new View(plan.getId(), hobby.getId(), hobby.getHobby().getId(), plan.getTitle(), hobby.getStatus().name(),
                plan.getStatus(), hobby.getSkillLevel(), com.ssafy.nyamnyam.domain.plan.dto.PlanResponses.time(plan.getStartedAt()),
                plan.getTotalWeeks(), current, overview ? null : selected, overview ? null : plan.getWeekTitles().get((short) selected),
                overview ? 0 : minutes(selectedItems), planProgress, planProgress, overview ? null : weekProgress,
                overdue.size(), editable(plan), !overview && editable(plan) && selected <= current,
                !overview && weekProgress.completed(), !overview && selected < plan.getTotalWeeks() ? selected + 1 : null,
                planProgress.completed(), weeks, overview ? List.of() : selectedItems.stream().map(Item::from).toList(),
                overdue.stream().map(Item::from).toList(), skipped, overview ? List.of() : supplies);
    }

    private static int minutes(List<UserPlanItem> items) {
        return items.stream().mapToInt(UserPlanItem::getExpectedMinutes).sum();
    }

    /** Bulk load current plans and template options, retaining the existing list response shape. */
    public List<UserHobbyResponse> enrich(Long userId, List<UserHobby> entries) {
        if (entries.isEmpty()) return List.of();
        var currentPlans = plans.findByUserHobbyUserIdAndStatusOrderByStartedAtDescIdDesc(userId, "ACTIVE").stream()
                .collect(Collectors.toMap(p -> p.getUserHobby().getId(), Function.identity()));
        var options = templates.findByHobbyIdInAndStatus(entries.stream().map(e -> e.getHobby().getId()).toList(), "PUBLISHED")
                .stream().collect(Collectors.groupingBy(PlanTemplate::getHobbyId,
                        Collectors.mapping(PlanTemplate::getSkillLevel, Collectors.toList())));
        LocalDateTime now = now();
        return entries.stream().map(entry -> {
            var response = UserHobbyResponse.from(entry);
            response.setPlanOptions(options.getOrDefault(entry.getHobby().getId(), List.of()).stream().sorted().toList());
            var plan = currentPlans.get(entry.getId());
            if (plan != null) response.attachPlan(view(plan, null, false, now));
            return response;
        }).toList();
    }

    public Summary summary(Long userId) {
        var entries = hobbies.findAllByUserIdWithHobby(userId);
        var cards = enrich(userId, entries);
        var active = cards.stream().filter(c -> "IN_PROGRESS".equals(c.getStatus()) && c.getPlanId() != null).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var status : UserHobbyStatus.values())
            counts.put(status.name(), entries.stream().filter(h -> h.getStatus() == status).count());
        return new Summary(active.stream().mapToInt(UserHobbyResponse::getRemainingThisWeekCount).sum(),
                active.stream().mapToInt(UserHobbyResponse::getOverdueCount).sum(),
                counts.get("IN_PROGRESS").intValue(), MAX_IN_PROGRESS, counts, active);
    }
}
