package com.ssafy.nyamnyam.domain.place;

import com.ssafy.nyamnyam.domain.place.collect.PlaceCollectService;
import com.ssafy.nyamnyam.domain.place.dto.response.PlaceResponse;
import com.ssafy.nyamnyam.domain.place.entity.Place;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.entity.PlaceStatus;
import com.ssafy.nyamnyam.domain.place.repository.PlaceRepository;
import com.ssafy.nyamnyam.domain.place.service.PlaceService;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    PlaceRepository placeRepository;

    @Mock
    PlaceCollectService placeCollectService;

    @InjectMocks
    PlaceService placeService;

    @Test
    @DisplayName("장소 검색 시 PUBLISHED 상태 장소 목록과 needsInfoCheck 플래그가 정상 계산된다")
    void searchPlacesCalculatesNeedsInfoCheck() {
        Region region = Region.builder().code("1111000000").sido("서울특별시").sigungu("종로구").build();

        // 1) 정상 장소 (최근 갱신, 운영시간 있음) -> needsInfoCheck = false
        Place validPlace = Place.builder()
                .region(region)
                .name("종로 클라이밍")
                .category(PlaceCategory.STUDIO)
                .address("서울특별시 종로구 세종대로 1")
                .lat(new BigDecimal("37.5"))
                .lng(new BigDecimal("126.9"))
                .openingHours("매일 10:00~22:00")
                .feeText("20,000원")
                .source("KAKAO_LOCAL")
                .sourceUpdatedAt(LocalDateTime.now().minusDays(10))
                .status(PlaceStatus.PUBLISHED)
                .build();

        // 2) 운영시간 누락 장소 -> needsInfoCheck = true
        Place noHoursPlace = Place.builder()
                .region(region)
                .name("미확인 암장")
                .category(PlaceCategory.GYM)
                .address("서울특별시 종로구 대학로 1")
                .openingHours(null)
                .source("ADMIN")
                .status(PlaceStatus.PUBLISHED)
                .build();

        given(placeRepository.findAll(any(Specification.class), any(Sort.class)))
                .willReturn(List.of(validPlace, noHoursPlace));

        List<PlaceResponse> responses = placeService.searchPlaces(1L, null, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).needsInfoCheck()).isFalse();
        assertThat(responses.get(1).needsInfoCheck()).isTrue();
    }
}
