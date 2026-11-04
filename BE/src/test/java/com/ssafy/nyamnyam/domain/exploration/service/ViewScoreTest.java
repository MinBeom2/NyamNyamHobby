package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationView;
import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ViewScoreTest {

    @Test void reactionDecidesScoreRegardlessOfWatchTime() {
        assertThat(ViewScore.of(ReactionType.LIKE, view(2, "0.04"))).isEqualTo(3);
        assertThat(ViewScore.of(ReactionType.LIKE, view(60, "1"))).isEqualTo(3);
        assertThat(ViewScore.of(ReactionType.DISLIKE, view(60, "1"))).isEqualTo(-3);
    }

    @Test void watchTimeIsOnlyAWeakSignalWithoutReaction() {
        assertThat(ViewScore.of(null, view(1, "0.02"))).isEqualTo(-0.3);
        assertThat(ViewScore.of(null, view(5, "0.1"))).isEqualTo(0);
        assertThat(ViewScore.of(null, view(10, "0.2"))).isEqualTo(0.5);
        // 짧은 영상을 끝까지 본 경우도 몰입으로 본다
        assertThat(ViewScore.of(null, view(6, "0.95"))).isEqualTo(0.5);
    }

    @Test void tenQuickSkipsEqualOneLike() {
        assertThat(ViewScore.SKIPPED * 10).isEqualTo(-ViewScore.LIKE);
    }

    private ExplorationView view(int seconds, String ratio) {
        return ExplorationView.builder().watchedSeconds((short) seconds).watchRatio(new BigDecimal(ratio)).build();
    }
}
