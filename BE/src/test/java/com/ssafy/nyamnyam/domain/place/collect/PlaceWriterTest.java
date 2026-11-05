package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.place.entity.Place;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.entity.PlaceStatus;
import com.ssafy.nyamnyam.domain.place.repository.PlaceRepository;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse.KakaoPlaceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceWriterTest {

    @Mock
    PlaceRepository placeRepository;

    @Mock
    RegionRepository regionRepository;

    @Mock
    com.ssafy.nyamnyam.global.infra.kakao.KakaoLocalClient kakaoLocalClient;

    @InjectMocks
    PlaceWriter placeWriter;

    Region mapoRegion;
    Hobby climbingHobby;

    @BeforeEach
    void setUp() {
        mapoRegion = Region.builder().code("1144000000").sido("서울특별시").sigungu("마포구").build();
        ReflectionTestUtils.setField(mapoRegion, "id", 14L);

        climbingHobby = Hobby.builder().name("클라이밍").build();
        ReflectionTestUtils.setField(climbingHobby, "id", 1L);
    }

    @Test
    @DisplayName("신규 장소를 정상 저장하고 취미를 연결한다 (CREATED)")
    void writeNewPlaceSuccess() {
        KakaoPlaceDocument doc = new KakaoPlaceDocument(
                "1001",
                "더클라임 마포점",
                "스포츠,레저 > 스포츠시설 > 암벽등반",
                "",
                "",
                "02-1234-5678",
                "서울 마포구 동교동 166-1",
                "서울 마포구 양화로 181",
                "126.924845",
                "37.557434",
                "https://place.map.kakao.com/1001",
                ""
        );

        given(regionRepository.findBySidoAndSigungu("서울특별시", "마포구")).willReturn(Optional.of(mapoRegion));
        given(placeRepository.findBySourceAndSourceRef("KAKAO_LOCAL", "1001")).willReturn(Optional.empty());

        PlaceWriter.WriteResult result = placeWriter.write(doc, climbingHobby);

        assertThat(result).isEqualTo(PlaceWriter.WriteResult.CREATED);

        ArgumentCaptor<Place> captor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());

        Place saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("더클라임 마포점");
        assertThat(saved.getCategory()).isEqualTo(PlaceCategory.GYM);
        assertThat(saved.getAddress()).isEqualTo("서울 마포구 양화로 181");
        assertThat(saved.getLat()).isEqualByComparingTo(new BigDecimal("37.557434"));
        assertThat(saved.getLng()).isEqualByComparingTo(new BigDecimal("126.924845"));
        assertThat(saved.getSource()).isEqualTo("KAKAO_LOCAL");
        assertThat(saved.getSourceRef()).isEqualTo("1001");
        assertThat(saved.getStatus()).isEqualTo(PlaceStatus.PUBLISHED);
        assertThat(saved.getHobbies()).contains(climbingHobby);
    }

    @Test
    @DisplayName("기존 장소가 존재하면 최신 정보로 갱신하고 새 취미를 추가한다 (UPDATED)")
    void writeExistingPlaceUpdatesAndAddsHobby() {
        KakaoPlaceDocument doc = new KakaoPlaceDocument(
                "1001",
                "더클라임 마포점 (수정됨)",
                "스포츠,레저 > 스포츠시설 > 암벽등반",
                "",
                "",
                "02-1234-5678",
                "서울 마포구 동교동 166-1",
                "서울 마포구 양화로 181",
                "126.924845",
                "37.557434",
                "https://place.map.kakao.com/1001",
                ""
        );

        Place existingPlace = Place.builder()
                .region(mapoRegion)
                .name("더클라임 마포점")
                .category(PlaceCategory.GYM)
                .address("서울 마포구 양화로 181")
                .lat(new BigDecimal("37.557434"))
                .lng(new BigDecimal("126.924845"))
                .source("KAKAO_LOCAL")
                .sourceRef("1001")
                .status(PlaceStatus.PUBLISHED)
                .build();

        Hobby yogaHobby = Hobby.builder().name("요가").build();
        ReflectionTestUtils.setField(yogaHobby, "id", 3L);

        given(regionRepository.findBySidoAndSigungu("서울특별시", "마포구")).willReturn(Optional.of(mapoRegion));
        given(placeRepository.findBySourceAndSourceRef("KAKAO_LOCAL", "1001")).willReturn(Optional.of(existingPlace));

        PlaceWriter.WriteResult result = placeWriter.write(doc, yogaHobby);

        assertThat(result).isEqualTo(PlaceWriter.WriteResult.UPDATED);
        assertThat(existingPlace.getName()).isEqualTo("더클라임 마포점 (수정됨)");
        assertThat(existingPlace.getHobbies()).contains(yogaHobby);
        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("서울 자치구 매핑에 실패하면 저장하지 않고 SKIPPED를 반환한다")
    void writeNonSeoulPlaceSkipped() {
        KakaoPlaceDocument doc = new KakaoPlaceDocument(
                "2001",
                "일산 클라이밍",
                "스포츠시설",
                "",
                "",
                "",
                "경기 고양시 일산동구 백석동 123",
                "경기 고양시 일산동구 중앙로 123",
                "126.7",
                "37.6",
                "https://place.map.kakao.com/2001",
                ""
        );

        PlaceWriter.WriteResult result = placeWriter.write(doc, climbingHobby);

        assertThat(result).isEqualTo(PlaceWriter.WriteResult.SKIPPED);
        verify(placeRepository, never()).save(any());
    }
}

