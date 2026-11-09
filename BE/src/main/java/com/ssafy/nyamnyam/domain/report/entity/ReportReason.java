package com.ssafy.nyamnyam.domain.report.entity;

/** 신고 사유(V1 reports.reason). 화면 문구와 짝을 맞춘다 */
public enum ReportReason {
    SPAM("스팸·홍보"),
    ABUSE("욕설·비방"),
    PRIVACY("개인정보 노출"),
    ADULT("부적절한 내용"),
    ETC("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
