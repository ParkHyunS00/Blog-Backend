package com.parkhyuns00.blog.domain.post.event;

import java.util.List;

public record PostDeletedEvent(
    List<String> imageObjectKeys
) {
    public PostDeletedEvent {
        imageObjectKeys = imageObjectKeys == null ? List.of() : List.copyOf(imageObjectKeys);
    }
}
