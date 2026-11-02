package com.ssafy.nyamnyam.domain.notification.event;

/**
 * 사용자가 취미 계획을 시작했다는 사실. PlanService.start가 트랜잭션 안에서 발행하고,
 * {@link PlanStartedNotificationListener}가 커밋 뒤에 받아 1주차 알림을 만든다.
 */
public record PlanStartedEvent(Long planId) {
}
