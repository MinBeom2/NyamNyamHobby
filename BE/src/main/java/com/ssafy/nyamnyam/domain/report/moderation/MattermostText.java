package com.ssafy.nyamnyam.domain.report.moderation;

import com.ssafy.nyamnyam.global.util.HtmlEscaper;

/**
 * 사용자가 쓴 글을 Mattermost에 안전하게 싣는다.
 * <ul>
 *   <li>저장된 글은 HTML 이스케이프 상태라 되돌린다(&amp;lt; → &lt;).</li>
 *   <li>@channel·@all 같은 멘션이 채널 전체 알림을 울리지 않게 @ 뒤에 폭 없는 공백을 넣는다.</li>
 *   <li>본문은 코드 블록 안에 넣어 마크다운(링크·이미지·굵게)으로 해석되지 않게 한다.</li>
 * </ul>
 */
public final class MattermostText {

    private static final String ZERO_WIDTH_SPACE = "​";

    private MattermostText() {
    }

    /** 한 줄 표시용(제목·닉네임). 줄바꿈은 공백으로, 마크다운 기호는 그대로 보이게 */
    public static String inline(String value, int maxLength) {
        String text = clean(value).replaceAll("\\s+", " ").strip();
        text = text.replace("`", "'").replace("*", "\\*").replace("_", "\\_").replace("[", "\\[").replace("|", "\\|");
        return truncate(text, maxLength);
    }

    /** 본문 표시용 코드 블록 */
    public static String codeBlock(String value, int maxLength) {
        String text = truncate(clean(value).strip(), maxLength).replace("```", "'''");
        return "```\n" + text + "\n```";
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String text = HtmlEscaper.unescape(value).replace("@", "@" + ZERO_WIDTH_SPACE);
        // 제어 문자 제거(줄바꿈·탭은 남김)
        return text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
    }

    static String truncate(String value, int maxLength) {
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        int end = value.offsetByCodePoints(0, maxLength);
        return value.substring(0, end) + "…";
    }
}
