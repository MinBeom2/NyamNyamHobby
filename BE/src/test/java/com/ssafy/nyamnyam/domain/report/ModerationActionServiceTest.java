package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.report.dto.MattermostActionRequest;
import com.ssafy.nyamnyam.domain.report.dto.MattermostActionResponse;
import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportAction;
import com.ssafy.nyamnyam.domain.report.entity.ReportReason;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.moderation.ActionSigner;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationDecision;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationMessageFactory;
import com.ssafy.nyamnyam.domain.report.repository.ReportRepository;
import com.ssafy.nyamnyam.domain.report.service.ModerationActionService;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModerationActionServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret";

    @Mock private ReportRepository reportRepository;
    @Mock private ModerationTargets targets;

    private ModerationActionService service;
    private final ActionSigner signer = new ActionSigner(SECRET);

    @BeforeEach
    void setUp() {
        ReportProperties properties = new ReportProperties(3, 30, false, "https://mm/hooks/x", SECRET,
                "https://k15d104.p.ssafy.io", "https://k15d104.p.ssafy.io");
        service = new ModerationActionService(properties, reportRepository, targets, new ModerationMessageFactory(properties));
    }

    private MattermostActionRequest click(ModerationDecision decision, ReportTargetType type, Object id, String sig) {
        Map<String, Object> context = new HashMap<>();
        context.put("decision", decision.name());
        context.put("targetType", type.name());
        context.put("targetId", id);
        context.put("sig", sig);
        return new MattermostActionRequest("u1", "minbeom", "p1", context);
    }

    private MattermostActionRequest signed(ModerationDecision decision, ReportTargetType type, long id) {
        return click(decision, type, id, signer.sign(decision, type, id));
    }

    private static List<Report> reports(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> Report.builder().reporterId(20L + i).targetType(ReportTargetType.POST).targetId(12L)
                        .reason(ReportReason.SPAM).detail(null).build())
                .toList();
    }

    private static Target post(State state) {
        return new Target(ReportTargetType.POST, 12L, 12L, 15L, "작성자", "제목", "본문", state, state == State.ACTIVE);
    }

    @SuppressWarnings("unchecked")
    private static Attachment attachmentOf(MattermostActionResponse response) {
        return ((List<Attachment>) response.update().props().get("attachments")).get(0);
    }

    @Test
    @DisplayName("숨기기: 대상을 숨기고 처리 전 신고를 ACTIONED·HIDE로, 메시지에서 버튼을 없앤다")
    void hide() {
        given(reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(ReportTargetType.POST, 12L, ReportStatus.RECEIVED))
                .willReturn(reports(2));
        given(targets.find(ReportTargetType.POST, 12L)).willReturn(Optional.of(post(State.HIDDEN)));

        MattermostActionResponse response = service.handle(signed(ModerationDecision.HIDE, ReportTargetType.POST, 12L));

        verify(targets).hide(ReportTargetType.POST, 12L);
        verify(reportRepository).resolveReceived(eq(ReportTargetType.POST), eq(12L), eq(ReportStatus.ACTIONED),
                eq(ReportAction.HIDE), eq("mattermost:@minbeom"), any());
        assertThat(response.ephemeralText()).isEqualTo("숨김 처리했어요.");
        Attachment attachment = attachmentOf(response);
        assertThat(attachment.actions()).isNull();
        assertThat(attachment.fields()).anySatisfy(f -> {
            assertThat(f.title()).isEqualTo("처리");
            assertThat(f.value()).startsWith("숨김 · @​minbeom · ");
        });
    }

    @Test
    @DisplayName("유지: 자동 숨김이면 다시 노출하고 DISMISSED·UNHIDE, 노출 중이었으면 DISMISSED·KEEP")
    void keep() {
        given(reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(any(), any(), any())).willReturn(reports(3));
        given(targets.find(any(), any())).willReturn(Optional.of(post(State.ACTIVE)));
        given(targets.unhide(ReportTargetType.POST, 12L)).willReturn(true, false);

        MattermostActionResponse unhidden = service.handle(signed(ModerationDecision.KEEP, ReportTargetType.POST, 12L));
        MattermostActionResponse kept = service.handle(signed(ModerationDecision.KEEP, ReportTargetType.POST, 12L));

        verify(reportRepository).resolveReceived(eq(ReportTargetType.POST), eq(12L), eq(ReportStatus.DISMISSED),
                eq(ReportAction.UNHIDE), anyString(), any());
        verify(reportRepository).resolveReceived(eq(ReportTargetType.POST), eq(12L), eq(ReportStatus.DISMISSED),
                eq(ReportAction.KEEP), anyString(), any());
        assertThat(unhidden.ephemeralText()).contains("다시 보이게");
        assertThat(kept.ephemeralText()).isEqualTo("그대로 두기로 했어요.");
        verify(targets, never()).hide(any(), any());
    }

    @Test
    @DisplayName("이미 처리된 신고면 대상 상태를 바꾸지 않는다(두 사람이 거의 동시에 누른 경우)")
    void alreadyProcessed() {
        given(reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(any(), any(), any())).willReturn(List.of());
        given(targets.find(any(), any())).willReturn(Optional.of(post(State.HIDDEN)));

        MattermostActionResponse response = service.handle(signed(ModerationDecision.KEEP, ReportTargetType.POST, 12L));

        verify(targets, never()).unhide(any(), any());
        verify(reportRepository, never()).resolveReceived(any(), any(), any(), any(), any(), any());
        assertThat(response.ephemeralText()).isEqualTo("이미 처리된 신고예요.");
    }

    @Test
    @DisplayName("Mattermost가 targetId를 12.0으로 돌려줘도 처리한다")
    void decimalTargetId() {
        given(reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(any(), any(), any())).willReturn(List.of());
        given(targets.find(any(), any())).willReturn(Optional.empty());

        String sig = signer.sign(ModerationDecision.HIDE, ReportTargetType.COMMENT, 12L);
        service.handle(click(ModerationDecision.HIDE, ReportTargetType.COMMENT, 12.0, sig));

        verify(reportRepository).findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(ReportTargetType.COMMENT, 12L, ReportStatus.RECEIVED);
    }

    @Test
    @DisplayName("서명이 틀리거나 다른 버튼의 서명이면 403")
    void invalidSignature() {
        String hideSig = signer.sign(ModerationDecision.HIDE, ReportTargetType.POST, 12L);

        assertThatThrownBy(() -> service.handle(click(ModerationDecision.KEEP, ReportTargetType.POST, 12L, hideSig)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.MODERATION_FORBIDDEN);
        assertThatThrownBy(() -> service.handle(click(ModerationDecision.HIDE, ReportTargetType.POST, 13L, hideSig)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.MODERATION_FORBIDDEN);
        assertThatThrownBy(() -> service.handle(click(ModerationDecision.HIDE, ReportTargetType.POST, 12L, "deadbeef")))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.MODERATION_FORBIDDEN);
        assertThatThrownBy(() -> service.handle(new MattermostActionRequest("u1", "x", "p1", null)))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.handle(click(ModerationDecision.HIDE, ReportTargetType.POST, "abc", hideSig)))
                .isInstanceOf(CustomException.class);
        verify(targets, never()).hide(any(), any());
    }

    @Test
    @DisplayName("서명 키가 설정되지 않았으면 모든 클릭을 거절")
    void disabledWithoutSecret() {
        ReportProperties noSecret = new ReportProperties(3, 30, false, "https://mm/hooks/x", "", "https://a", "https://b");
        ModerationActionService disabled = new ModerationActionService(noSecret, reportRepository, targets,
                new ModerationMessageFactory(noSecret));

        assertThatThrownBy(() -> disabled.handle(signed(ModerationDecision.HIDE, ReportTargetType.POST, 12L)))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.MODERATION_FORBIDDEN);
    }
}
