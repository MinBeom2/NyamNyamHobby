package com.ssafy.nyamnyam.domain.user.dto.response;

import java.util.List;

/** 표시 이름 대신 DB ID로 온보딩을 요청할 수 있도록 선택지를 제공한다. */
public record OnboardingOptionsResponse(List<HobbyOption> hobbies, List<RegionResponse> regions) {
    public record HobbyOption(Long hobbyId, String name, Long categoryId, String categoryName) {}
}
