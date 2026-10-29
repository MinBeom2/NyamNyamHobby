package com.ssafy.nyamnyam.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 좋아요 등록·취소 결과: 변경된 총 좋아요 수와 내 좋아요 여부 */
public record PostLikeResponse(int likeCount, @JsonProperty("isLiked") boolean isLiked) {
}
