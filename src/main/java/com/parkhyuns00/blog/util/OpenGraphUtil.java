package com.parkhyuns00.blog.util;

import com.parkhyuns00.blog.config.cors.dto.CorsProperties;
import com.parkhyuns00.blog.domain.post.service.dto.PostOpenGraphDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@RequiredArgsConstructor
public class OpenGraphUtil {

    private static final String SITE_NAME = "blog.parkhyuns00.com";
    private static final String DEFAULT_DESCRIPTION = "현수의 성장 아카이브";

    private final CorsProperties corsProperties;

    public String render(PostOpenGraphDto post) {
        String siteOrigin = corsProperties.allowedOrigins().getFirst();
        if (siteOrigin.endsWith("/")) {
            siteOrigin = siteOrigin.substring(0, siteOrigin.length() - 1);
        }

        String title = escape(post.title());
        String description = escape(post.summary() == null || post.summary().isBlank()
            ? DEFAULT_DESCRIPTION : post.summary());
        String postUrl = escape(siteOrigin + "/posts/" + post.postId());
        boolean defaultImage = post.thumbnailImageId() == null;
        String imageUrl = defaultImage
            ? siteOrigin + "/og-default.png"
            : siteOrigin + "/api/post-images/" + post.thumbnailImageId();

        String imageMetadata = meta("og:image:type", defaultImage ? "image/png" : post.mimeType())
            + dimensionMeta("og:image:width", defaultImage ? Integer.valueOf(1200) : post.width())
            + dimensionMeta("og:image:height", defaultImage ? Integer.valueOf(630) : post.height());

        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>%s | %s</title>
                <meta name="description" content="%s">
                <meta property="og:type" content="article">
                <meta property="og:site_name" content="%s">
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:image" content="%s">
                <meta property="og:image:alt" content="%s">
                <meta property="og:locale" content="ko_KR">
            %s</head>
            <body>
                <a href="%s">%s</a>
            </body>
            </html>
            """.formatted(title, SITE_NAME, description, SITE_NAME, title, description,
                postUrl, escape(imageUrl), title, imageMetadata, postUrl, title);
    }

    private String dimensionMeta(String property, Integer value) {
        return value == null || value <= 0 ? "" : meta(property, value.toString());
    }

    private String meta(String property, String value) {
        return value == null || value.isBlank() ? ""
            : "    <meta property=\"%s\" content=\"%s\">\n".formatted(property, escape(value));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
