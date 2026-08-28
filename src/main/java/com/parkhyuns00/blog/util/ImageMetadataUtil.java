package com.parkhyuns00.blog.util;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.util.dto.ImageMetadata;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class ImageMetadataUtil {

    public ImageMetadata extract(byte[] content) {
        try (
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)
        ) {
            if (imageInputStream == null) {
                throw metadataReadFailed();
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw metadataReadFailed();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw metadataReadFailed();
                }

                return new ImageMetadata(width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new PostException(PostExceptionCode.POST_IMAGE_METADATA_READ_FAILED, exception);
        }
    }

    private PostException metadataReadFailed() {
        return new PostException(PostExceptionCode.POST_IMAGE_METADATA_READ_FAILED);
    }
}
