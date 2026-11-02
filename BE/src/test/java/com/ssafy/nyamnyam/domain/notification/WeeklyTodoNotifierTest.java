package com.ssafy.nyamnyam.domain.notification;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.repository.WeeklyTodoTargetRepository;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.domain.notification.service.WeeklyTodoNotifier;
import com.ssafy.nyamnyam.domain.plan.entity.UserPlan;
import com.ssafy.nyamnyam.domain.plan.repository.UserPlanRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.ssafy.nyamnyam.domain.plan.PlanTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 주차 변경 알림. PlanTestFixture의 계획은 2026-10-01 15:00 UTC(서울 10/2) 시작, 3주, 주차마다 활동 1개.
 * 서울 날짜 기준 7일이 지나면 2주차다. 1주차 알림은 배치가 아니라 시작할 때 보낸다.
 */
class WeeklyTodoNotifierTest {

    private final WeeklyTodoTargetRepository targets = mock(WeeklyTodoTargetRepository.class);
    private final UserPlanRepository plans = mock(UserPlanRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);

    private WeeklyTodoNotifier notifierAt(LocalDateTime utcNow) {
        return new WeeklyTodoNotifier(targets, plans, notifications, Clock.fixed(utcNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    /** 사용자 7, 취미 3, 계획 55(PlanTestFixture) */
    private static UserPlan activePlan() {
        return plan(entry(user(7), hobby(3), 12));
    }

    @Test
    @DisplayName("1주차에는 알리지 않는다")
    void firstWeekIsSilent() {
        given(targets.findActivePlans(UserHobbyStatus.IN_PROGRESS)).willReturn(List.of(activePlan()));

        assertThat(notifierAt(START.plusDays(6)).notifyWeekChanges()).isZero();
        verifyNoInteractions(notifications);
    }

    @Test
    @DisplayName("서울 날짜로 7일이 지나면 2주차 알림: 사용자·제목·주차 제목·활동 수·링크·중복키")
    void secondWeekNotifies() {
        given(targets.findActivePlans(UserHobbyStatus.IN_PROGRESS)).willReturn(List.of(activePlan()));
        given(notifications.create(any())).willReturn(true);

        int created = notifierAt(START.plusDays(7)).notifyWeekChanges();

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notifications).create(captor.capture());
        NotificationMessage m = captor.getValue();
        assertThat(created).isEqualTo(1);
        assertThat(m.userId()).isEqualTo(7L);
        assertThat(m.type()).isEqualTo(NotificationType.WEEKLY_TODO);
        assertThat(m.title()).isEqualTo("취미3 2주차가 시작됐어요");
        assertThat(m.body()).isEqualTo("이번 주 '제목2' 활동 1개를 확인해 보세요.");
        assertThat(m.linkPath()).isEqualTo("/my-hobbies/3/plan");
        assertThat(m.dedupKey()).isEqualTo("WEEKLY_TODO:55:2");
    }

    @Test
    @DisplayName("이미 보낸 주차면(중복키 충돌) 새로 만든 수에 세지 않는다")
    void alreadySentWeekIsNotCounted() {
        given(targets.findActivePlans(UserHobbyStatus.IN_PROGRESS)).willReturn(List.of(activePlan()));
        given(notifications.create(any())).willReturn(false);

        assertThat(notifierAt(START.plusDays(8)).notifyWeekChanges()).isZero();
    }

    @Test
    @DisplayName("마지막 주차를 지나도 총 주차(3주차)로 고정되어 같은 키가 된다")
    void afterLastWeekStaysAtLastWeek() {
        given(targets.findActivePlans(UserHobbyStatus.IN_PROGRESS)).willReturn(List.of(activePlan()));
        given(notifications.create(any())).willReturn(true);

        notifierAt(START.plusDays(40)).notifyWeekChanges();

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notifications).create(captor.capture());
        assertThat(captor.getValue().dedupKey()).isEqualTo("WEEKLY_TODO:55:3");
    }

    @Test
    @DisplayName("계획을 시작하면 바로 1주차 알림: 같은 키 형식(:1)이라 배치와 겹치지 않는다")
    void planStartedSendsFirstWeek() {
        given(plans.findById(55L)).willReturn(java.util.Optional.of(activePlan()));
        given(notifications.create(any())).willReturn(true);

        assertThat(notifierAt(START).notifyPlanStarted(55L)).isTrue();

        ArgumentCaptor<NotificationMessage> captor = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notifications).create(captor.capture());
        NotificationMessage m = captor.getValue();
        assertThat(m.title()).isEqualTo("취미3 1주차가 시작됐어요");
        assertThat(m.body()).isEqualTo("이번 주 '제목1' 활동 1개를 확인해 보세요.");
        assertThat(m.dedupKey()).isEqualTo("WEEKLY_TODO:55:1");
    }

    @Test
    @DisplayName("시작 알림: 계획을 못 찾으면 아무것도 하지 않는다")
    void planStartedMissingPlan() {
        given(plans.findById(99L)).willReturn(java.util.Optional.empty());

        assertThat(notifierAt(START).notifyPlanStarted(99L)).isFalse();
        verifyNoInteractions(notifications);
    }
}
