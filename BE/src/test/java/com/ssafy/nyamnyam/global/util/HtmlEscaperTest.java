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
}
