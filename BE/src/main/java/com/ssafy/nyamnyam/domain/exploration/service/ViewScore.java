package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationView;
import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;

import java.math.BigDecimal;

/**
 * 숏폼 1개가 취미 관심도에 더하는 점수. 탐색 중 관심 주제 막대와 최종 추천이 같은 규칙을 쓴다.
 *
 * <p>좋아요·싫어요는 확실한 호불호라 크게 반영하고, 시청 시간은 반응이 없을 때만 보조로 쓴다.
 * 숏폼은 2~3초 보고도 좋아요를 누를 수 있으므로 반응이 있는 영상은 시청 시간을 보지 않는다.
 * 시청 시간은 영상 길이 차이로 불리해지지 않도록 비율이 아니라 초 단위로 본다.
 * 반응 없이 넘긴 영상은 약하게만 깎아서, 노출이 많다는 이유로 좋아요가 묻히지 않게 한다
 * (바로 넘긴 영상 10개 = 좋아요 1개).</p>
 */
public final class ViewScore {

    public static final double LIKE = 3;
    public static final double DISLIKE = -3;
    /** 반응 없이 2초 안에 넘김 */
    public static final double SKIPPED = -0.3;
    /** 반응 없이 10초 이상 또는 거의 끝까지 봄 */
    public static final double WATCHED = 0.5;

    static final int SKIP_SECONDS = 2;
    static final int WATCHED_SECONDS = 10;
    static final double WATCHED_RATIO = 0.9;

    private ViewScore() {
    }

    public static double of(ReactionType reaction, ExplorationView view) {
        if (reaction == ReactionType.LIKE) return LIKE;
        if (reaction == ReactionType.DISLIKE) return DISLIKE;
        int seconds = view.getWatchedSeconds() == null ? 0 : view.getWatchedSeconds();
        double ratio = view.getWatchRatio() == null ? 0 : view.getWatchRatio().doubleValue();
        if (seconds >= WATCHED_SECONDS || ratio >= WATCHED_RATIO) return WATCHED;
        if (seconds < SKIP_SECONDS) return SKIPPED;
        return 0;
    }

    public static BigDecimal decimalOf(ReactionType reaction, ExplorationView view) {
        return BigDecimal.valueOf(of(reaction, view));
    }
}
