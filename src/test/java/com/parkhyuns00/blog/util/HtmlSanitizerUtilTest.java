package com.parkhyuns00.blog.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HtmlSanitizerUtilTest {

    private final HtmlSanitizerUtil htmlSanitizerUtil = new HtmlSanitizerUtil();

    @Test
    @DisplayName("빈 수식 요소와 인라인 및 블록 LaTeX 데이터를 유지한다.")
    void test_sanitize_allow_math_data() {
        String html = """
              <p>수식: <span data-type="inline-math" data-latex="a \\times b"></span></p>
              <div data-type="block-math" data-latex="\\frac{a}{b}"></div>
              """;

        assertThat(htmlSanitizerUtil.sanitize(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "block-math", "unknown", "INLINE-MATH", "inline-math block-math"})
    @DisplayName("span의 data-type은 inline-math만 허용한다.")
    void test_sanitize_remove_invalid_inline_math_type(String type) {
        String html = "<span data-type=\"" + type + "\" data-latex=\"x\"></span>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<span data-latex=\"x\"></span>");
    }

    @Test
    @DisplayName("수식 속성은 지정된 태그에만 허용한다.")
    void test_sanitize_restrict_math_attributes_to_elements() {
        String html = "<p data-type=\"inline-math\" data-latex=\"x\">본문</p>"
            + "<details data-type=\"toggle\" data-latex=\"x\"><summary>제목</summary></details>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<p>본문</p><details data-type=\"toggle\"><summary>제목</summary></details>");
    }

    @Test
    @DisplayName("수식 데이터의 특수문자는 속성 안에 보존하고 HTML 이벤트는 제거한다.")
    void test_sanitize_math_data_with_special_characters_and_xss() {
        String html = """
              <span data-type="inline-math" data-latex="x &lt; y &amp; \\text{&quot;값&quot;}" onclick="alert(1)"></span>
              <div data-type="block-math" data-latex="\\frac{a}{b}" style="color: red">
                <script>alert(1)</script>
                <img src="/api/post-images/1" onerror="alert(1)">
              </div>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("data-type=\"inline-math\"", "data-type=\"block-math\"",
                "data-latex=\"x &lt; y &amp; \\text{", "data-latex=\"\\frac{a}{b}\"")
            .doesNotContain("onclick", "onerror", "style=", "<script", "alert(");
        assertThat(htmlSanitizerUtil.sanitize(result)).isEqualTo(result);
    }

    @Test
    @DisplayName("허용된 HTML 태그는 유지한다.")
    void test_sanitize_allowed_html_success() {
        String html = """
              <h2>제목</h2>
              <p><strong>강조된 내용</strong></p>
              <blockquote>인용문</blockquote>
              <ul><li>목록</li></ul>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<h2>제목</h2>")
            .contains("<p><strong>강조된 내용</strong></p>")
            .contains("<blockquote>인용문</blockquote>")
            .contains("<ul><li>목록</li></ul>");
    }

    @Test
    @DisplayName("인용문 스타일 속성과 제목, 아이콘, 본문 구조를 유지한다.")
    void test_sanitize_allow_blockquote_style() {
        String html = """
              <blockquote data-quote-style="note">
                <div class="quote-title">
                  <span class="quote-icon"></span>
                  <span>Note</span>
                </div>
                <div class="quote-content">
                  <p>인용 본문</p>
                </div>
              </blockquote>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<blockquote data-quote-style=\"note\">")
            .contains("<div class=\"quote-title\">")
            .contains("<span class=\"quote-icon\"></span>")
            .contains("Note")
            .contains("<div class=\"quote-content\">")
            .contains("<p>인용 본문</p>")
            .contains("</blockquote>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"note", "tip", "important", "warning", "caution"})
    @DisplayName("허용 목록에 포함된 인용문 스타일을 유지한다.")
    void test_sanitize_allow_supported_quote_styles(String style) {
        String html = "<blockquote data-quote-style=\"" + style + "\"><p>인용 본문</p></blockquote>";

        assertThat(htmlSanitizerUtil.sanitize(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "unknown", "NOTE", "note tip", "note-warning",
        "javascript:alert(1)", "note&quot; onclick=&quot;alert(1)",
        "&lt;script&gt;alert(1)&lt;/script&gt;"
    })
    @DisplayName("허용되지 않은 인용문 스타일과 속성 내부의 XSS 입력을 제거한다.")
    void test_sanitize_remove_invalid_quote_styles(String style) {
        String html = "<blockquote data-quote-style=\"" + style + "\"><p>인용 본문</p></blockquote>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<blockquote><p>인용 본문</p></blockquote>");
    }

    @Test
    @DisplayName("인용문 스타일은 blockquote에만 허용하고 이벤트와 style 속성은 제거한다.")
    void test_sanitize_restrict_blockquote_attributes() {
        String html = """
              <blockquote data-quote-style="note" onclick="alert('xss')" style="color: red">
                <p data-quote-style="note">인용 본문</p>
                <script>alert('xss')</script>
                <a href="javascript:alert('xss')">위험한 링크</a>
                <img src="/api/post-images/1" onerror="alert('xss')">
              </blockquote>
              <div data-quote-style="note">일반 본문</div>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<blockquote data-quote-style=\"note\">")
            .contains("<p>인용 본문</p>")
            .contains("<div>일반 본문</div>")
            .contains("src=\"/api/post-images/1\"")
            .doesNotContain("onclick", "onerror", "style=\"color", "alert(", "<script", "javascript:");
    }

    @Test
    @DisplayName("표의 equal 레이아웃과 셀 구조를 유지한다.")
    void test_sanitize_allow_equal_table_layout() {
        String html = "<table data-table-layout=\"equal\"><tbody><tr><td>항목</td><td>설명</td></tr></tbody></table>";

        assertThat(htmlSanitizerUtil.sanitize(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "auto", "EQUAL", "equal auto", "javascript:alert(1)",
        "equal&quot; onclick=&quot;alert(1)"
    })
    @DisplayName("허용되지 않은 표 레이아웃은 제거하고 본문은 유지한다.")
    void test_sanitize_remove_invalid_table_layout(String layout) {
        String html = "<table data-table-layout=\"" + layout
            + "\"><tbody><tr><td>항목</td></tr></tbody></table>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<table><tbody><tr><td>항목</td></tr></tbody></table>");
    }

    @Test
    @DisplayName("표 레이아웃은 table에만 허용하고 이벤트와 인라인 스타일은 제거한다.")
    void test_sanitize_restrict_table_layout_attributes() {
        String html = """
              <table data-table-layout="equal" onclick="alert(1)" style="table-layout: fixed">
                <tbody data-table-layout="equal"><tr data-table-layout="equal">
                  <td data-table-layout="equal" colspan="2">항목</td>
                </tr></tbody>
              </table>
              <div data-table-layout="equal">본문</div>
              """;

        assertThat(htmlSanitizerUtil.sanitize(html))
            .containsOnlyOnce("data-table-layout=\"equal\"")
            .contains("<table data-table-layout=\"equal\">", "<td colspan=\"2\">항목</td>", "<div>본문</div>")
            .doesNotContain("onclick", "style=", "alert(");
    }

    @ParameterizedTest
    @ValueSource(strings = {"striped", "minimal"})
    @DisplayName("허용된 표 테마와 equal 레이아웃을 함께 유지한다.")
    void test_sanitize_allow_table_themes(String theme) {
        String html = "<table data-table-layout=\"equal\" data-table-theme=\"" + theme
            + "\"><tbody><tr><td>항목</td><td>설명</td></tr></tbody></table>";

        assertThat(htmlSanitizerUtil.sanitize(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "unknown", "STRIPED", "striped minimal", "javascript:alert(1)",
        "striped&quot; onclick=&quot;alert(1)"
    })
    @DisplayName("허용되지 않은 표 테마를 제거하고 레이아웃과 본문은 유지한다.")
    void test_sanitize_remove_invalid_table_themes(String theme) {
        String html = "<table data-table-layout=\"equal\" data-table-theme=\"" + theme
            + "\"><tbody><tr><td>항목</td></tr></tbody></table>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<table data-table-layout=\"equal\"><tbody><tr><td>항목</td></tr></tbody></table>");
    }

    @Test
    @DisplayName("표 테마는 table에만 허용하고 이벤트와 인라인 스타일은 제거한다.")
    void test_sanitize_restrict_table_theme_attributes() {
        String html = """
              <table data-table-theme="striped" onclick="alert(1)" style="color: red">
                <tbody data-table-theme="striped"><tr data-table-theme="striped">
                  <td data-table-theme="striped">항목</td>
                </tr></tbody>
              </table>
              <div data-table-theme="minimal">본문</div>
              """;

        assertThat(htmlSanitizerUtil.sanitize(html))
            .containsOnlyOnce("data-table-theme=\"striped\"")
            .contains("<table data-table-theme=\"striped\">", "<td>항목</td>", "<div>본문</div>")
            .doesNotContain("minimal", "onclick", "style=", "alert(");
    }

    @Test
    @DisplayName("접기 태그와 타입, 제목 및 본문 구조를 유지한다.")
    void test_sanitize_allow_toggle_structure() {
        String html = """
              <details data-type="toggle">
                <summary>접기 제목</summary>
                <div data-type="toggle-content">
                  <p>접힌 내용</p>
                </div>
              </details>
              """;

        assertThat(htmlSanitizerUtil.sanitize(html)).isEqualTo(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "TOGGLE", "toggle-content", "toggle&quot; ontoggle=&quot;alert(1)"})
    @DisplayName("details의 허용되지 않은 타입은 제거하고 접기 구조는 유지한다.")
    void test_sanitize_remove_invalid_toggle_type(String type) {
        String html = "<details data-type=\"" + type + "\"><summary>제목</summary><p>본문</p></details>";

        assertThat(htmlSanitizerUtil.sanitize(html))
            .isEqualTo("<details><summary>제목</summary><p>본문</p></details>");
    }

    @Test
    @DisplayName("접기 구조의 이벤트, 인라인 스타일, 스크립트와 위험한 링크를 제거한다.")
    void test_sanitize_remove_toggle_xss() {
        String html = """
              <details data-type="toggle" ontoggle="alert(1)" style="color: red">
                <summary data-type="toggle" onclick="alert(1)">제목</summary>
                <div data-type="toggle-content">
                  <script>alert(1)</script>
                  <a href="javascript:alert(1)">본문</a>
                </div>
              </details>
              """;

        assertThat(htmlSanitizerUtil.sanitize(html))
            .contains("<details data-type=\"toggle\">", "<summary>제목</summary>",
                "<div data-type=\"toggle-content\">", "본문", "</details>")
            .doesNotContain("ontoggle", "onclick", "style=", "<script", "javascript:", "alert(");
    }

    @Test
    @DisplayName("script 태그와 내부 스크립트를 제거한다.")
    void test_sanitize_remove_script_element() {
        String html = """
              <p>정상 내용</p>
              <script>alert('xss')</script>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<p>정상 내용</p>")
            .doesNotContain("<script")
            .doesNotContain("alert('xss')");
    }

    @Test
    @DisplayName("iframe 등 허용되지 않은 태그를 제거한다.")
    void test_sanitize_remove_disallowed_elements() {
        String html = """
              <p>정상 내용</p>
              <iframe src="https://example"></iframe>
              <object data="https://example"></object>
              <form><input type="text"></form>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<p>정상 내용</p>")
            .doesNotContain("<iframe")
            .doesNotContain("<object")
            .doesNotContain("<form")
            .doesNotContain("<input");
    }

    @Test
    @DisplayName("HTML 이벤트 속성을 제거한다.")
    void test_sanitize_remove_event_attributes() {
        String html = """
              <p onclick="alert('xss')">본문</p>
              <img src="/api/post-images/1"
                   alt="이미지"
                   onerror="alert('xss')">
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("본문")
            .contains("/api/post-images/1")
            .doesNotContain("onclick")
            .doesNotContain("onerror")
            .doesNotContain("alert('xss')");
    }

    @Test
    @DisplayName("HTTP와 HTTPS 요청을 허용한다.")
    void test_sanitize_allow_http_and_https_links() {
        String html = """
              <a href="http://example.com">HTTP</a>
              <a href="https://example.com">HTTPS</a>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("href=\"http://example.com\"")
            .contains("href=\"https://example.com\"");
    }

    @Test
    @DisplayName("javascript 프로토콜 링크를 제거한다.")
    void test_sanitize_remove_javascript_link() {
        String html = "<a href=\"javascript:alert('xss')\">위험한 링크</a>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("위험한 링크")
            .doesNotContain("javascript:")
            .doesNotContain("href=");
    }

    @Test
    @DisplayName("게시글 이미지 API 경로를 이미지 주소로 허용한다.")
    void test_sanitize_allow_post_image_path() {
        String html = "<img src=\"/api/post-images/123\" alt=\"게시글 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("src=\"/api/post-images/123\"")
            .contains("alt=\"게시글 이미지\"");
    }

    @Test
    @DisplayName("외부 이미지 주소를 제거한다.")
    void test_sanitize_remove_external_image_source() {
        String html = "<img src=\"https://evil.example/image.png\" alt=\"외부 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("https://evil.example/image.png")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("data 이미지 주소를 제거한다.")
    void test_sanitize_remove_data_image_source() {
        String html = "<img src=\"data:image/png;base64,AAAA\" alt=\"인라인 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("data:image")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("blob 이미지 주소를 제거한다.")
    void test_sanitize_remove_blob_image_source() {
        String html = "<img src=\"blob:https://example.com/image\" alt=\"Blob 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("blob:")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("양수가 아닌 게시글 이미지 ID 경로를 제거한다.")
    void test_sanitize_remove_invalid_post_image_path() {
        String html = "<img src=\"/api/post-images/0\" alt=\"잘못된 이미지\">";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("/api/post-images/0")
            .doesNotContain("src=");
    }

    @Test
    @DisplayName("올바른 코드 언어 클래스는 유지한다.")
    void test_sanitize_allow_valid_code_class() {
        String html = "<pre><code class=\"language-kotlin\">val name = \"blog\"</code></pre>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("class=\"language-kotlin\"")
            .contains("val name")
            .contains("blog");
    }

    @Test
    @DisplayName("허용된 형식이 아닌 코드 클래스는 제거한다.")
    void test_sanitize_remove_invalid_code_class() {
        String html = "<pre><code class=\"language-java malicious\">code</code></pre>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("code")
            .doesNotContain("class=");
    }

    @Test
    @DisplayName("HEX 형식의 형광펜 색상을 소문자로 정규화한다.")
    void test_sanitize_allow_hex_mark_color() {
        String html = "<mark data-color=\"#FDE047\">강조</mark>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("data-color=\"#fde047\"")
            .contains(">강조</mark>");
    }

    @Test
    @DisplayName("HEX 형식이 아닌 형광펜 색상을 제거한다.")
    void test_sanitize_remove_invalid_mark_color() {
        String html = "<mark data-color=\"red\">강조</mark>";

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("<mark>강조</mark>")
            .doesNotContain("data-color");
    }

    @Test
    @DisplayName("style 속성을 제거한다.")
    void test_sanitize_remove_style_attribute() {
        String html = """
              <p style="color: red">본문</p>
              <mark
                  data-color="#fde047"
                  style="background-image: url(javascript:alert('xss'))">
                  강조
              </mark>
              """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("본문")
            .contains("data-color=\"#fde047\"")
            .doesNotContain("style=")
            .doesNotContain("javascript:");
    }

    @Test
    @DisplayName("null HTML을 입력하면 null을 반환한다.")
    void test_sanitize_return_null_when_html_null() {
        String result = htmlSanitizerUtil.sanitize(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("img 태그의 유효한 width와 height 속성을 허용한다.")
    void test_sanitize_allow_valid_image_dimensions() {
        String html = """
          <img
              src="/api/post-images/1"
              alt="본문 이미지"
              width="1200"
              height="630"
          >
          """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("src=\"/api/post-images/1\"")
            .contains("alt=\"본문 이미지\"")
            .contains("width=\"1200\"")
            .contains("height=\"630\"");
    }

    @Test
    @DisplayName("img 태그의 올바르지 않은 width와 height 속성을 제거한다.")
    void test_sanitize_remove_invalid_image_dimensions() {
        String html = """
          <p>
              <img
                  src="/api/post-images/1"
                  width="0"
                  height="630px"
                  alt="본문 이미지"
              >
          </p>
          """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .contains("src=\"/api/post-images/1\"")
            .contains("alt=\"본문 이미지\"")
            .doesNotContain("width=")
            .doesNotContain("height=");
    }

    @Test
    @DisplayName("img 크기 속성에 포함된 임의 문자열을 제거한다.")
    void test_sanitize_remove_malicious_image_dimensions() {
        String html = """
          <img
              src="/api/post-images/1"
              width="1200 onclick=alert(1)"
              height="-630"
              alt="본문 이미지"
          >
          """;

        String result = htmlSanitizerUtil.sanitize(html);

        assertThat(result)
            .doesNotContain("width=")
            .doesNotContain("height=")
            .doesNotContain("onclick");
    }
}
