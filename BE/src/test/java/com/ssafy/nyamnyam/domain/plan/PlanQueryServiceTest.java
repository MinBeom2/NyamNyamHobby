package com.ssafy.nyamnyam.domain.plan;

import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.plan.repository.*;
import com.ssafy.nyamnyam.domain.plan.service.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.error.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static com.ssafy.nyamnyam.domain.plan.PlanTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanQueryServiceTest {
    @Mock UserPlanRepository plans;
    @Mock PlanTemplateRepository templates;
    @Mock UserHobbyRepository hobbies;
    PlanQueryService service;
    UserHobby entry;
    UserPlan plan;
    @BeforeEach void setup() {
        entry = entry(user(1), hobby(3), 12);
        plan = plan(entry);
        service = new PlanQueryService(plans, templates, hobbies,
                Clock.fixed(START.plusDays(8).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    @Test void defaultWeekAggregatesOverdueSuppliesAndSeparateProgressScopes() {
        var first = plan.getItems().iterator().next();
        first.changeStatus(ItemStatus.DONE, START);
        var view = service.view(plan, null, false, service.now());
        assertThat(view.currentWeek()).isEqualTo(2);
        assertThat(view.selectedWeek()).isEqualTo(2);
        assertThat(view.weekTitle()).isEqualTo("제목2");
        assertThat(view.planProgress().done()).isEqualTo(1);
        assertThat(view.weekProgress().done()).isZero();
        assertThat(view.weekExpectedMinutes()).isEqualTo(30);
        assertThat(view.supplies()).containsExactly("연필", "종이");
        assertThat(view.startedAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(view.startedAt().getDayOfMonth()).isEqualTo(2);
        assertThat(view.weeks()).extracting(w -> w.status()).containsExactly("PAST", "CURRENT", "FUTURE");
        assertThat(view.items()).hasSize(1);
    }

    @Test void overviewShowsAllWeeksAndOverdueDetailsAndFuturePreviewUsesUserCopy() {
        var view = service.view(plan, null, true, service.now());
        assertThat(view.weeks()).hasSize(3);
        assertThat(view.overdueItems()).hasSize(1);
        assertThat(view.overdueItems().get(0).itemId()).isEqualTo(901);
        assertThat(view.overdueItems().get(0).weekNo()).isEqualTo(1);
        assertThat(view.selectedWeek()).isNull();
        assertThat(view.canUpdateItems()).isFalse();
        var future = service.view(plan, 3, false, service.now());
        assertThat(future.items().get(0).itemId()).isEqualTo(903);
        assertThat(future.canUpdateItems()).isFalse();
        assertThat(future.canEditPlan()).isTrue();
        assertThat(future.nextWeek()).isNull();
        assertThat(entry.getStartedAt()).isEqualTo(START);
        assertThat(future.currentWeek()).isEqualTo(2);
    }

    @Test void skippedDoesNotAlsoAppearInOverdueAndAllSkippedIsProcessedWithZeroDoneRate() {
        plan.getItems().forEach(i -> i.changeStatus(ItemStatus.SKIPPED, START));
        var view = service.view(plan, 2, false, service.now());
        assertThat(view.overdueItems()).isEmpty();
        assertThat(view.skippedItems()).hasSize(3);
        assertThat(view.weekCompleted()).isTrue();
        assertThat(view.planCompleted()).isTrue();
        assertThat(view.planProgress().rate()).isZero();
        assertThat(entry.getStatus()).isEqualTo(UserHobbyStatus.IN_PROGRESS);
    }

    @Test void emptyPlanAndEmptyWeekAreNotCompletedAndHaveZeroRatio() {
        plan.getItems().forEach(i -> i.delete(START));
        var view = service.view(plan, 1, false, service.now());
        assertThat(view.planProgress().total()).isZero();
        assertThat(view.planProgress().rate()).isZero();
        assertThat(view.weekCompleted()).isFalse();
        assertThat(view.planCompleted()).isFalse();
        assertThat(view.weekExpectedMinutes()).isZero();
        assertThat(view.supplies()).isEmpty();
    }

    @Test void rejectsUnknownPlanForeignOwnerAndInvalidWeek() {
        assertThatThrownBy(() -> service.get(1L, 55L, null, false)).isInstanceOfSatisfying(CustomException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND));
        when(plans.findById(55L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.get(2L, 55L, null, false)).isInstanceOfSatisfying(CustomException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMMON_FORBIDDEN));
        for (int week : new int[]{0, -1, 4})
            assertThatThrownBy(() -> service.get(1L, 55L, week, false)).isInstanceOfSatisfying(CustomException.class,
                    ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT));
    }

    @Test void homeExcludesPausedPlansAndOverdueFromRemainingCountAndIncludesAllStatusCounts() {
        var paused = entry(user(1), hobby(4), 13); paused.pause(START);
        var pausedPlan = id(plan(paused), 56);
        when(hobbies.findAllByUserIdWithHobby(1L)).thenReturn(List.of(entry, paused));
        when(plans.findByUserHobbyUserIdAndStatusOrderByStartedAtDescIdDesc(1L, "ACTIVE"))
                .thenReturn(List.of(plan, pausedPlan));
        when(templates.findByHobbyIdInAndStatus(any(), eq("PUBLISHED"))).thenReturn(List.of(template(3)));
        var summary = service.summary(1L);
        assertThat(summary.inProgressCount()).isEqualTo(1);
        assertThat(summary.maxInProgressCount()).isEqualTo(3);
        assertThat(summary.remainingThisWeekCount()).isEqualTo(1);
        assertThat(summary.overdueCount()).isEqualTo(1);
        assertThat(summary.statusCounts()).containsEntry("PAUSED", 1L).containsEntry("STOPPED", 0L);
        assertThat(summary.plans()).hasSize(1);
        var card = summary.plans().get(0);
        assertThat(card.getPlanId()).isEqualTo(55);
        assertThat(card.getTotalWeeks()).isEqualTo(3);
        assertThat(card.getCurrentWeek()).isEqualTo(2);
        assertThat(card.getAvailableSkillLevels()).containsExactly("BEGINNER");
        assertThat(card.isPlanReady()).isTrue();
    }

    @Test void emptySummaryHasZeroCountsAndNoPlans() {
        var summary = service.summary(1L);
        assertThat(summary.remainingThisWeekCount()).isZero();
        assertThat(summary.inProgressCount()).isZero();
        assertThat(summary.plans()).isEmpty();
        verifyNoInteractions(plans, templates);
    }
}
