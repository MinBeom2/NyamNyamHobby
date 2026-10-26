package com.ssafy.nyamnyam.global.infra.youtube;

import java.util.List;

/**
 * search.list 한 페이지 결과.
 *
 * @param nextPageToken 다음 페이지가 없으면 null
 */
public record YoutubeSearchPage(List<String> videoIds, String nextPageToken) {
}
