package com.parkhyuns00.blog.domain.post.event;

import com.parkhyuns00.blog.util.GarageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostImageCleanupEventListener {

    private final GarageUtil garageUtil;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostImageCleanupEvent event) {
        for (String objectKey : event.imageObjectKeys()) {
            try {
                garageUtil.deleteObject(objectKey);
            } catch (RuntimeException exception) {
                log.error("Failed to delete post image from object storage. objectKey={}", objectKey, exception);
            }
        }
    }
}
