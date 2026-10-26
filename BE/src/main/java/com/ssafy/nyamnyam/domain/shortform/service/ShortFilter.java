package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 수집 영상 걸러 내기. 외부 호출·DB 접근 없이 입력만으로 판단하는 순수 로직이라 단위 테스트로 검증한다.
 *
 * <ul>
 *   <li>REJECT: 영구 제외. 사유와 함께 저장해 다시 수집하지 않는다(TOO_LONG 등 shorts.reject_reason 값).</li>
 *   <li>SKIP: 이번에만 제외하고 저장하지 않는다(조회수 미달·채널 상한 등 나중에 바뀔 수 있는 조건).</li>
 *   <li>PASS: GPT 판별로 넘어간다.</li>
 * </ul>
 */
@Component
public class ShortFilter {

    public static final String TOO_LONG = "TOO_LONG";
    public static final String NOT_EMBEDDABLE = "NOT_EMBEDDABLE";
    public static final String REGION_BLOCKED = "REGION_BLOCKED";
    public static final String NOT_PUBLIC = "NOT_PUBLIC";
    public static final String NOT_HOBBY = "NOT_HOBBY";

    public static final String SKIP_NO_DURATION = "NO_DURATION";
    public static final String SKIP_LOW_VIEWS = "LOW_VIEWS";
    public static final String SKIP_BLOCKED_CHANNEL = "BLOCKED_CHANNEL";
    public static final String SKIP_CHANNEL_LIMIT = "CHANNEL_LIMIT";

    private static final String KOREA = "KR";

    public enum Outcome { PASS, REJECT, SKIP }

    public record Judgement(YoutubeVideo video, Outcome outcome, String reason) {
    }

    /**
     * @param maxDurationSeconds 쇼츠로 인정할 최대 길이
     * @param minViewCount       조회수 하한
     * @param maxPerChannel      한 취미에서 같은 채널 영상 최대 수
     */
    public record Policy(int maxDurationSeconds, long minViewCount, int maxPerChannel) {
    }

    /** 판별 결과. confidence는 GPT 신뢰도(0~1), 판별하지 못했으면 null */
    public record Decision(ShortStatus status, String rejectReason, Double confidence) {
    }

    /**
     * @param blockedChannelIds 차단(REJECTED)된 채널
     * @param existingPerChannel 이 취미에 이미 저장된 채널별 영상 수(CANDIDATE·NEEDS_REVIEW·ACTIVE)
     */
    public List<Judgement> judge(List<YoutubeVideo> videos,
                                 Set<String> blockedChannelIds,
                                 Map<String, Long> existingPerChannel,
                                 Policy policy) {
        Map<String, Long> perChannel = new HashMap<>(existingPerChannel);
        List<Judgement> result = new ArrayList<>(videos.size());

        for (YoutubeVideo video : videos) {
            String rejectReason = permanentRejectReason(video, policy);
            if (rejectReason != null) {
                result.add(new Judgement(video, Outcome.REJECT, rejectReason));
                continue;
            }

            String skipReason = skipReason(video, blockedChannelIds, perChannel, policy);
            if (skipReason != null) {
                result.add(new Judgement(video, Outcome.SKIP, skipReason));
                continue;
            }

            if (video.channelId() != null) {
                perChannel.merge(video.channelId(), 1L, Long::sum);
            }
            result.add(new Judgement(video, Outcome.PASS, null));
        }
        return result;
    }

    /**
     * GPT 판별 결과로 저장 상태를 정한다.
     * 취미 영상이 확실하면 CANDIDATE(주간 반영 때 노출), 취미 영상이 아닌 게 확실하면 REJECTED,
     * 판별 실패·애매하면 NEEDS_REVIEW(관리자 승인 전까지 노출하지 않음).
     */
    public static Decision decide(ShortContentClassifier.Verdict verdict, double threshold) {
        if (verdict == null) {
            return new Decision(ShortStatus.NEEDS_REVIEW, null, null);
        }
        double confidence = verdict.confidence();
        if (confidence >= threshold) {
            return verdict.hobbyContent()
                    ? new Decision(ShortStatus.CANDIDATE, null, confidence)
                    : new Decision(ShortStatus.REJECTED, NOT_HOBBY, confidence);
        }
        return new Decision(ShortStatus.NEEDS_REVIEW, null, confidence);
    }

    private String permanentRejectReason(YoutubeVideo video, Policy policy) {
        if (!"public".equals(video.privacyStatus())) {
            return NOT_PUBLIC;
        }
        if (!video.embeddable()) {
            return NOT_EMBEDDABLE;
        }
        if (isBlockedInKorea(video)) {
            return REGION_BLOCKED;
        }
        if (video.durationSeconds() > policy.maxDurationSeconds()) {
            return TOO_LONG;
        }
        return null;
    }

    private String skipReason(YoutubeVideo video, Set<String> blockedChannelIds,
                              Map<String, Long> perChannel, Policy policy) {
        if (video.durationSeconds() <= 0) {
            return SKIP_NO_DURATION;
        }
        if (video.channelId() != null && blockedChannelIds.contains(video.channelId())) {
            return SKIP_BLOCKED_CHANNEL;
        }
        if (video.viewCount() < policy.minViewCount()) {
            return SKIP_LOW_VIEWS;
        }
        if (video.channelId() != null && perChannel.getOrDefault(video.channelId(), 0L) >= policy.maxPerChannel()) {
            return SKIP_CHANNEL_LIMIT;
        }
        return null;
    }

    private static boolean isBlockedInKorea(YoutubeVideo video) {
        if (video.blockedRegions() != null && video.blockedRegions().contains(KOREA)) {
            return true;
        }
        return video.allowedRegions() != null && !video.allowedRegions().contains(KOREA);
    }
}
