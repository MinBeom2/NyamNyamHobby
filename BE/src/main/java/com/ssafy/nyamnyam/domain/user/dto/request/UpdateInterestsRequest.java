package com.ssafy.nyamnyam.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 마이페이지 '내 취미 선택'. 보낸 목록으로 전체를 교체한다(빈 목록이면 모두 해제). */
public record UpdateInterestsRequest(
        @NotNull @Size(max = 5, message = "내 취미는 최대 5개까지 선택할 수 있습니다.")
        List<@NotNull @Positive Long> hobbyIds
) {}
