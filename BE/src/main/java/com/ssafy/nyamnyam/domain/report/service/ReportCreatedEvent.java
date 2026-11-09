package com.ssafy.nyamnyam.domain.report.service;

/** 신고가 저장됨(즉시 알림용) */
public record ReportCreatedEvent(Long reportId) {
}
