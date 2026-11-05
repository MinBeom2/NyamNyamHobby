package com.ssafy.nyamnyam.domain.notification.scheduler;

import com.ssafy.nyamnyam.domain.notification.service.NewFeedNotifier;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.domain.notification.service.WeeklyTodoNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 알림 배치: 주차 변경·새 피드 알림(매일 09:00)과 90일 지난 알림 정리(매일 04:30). 시각은 서울 기준 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final WeeklyTodoNotifier weeklyTodoNotifier;
    private final NewFeedNotifier newFeedNotifier;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.notification.weekly-todo-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void weeklyTodo() {
        try {
            weeklyTodoNotifier.notifyWeekChanges();
        } catch (RuntimeException e) {
            log.error("[WEEKLY_TODO] failed", e);
        }
    }

    /** 내 취미·관심 취미의 새 정보 피드, 하루 1건 묶음(수집 05:00·05:30, 분류 06:00 뒤) */
    @Scheduled(cron = "${app.notification.new-feed-cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void newFeed() {
        try {
            newFeedNotifier.notifyNewFeeds();
        } catch (RuntimeException e) {
            log.error("[NEW_FEED] failed", e);
        }
    }

    @Scheduled(cron = "${app.notification.purge-cron:0 30 4 * * *}", zone = "Asia/Seoul")
    public void purgeExpired() {
        try {
            log.info("[NOTIFICATION_PURGE] deleted={}", notificationService.purgeExpired());
        } catch (RuntimeException e) {
            log.error("[NOTIFICATION_PURGE] failed", e);
        }
    }
}
