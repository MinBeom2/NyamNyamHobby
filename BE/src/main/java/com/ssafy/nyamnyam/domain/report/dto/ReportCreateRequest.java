package com.ssafy.nyamnyam.domain.report.dto;

import com.ssafy.nyamnyam.domain.report.entity.ReportReason;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 게시글·댓글 신고. targetType은 POST|COMMENT, reason은 SPAM|ABUSE|PRIVACY|ADULT|ETC */
public record ReportCreateRequest(
        @NotNull(message = "신고 대상을 선택해 주세요.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상을 선택해 주세요.")
        Long targetId,

        @NotNull(message = "신고 사유를 선택해 주세요.")
        ReportReason reason,

        @Size(max = 300, message = "상세 사유는 300자 이하로 입력해 주세요.")
        String detail
) {
}
