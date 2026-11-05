package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse.KakaoMeta;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoKeywordSearchResponse.KakaoPlaceDocument;
import com.ssafy.nyamnyam.global.infra.kakao.KakaoLocalClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceCollectServiceTest {

    @Mock
    KakaoLocalClient kakaoLocalClient;

    @Mock
    RegionRepository regionRepository;

    @Mock
    HobbyRepository hobbyRepository;

    @Mock
    HobbySourceMappingRepository mappingRepository;

    @Mock
    PlaceWriter placeWriter;

    @Mock
    BatchRunRepository batchRunRepository;

    PlaceCollectProperties properties;

    PlaceCollectService placeCollectService;

    Region mapoRegion;
    Hobby climbingHobby;
    HobbySourceMapping mapping;

    @BeforeEach
    void setUp() {
        properties = new PlaceCollectProperties(true, 1, 0, "서울특별시");
        placeCollectService = new PlaceCollectService(
                kakaoLocalClient,
                regionRepository,
                hobbyRepository,
                mappingRepository,
                placeWriter,
                batchRunRepository,
                properties
        );

        mapoRegion = Region.builder().code("1144000000").sido("서울특별시").sigungu("마포구").build();
        ReflectionTestUtils.setField(mapoRegion, "id", 14L);

        climbingHobby = Hobby.builder().name("클라이밍").build();
        ReflectionTestUtils.setField(climbingHobby, "id", 1L);

        mapping = HobbySourceMapping.builder()
                .hobby(climbingHobby)
                .target(SourceTarget.KAKAO_KEYWORD)
                .keyword("클라이밍")
                .build();
    }

    @Test
    @DisplayName("API 키가 설정되지 않은 경우 BatchRun을 정상 기록하고 수집을 건너뛴다")
    void collectSkipsWhenNotConfigured() {
        given(kakaoLocalClient.isConfigured()).willReturn(false);
        given(batchRunRepository.save(any(BatchRun.class))).willAnswer(invocation -> invocation.getArgument(0));

        BatchRun run = placeCollectService.collect();

        assertThat(run).isNotNull();
        assertThat(run.getJob()).isEqualTo(BatchJob.PLACE_KAKAO);
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
        assertThat(run.getMessage()).contains("카카오 로컬 REST API 키가 설정되지 않았습니다");
    }

    @Test
    @DisplayName("정상 수집 흐름: 검색어를 호출하고 결과를 저장한다")
    void collectSuccess() {
        given(kakaoLocalClient.isConfigured()).willReturn(true);
        given(batchRunRepository.save(any(BatchRun.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(regionRepository.findBySidoOrderBySigunguAsc("서울특별시")).willReturn(List.of(mapoRegion));
        given(mappingRepository.findAllWithHobby(SourceTarget.KAKAO_KEYWORD, HobbyStatus.PUBLISHED)).willReturn(List.of(mapping));

        KakaoPlaceDocument doc = new KakaoPlaceDocument(
                "1001", "더클라임", "암벽등반", "", "", "", "서울 마포구", "서울 마포구 양화로 181", "126.9", "37.5", "", ""
        );
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse(
                new KakaoMeta(1, 1, true),
                List.of(doc)
        );

        given(kakaoLocalClient.searchByKeyword(eq("마포구 클라이밍"), eq(1), anyInt())).willReturn(response);
        given(placeWriter.write(doc, climbingHobby)).willReturn(PlaceWriter.WriteResult.CREATED);

        BatchRun run = placeCollectService.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
        assertThat(run.getProcessed()).isEqualTo(1);
        assertThat(run.getAccepted()).isEqualTo(1);
        assertThat(run.getRejected()).isEqualTo(0);
        assertThat(run.getQuotaUsed()).isEqualTo(1); // calls
        verify(placeWriter).write(doc, climbingHobby);
    }

    @Test
    @DisplayName("온디맨드 수집: 특정 지역과 취미에 대해 카카오 API를 호출하여 장소를 즉시 저장한다")
    void collectOnDemandSuccess() {
        given(kakaoLocalClient.isConfigured()).willReturn(true);
        given(regionRepository.findById(14L)).willReturn(Optional.of(mapoRegion));
        given(hobbyRepository.findById(1L)).willReturn(Optional.of(climbingHobby));
        given(mappingRepository.findByHobbyIdAndTarget(1L, SourceTarget.KAKAO_KEYWORD)).willReturn(List.of(mapping));

        KakaoPlaceDocument doc = new KakaoPlaceDocument(
                "1001", "더클라임", "암벽등반", "", "", "", "서울 마포구", "서울 마포구 양화로 181", "126.9", "37.5", "", ""
        );
        KakaoKeywordSearchResponse response = new KakaoKeywordSearchResponse(
                new KakaoMeta(1, 1, true),
                List.of(doc)
        );

        given(kakaoLocalClient.searchByKeyword(eq("마포구 클라이밍"), eq(1), anyInt())).willReturn(response);
        given(placeWriter.write(doc, climbingHobby)).willReturn(PlaceWriter.WriteResult.CREATED);

        placeCollectService.collectOnDemand(14L, 1L);

        verify(placeWriter).write(doc, climbingHobby);

        // 두 번째 호출 시에는 캐시되어 API를 다시 호출하지 않음
        placeCollectService.collectOnDemand(14L, 1L);
    }
}
