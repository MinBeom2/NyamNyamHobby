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
    @Mock HobbySourceMappingRepository mappings;
    @Mock FeedWriter writer;
    @Mock BatchRunRepository runs;

    FeedCollectService service;
    Hobby climbing;
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach void setUp() {
        service = new FeedCollectService(tour, kopis, mappings, writer, runs,
                new FeedCollectProperties(true, "11", "11", 60, 100, 5, 50, 0.8));
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
}
