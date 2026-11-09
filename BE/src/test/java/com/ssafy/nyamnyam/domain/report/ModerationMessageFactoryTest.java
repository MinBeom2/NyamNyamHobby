package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.report.entity.Report;
import com.ssafy.nyamnyam.domain.report.entity.ReportReason;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.moderation.ActionSigner;
import com.ssafy.nyamnyam.domain.report.moderation.MattermostText;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationDecision;
import com.ssafy.nyamnyam.domain.report.moderation.ModerationMessageFactory;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Action;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Attachment;
import com.ssafy.nyamnyam.global.infra.mattermost.MattermostMessage.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationMessageFactoryTest {

    private static final String SECRET = "test-secret-test-secret-test-secret";

    private static ModerationMessageFactory factory(String secret) {
        return new ModerationMessageFactory(new ReportProperties(3, 30, false, "https://mm/hooks/x", secret,
                "https://k15d104.p.ssafy.io/", "https://k15d104.p.ssafy.io"));
    }

    private static Report report(ReportReason reason, String detail) {
        return Report.builder().reporterId(1L).targetType(ReportTargetType.COMMENT).targetId(7L).reason(reason).detail(detail).build();
    }

    private static String field(Attachment attachment, String title) {
        return attachment.fields().stream().filter(f -> f.title().equals(title)).map(Field::value).findFirst().orElse(null);
    }

    @Test
    @DisplayName("처리 전 알림: 링크·상태·사유·상세 사유와 서명된 [숨기기]·[유지] 버튼")
    void pending() {
        Target comment = new Target(ReportTargetType.COMMENT, 7L, 12L, 15L, "벽타는펭귄", null,
                "광고 &lt;b&gt;클릭&lt;/b&gt;", State.HIDDEN, true);
        List<Report> received = List.of(report(ReportReason.SPAM, "광고 링크"), report(ReportReason.ABUSE, null),
                report(ReportReason.SPAM, "도배"));

        Attachment attachment = factory(SECRET).pending(ReportTargetType.COMMENT, 7L, comment, received);

        assertThat(attachment.title()).isEqualTo("댓글 #7 (게시글 #12)");
        assertThat(attachment.titleLink()).isEqualTo("https://k15d104.p.ssafy.io/community/12");
        assertThat(attachment.text()).isEqualTo("```\n광고 <b>클릭</b>\n```");
        assertThat(field(attachment, "상태")).isEqualTo("숨김");
        assertThat(field(attachment, "처리 전 신고")).isEqualTo("3건");
        assertThat(field(attachment, "사유")).isEqualTo("스팸·홍보 2 · 욕설·비방 1");
        assertThat(field(attachment, "상세 사유")).contains("- 광고 링크", "- 도배");

        List<Action> actions = attachment.actions();
        assertThat(actions).extracting(Action::id).containsExactly("hide", "keep");
        assertThat(actions.get(0).integration().url()).isEqualTo("https://k15d104.p.ssafy.io/api/v1/internal/moderation/actions");
        assertThat(actions.get(0).integration().context())
                .containsEntry("decision", "HIDE")
                .containsEntry("targetType", "COMMENT")
                .containsEntry("targetId", 7L)
                .containsEntry("sig", new ActionSigner(SECRET).sign(ModerationDecision.HIDE, ReportTargetType.COMMENT, 7L));
    }

    @Test
    @DisplayName("서명 키가 없으면 버튼 없이 알림만, 처리 후 메시지도 버튼 없음")
    void noActions() {
        Target post = new Target(ReportTargetType.POST, 12L, 12L, 15L, "작성자", "Q&amp;A", "본문", State.ACTIVE, true);

        Attachment pending = factory("").pending(ReportTargetType.POST, 12L, post, List.of(report(ReportReason.ETC, null)));
        Attachment done = factory(SECRET).done(ReportTargetType.POST, 12L, post, List.of(), "숨김 · @minbeom");

        assertThat(pending.actions()).isNull();
        assertThat(pending.title()).isEqualTo("게시글 #12 · Q&A");
        assertThat(done.actions()).isNull();
        assertThat(field(done, "처리")).isEqualTo("숨김 · @minbeom");
        assertThat(field(done, "사유")).isEqualTo("-");
    }

    @Test
    @DisplayName("사용자 글은 멘션·마크다운이 동작하지 않게 싣는다")
    void sanitize() {
        assertThat(MattermostText.inline("@channel *공지* [링크](http://x)", 100))
                .isEqualTo("@\u200Bchannel \\*공지\\* \\[링크](http://x)");
        assertThat(MattermostText.codeBlock("```탈출``` @all", 100)).isEqualTo("```\n'''탈출''' @\u200Ball\n```");
        assertThat(MattermostText.inline("가나다라마", 3)).isEqualTo("가나다…");
        assertThat(MattermostText.inline(null, 10)).isEmpty();
    }
}
