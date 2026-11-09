package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.report.ReportProperties;
import com.ssafy.nyamnyam.domain.report.dto.ReportCreateRequest;
import com.ssafy.nyamnyam.domain.report.dto.ReportCreateResponse;
import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.repository.ReportRepository;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글·댓글 신고 접수.
 * <ul>
 *   <li>노출 중인 대상만, 본인 글이 아닌 것만, 대상마다 한 번만 신고할 수 있다.</li>
 *   <li>서로 다른 사용자의 처리 전 신고가 기준(기본 3건)에 닿으면 즉시 숨긴다. 최종 판단은 Mattermost에서 한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    static final int DETAIL_MAX_LENGTH = 300;

    private final ReportRepository reportRepository;
    private final ModerationTargets targets;
    private final ReportProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReportCreateResponse create(Long reporterId, ReportCreateRequest request) {
        if (request.targetType() == ReportTargetType.USER) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "게시글과 댓글만 신고할 수 있어요.");
        }
        Target target = targets.find(request.targetType(), request.targetId())
                .filter(Target::isReportable)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "신고할 글이나 댓글을 찾을 수 없어요."));
        if (reporterId.equals(target.authorId())) {
            throw new CustomException(ErrorCode.REPORT_SELF);
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, target.type(), target.id())) {
            throw new CustomException(ErrorCode.REPORT_DUPLICATED);
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(target.type())
                .targetId(target.id())
                .reason(request.reason())
                .detail(normalizeDetail(request.detail()))
                .build();
        try {
            reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자가 거의 동시에 두 번 보낸 경우(uk_report)
            throw new CustomException(ErrorCode.REPORT_DUPLICATED);
        }

        autoHideIfNeeded(target);
        eventPublisher.publishEvent(new ReportCreatedEvent(report.getId()));
        return new ReportCreateResponse(report.getId());
    }

    private void autoHideIfNeeded(Target target) {
        long received = reportRepository.countByTargetTypeAndTargetIdAndStatus(target.type(), target.id(), ReportStatus.RECEIVED);
        if (received >= properties.autoHideThreshold() && targets.hide(target.type(), target.id())) {
            log.info("Report auto-hide: {} #{} ({} reports)", target.type(), target.id(), received);
        }
    }

    /** 줄바꿈 통일·앞뒤 공백 제거. 비었으면 null */
    static String normalizeDetail(String detail) {
        if (detail == null) {
            return null;
        }
        String normalized = detail.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > DETAIL_MAX_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "상세 사유는 300자 이하로 입력해 주세요.");
        }
        return normalized;
    }
}
