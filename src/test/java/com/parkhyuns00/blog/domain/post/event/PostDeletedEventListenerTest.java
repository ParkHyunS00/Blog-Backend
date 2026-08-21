package com.parkhyuns00.blog.domain.post.event;

import static org.mockito.Mockito.*;

import com.parkhyuns00.blog.util.GarageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PostDeletedEventListenerTest {

    private final GarageUtil garageUtil = mock(GarageUtil.class);
    private final PostDeletedEventListener listener = new PostDeletedEventListener(garageUtil);

    @Test
    @DisplayName("게시글 삭제 후 연결된 Garage 이미지 객체를 삭제한다.")
    void test_handle_post_deleted_event_success() {
        PostDeletedEvent event = new PostDeletedEvent(List.of("posts/thumbnail/test.png", "posts/content/test.png"));

        listener.handle(event);

        verify(garageUtil).deleteObject("posts/thumbnail/test.png");
        verify(garageUtil).deleteObject("posts/content/test.png");
    }

    @Test
    @DisplayName("일부 이미지 삭제에 실패해도 나머지 이미지 삭제를 계속한다.")
    void test_continue_when_image_delete_failed() {
        doThrow(new RuntimeException("delete failed"))
            .when(garageUtil)
            .deleteObject("posts/thumbnail/test.png");

        PostDeletedEvent event = new PostDeletedEvent(List.of("posts/thumbnail/test.png", "posts/content/test.png"));

        listener.handle(event);

        verify(garageUtil).deleteObject("posts/thumbnail/test.png");
        verify(garageUtil).deleteObject("posts/content/test.png");
    }
}
