package com.ssafy.nyamnyam.domain.report.moderation;

import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Mattermost 버튼 서명. 버튼 context에 "처리·대상 종류·대상 ID"의 HMAC-SHA256을 넣고, 클릭이 돌아오면 다시 계산해 비교한다.
 * 비밀 키를 그대로 context에 넣지 않으므로, 서명 하나가 새도 그 버튼(그 대상의 그 처리)만 흉내 낼 수 있다.
 */
public class ActionSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public ActionSigner(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("서명 키가 비어 있습니다.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(ModerationDecision decision, ReportTargetType targetType, Long targetId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] digest = mac.doFinal(payload(decision, targetType, targetId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 서명을 만들 수 없습니다.", e);
        }
    }

    /** 시간 차로 서명을 추측하지 못하게 고정 시간 비교 */
    public boolean verify(ModerationDecision decision, ReportTargetType targetType, Long targetId, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = sign(decision, targetType, targetId).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String payload(ModerationDecision decision, ReportTargetType targetType, Long targetId) {
        return decision.name() + ":" + targetType.name() + ":" + targetId;
    }
}
