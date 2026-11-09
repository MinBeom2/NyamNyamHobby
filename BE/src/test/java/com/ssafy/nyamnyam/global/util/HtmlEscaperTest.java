package com.ssafy.nyamnyam.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEscaperTest {

    @Test
    @DisplayName("& < > \" ' 를 엔티티로 바꾸고 나머지는 그대로")
    void escape() {
        assertThat(HtmlEscaper.escape("<img src=x onerror=\"alert('1')\"> & 한글"))
                .isEqualTo("&lt;img src=x onerror=&quot;alert(&#39;1&#39;)&quot;&gt; &amp; 한글");
        assertThat(HtmlEscaper.escape("평범한 글\n줄바꿈")).isEqualTo("평범한 글\n줄바꿈");
        assertThat(HtmlEscaper.escape(null)).isNull();
    }

    @Test
    @DisplayName("unescape는 escape를 되돌리고, &amp;lt; 같은 이중 이스케이프도 한 단계만 푼다")
    void unescape() {
        String raw = "<script>alert('x') & \"y\"</script>";
        assertThat(HtmlEscaper.unescape(HtmlEscaper.escape(raw))).isEqualTo(raw);
        assertThat(HtmlEscaper.unescape("&amp;lt;")).isEqualTo("&lt;");
        assertThat(HtmlEscaper.unescape("plain")).isEqualTo("plain");
        assertThat(HtmlEscaper.unescape(null)).isNull();
    }
}
