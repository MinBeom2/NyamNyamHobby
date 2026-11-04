package com.ssafy.nyamnyam.global.infra.publicdata;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공공·문화 데이터의 제목·소개에 섞여 오는 HTML 문자 참조를 화면용 글자로 되돌린다.
 * 문화포털은 XML 안에 '&amp;lt;'처럼 두 번 감싸서 보내 한 번 풀어도 '&lt;'가 남으므로, 더 바뀌지 않을 때까지(최대 3번) 푼다.
 * 모르는 이름의 참조는 그대로 둔다.
 */
public final class HtmlText {

    private static final Pattern ENTITY = Pattern.compile("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,15});");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"), Map.entry("quot", "\""),
            Map.entry("apos", "'"), Map.entry("nbsp", " "), Map.entry("middot", "·"), Map.entry("bull", "•"),
            Map.entry("hellip", "…"), Map.entry("ndash", "–"), Map.entry("mdash", "—"),
            Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"), Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("laquo", "«"), Map.entry("raquo", "»"), Map.entry("lang", "〈"), Map.entry("rang", "〉"),
            Map.entry("times", "×"), Map.entry("copy", "©"), Map.entry("reg", "®"), Map.entry("trade", "™"));

    private HtmlText() {
    }

    /** 문자 참조만 푼다(태그는 그대로). 공백만 남으면 null */
    public static String unescape(String value) {
        if (value == null) return null;
        String text = value;
        for (int i = 0; i < 3; i++) {
            String next = decodeOnce(text);
            if (next.equals(text)) break;
            text = next;
        }
        text = text.trim();
        return text.isEmpty() ? null : text;
    }

    /** 태그를 걷어 낸 뒤 문자 참조를 풀고 공백을 하나로 모은다(소개 본문용). 비면 null */
    public static String plain(String html) {
        if (html == null) return null;
        String text = TAG.matcher(html.replaceAll("(?i)<br\\s*/?>", " ")).replaceAll("");
        text = unescape(text);
        if (text == null) return null;
        text = text.replace(' ', ' ').replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }

    private static String decodeOnce(String text) {
        if (text.indexOf('&') < 0) return text;
        Matcher matcher = ENTITY.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String decoded = decode(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(decoded == null ? matcher.group() : decoded));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String decode(String body) {
        if (body.charAt(0) != '#') return NAMED.get(body.toLowerCase());
        try {
            int code = body.length() > 1 && (body.charAt(1) == 'x' || body.charAt(1) == 'X')
                    ? Integer.parseInt(body.substring(2), 16) : Integer.parseInt(body.substring(1));
            return Character.isValidCodePoint(code) ? new String(Character.toChars(code)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
