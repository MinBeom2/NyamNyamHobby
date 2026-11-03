package com.ssafy.nyamnyam.domain.shortform.dto.response;

import java.util.List;

public record SavedShortSummaryResponse(long totalCount, List<HobbyCount> hobbies) {
    // 취미가 없는 보관 항목도 집계하며 이때 id와 name은 null이다.
    public record HobbyCount(Long id, String name, long count) {}
}
