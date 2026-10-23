package com.ssafy.nyamnyam.domain.recommendation.model;

/** similarity는 사용자 선호 입력에서 -1~1, 취미 간 비교에서 0~1이다. */
public record HobbyMatch(long hobbyId, String name, String summary,
                         String thumbnailUrl, double similarity, int rank) {
}
