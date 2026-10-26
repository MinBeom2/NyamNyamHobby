package com.ssafy.nyamnyam.global.infra.youtube;

/** YouTube Data API 호출 실패. 메시지에 API 키는 넣지 않는다. */
public class YoutubeApiException extends RuntimeException {

    public YoutubeApiException(String message) {
        super(message);
    }
}
