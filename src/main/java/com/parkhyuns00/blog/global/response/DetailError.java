package com.parkhyuns00.blog.global.response;

import java.util.List;

public record DetailError(
    String field,
    List<String> reasons
) {
}
