package com.ssafy.nyamnyam.domain.notification.event;

import com.ssafy.nyamnyam.domain.notification.service.WeeklyTodoNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 계획 시작이 커밋된 뒤 1주차 알림을 바로 저장한다. 알림 저장이 실패해도 시작하기 응답에는 영향을 주지 않는다.
 * 키(WEEKLY_TODO:{계획}:1)가 주차 알림과 같아서 같은 계획에 1주차 알림이 두 번 가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanStartedNotificationListener {

    private final WeeklyTodoNotifier weeklyTodoNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlanStarted(PlanStartedEvent event) {
        try {
            weeklyTodoNotifier.notifyPlanStarted(event.planId());
        } catch (RuntimeException e) {
            log.warn("[WEEKLY_TODO] start notification failed: planId={}, error={}",
                    event.planId(), e.getClass().getSimpleName());
        }
    }
}
