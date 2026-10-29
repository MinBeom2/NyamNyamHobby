package com.ssafy.nyamnyam.global.upload;

/** @param width·height 읽지 못하면 null */
public record ImageUploadResponse(String url, Integer width, Integer height) {
}
