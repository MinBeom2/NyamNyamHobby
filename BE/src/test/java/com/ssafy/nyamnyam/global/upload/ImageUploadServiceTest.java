package com.ssafy.nyamnyam.global.upload;

import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadServiceTest {

    @TempDir
    Path tempDir;

    private ImageUploadService service() {
        return new ImageUploadService(new UploadProperties(tempDir.toString(), "https://k15d104.p.ssafy.io/"));
    }

    private static byte[] image(String format, int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    /** VP8X 헤더만 가진 최소 WebP(너비 1080, 높이 720) */
    private static byte[] webpVp8x(int w, int h) {
        byte[] b = new byte[40];
        System.arraycopy("RIFF".getBytes(), 0, b, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, b, 8, 4);
        System.arraycopy("VP8X".getBytes(), 0, b, 12, 4);
        int wm = w - 1, hm = h - 1;
        b[24] = (byte) wm; b[25] = (byte) (wm >> 8); b[26] = (byte) (wm >> 16);
        b[27] = (byte) hm; b[28] = (byte) (hm >> 8); b[29] = (byte) (hm >> 16);
        return b;
    }

    @Test
    @DisplayName("PNG·JPEG·WEBP를 저장하고 공개 URL과 크기를 돌려줌")
    void uploadFormats() throws IOException {
        ImageUploadService service = service();

        ImageUploadResponse png = service.upload(image("png", 64, 32), UploadPurpose.POST);
        ImageUploadResponse jpg = service.upload(image("jpg", 40, 20), UploadPurpose.POST);
        ImageUploadResponse webp = service.upload(webpVp8x(1080, 720), UploadPurpose.POST);

        assertThat(png.url()).startsWith("https://k15d104.p.ssafy.io/api/v1/uploads/images/post/").endsWith(".png");
        assertThat(png.width()).isEqualTo(64);
        assertThat(png.height()).isEqualTo(32);
        assertThat(jpg.url()).endsWith(".jpg");
        assertThat(jpg.width()).isEqualTo(40);
        assertThat(webp.url()).endsWith(".webp");
        assertThat(webp.width()).isEqualTo(1080);
        assertThat(webp.height()).isEqualTo(720);

        String relative = png.url().substring("https://k15d104.p.ssafy.io/api/v1/uploads/images/".length());
        Path saved = tempDir.resolve("images").resolve(relative);
        assertThat(Files.exists(saved)).isTrue();
        assertThat(relative).matches("post/\\d{4}/\\d{2}/[0-9a-f-]{36}\\.png");
    }

    @Test
    @DisplayName("프로필 이미지는 profile 폴더, 게시글 이미지는 post 폴더에 따로 저장")
    void separatesFolders() throws IOException {
        ImageUploadService service = service();
        String base = "https://k15d104.p.ssafy.io/api/v1/uploads/images/";

        ImageUploadResponse profile = service.upload(image("png", 8, 8), UploadPurpose.PROFILE);
        ImageUploadResponse post = service.upload(image("png", 8, 8), UploadPurpose.POST);

        assertThat(profile.url()).startsWith(base + "profile/");
        assertThat(post.url()).startsWith(base + "post/");
        assertThat(Files.exists(tempDir.resolve("images").resolve(profile.url().substring(base.length())))).isTrue();
        assertThat(Files.exists(tempDir.resolve("images").resolve(post.url().substring(base.length())))).isTrue();
    }

    @Test
    @DisplayName("이미지가 아닌 파일·확장자만 바꾼 파일·빈 파일은 UPLOAD_INVALID_FILE")
    void rejectsInvalid() {
        ImageUploadService service = service();
        assertThatThrownBy(() -> service.upload("<script>alert(1)</script>".getBytes(), UploadPurpose.POST))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE);
        assertThatThrownBy(() -> service.upload(new byte[0], UploadPurpose.POST))
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE);

        byte[] fakeJpeg = new byte[100];
        fakeJpeg[0] = (byte) 0xFF; fakeJpeg[1] = (byte) 0xD8; fakeJpeg[2] = (byte) 0xFF;
        assertThatThrownBy(() -> service.upload(fakeJpeg, UploadPurpose.POST))
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE);
    }

    @Test
    @DisplayName("용도별 최대 크기를 넘으면 UPLOAD_TOO_LARGE (프로필 2MB)")
    void rejectsTooLarge() {
        byte[] big = Arrays.copyOf(webpVp8x(10, 10), 2 * 1024 * 1024 + 1);
        assertThatThrownBy(() -> service().upload(big, UploadPurpose.PROFILE))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_TOO_LARGE);
    }

    @Test
    @DisplayName("매직 바이트로 형식 판별")
    void detect() throws IOException {
        assertThat(ImageFormat.detect(image("png", 2, 2))).isEqualTo(ImageFormat.PNG);
        assertThat(ImageFormat.detect(image("jpg", 2, 2))).isEqualTo(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(webpVp8x(2, 2))).isEqualTo(ImageFormat.WEBP);
        assertThat(ImageFormat.detect("GIF89a-not-allowed".getBytes())).isNull();
    }
}
