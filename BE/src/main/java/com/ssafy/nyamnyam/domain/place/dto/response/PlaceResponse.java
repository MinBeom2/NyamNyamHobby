package com.ssafy.nyamnyam.domain.place.dto.response;

import com.ssafy.nyamnyam.domain.place.entity.Place;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;

import java.math.BigDecimal;

public record PlaceResponse(
        Long id,
        String name,
        PlaceCategory category,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String openingHours,
        String feeText,
        String externalUrl,
        String thumbnailUrl,
        String source,
        boolean needsInfoCheck
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getLat(),
                place.getLng(),
                place.getOpeningHours(),
                place.getFeeText(),
                place.getExternalUrl(),
                place.getThumbnailUrl(),
                place.getSource(),
                place.computeNeedsInfoCheck()
        );
    }
}
