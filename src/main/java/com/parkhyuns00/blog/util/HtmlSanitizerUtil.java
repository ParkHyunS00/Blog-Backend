package com.parkhyuns00.blog.util;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class HtmlSanitizerUtil {

    private static final Set<String> ALLOWED_QUOTE_STYLES = Set.of("note", "tip", "important", "warning", "caution");
    private static final Set<String> ALLOWED_TABLE_THEMES = Set.of("striped", "minimal");
    private static final Pattern POST_IMAGE_PATH_PATTERN = Pattern.compile("^/api/post-images/[1-9][0-9]*$");
    private static final Pattern CODE_CLASS_PATTERN = Pattern.compile("^language-[A-Za-z0-9][A-Za-z0-9_+.-]{0,63}$");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern IMAGE_DIMENSION_PATTERN = Pattern.compile("^[1-9][0-9]{0,4}$");
    private static final AttributePolicy POST_IMAGE_SOURCE_POLICY = (_, _, value) ->
        POST_IMAGE_PATH_PATTERN.matcher(value).matches() ? value : null;
    private static final AttributePolicy CODE_CLASS_POLICY = (_, _, value) ->
        CODE_CLASS_PATTERN.matcher(value).matches() ? value : null;
    private static final AttributePolicy HEX_COLOR_POLICY = (_, _, value) -> {
        String normalizedValue = value.toLowerCase(Locale.ROOT);
        return HEX_COLOR_PATTERN
            .matcher(normalizedValue)
            .matches() ? normalizedValue : null;
    };
    private static final AttributePolicy IMAGE_DIMENSION_POLICY = (_, _, value) ->
        IMAGE_DIMENSION_PATTERN.matcher(value).matches() ? value : null;
    private static final AttributePolicy QUOTE_STYLE_POLICY = (_, _, value) ->
        ALLOWED_QUOTE_STYLES.contains(value) ? value : null;
    private static final AttributePolicy TABLE_LAYOUT_POLICY = (_, _, value) ->
        "equal".equals(value) ? value : null;
    private static final AttributePolicy TABLE_THEME_POLICY = (_, _, value) ->
        ALLOWED_TABLE_THEMES.contains(value) ? value : null;
    private static final AttributePolicy TOGGLE_TYPE_POLICY = (_, _, value) ->
        "toggle".equals(value) ? value : null;
    private static final AttributePolicy INLINE_MATH_TYPE_POLICY = (_, _, value) ->
        "inline-math".equals(value) ? value : null;

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
        .allowElements(
            "p", "br", "h1", "h2", "h3", "h4", "h5", "h6",
            "strong", "em", "u", "s", "mark", "code", "pre",
            "blockquote", "ul", "ol", "li", "a", "hr", "figure", "img", "figcaption",
            "table", "colgroup", "col", "thead", "tbody", "tfoot",
            "tr", "th", "td", "div", "span", "details", "summary"
        )
        .allowUrlProtocols("http", "https")
        .allowAttributes("href")
        .onElements("a")
        .requireRelNofollowOnLinks()

        .allowAttributes("src")
        .matching(POST_IMAGE_SOURCE_POLICY)
        .onElements("img")

        .allowAttributes("alt")
        .onElements("img")

        .allowAttributes("width", "height")
        .matching(IMAGE_DIMENSION_POLICY)
        .onElements("img")

        .allowAttributes("class")
        .matching(CODE_CLASS_POLICY)
        .onElements("code")

        .allowAttributes("data-color")
        .matching(HEX_COLOR_POLICY)
        .onElements("mark")

        .allowAttributes("data-quote-style")
        .matching(QUOTE_STYLE_POLICY)
        .onElements("blockquote")

        .allowAttributes("class")
        .onElements("figure", "div", "span")
        .allowAttributes("data-type")
        .onElements("div")

        .allowAttributes("data-type")
        .matching(INLINE_MATH_TYPE_POLICY)
        .onElements("span")

        .allowAttributes("data-latex")
        .onElements("span", "div")

        .allowAttributes("data-type")
        .matching(TOGGLE_TYPE_POLICY)
        .onElements("details")

        .allowAttributes("data-table-layout")
        .matching(TABLE_LAYOUT_POLICY)
        .onElements("table")

        .allowAttributes("data-table-theme")
        .matching(TABLE_THEME_POLICY)
        .onElements("table")

        .allowAttributes("colspan", "rowspan", "align")
        .onElements("td", "th")
        .toFactory();

    public String sanitize(String html) {
        if (html == null) return null;
        return POLICY.sanitize(html);
    }
}
