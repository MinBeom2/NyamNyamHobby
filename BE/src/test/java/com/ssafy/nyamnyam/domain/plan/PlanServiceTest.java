package com.ssafy.nyamnyam.domain.plan;

import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.plan.dto.*;
import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.plan.repository.*;
import com.ssafy.nyamnyam.domain.plan.service.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.*;
import com.ssafy.nyamnyam.global.error.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static com.ssafy.nyamnyam.domain.plan.PlanTestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {
    @Mock UserRepository users;
    @Mock UserHobbyRepository hobbies;
    @Mock HobbyRepository catalog;
    @Mock PlanTemplateRepository templates;
    @Mock UserPlanRepository plans;
    PlanQueryService query;
    PlanService service;
    User owner;
    HobbyHolder fixture;
    record HobbyHolder(UserHobby entry, UserPlan plan) {}

    @BeforeEach void setup() {
        owner = user(1);
        var entry = entry(owner, hobby(3), 12);
        fixture = new HobbyHolder(entry, plan(entry));
        query = new PlanQueryService(plans, templates, hobbies, Clock.fixed(START.plusDays(7).toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        service = new PlanService(users, hobbies, catalog, templates, plans, query);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(owner));
    }
    void planLookup() { when(plans.findById(55L)).thenReturn(Optional.of(fixture.plan())); }
    void stateLookup() {
        when(hobbies.findByUserIdAndHobbyId(1L, 3L)).thenReturn(Optional.of(fixture.entry()));
        when(plans.findByUserHobbyUserIdAndStatusOrderByStartedAtDescIdDesc(1L, "ACTIVE"))
                .thenReturn(List.of(fixture.plan()));
    }
    void startLookup() {
        when(catalog.findById(3L)).thenReturn(Optional.of(fixture.entry().getHobby()));
        when(hobbies.findByUserIdAndHobbyId(1L, 3L)).thenReturn(Optional.of(fixture.entry()));
    }
    void error(Runnable action, ErrorCode error) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(error));
    }

    @Test void startCopiesTemplateVersionTitlesAndActivities() {
        fixture.entry().updateStatus(UserHobbyStatus.SAVED);
        startLookup();
        var original = template(3);
        when(templates.findByHobbyIdAndSkillLevelAndStatus(3L, "BEGINNER", "PUBLISHED")).thenReturn(Optional.of(original));
        when(plans.saveAndFlush(any())).thenAnswer(inv -> id(inv.getArgument(0), 70));
        var result = service.start(1L, 3L, new StartPlanRequest("BEGINNER"));
        assertThat(result.planId()).isEqualTo(70);
        var capture = ArgumentCaptor.forClass(UserPlan.class);
        verify(plans).saveAndFlush(capture.capture());
        var copy = capture.getValue();
        assertThat(copy.getTemplateVersion()).isEqualTo(2);
        assertThat(copy.getWeekTitles()).containsEntry((short) 2, "제목2");
        assertThat(copy.getItems()).hasSize(3).allMatch(i -> !i.isCustom());
        original.addWeek((short) 2, "수정된 원본");
        copy.getItems().iterator().next().changeMemo("내 메모", query.now());
        assertThat(copy.getWeekTitles().get((short) 2)).isEqualTo("제목2");
        assertThat(original.getItems()).allMatch(i -> i.getTitle().startsWith("활동"));
        var order = inOrder(users, hobbies, plans);
        order.verify(users).findByIdForUpdate(1L);
        order.verify(hobbies).findByUserIdAndHobbyId(1L, 3L);
        order.verify(hobbies).countByUserIdAndStatus(1L, UserHobbyStatus.IN_PROGRESS);
        order.verify(plans).saveAndFlush(any());
    }

    @Test void startRejectsFourthActiveHobbyBeforeCopying() {
        fixture.entry().updateStatus(UserHobbyStatus.SAVED);
        startLookup();
        when(hobbies.countByUserIdAndStatus(1L, UserHobbyStatus.IN_PROGRESS)).thenReturn(3L);
        error(() -> service.start(1L, 3L, new StartPlanRequest("BEGINNER")), ErrorCode.MY_HOBBY_LIMIT);
        verifyNoInteractions(templates, plans);
    }

    @ParameterizedTest @EnumSource(value = UserHobbyStatus.class, names = {"IN_PROGRESS", "PAUSED", "STOPPED", "COMPLETED"})
    void startRejectsDuplicateOrTerminalState(UserHobbyStatus status) {
        fixture.entry().updateStatus(status);
        startLookup();
        assertThatThrownBy(() -> service.start(1L, 3L, new StartPlanRequest("BEGINNER"))).isInstanceOf(CustomException.class);
        verifyNoInteractions(templates, plans);
    }

    @Test void missingOrEmptyTemplateDoesNotChangeSavedState() {
        fixture.entry().updateStatus(UserHobbyStatus.SAVED);
        startLookup();
        error(() -> service.start(1L, 3L, new StartPlanRequest("NOVICE")), ErrorCode.MY_PLAN_NOT_READY);
        when(templates.findByHobbyIdAndSkillLevelAndStatus(3L, "BEGINNER", "PUBLISHED"))
                .thenReturn(Optional.of(new PlanTemplate(3L, "BEGINNER", "빈 계획", (short) 3, 1, "PUBLISHED", START)));
        error(() -> service.start(1L, 3L, new StartPlanRequest("BEGINNER")), ErrorCode.MY_PLAN_NOT_READY);
        assertThat(fixture.entry().getStatus()).isEqualTo(UserHobbyStatus.SAVED);
        verify(plans, never()).saveAndFlush(any());
    }

    @Test void resumeUsesSamePlanAndChecksLimit() {
        fixture.entry().pause(START.plusDays(1));
        stateLookup();
        when(hobbies.countByUserIdAndStatus(1L, UserHobbyStatus.IN_PROGRESS)).thenReturn(3L, 2L);
        error(() -> service.changeStatus(1L, 3L, new HobbyStatusRequest("IN_PROGRESS")), ErrorCode.MY_HOBBY_LIMIT);
        var result = service.changeStatus(1L, 3L, new HobbyStatusRequest("IN_PROGRESS"));
        assertThat(result.planId()).isEqualTo(55);
        assertThat(fixture.entry().getStartedAt()).isEqualTo(START);
        assertThat(fixture.entry().getPausedSeconds()).isEqualTo(6 * 86400);
        verify(plans, never()).saveAndFlush(any());
    }

    @Test void pauseAndStopAreIdempotentAndPreserveActivities() {
        stateLookup();
        var first = service.changeStatus(1L, 3L, new HobbyStatusRequest("PAUSED"));
        var repeated = service.changeStatus(1L, 3L, new HobbyStatusRequest("PAUSED"));
        assertThat(repeated.pausedAt()).isEqualTo(first.pausedAt());
        service.changeStatus(1L, 3L, new HobbyStatusRequest("STOPPED"));
        service.changeStatus(1L, 3L, new HobbyStatusRequest("STOPPED"));
        assertThat(fixture.plan().visibleItems()).hasSize(3);
        error(() -> service.changeStatus(1L, 3L, new HobbyStatusRequest("IN_PROGRESS")), ErrorCode.MY_HOBBY_INVALID_TRANSITION);
    }

    @Test void memoOnlyPatchKeepsStatusAndNullClearsMemo() {
        planLookup();
        var request = new UpdatePlanItemRequest(); request.setMemo("기록");
        var result = service.update(1L, 55L, 902L, request);
        assertThat(result.status()).isEqualTo("TODO");
        assertThat(result.memo()).isEqualTo("기록");
        request.setMemo(null);
        assertThat(service.update(1L, 55L, 902L, request).memo()).isNull();
    }

    @Test void doneRetriesKeepFirstCompletionTimeAndUndoClearsIt() {
        planLookup();
        var request = new UpdatePlanItemRequest(); request.setStatus(ItemStatus.DONE);
        var first = service.update(1L, 55L, 902L, request);
        var completed = fixture.plan().getItems().stream().filter(i -> i.getId() == 902).findFirst().orElseThrow();
        completed.changeStatus(ItemStatus.DONE, START.plusDays(20));
        assertThat(PlanResponses.time(completed.getCompletedAt())).isEqualTo(first.completedAt());
        assertThat(first.weekCompleted()).isTrue();
        assertThat(first.planCompleted()).isFalse();
        assertThat(first.planProgress().rate()).isCloseTo(1.0 / 3, within(0.00001));
        request.setStatus(ItemStatus.SKIPPED);
        var skipped = service.update(1L, 55L, 902L, request);
        assertThat(skipped.completedAt()).isNull();
        assertThat(skipped.weekCompleted()).isTrue();
        assertThat(skipped.weekProgress().rate()).isZero();
        request.setStatus(ItemStatus.TODO);
        assertThat(service.update(1L, 55L, 902L, request).weekCompleted()).isFalse();
    }

    @Test void futureProgressAndMemoAreRejectedEvenWhenCombinedWithMove() {
        planLookup();
        var request = new UpdatePlanItemRequest(); request.setMemo("미래");
        error(() -> service.update(1L, 55L, 903L, request), ErrorCode.TODO_FUTURE_WEEK);
        request.setWeekNo(1);
        error(() -> service.update(1L, 55L, 903L, request), ErrorCode.TODO_FUTURE_WEEK);
        request.setWeekNo(3);
        error(() -> service.update(1L, 55L, 901L, request), ErrorCode.TODO_FUTURE_WEEK);
    }

    @Test void futurePlanEditingIsAllowedAndMovesAppendToDestination() {
        planLookup();
        var request = new UpdatePlanItemRequest(); request.setWeekNo(3); request.setExpectedMinutes(45);
        var result = service.update(1L, 55L, 901L, request);
        assertThat(result.item().weekNo()).isEqualTo(3);
        assertThat(result.item().seq()).isEqualTo(2);
        assertThat(result.item().expectedMinutes()).isEqualTo(45);
        assertThat(fixture.plan().getItems()).hasSize(3);
        var same = service.update(1L, 55L, 901L, request);
        assertThat(same.item().seq()).isEqualTo(2);
    }

    @Test void softDeleteExcludesDoneActivitiesFromAggregatesAndIsIdempotent() {
        planLookup();
        fixture.plan().getItems().iterator().next().changeStatus(ItemStatus.DONE, query.now());
        service.delete(1L, 55L, 901L);
        service.delete(1L, 55L, 901L);
        assertThat(fixture.plan().getItems()).hasSize(3);
        assertThat(fixture.plan().visibleItems()).hasSize(2);
        var view = query.view(fixture.plan(), 1, false, query.now());
        assertThat(view.planProgress().done()).isZero();
        assertThat(view.weekProgress().total()).isZero();
        assertThat(view.weekCompleted()).isFalse();
        error(() -> service.update(1L, 55L, 901L, new UpdatePlanItemRequest()), ErrorCode.COMMON_NOT_FOUND);
    }

    @Test void addingCustomItemReturnsIdentityAndUpdatedWeekProgress() {
        planLookup();
        when(plans.saveAndFlush(any())).thenAnswer(inv -> {
            fixture.plan().getItems().stream().filter(i -> i.getId() == null).forEach(i -> id(i, 950));
            return fixture.plan();
        });
        var result = service.create(1L, 55L, new CreatePlanItemRequest(2, " 새 활동 ", "설명", 15, "종이", "기준"));
        assertThat(result.itemId()).isEqualTo(950);
        assertThat(result.item().isCustom()).isTrue();
        assertThat(result.item().title()).isEqualTo("새 활동");
        assertThat(result.item().seq()).isEqualTo(2);
        assertThat(result.weekProgress().total()).isEqualTo(2);
    }

    @ParameterizedTest @EnumSource(value = UserHobbyStatus.class, names = {"PAUSED", "STOPPED", "COMPLETED"})
    void readonlyHobbiesRejectAllMutations(UserHobbyStatus status) {
        planLookup(); fixture.entry().updateStatus(status);
        error(() -> service.delete(1L, 55L, 901L), ErrorCode.TODO_PLAN_NOT_ACTIVE);
        error(() -> service.create(1L, 55L, new CreatePlanItemRequest(1, "제목", null, 15, null, null)), ErrorCode.TODO_PLAN_NOT_ACTIVE);
        error(() -> service.update(1L, 55L, 901L, new UpdatePlanItemRequest()), ErrorCode.TODO_PLAN_NOT_ACTIVE);
    }

    @Test void archivedPlansOtherOwnersAndCrossPlanItemIdsAreRejected() {
        planLookup();
        error(() -> service.delete(1L, 55L, 10000L), ErrorCode.COMMON_NOT_FOUND);
        ReflectionTestUtils.setField(fixture.plan(), "status", "ARCHIVED");
        error(() -> service.delete(1L, 55L, 901L), ErrorCode.TODO_PLAN_NOT_ACTIVE);
        ReflectionTestUtils.setField(owner, "id", 2L);
        error(() -> service.delete(1L, 55L, 901L), ErrorCode.COMMON_FORBIDDEN);
    }
}
