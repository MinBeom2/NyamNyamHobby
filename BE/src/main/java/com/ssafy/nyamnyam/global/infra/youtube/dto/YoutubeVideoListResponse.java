package com.ssafy.nyamnyam.global.infra.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** videos.list 응답(part=snippet,contentDetails,status,statistics). 쓰는 필드만 받는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeVideoListResponse(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String id, Snippet snippet, ContentDetails contentDetails, Status status, Statistics statistics) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippet(String publishedAt, String channelId, String channelTitle,
                          String title, String description, Thumbnails thumbnails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnails(Thumbnail high, Thumbnail medium, @JsonProperty("default") Thumbnail standardDefault) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(String url) {
    }

    /** duration은 ISO 8601 기간(예: PT58S). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentDetails(String duration, RegionRestriction regionRestriction) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegionRestriction(List<String> allowed, List<String> blocked) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String privacyStatus, Boolean embeddable) {
    }

    /** API가 숫자를 문자열로 준다. 조회수를 숨긴 영상은 viewCount가 없다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Statistics(String viewCount) {
    }
}
