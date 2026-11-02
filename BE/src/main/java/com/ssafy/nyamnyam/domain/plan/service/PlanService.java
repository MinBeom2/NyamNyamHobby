package com.ssafy.nyamnyam.domain.plan.service;

import com.ssafy.nyamnyam.domain.notification.event.PlanStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.plan.dto.*;
import com.ssafy.nyamnyam.domain.plan.dto.PlanResponses.*;
import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.plan.repository.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.*;
import com.ssafy.nyamnyam.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanService {
    private final UserRepository users;
    private final UserHobbyRepository hobbies;
    private final HobbyRepository catalog;
    private final PlanTemplateRepository templates;
    private final UserPlanRepository plans;
    private final PlanQueryService query;
    private final ApplicationEventPublisher events;

    /** All plan mutations acquire this same lock BEFORE loading hobby/plan state. */
    private User lockUser(Long userId) {
        return users.findByIdForUpdate(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkLimit(Long userId) {
        if (hobbies.countByUserIdAndStatus(userId, UserHobbyStatus.IN_PROGRESS) >= PlanQueryService.MAX_IN_PROGRESS)
            throw new CustomException(ErrorCode.MY_HOBBY_LIMIT);
    }

    public Started start(Long userId, Long hobbyId, StartPlanRequest request) {
        var user = lockUser(userId);
        var hobby = catalog.findById(hobbyId).filter(h -> h.getStatus() == HobbyStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
        var entry = hobbies.findByUserIdAndHobbyId(userId, hobbyId).orElse(null);
        if (entry != null && entry.getStatus() != UserHobbyStatus.SAVED) {
            throw new CustomException(entry.getStatus() == UserHobbyStatus.IN_PROGRESS
                    || entry.getStatus() == UserHobbyStatus.PAUSED ? ErrorCode.MY_HOBBY_IN_PROGRESS
                    : ErrorCode.MY_HOBBY_INVALID_TRANSITION);
        }
        checkLimit(userId);
        var template = templates.findByHobbyIdAndSkillLevelAndStatus(hobbyId, request.skillLevel(), "PUBLISHED")
                .orElseThrow(() -> new CustomException(ErrorCode.MY_PLAN_NOT_READY));
        validateTemplate(template);
        LocalDateTime now = query.now();
        if (entry == null) {
            entry = hobbies.save(UserHobby.builder().user(user).hobby(hobby).savedAt(now).build());
        } else {
            catalog.adjustSaveCount(hobbyId, -1);
        }
        entry.start(request.skillLevel(), now);
        var plan = plans.saveAndFlush(new UserPlan(entry, template, now));
        events.publishEvent(new PlanStartedEvent(plan.getId())); // 커밋 뒤 1주차 알림(notification 도메인)
        return new Started(entry.getId(), plan.getId(), entry.getStatus().name(), plan.getTitle());
    }

    private void validateTemplate(PlanTemplate template) {
        if (template.getTotalWeeks() < 1 || template.getItems().isEmpty()
                || template.getWeekTitles().size() != template.getTotalWeeks()) notReady();
        for (int week = 1; week <= template.getTotalWeeks(); week++) {
            if (template.getWeekTitles().get((short) week) == null || template.getWeekTitles().get((short) week).isBlank()) notReady();
        }
        if (template.getItems().stream().anyMatch(i -> i.getWeekNo() < 1 || i.getWeekNo() > template.getTotalWeeks()
                || i.getSeq() < 1 || i.getExpectedMinutes() < 1 || i.getExpectedMinutes() > 1440)) notReady();
        long distinctPositions = template.getItems().stream().map(i -> i.getWeekNo() + ":" + i.getSeq()).distinct().count();
        if (distinctPositions != template.getItems().size()) notReady();
    }

    private void notReady() { throw new CustomException(ErrorCode.MY_PLAN_NOT_READY); }

    public HobbyState changeStatus(Long userId, Long hobbyId, HobbyStatusRequest request) {
        lockUser(userId);
        var entry = hobbies.findByUserIdAndHobbyId(userId, hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        var target = UserHobbyStatus.valueOf(request.status());
        var previous = entry.getStatus();
        var plan = plans.findByUserHobbyUserIdAndStatusOrderByStartedAtDescIdDesc(userId, "ACTIVE").stream()
                .filter(p -> p.getUserHobby().getId().equals(entry.getId())).findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MY_HOBBY_INVALID_TRANSITION));
        var now = query.now();
        if (previous != target) {
            if (previous == UserHobbyStatus.IN_PROGRESS && target == UserHobbyStatus.PAUSED) entry.pause(now);
            else if (previous == UserHobbyStatus.PAUSED && target == UserHobbyStatus.IN_PROGRESS) {
                checkLimit(userId);
                entry.resume(now);
            } else if ((previous == UserHobbyStatus.PAUSED || previous == UserHobbyStatus.IN_PROGRESS)
                    && target == UserHobbyStatus.STOPPED) entry.stop(now);
            else throw new CustomException(ErrorCode.MY_HOBBY_INVALID_TRANSITION);
        }
        return new HobbyState(entry.getId(), plan.getId(), entry.getStatus().name(),
                entry.currentWeek(plan.getTotalWeeks(), now), PlanResponses.time(plan.getStartedAt()),
                PlanResponses.time(entry.getPausedAt()));
    }

    private UserPlan mutablePlan(Long userId, Long planId) {
        lockUser(userId);
        var plan = query.owned(userId, planId);
        if (!PlanQueryService.editable(plan)) throw new CustomException(ErrorCode.TODO_PLAN_NOT_ACTIVE);
        return plan;
    }

    private UserPlanItem item(UserPlan plan, Long itemId, boolean allowDeleted) {
        return plan.getItems().stream().filter(i -> i.getId().equals(itemId))
                .filter(i -> allowDeleted || i.getDeletedAt() == null).findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
    }

    public Mutation update(Long userId, Long planId, Long itemId, UpdatePlanItemRequest request) {
        var plan = mutablePlan(userId, planId);
        var item = item(plan, itemId, false);
        var now = query.now();
        int current = plan.getUserHobby().currentWeek(plan.getTotalWeeks(), now);
        int destination = request.getWeekNo() == null ? item.getWeekNo() : request.getWeekNo();
        PlanQueryService.validateWeek(plan, destination);
        // A combined move+completion cannot bypass the original or destination future-week rule.
        if ((request.isStatusPresent() || request.isMemoPresent())
                && (item.getWeekNo() > current || destination > current))
            throw new CustomException(ErrorCode.TODO_FUTURE_WEEK);
        if (destination != item.getWeekNo()) {
            var oldWeek = item.getWeekNo();
            item.move((short) destination, nextSeq(plan, destination), now);
            renumber(plan, oldWeek, now);
        }
        if (request.getExpectedMinutes() != null) item.changeExpectedMinutes(request.getExpectedMinutes().shortValue(), now);
        if (request.isStatusPresent()) item.changeStatus(request.getStatus(), now);
        if (request.isMemoPresent()) item.changeMemo(request.getMemo(), now);
        return mutation(plan, item, now);
    }

    public Mutation create(Long userId, Long planId, CreatePlanItemRequest request) {
        var plan = mutablePlan(userId, planId);
        PlanQueryService.validateWeek(plan, request.weekNo());
        var now = query.now();
        var item = new UserPlanItem(plan, request.weekNo().shortValue(), nextSeq(plan, request.weekNo()),
                request.title().trim(), request.description(), request.expectedMinutes().shortValue(),
                request.supplies(), request.doneCriteria(), now);
        plan.addItem(item);
        plans.saveAndFlush(plan);
        return mutation(plan, item, now);
    }

    public void delete(Long userId, Long planId, Long itemId) {
        var plan = mutablePlan(userId, planId);
        var item = item(plan, itemId, true);
        if (item.getDeletedAt() != null) return;
        var now = query.now();
        item.delete(now);
        renumber(plan, item.getWeekNo(), now);
    }

    private short nextSeq(UserPlan plan, int week) {
        int size = (int) plan.visibleItems().stream().filter(i -> i.getWeekNo() == week).count();
        if (size >= Short.MAX_VALUE) throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "주차 활동 수가 너무 많습니다.");
        int max = plan.visibleItems().stream().filter(i -> i.getWeekNo() == week).mapToInt(UserPlanItem::getSeq).max().orElse(0);
        if (max >= Short.MAX_VALUE) {
            renumber(plan, week, query.now());
            return (short) (size + 1);
        }
        return (short) (max + 1);
    }

    private void renumber(UserPlan plan, int week, LocalDateTime now) {
        short seq = 1;
        for (var entry : plan.visibleItems()) if (entry.getWeekNo() == week) entry.move((short) week, seq++, now);
    }

    private Mutation mutation(UserPlan plan, UserPlanItem item, LocalDateTime now) {
        var view = query.view(plan, (int) item.getWeekNo(), false, now);
        return new Mutation(item.getId(), item.getStatus().name(), item.getMemo(), PlanResponses.time(item.getCompletedAt()),
                Item.from(item), view.planProgress(), view.planProgress(), view.weekProgress(), view.weekCompleted(),
                view.nextWeek(), view.planCompleted());
    }
}
