package com.ssafy.nyamnyam.global.util;

/**
 * 사용자 입력 HTML 이스케이프(명세 FR-BOARD-02: 서버는 HTML을 이스케이프해 저장).
 * & < > " ' 다섯 글자를 HTML 엔티티로 바꿔, 저장된 글이 HTML로 해석돼도 태그·스크립트로 동작하지 않게 한다.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
