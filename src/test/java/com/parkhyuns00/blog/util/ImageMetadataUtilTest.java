package com.parkhyuns00.blog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkhyuns00.blog.domain.post.exception.PostException;
import com.parkhyuns00.blog.domain.post.exception.PostExceptionCode;
import com.parkhyuns00.blog.util.dto.ImageMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ImageMetadataUtilTest {

    private final ImageMetadataUtil imageMetadataUtil = new ImageMetadataUtil();

    @Test
    @DisplayName("이미지 바이트에서 너비와 높이를 추출한다.")
    void test_extract_image_metadata_success() throws IOException {
        BufferedImage image = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ImageIO.write(image, "png", outputStream);

        ImageMetadata result = imageMetadataUtil.extract(outputStream.toByteArray());

        assertThat(result.width()).isEqualTo(320);
        assertThat(result.height()).isEqualTo(180);
    }

    @Test
    @DisplayName("이미지 메타데이터를 읽을 수 없으면 예외가 발생한다.")
    void test_extract_image_metadata_fail_when_invalid_image() {
        byte[] invalidContent = "invalid-image".getBytes();

        assertThatThrownBy(() -> imageMetadataUtil.extract(invalidContent))
            .isInstanceOf(PostException.class)
            .extracting("exceptionCode")
            .isEqualTo(PostExceptionCode.POST_IMAGE_METADATA_READ_FAILED);
    }

    @Test
    @DisplayName("Webp 이미지에서 너비와 높이를 추출한다.")
    void test_extract_webp_metadata_success() {
        ImageMetadata result = imageMetadataUtil.extract(createWebpHeader(320, 180));

        assertThat(result.width()).isEqualTo(320);
        assertThat(result.height()).isEqualTo(180);
    }

    private byte[] createWebpHeader(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);

        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(22);
        buffer.put("WEBP".getBytes(StandardCharsets.US_ASCII));
        buffer.put("VP8X".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(10);
        buffer.putInt(0);
        putUnsigned24Bit(buffer, width - 1);
        putUnsigned24Bit(buffer, height - 1);

        return buffer.array();
    }

    private void putUnsigned24Bit(ByteBuffer buffer, int value) {
        buffer.put((byte) value);
        buffer.put((byte) (value >>> 8));
        buffer.put((byte) (value >>> 16));
    }
}
