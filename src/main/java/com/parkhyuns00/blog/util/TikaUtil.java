package com.parkhyuns00.blog.util;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TikaUtil {

    private static final String IMAGE_PREFIX = "image/";

    private final Tika tika;

    public String detectMimeType(byte[] content) {
        return tika.detect(content);
    }

    public boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith(IMAGE_PREFIX);
    }
}
