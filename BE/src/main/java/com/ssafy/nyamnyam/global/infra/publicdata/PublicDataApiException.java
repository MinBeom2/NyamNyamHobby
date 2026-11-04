package com.ssafy.nyamnyam.global.infra.publicdata;

/** 공공·문화 데이터 API 호출 실패. 메시지에는 인증키가 들어가지 않게 원인 종류만 담는다 */
public class PublicDataApiException extends RuntimeException {

    public PublicDataApiException(String message) {
        super(message);
    }
}
