package com.ssafy.nyamnyam.global.upload;

/**
 * 업로드 용도. 용도별로 저장 폴더와 최대 크기가 다르다(API 명세 3.8: 프로필 2MB, 게시판 5MB).
 * 저장 위치: {app.upload.dir}/images/{folder}/yyyy/MM/{uuid}.{ext}
 */
public enum UploadPurpose {
    PROFILE("profile", 2L * 1024 * 1024),
    POST("post", 5L * 1024 * 1024);

    private final String folder;
    private final long maxBytes;

    UploadPurpose(String folder, long maxBytes) {
        this.folder = folder;
        this.maxBytes = maxBytes;
    }

    public String folder() {
        return folder;
    }

    public long maxBytes() {
        return maxBytes;
    }
}
