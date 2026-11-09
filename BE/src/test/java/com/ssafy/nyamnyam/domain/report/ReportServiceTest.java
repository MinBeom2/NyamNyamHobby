package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.report.dto.ReportCreateRequest;
import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportReason;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.repository.ReportRepository;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.domain.report.service.ReportCreatedEvent;
import com.ssafy.nyamnyam.domain.report.service.ReportService;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private ModerationTargets targets;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ReportService service;

    @BeforeEach
    void setUp() {
        ReportProperties properties = new ReportProperties(3, 30, false, "", "", "http://localhost:8080", "http://localhost:5173");
        service = new ReportService(reportRepository, targets, properties, eventPublisher);
    }

    private static Target post(long id, long authorId, State state) {
        return new Target(ReportTargetType.POST, id, id, authorId, "작성자", "제목", "본문", state, state == State.ACTIVE);
    }

    private static Target comment(long id, long authorId, boolean postActive) {
        return new Target(ReportTargetType.COMMENT, id, 100L, authorId, "작성자", null, "댓글", State.ACTIVE, postActive);
    }

    @Test
    @DisplayName("신고 저장: 상세 사유를 정리하고 처리 전(RECEIVED) 상태로 저장")
    void create() {
        given(targets.find(ReportTargetType.POST, 12L)).willReturn(Optional.of(post(12, 15, State.ACTIVE)));
        given(reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTargetType.POST, 12L, ReportStatus.RECEIVED)).willReturn(1L);

        service.create(16L, new ReportCreateRequest(ReportTargetType.POST, 12L, ReportReason.SPAM, "  광고 링크가\r\n있어요  "));

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getReporterId()).isEqualTo(16L);
        assertThat(saved.getValue().getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(saved.getValue().getDetail()).isEqualTo("광고 링크가\n있어요");
        assertThat(saved.getValue().getStatus()).isEqualTo(ReportStatus.RECEIVED);
        verify(targets, never()).hide(any(), any());
        verify(eventPublisher).publishEvent(any(ReportCreatedEvent.class)); // 즉시 알림용(커밋 뒤 처리)
    }

    @Test
    @DisplayName("서로 다른 사용자 신고가 3건이 되면 바로 숨김")
    void autoHideAtThreshold() {
        given(targets.find(ReportTargetType.COMMENT, 7L)).willReturn(Optional.of(comment(7, 15, true)));
        given(reportRepository.countByTargetTypeAndTargetIdAndStatus(ReportTargetType.COMMENT, 7L, ReportStatus.RECEIVED)).willReturn(3L);
        given(targets.hide(ReportTargetType.COMMENT, 7L)).willReturn(true);

        service.create(16L, new ReportCreateRequest(ReportTargetType.COMMENT, 7L, ReportReason.ABUSE, null));

        verify(targets).hide(ReportTargetType.COMMENT, 7L);
    }

    @Test
    @DisplayName("본인 글은 400, 이미 신고했으면 409")
    void selfAndDuplicate() {
        given(targets.find(ReportTargetType.POST, 12L)).willReturn(Optional.of(post(12, 16, State.ACTIVE)));
        assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.POST, 12L, ReportReason.SPAM, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_SELF);

        willReturn(Optional.of(post(13, 15, State.ACTIVE))).given(targets).find(ReportTargetType.POST, 13L);
        given(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(16L, ReportTargetType.POST, 13L)).willReturn(true);
        assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.POST, 13L, ReportReason.SPAM, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_DUPLICATED);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시에 두 번 눌러 유니크 제약에 걸려도 409")
    void duplicateByConstraint() {
        given(targets.find(ReportTargetType.POST, 12L)).willReturn(Optional.of(post(12, 15, State.ACTIVE)));
        given(reportRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("uk_report"));

        assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.POST, 12L, ReportReason.SPAM, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.REPORT_DUPLICATED);
    }

    @Test
    @DisplayName("숨김·삭제된 대상, 숨겨진 게시글의 댓글, 없는 대상, 사용자 신고는 받지 않음")
    void notReportable() {
        willReturn(Optional.of(post(1, 15, State.HIDDEN))).given(targets).find(ReportTargetType.POST, 1L);
        willReturn(Optional.of(comment(2, 15, false))).given(targets).find(ReportTargetType.COMMENT, 2L);
        willReturn(Optional.empty()).given(targets).find(ReportTargetType.POST, 3L);

        for (long id : new long[]{1, 3}) {
            assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.POST, id, ReportReason.SPAM, null)))
                    .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        }
        assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.COMMENT, 2L, ReportReason.SPAM, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        assertThatThrownBy(() -> service.create(16L, new ReportCreateRequest(ReportTargetType.USER, 9L, ReportReason.SPAM, null)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }
}
