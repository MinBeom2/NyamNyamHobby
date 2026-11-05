package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import com.ssafy.nyamnyam.global.infra.publicdata.SeoulReservationClient;
import com.ssafy.nyamnyam.global.infra.publicdata.SeoulReservationClient.Course;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedClassCollectServiceTest {

    @Mock SeoulReservationClient seoul;
    @Mock HobbySourceMappingRepository mappings;
    @Mock RegionRepository regions;
    @Mock FeedWriter writer;
    @Mock BatchRunRepository runs;

    FeedClassCollectService service;
    Hobby yoga;
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach void setUp() {
        service = new FeedClassCollectService(seoul, mappings, regions, writer, runs,
                new FeedCollectProperties(true, "11", "11", 60, 100, 5, 50, 0.8, "서울", "서울특별시", 100, 5, 0, 0, 1000, 5));
        yoga = Hobby.builder().name("요가").build();
        ReflectionTestUtils.setField(yoga, "id", 3L);
        given(runs.save(any(BatchRun.class))).willAnswer(call -> call.getArgument(0));
        lenient().when(mappings.findAllWithHobby(SourceTarget.EVENT_KEYWORD, HobbyStatus.PUBLISHED)).thenReturn(List.of(
                HobbySourceMapping.builder().hobby(yoga).target(SourceTarget.EVENT_KEYWORD).keyword("요가").build()));
        lenient().when(regions.findBySidoOrderBySigunguAsc("서울특별시")).thenReturn(List.of(
                Region.builder().code("11110").sido("서울특별시").sigungu("종로구").build()));
    }

    private Course course(String id, String title, LocalDate end) {
        return new Course(id, title, "체육", "접수중", "무료", "종로문화체육센터", "성인",
                "https://yeyak.seoul.go.kr/web/reservation/selectReservView.do?rsv_svc_id=" + id, "http://img",
                "종로구", today.minusDays(3), end);
    }

    @Test void savesHobbyClassesWithStatusInSummary() {
        given(seoul.isConfigured()).willReturn(true);
        given(seoul.list(1, 1000)).willReturn(new SeoulReservationClient.CoursePage(List.of(
                course("S1", "저녁 힐링 요가 교실", today.plusDays(20)),
                course("S2", "경희궁 역사 탐험대", today.plusDays(20)),
                course("S3", "아침 요가 (지난 기수)", today.minusDays(1))), 3));
        given(writer.create(any(), eq(yoga), anyDouble())).willReturn(FeedWriter.Result.CREATED);

        BatchRun run = service.collect();

        ArgumentCaptor<FeedCandidate> saved = ArgumentCaptor.forClass(FeedCandidate.class);
        verify(writer).create(saved.capture(), eq(yoga), eq(0.8));
        assertThat(saved.getValue()).satisfies(c -> {
            assertThat(c.origin()).isEqualTo(FeedOrigin.SEOUL_RESERVATION);
            assertThat(c.type()).isEqualTo(FeedType.CLASS);
            assertThat(c.summary()).isEqualTo("접수중 · 무료 · 종로문화체육센터");
            assertThat(c.sourceUrl()).endsWith("rsv_svc_id=S1");
            assertThat(c.regionCode()).isEqualTo("11110");
        });
        assertThat(run.getJob()).isEqualTo(BatchJob.CLASS_COLLECT);
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
        assertThat(run.getAccepted()).isEqualTo(1);
        assertThat(run.getRejected()).isEqualTo(2); // 취미와 무관 1, 운영 끝남 1
    }

    @Test void existingClassRefreshesStatusSummary() {
        given(seoul.isConfigured()).willReturn(true);
        given(seoul.list(1, 1000)).willReturn(new SeoulReservationClient.CoursePage(List.of(
                course("S1", "저녁 힐링 요가 교실", today.plusDays(20))), 1));
        given(writer.refreshIfExists(eq(FeedOrigin.SEOUL_RESERVATION), eq("S1"), any(), any(), any(), any(),
                eq("접수중 · 무료 · 종로문화체육센터"))).willReturn(true);

        service.collect();

        verify(writer, never()).create(any(), any(), anyDouble());
    }

    @Test void missingKeyOrApiErrorIsRecordedAsFailed() {
        given(seoul.isConfigured()).willReturn(true);
        given(seoul.list(1, 1000)).willThrow(new PublicDataApiException("서울 공공서비스예약 오류 응답: INFO-100"));

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
        assertThat(run.getMessage()).contains("INFO-100");
    }
}
