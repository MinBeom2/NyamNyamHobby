package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.report.ReportProperties;
import com.ssafy.nyamnyam.domain.report.dto.MattermostActionRequest;
import com.ssafy.nyamnyam.domain.report.dto.MattermostActionResponse;
import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportAction;
import com.ssafy.nyamnyam.domain.report.entity.ReportStatus;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.moderation.ActionSigner;
import com.ssafy.nyamnyam.domain.report.moderation.MattermostText;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationDecision;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationMessageFactory;
import com.ssafy.nyamnyam.domain.report.repository.ReportRepository;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Mattermost 버튼으로 신고를 처리한다.
 * <ul>
 *   <li>숨기기: 노출 중이면 숨기고, 처리 전 신고를 ACTIONED·HIDE로.</li>
 *   <li>유지: 자동 숨김 상태면 다시 노출(UNHIDE), 아니면 그대로(KEEP). 처리 전 신고를 DISMISSED로.</li>
 *   <li>처리 전 신고가 없으면(다른 사람이 먼저 처리) 상태는 건드리지 않고 메시지만 최신으로 바꾼다.</li>
 * </ul>
 */
@Slf4j
@Service
public class ModerationActionService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private final ReportRepository reportRepository;
    private final ModerationTargets targets;
    private final ModerationMessageFactory messageFactory;
    private final ActionSigner signer;

    public ModerationActionService(ReportProperties properties, ReportRepository reportRepository,
                                   ModerationTargets targets, ModerationMessageFactory messageFactory) {
        this.reportRepository = reportRepository;
        this.targets = targets;
        this.messageFactory = messageFactory;
        this.signer = properties.actionsEnabled() ? new ActionSigner(properties.actionSecret()) : null;
    }

    /** 버튼 하나의 정보(서명 확인 후) */
    record Command(ModerationDecision decision, ReportTargetType targetType, Long targetId) {
    }

    @Transactional
    public MattermostActionResponse handle(MattermostActionRequest request) {
        Command command = verify(request);
        String actor = actor(request);
        List<Report> received = reportRepository.findByTargetTypeAndTargetIdAndStatusOrderByIdAsc(
                command.targetType(), command.targetId(), ReportStatus.RECEIVED);

        String result;
        String ephemeral;
        if (received.isEmpty()) {
            result = "이미 처리된 신고";
            ephemeral = "이미 처리된 신고예요.";
        } else if (command.decision() == ModerationDecision.HIDE) {
            targets.hide(command.targetType(), command.targetId());
            resolve(command, ReportStatus.ACTIONED, ReportAction.HIDE, actor);
            result = "숨김";
            ephemeral = "숨김 처리했어요.";
        } else {
            boolean unhidden = targets.unhide(command.targetType(), command.targetId());
            resolve(command, ReportStatus.DISMISSED, unhidden ? ReportAction.UNHIDE : ReportAction.KEEP, actor);
            result = unhidden ? "유지 (자동 숨김 해제)" : "유지";
            ephemeral = unhidden ? "자동 숨김을 풀고 다시 보이게 했어요." : "그대로 두기로 했어요.";
        }
        log.info("Moderation {} {} #{} by {} ({} reports)", command.decision(), command.targetType(),
                command.targetId(), actor, received.size());

        Target target = targets.find(command.targetType(), command.targetId()).orElse(null);
        String resultLine = result + " · " + MattermostText.inline(actor, 40) + " · "
                + ZonedDateTime.now(ReportDigestService.KST).format(TIME);
        Attachment done = messageFactory.done(command.targetType(), command.targetId(), target, received, resultLine);
        return new MattermostActionResponse(
                new MattermostActionResponse.Update(null, Map.of("attachments", List.of(done))),
                ephemeral);
    }

    private void resolve(Command command, ReportStatus status, ReportAction action, String actor) {
        String note = "mattermost:" + actor;
        reportRepository.resolveReceived(command.targetType(), command.targetId(), status, action,
                note.length() > 300 ? note.substring(0, 300) : note, LocalDateTime.now());
    }

    /** context를 읽고 서명을 확인한다. 하나라도 어긋나면 403 */
    Command verify(MattermostActionRequest request) {
        if (signer == null || request == null || request.context() == null) {
            throw new CustomException(ErrorCode.MODERATION_FORBIDDEN);
        }
        Map<String, Object> context = request.context();
        try {
            ModerationDecision decision = ModerationDecision.valueOf(String.valueOf(context.get("decision")));
            ReportTargetType targetType = ReportTargetType.valueOf(String.valueOf(context.get("targetType")));
            // Mattermost가 숫자를 12 또는 12.0으로 돌려줄 수 있다
            Long targetId = new BigDecimal(String.valueOf(context.get("targetId"))).longValueExact();
            Object sig = context.get("sig");
            if (signer.verify(decision, targetType, targetId, sig != null ? sig.toString() : null)) {
                return new Command(decision, targetType, targetId);
            }
        } catch (IllegalArgumentException | ArithmeticException e) {
            // 형식이 틀린 값(NumberFormatException 포함)
        }
        log.warn("Moderation action rejected: invalid signature or context");
        throw new CustomException(ErrorCode.MODERATION_FORBIDDEN);
    }

    private static String actor(MattermostActionRequest request) {
        if (request.userName() != null && !request.userName().isBlank()) {
            return "@" + request.userName();
        }
        return request.userId() != null ? "user:" + request.userId() : "unknown";
    }
}
