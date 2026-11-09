package com.ssafy.nyamnyam.domain.report.moderation;

import com.ssafy.nyamnyam.domain.report.ReportProperties;
import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportReason;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Action;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Field;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Integration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 신고 대상 하나를 Mattermost 메시지 첨부로 만든다.
 * 처리 전이면 [숨기기]·[유지] 버튼을 달고, 처리 후에는 버튼 없이 처리 결과 필드를 단다.
 */
@Component
public class ModerationMessageFactory {

    public static final String ACTION_PATH = "/api/v1/internal/moderation/actions";

    static final String COLOR_ACTIVE = "#E8A23B";
    static final String COLOR_HIDDEN = "#D24B4E";
    static final String COLOR_DONE = "#8A8A8A";
    private static final int MAX_DETAILS = 3;

    private final ReportProperties properties;
    private final ActionSigner signer;

    public ModerationMessageFactory(ReportProperties properties) {
        this.properties = properties;
        this.signer = properties.actionsEnabled() ? new ActionSigner(properties.actionSecret()) : null;
    }

    /** 처리 전 신고 알림(버튼 포함, 서명 키가 없으면 버튼 없이) */
    public Attachment pending(ReportTargetType type, Long id, Target target, List<Report> received) {
        List<Field> fields = baseFields(target, received);
        String color = target != null && target.state() == State.HIDDEN ? COLOR_HIDDEN : COLOR_ACTIVE;
        List<Action> actions = signer != null ? actions(type, id) : null;
        return attachment(type, id, target, received.size(), color, fields, actions);
    }

    /** 처리 후 메시지(버튼 없음). result 예: "숨김 · @minbeom · 11/05 14:12" */
    public Attachment done(ReportTargetType type, Long id, Target target, List<Report> received, String result) {
        List<Field> fields = baseFields(target, received);
        fields.add(new Field("처리", result, false));
        return attachment(type, id, target, received.size(), COLOR_DONE, fields, null);
    }

    private Attachment attachment(ReportTargetType type, Long id, Target target, int count, String color,
                                  List<Field> fields, List<Action> actions) {
        String label = label(type, id, target);
        String link = target != null ? trimSlash(properties.frontendUrl()) + "/community/" + target.postId() : null;
        String body = target != null ? MattermostText.codeBlock(target.content(), 300) : "_대상을 찾을 수 없어요._";
        return new Attachment("신고 · " + label + " · " + count + "건", color, label, link, body, fields, actions);
    }

    private static String label(ReportTargetType type, Long id, Target target) {
        if (type == ReportTargetType.POST) {
            String title = target != null ? " · " + MattermostText.inline(target.title(), 60) : "";
            return "게시글 #" + id + title;
        }
        String post = target != null ? " (게시글 #" + target.postId() + ")" : "";
        return "댓글 #" + id + post;
    }

    private static List<Field> baseFields(Target target, List<Report> received) {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field("상태", target != null ? target.state().label() : "없음", true));
        if (target != null) {
            fields.add(new Field("작성자", MattermostText.inline(target.authorNickname(), 20) + " (#" + target.authorId() + ")", true));
        }
        fields.add(new Field("처리 전 신고", received.size() + "건", true));
        fields.add(new Field("사유", reasons(received), true));

        List<String> details = received.stream()
                .map(Report::getDetail)
                .filter(d -> d != null && !d.isBlank())
                .limit(MAX_DETAILS)
                .map(d -> "- " + d.replaceAll("\\s+", " "))
                .toList();
        if (!details.isEmpty()) {
            fields.add(new Field("상세 사유", MattermostText.codeBlock(String.join("\n", details), 400), false));
        }
        return fields;
    }

    /** "스팸·홍보 2 · 욕설·비방 1" (enum 순서) */
    static String reasons(List<Report> received) {
        if (received.isEmpty()) {
            return "-";
        }
        Map<ReportReason, Long> counts = received.stream()
                .collect(Collectors.groupingBy(Report::getReason, () -> new EnumMap<>(ReportReason.class), Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> e.getKey().label() + " " + e.getValue())
                .collect(Collectors.joining(" · "));
    }

    private List<Action> actions(ReportTargetType type, Long id) {
        String url = trimSlash(properties.publicBaseUrl()) + ACTION_PATH;
        return List.of(
                new Action("hide", "숨기기", "danger", new Integration(url, context(ModerationDecision.HIDE, type, id))),
                new Action("keep", "유지", "default", new Integration(url, context(ModerationDecision.KEEP, type, id))));
    }

    private Map<String, Object> context(ModerationDecision decision, ReportTargetType type, Long id) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("decision", decision.name());
        context.put("targetType", type.name());
        context.put("targetId", id);
        context.put("sig", signer.sign(decision, type, id));
        return context;
    }

    private static String trimSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
