package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFilter.Decision;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFilter.Judgement;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFilter.Outcome;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortFilterTest {

    private final ShortFilter filter = new ShortFilter();
    private final ShortFilter.Policy policy = new ShortFilter.Policy(180, 1000, 2);

    static YoutubeVideo video(String id, String channelId, int duration, String privacy, boolean embeddable,
                              List<String> allowed, List<String> blocked, long views) {
        return new YoutubeVideo(id, channelId, "채널", "제목 " + id, "설명", "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg",
                duration, LocalDateTime.of(2026, 10, 20, 10, 0), privacy, embeddable, allowed, blocked, views);
    }

    static YoutubeVideo ok(String id, String channelId) {
        return video(id, channelId, 50, "public", true, null, null, 5_000);
    }

    private Judgement judgeOne(YoutubeVideo video) {
        return filter.judge(List.of(video), Set.of(), Map.of(), policy).get(0);
    }

    @Test
    @DisplayName("조건을 모두 만족하면 PASS")
    void pass() {
        Judgement judgement = judgeOne(ok("v1", "c1"));
        assertThat(judgement.outcome()).isEqualTo(Outcome.PASS);
        assertThat(judgement.reason()).isNull();
    }

    @Test
    @DisplayName("공개 아님 · 퍼가기 불가 · 한국 차단 · 180초 초과는 사유와 함께 영구 제외")
    void permanentRejects() {
        assertThat(judgeOne(video("a", "c", 50, "unlisted", true, null, null, 5_000)).reason()).isEqualTo(ShortFilter.NOT_PUBLIC);
        assertThat(judgeOne(video("b", "c", 50, "public", false, null, null, 5_000)).reason()).isEqualTo(ShortFilter.NOT_EMBEDDABLE);
        assertThat(judgeOne(video("c", "c", 50, "public", true, null, List.of("JP", "KR"), 5_000)).reason()).isEqualTo(ShortFilter.REGION_BLOCKED);
        assertThat(judgeOne(video("d", "c", 50, "public", true, List.of("US"), null, 5_000)).reason()).isEqualTo(ShortFilter.REGION_BLOCKED);
        assertThat(judgeOne(video("e", "c", 181, "public", true, null, null, 5_000)).reason()).isEqualTo(ShortFilter.TOO_LONG);

        assertThat(judgeOne(video("f", "c", 180, "public", true, List.of("KR"), List.of("JP"), 5_000)).outcome())
                .isEqualTo(Outcome.PASS);
    }

    @Test
    @DisplayName("길이 모름 · 조회수 미달 · 차단 채널은 저장하지 않고 건너뜀")
    void skips() {
        assertThat(judgeOne(video("a", "c", -1, "public", true, null, null, 5_000)))
                .extracting(Judgement::outcome, Judgement::reason)
                .containsExactly(Outcome.SKIP, ShortFilter.SKIP_NO_DURATION);
        assertThat(judgeOne(video("b", "c", 50, "public", true, null, null, 999)))
                .extracting(Judgement::outcome, Judgement::reason)
                .containsExactly(Outcome.SKIP, ShortFilter.SKIP_LOW_VIEWS);

        Judgement blocked = filter.judge(List.of(ok("c", "bad")), Set.of("bad"), Map.of(), policy).get(0);
        assertThat(blocked.reason()).isEqualTo(ShortFilter.SKIP_BLOCKED_CHANNEL);
    }

    @Test
    @DisplayName("같은 채널은 기존 저장 수를 포함해 취미당 상한까지만 통과")
    void channelLimit() {
        List<Judgement> result = filter.judge(
                List.of(ok("v1", "c1"), ok("v2", "c1"), ok("v3", "c2"), ok("v4", "c2"), ok("v5", "c2")),
                Set.of(),
                Map.of("c1", 1L),
                policy);

        assertThat(result).extracting(Judgement::outcome).containsExactly(
                Outcome.PASS, Outcome.SKIP, Outcome.PASS, Outcome.PASS, Outcome.SKIP);
        assertThat(result.get(1).reason()).isEqualTo(ShortFilter.SKIP_CHANNEL_LIMIT);
    }

    @Test
    @DisplayName("영구 제외 영상은 채널 상한을 차지하지 않음")
    void rejectedDoesNotConsumeChannelLimit() {
        List<Judgement> result = filter.judge(
                List.of(video("long", "c1", 300, "public", true, null, null, 5_000), ok("v1", "c1"), ok("v2", "c1")),
                Set.of(), Map.of(), policy);

        assertThat(result).extracting(Judgement::outcome)
                .containsExactly(Outcome.REJECT, Outcome.PASS, Outcome.PASS);
    }

    @Test
    @DisplayName("GPT 판별: 취미 영상 0.7 이상만 CANDIDATE, 아니라고 0.9 이상일 때만 REJECTED, 나머지는 NEEDS_REVIEW")
    void decide() {
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(true, 0.9), 0.7, 0.9))
                .isEqualTo(new Decision(ShortStatus.CANDIDATE, null, 0.9));
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(true, 0.7), 0.7, 0.9).status())
                .isEqualTo(ShortStatus.CANDIDATE);
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(true, 0.5), 0.7, 0.9).status())
                .isEqualTo(ShortStatus.NEEDS_REVIEW);
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(false, 0.95), 0.7, 0.9))
                .isEqualTo(new Decision(ShortStatus.REJECTED, ShortFilter.NOT_HOBBY, 0.95));
        // "아님"이지만 확신이 0.9 미만이면 버리지 않고 보류
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(false, 0.8), 0.7, 0.9))
                .isEqualTo(new Decision(ShortStatus.NEEDS_REVIEW, null, 0.8));
        assertThat(ShortFilter.decide(new ShortContentClassifier.Verdict(false, 0.4), 0.7, 0.9).status())
                .isEqualTo(ShortStatus.NEEDS_REVIEW);
        assertThat(ShortFilter.decide(null, 0.7, 0.9))
                .isEqualTo(new Decision(ShortStatus.NEEDS_REVIEW, null, null));
    }
}
