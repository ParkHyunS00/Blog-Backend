package com.parkhyuns00.blog.domain.post.event;

import java.util.List;

public record PostImageCleanupEvent(
    List<String> imageObjectKeys
) {
    public PostImageCleanupEvent {
        imageObjectKeys = imageObjectKeys == null ? List.of() : List.copyOf(imageObjectKeys);
    }
}
