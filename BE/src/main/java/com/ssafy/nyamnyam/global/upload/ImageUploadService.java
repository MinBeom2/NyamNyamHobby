package com.ssafy.nyamnyam.global.upload;

import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 이미지 업로드: 형식·크기를 검사하고 용도별 폴더({dir}/images/{profile|post}/yyyy/MM/{uuid}.{ext})에 저장한 뒤
 * 공개 URL을 돌려준다.
 * 파일 이름은 서버가 만들므로 사용자가 보낸 이름은 쓰지 않는다(경로 조작 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UploadProperties properties;

    public ImageUploadResponse upload(byte[] bytes, UploadPurpose purpose) {
        if (bytes == null || bytes.length == 0) {
            throw new CustomException(ErrorCode.UPLOAD_INVALID_FILE, "빈 파일은 올릴 수 없어요.");
        }
        if (bytes.length > purpose.maxBytes()) {
            throw new CustomException(ErrorCode.UPLOAD_TOO_LARGE,
                    "이미지는 " + (purpose.maxBytes() / 1024 / 1024) + "MB 이하만 올릴 수 있어요.");
        }
        ImageFormat format = ImageFormat.detect(bytes);
        if (format == null) {
            throw new CustomException(ErrorCode.UPLOAD_INVALID_FILE);
        }
        int[] size = format.readSize(bytes);
        if (format == ImageFormat.JPEG && size == null) {
            // JPEG 머리만 맞고 실제로는 읽을 수 없는 파일
            throw new CustomException(ErrorCode.UPLOAD_INVALID_FILE, "이미지 파일을 읽을 수 없어요.");
        }

        LocalDate today = LocalDate.now(KST);
        String relative = "%s/%d/%02d/%s.%s".formatted(purpose.folder(),
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), format.extension());
        Path target = imagesRoot().resolve(relative).normalize();

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            log.error("Image save failed: {}", target, e);
            throw new CustomException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, "이미지를 저장하지 못했어요.");
        }

        return new ImageUploadResponse(
                properties.imageUrlPrefix() + relative,
                size != null ? size[0] : null,
                size != null ? size[1] : null
        );
    }

    /** 이미지 저장 루트({dir}/images). 리소스 핸들러도 같은 경로를 쓴다 */
    public Path imagesRoot() {
        return Paths.get(properties.dir()).toAbsolutePath().normalize().resolve("images");
    }
}
