package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.infra.publicdata.CulturePortalClient;
import com.ssafy.nyamnyam.global.infra.publicdata.KopisClient;
import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import com.ssafy.nyamnyam.global.infra.publicdata.TourApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedCollectServiceTest {

    @Mock TourApiClient tour;
    @Mock KopisClient kopis;
    @Mock CulturePortalClient culture;
    @Mock RegionRepository regions;
    @Mock HobbySourceMappingRepository mappings;
    @Mock FeedWriter writer;
    @Mock BatchRunRepository runs;

    FeedCollectService service;
    Hobby climbing;
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach void setUp() {
        service = new FeedCollectService(tour, kopis, culture, regions, mappings, writer, runs,
                new FeedCollectProperties(true, "11", "11", 60, 100, 5, 50, 0.8, "서울", "서울특별시", 100, 5, 0, 0));
        climbing = Hobby.builder().name("클라이밍").build();
        ReflectionTestUtils.setField(climbing, "id", 1L);
        given(runs.save(any(BatchRun.class))).willAnswer(call -> call.getArgument(0));
        lenient().when(mappings.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED)).thenReturn(List.of(
                HobbySourceMapping.builder().hobby(climbing).target(SourceTarget.EVENT_KEYWORD).keyword("클라이밍").build()));
    }

    @Test void savesOnlyHobbyRelatedFestivalsWithDetail() {
        given(tour.isConfigured()).willReturn(true);
        given(tour.searchFestivals("11", today, 1, 100)).willReturn(new TourApiClient.FestivalPage(List.of(
                new TourApiClient.Festival("1", "서울 클라이밍 페스티벌", today, today.plusDays(2), "http://img", "11710"),
                new TourApiClient.Festival("2", "가락 옥토버페스트", today, today.plusDays(2), null, "11710")), 2));
        given(tour.detail("1")).willReturn(Optional.of(new TourApiClient.FestivalDetail("볼더링 체험", "https://fest.example.com")));
        given(writer.create(any(), eq(climbing), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        ArgumentCaptor<FeedCandidate> saved = ArgumentCaptor.forClass(FeedCandidate.class);
        verify(writer).create(saved.capture(), eq(climbing), eq(0.8));
        assertThat(saved.getValue()).satisfies(c -> {
            assertThat(c.origin()).isEqualTo(FeedOrigin.TOUR_API);
            assertThat(c.type()).isEqualTo(FeedType.EVENT);
            assertThat(c.summary()).isEqualTo("볼더링 체험");
            assertThat(c.sourceUrl()).isEqualTo("https://fest.example.com");
            assertThat(c.regionCode()).isEqualTo("11710");
        });
        verify(tour, never()).detail("2"); // 취미와 무관한 행사는 상세를 부르지 않는다
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
        assertThat(run.getAccepted()).isEqualTo(1);
        assertThat(run.getRejected()).isEqualTo(1);
        assertThat(run.getMessage()).contains("kopis 키 없음");
    }

    @Test void alreadyCollectedFestivalIsRefreshedWithoutDetailCall() {
        given(tour.isConfigured()).willReturn(true);
        given(tour.searchFestivals("11", today, 1, 100)).willReturn(new TourApiClient.FestivalPage(List.of(
                new TourApiClient.Festival("1", "서울 클라이밍 페스티벌", today, today.plusDays(3), null, null)), 1));
        given(writer.refreshIfExists(FeedOrigin.TOUR_API, "1", "서울 클라이밍 페스티벌", null, today, today.plusDays(3)))
                .willReturn(true);

        service.collect();

        verify(tour, never()).detail(any());
        verify(writer, never()).create(any(), any(), anyDouble());
    }

    @Test void oneSourceFailingKeepsTheOtherAndMarksPartial() {
        given(tour.isConfigured()).willReturn(true);
        given(kopis.isConfigured()).willReturn(true);
        given(tour.searchFestivals(any(), any(), anyInt(), anyInt())).willThrow(new PublicDataApiException("TourAPI 오류 응답: 30"));
        given(kopis.search(eq("클라이밍"), eq("11"), eq(today), eq(today.plusDays(60)), eq(1), eq(50))).willReturn(List.of(
                new KopisClient.Performance("PF1", "클라이밍 뮤지컬", today, today.plusDays(1), "http://p", "홀", "뮤지컬")));
        given(writer.create(any(), eq(climbing), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        ArgumentCaptor<FeedCandidate> saved = ArgumentCaptor.forClass(FeedCandidate.class);
        verify(writer).create(saved.capture(), eq(climbing), anyDouble());
        assertThat(saved.getValue().sourceUrl()).endsWith("mt20Id=PF1");
        assertThat(saved.getValue().summary()).isEqualTo("뮤지컬 · 홀");
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        assertThat(run.getMessage()).contains("tour 실패");
    }

    @Test void keepsOnlyExhibitionsAndFallsBackToExhibitionHobby() {
        Hobby exhibition = Hobby.builder().name("전시 관람").build();
        ReflectionTestUtils.setField(exhibition, "id", 30L);
        given(mappings.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED)).willReturn(List.of(
                HobbySourceMapping.builder().hobby(climbing).target(SourceTarget.EVENT_KEYWORD).keyword("클라이밍").build(),
                HobbySourceMapping.builder().hobby(exhibition).target(SourceTarget.EVENT_KEYWORD).keyword("갤러리").build()));
        given(culture.isConfigured()).willReturn(true);
        given(regions.findBySidoOrderBySigunguAsc("서울특별시")).willReturn(List.of(
                Region.builder().code("11710").sido("서울특별시").sigungu("송파구").build()));
        given(culture.searchByRealm("서울", "D000", today, today.plusDays(60), 1, 100)).willReturn(new CulturePortalClient.EventPage(List.of(
                new CulturePortalClient.Event("1", "현대 회화 기획전", today, today.plusDays(10), "미술관", "전시", "송파구", "http://t"),
                new CulturePortalClient.Event("2", "롯콘 마티네", today, today.plusDays(1), "홀", "음악/콘서트", "송파구", null)), 2));
        given(culture.detail("1")).willReturn(Optional.of(new CulturePortalClient.EventDetail(null, "http://museum", null)));
        given(writer.create(any(), eq(exhibition), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        ArgumentCaptor<FeedCandidate> saved = ArgumentCaptor.forClass(FeedCandidate.class);
        verify(writer).create(saved.capture(), eq(exhibition), anyDouble());
        assertThat(saved.getValue()).satisfies(c -> {
            assertThat(c.origin()).isEqualTo(FeedOrigin.CULTURE_PORTAL);
            assertThat(c.type()).isEqualTo(FeedType.SHOW);
            assertThat(c.sourceUrl()).isEqualTo("http://museum"); // 예매 링크가 없으면 장소 홈페이지
            assertThat(c.summary()).isEqualTo("미술관");           // 소개가 없으면 장소 이름
            assertThat(c.regionCode()).isEqualTo("11710");
        });
        verify(culture, never()).detail("2"); // 전시가 아니면 상세를 부르지 않는다
        assertThat(run.getRejected()).isEqualTo(1);
    }

    @Test void oneKopisKeywordFailingSkipsOnlyThatKeyword() {
        Hobby guitar = Hobby.builder().name("기타").build();
        ReflectionTestUtils.setField(guitar, "id", 18L);
        given(mappings.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED)).willReturn(List.of(
                HobbySourceMapping.builder().hobby(climbing).target(SourceTarget.EVENT_KEYWORD).keyword("클라이밍").build(),
                HobbySourceMapping.builder().hobby(guitar).target(SourceTarget.EVENT_KEYWORD).keyword("기타").build()));
        given(kopis.isConfigured()).willReturn(true);
        // 재시도까지 두 번 모두 실패
        given(kopis.search(eq("클라이밍"), any(), any(), any(), anyInt(), anyInt()))
                .willThrow(new PublicDataApiException("KOPIS 호출 실패(클라이밍): HTTP 400"));
        given(kopis.search(eq("기타"), any(), any(), any(), anyInt(), anyInt())).willReturn(List.of(
                new KopisClient.Performance("PF2", "기타 리사이틀", today, today, null, "홀", "클래식")));
        given(writer.create(any(), eq(guitar), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        verify(writer).create(any(), eq(guitar), anyDouble());
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        assertThat(run.getMessage()).contains("kopis 검색어 1/2개 실패(클라이밍)");
    }

    @Test void allKopisKeywordsFailingMarksKopisFailed() {
        given(kopis.isConfigured()).willReturn(true);
        given(kopis.search(any(), any(), any(), any(), anyInt(), anyInt()))
                .willThrow(new PublicDataApiException("KOPIS 호출 실패(클라이밍): HTTP 400"));

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
        assertThat(run.getMessage()).contains("kopis 실패(KOPIS 호출 실패(클라이밍): HTTP 400)");
    }

    @Test void kopisRetriesOnceAfterATemporaryBlock() {
        given(kopis.isConfigured()).willReturn(true);
        given(kopis.search(eq("클라이밍"), any(), any(), any(), anyInt(), anyInt()))
                .willThrow(new PublicDataApiException("KOPIS 호출 실패(클라이밍): HTTP 400 400 Bad Request"))
                .willReturn(List.of(new KopisClient.Performance("PF3", "클라이밍 뮤지컬", today, today, null, "홀", "뮤지컬")));
        given(writer.create(any(), eq(climbing), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        verify(kopis, org.mockito.Mockito.times(2)).search(eq("클라이밍"), any(), any(), any(), anyInt(), anyInt());
        verify(writer).create(any(), eq(climbing), anyDouble());
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
    }
}
