package com.ssafy.nyamnyam.global.infra.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoImageSearchResponse(
        KakaoMeta meta,
        List<KakaoImageDocument> documents
) {
    public record KakaoMeta(
            @JsonProperty("total_count") int totalCount,
            @JsonProperty("pageable_count") int pageableCount,
            @JsonProperty("is_end") boolean isEnd
    ) {}

    public record KakaoImageDocument(
            String collection,
            @JsonProperty("thumbnail_url") String thumbnailUrl,
            @JsonProperty("image_url") String imageUrl,
            int width,
            int height,
            @JsonProperty("display_sitename") String displaySitename,
            @JsonProperty("doc_url") String docUrl,
            String datetime
    ) {}
}

