package com.ssafy.nyamnyam.global.infra.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** search.list 응답(part=id). 쓰는 필드만 받는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeSearchResponse(String nextPageToken, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Id id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Id(String kind, String videoId) {
    }
}
