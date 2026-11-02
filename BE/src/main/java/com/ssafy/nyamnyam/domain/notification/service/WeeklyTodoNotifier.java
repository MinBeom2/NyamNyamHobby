package com.ssafy.nyamnyam.domain.notification.service;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.repository.WeeklyTodoTargetRepository;
import com.ssafy.nyamnyam.domain.plan.entity.UserPlan;
import com.ssafy.nyamnyam.domain.plan.repository.UserPlanRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 주차 시작 알림(FR-NOTI-01 생성 대상 4). 시작일 기준 7일마다 주차가 바뀐다.
 * <ul>
 *   <li>계획을 시작하면 바로 1주차 알림({@link #notifyPlanStarted})</li>
 *   <li>하루 한 번 진행 중 계획을 훑어 현재 주차가 2주차 이상이면 그 주차 알림({@link #notifyWeekChanges})</li>
 * </ul>
 * 키가 WEEKLY_TODO:{계획}:{주차}라 같은 주차는 한 번만 가고, 배치가 며칠 멈췄다 돌아도 현재 주차만 보낸다.
 * 현재 주차 계산은 화면과 같은 {@link UserHobby#currentWeek}(서울 날짜, 일시중지 기간 제외)를 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyTodoNotifier {

    private final WeeklyTodoTargetRepository targetRepository;
    private final UserPlanRepository userPlanRepository;
    private final NotificationService notificationService;
    private final Clock planClock;

    /** 매일 배치. 1주차는 시작할 때 이미 보냈으므로 2주차부터 본다. @return 새로 만든 알림 수 */
    @Transactional(readOnly = true)
    public int notifyWeekChanges() {
        LocalDateTime now = LocalDateTime.now(planClock);
        int created = 0;
        for (UserPlan plan : targetRepository.findActivePlans(UserHobbyStatus.IN_PROGRESS)) {
            int week = plan.getUserHobby().currentWeek(plan.getTotalWeeks(), now);
            if (week >= 2 && notificationService.create(messageFor(plan, week))) {
                created++;
            }
        }
        log.info("[WEEKLY_TODO] notifications created={}", created);
        return created;
    }

    /**
     * 계획 시작 직후 1주차 알림. 시작 트랜잭션이 커밋된 뒤(AFTER_COMMIT) 불리므로 새 트랜잭션에서 읽는다.
     * @return 새로 만들었으면 true
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean notifyPlanStarted(Long planId) {
        return userPlanRepository.findById(planId)
                .map(plan -> notificationService.create(messageFor(plan, 1)))
                .orElse(false);
    }

    static NotificationMessage messageFor(UserPlan plan, int week) {
        UserHobby entry = plan.getUserHobby();
        String hobbyName = entry.getHobby().getName();
        String weekTitle = plan.getWeekTitles().getOrDefault((short) week, week + "주차");
        long todo = plan.visibleItems().stream().filter(i -> i.getWeekNo() == week).count();
        String body = todo > 0
                ? "이번 주 '" + weekTitle + "' 활동 " + todo + "개를 확인해 보세요."
                : "이번 주 '" + weekTitle + "'을(를) 확인해 보세요.";
        return new NotificationMessage(entry.getUser().getId(), NotificationType.WEEKLY_TODO,
                hobbyName + " " + week + "주차가 시작됐어요", body,
                "/my-hobbies/" + entry.getHobby().getId() + "/plan",
                "WEEKLY_TODO:" + plan.getId() + ":" + week);
    }
}
