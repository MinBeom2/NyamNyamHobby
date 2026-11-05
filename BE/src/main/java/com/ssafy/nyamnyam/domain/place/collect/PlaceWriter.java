package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.place.entity.Place;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.entity.PlaceStatus;
import com.ssafy.nyamnyam.domain.place.repository.PlaceRepository;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse.KakaoPlaceDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceWriter {

    public enum WriteResult {
        CREATED,
        UPDATED,
        SKIPPED
    }

    private static final String SOURCE_KAKAO = "KAKAO_LOCAL";
    private static final int NAME_MAX = 100;
    private static final int ADDRESS_MAX = 200;
    private static final int URL_MAX = 500;

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;

    /**
     * 카카오 로컬 검색 결과 한 건을 places 테이블에 저장(UPSERT)하고 취미(N:M)를 연결한다.
     */
    @Transactional
    public WriteResult write(KakaoPlaceDocument doc, Hobby hobby) {
        if (doc == null || doc.id() == null || doc.id().isBlank()) {
            return WriteResult.SKIPPED;
        }

        String address = resolveAddress(doc);
        if (address.isBlank()) {
            return WriteResult.SKIPPED;
        }

        Region region = resolveRegion(address);
        if (region == null && doc.addressName() != null && !doc.addressName().isBlank()) {
            region = resolveRegion(doc.addressName());
        }
        if (region == null) {
            // 서울 25개 자치구에 속하지 않거나 지역 매핑이 불가능한 경우 건너뜀
            return WriteResult.SKIPPED;
        }

        BigDecimal lat = parseCoordinate(doc.y());
        BigDecimal lng = parseCoordinate(doc.x());
        if (lat == null || lng == null) {
            return WriteResult.SKIPPED;
        }

        PlaceCategory category = PlaceCategoryClassifier.classify(doc.categoryName());

        Optional<Place> existingOpt = placeRepository.findBySourceAndSourceRef(SOURCE_KAKAO, doc.id());
        if (existingOpt.isPresent()) {
            Place place = existingOpt.get();
            place.updateFromSource(
                    cut(doc.placeName(), NAME_MAX),
                    cut(address, ADDRESS_MAX),
                    lat,
                    lng,
                    cut(doc.placeUrl(), URL_MAX),
                    category,
                    region
            );
            place.addHobbyIfAbsent(hobby);
            return WriteResult.UPDATED;
        }

        LocalDateTime now = LocalDateTime.now();
        Place place = Place.builder()
                .region(region)
                .name(cut(doc.placeName(), NAME_MAX))
                .category(category)
                .address(cut(address, ADDRESS_MAX))
                .lat(lat)
                .lng(lng)
                .externalUrl(cut(doc.placeUrl(), URL_MAX))
                .source(SOURCE_KAKAO)
                .sourceRef(doc.id())
                .sourceUpdatedAt(now)
                .status(PlaceStatus.PUBLISHED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        place.addHobbyIfAbsent(hobby);
        placeRepository.save(place);
        return WriteResult.CREATED;
    }

    private String resolveAddress(KakaoPlaceDocument doc) {
        if (doc.roadAddressName() != null && !doc.roadAddressName().isBlank()) {
            return doc.roadAddressName().trim();
        }
        if (doc.addressName() != null && !doc.addressName().isBlank()) {
            return doc.addressName().trim();
        }
        return "";
    }

    private Region resolveRegion(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String[] tokens = address.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }

        String sidoToken = tokens[0];
        if ("서울".equals(sidoToken) || "서울특별시".equals(sidoToken)) {
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].endsWith("구")) {
                    return regionRepository.findBySidoAndSigungu("서울특별시", tokens[i]).orElse(null);
                }
            }
        }
        return null;
    }

    private BigDecimal parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

