package com.ssafy.nyamnyam.global.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 업로드 파일 저장 설정(app.upload.*). 파일은 서버 디스크에 저장하고, 백엔드가 /api/v1/uploads/images/** 로 내보낸다.
 *
 * @param dir           저장 폴더. 서버는 컨테이너의 /app/uploads(도커 볼륨), 로컬은 ./uploads
 * @param publicBaseUrl 응답 URL 앞부분. 서버는 https://k15d104.p.ssafy.io, 로컬은 http://localhost:8080
 */
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        @DefaultValue("./uploads") String dir,
        @DefaultValue("http://localhost:8080") String publicBaseUrl
) {
    /** 이미지가 공개되는 경로(컨트롤러·리소스 핸들러·보안 설정이 함께 쓴다) */
    public static final String IMAGE_PATH = "/api/v1/uploads/images/";

    /** 우리 서버가 발급한 이미지 URL은 모두 이 문자열로 시작한다 */
    public String imageUrlPrefix() {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + IMAGE_PATH;
    }

    /** 용도별 이미지 URL 앞부분(예: .../api/v1/uploads/images/post/). 게시글은 post 이미지만 첨부할 수 있다 */
    public String imageUrlPrefix(UploadPurpose purpose) {
        return imageUrlPrefix() + purpose.folder() + "/";
    }
}
