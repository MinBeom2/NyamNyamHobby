package com.ssafy.nyamnyam.domain.shortform.dto.response;

import com.ssafy.nyamnyam.domain.exploration.dto.response.ShortDetailResponse;

import java.util.List;

/**
 * 자유 탐색 숏폼 한 묶음.
 *
 * @param seed       섞는 순서 값(1~2^31-1). 다음 요청에 그대로 보내야 같은 순서로 이어진다
 * @param nextCursor 다음 요청의 cursor. 한 바퀴를 다 봤으면 null(화면은 seed 없이 다시 요청해 새로 섞는다)
 * @param total      공개 영상 전체 수
 */
public record ShortFeedResponse(List<ShortDetailResponse> items, long seed, Integer nextCursor, int total) {
}
