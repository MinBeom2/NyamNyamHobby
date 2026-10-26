package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbySourceMappingRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.YoutubeChannelRepository;
import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectWriter.SaveResult;
import com.ssafy.nyamnyam.domain.shortform.service.ShortCollectWriter.ShortDraft;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeClient;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeQuotaExceededException;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeSearchPage;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortCollectServiceTest {

    @Mock private YoutubeClient youtubeClient;
    @Mock private ShortContentClassifier classifier;
    @Mock private ShortCollectWriter writer;
    @Mock private ShortRepository shortRepository;
    @Mock private YoutubeChannelRepository channelRepository;
    @Mock private HobbySourceMappingRepository mappingRepository;
    @Mock private BatchRunRepository batchRunRepository;

    private ShortCollectService service;

    @BeforeEach
    void setUp() {
        ShortCollectProperties properties = new ShortCollectProperties(true, 2, 40, 3, 1000, 180, 9000, 0.7, 20);
        service = new ShortCollectService(properties, youtubeClient, new ShortFilter(), classifier, writer,
                shortRepository, channelRepository, mappingRepository, batchRunRepository);
        lenient().when(batchRunRepository.save(any(BatchRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Hobby hobby(long id, String name) {
        Hobby hobby = Hobby.builder().name(name).status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    private static HobbySourceMapping mapping(long id, Hobby hobby, String keyword, LocalDateTime lastCollectedAt) {
        HobbySourceMapping mapping = HobbySourceMapping.builder()
                .hobby(hobby).target(SourceTarget.SHORT_KEYWORD).keyword(keyword).build();
        ReflectionTestUtils.setField(mapping, "id", id);
        ReflectionTestUtils.setField(mapping, "lastCollectedAt", lastCollectedAt);
        return mapping;
    }

    private void givenTargets(HobbySourceMapping... mappings) {
        given(mappingRepository.findAllWithHobby(SourceTarget.SHORT_KEYWORD, HobbyStatus.PUBLISHED))
                .willReturn(List.of(mappings));
        given(shortRepository.countGroupByHobby(anyList())).willReturn(List.of());
    }

    @Test
    @DisplayName("YouTube 키가 없으면 검색하지 않고 FAILED로 기록")
    void apiKeyMissing() {
        given(youtubeClient.isConfigured()).willReturn(false);

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
        assertThat(run.getMessage()).contains("YOUTUBE_API_KEY");
        verify(youtubeClient, never()).searchShorts(anyString(), any());
    }

    @Test
    @DisplayName("오늘 할당량 상한에 닿으면 검색하지 않고 PARTIAL로 남김")
    void dailyQuotaReached() {
        given(youtubeClient.isConfigured()).willReturn(true);
        given(batchRunRepository.sumQuotaUsedSince(anyList(), any())).willReturn(8_950L);
        givenTargets(mapping(1L, hobby(1L, "클라이밍"), "볼더링", null));

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        assertThat(run.getMessage()).contains("할당량");
        verify(youtubeClient, never()).searchShorts(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("검색 → 이미 있는 영상 제외 → 필터 → GPT 판별 결과대로 저장하고 집계")
    void collectKeyword() {
        Hobby climbing = hobby(1L, "클라이밍");
        givenTargets(mapping(10L, climbing, "볼더링", null));
        given(youtubeClient.isConfigured()).willReturn(true);
        given(batchRunRepository.sumQuotaUsedSince(anyList(), any())).willReturn(0L);

        given(youtubeClient.searchShorts("볼더링", null))
                .willReturn(new YoutubeSearchPage(List.of("good", "long", "few", "old"), "NEXT"));
        given(shortRepository.findExistingVideoIds(anyList())).willReturn(List.of("old"));

        YoutubeVideo good = ShortFilterTest.ok("good", "c1");
        YoutubeVideo tooLong = ShortFilterTest.video("long", "c2", 600, "public", true, null, null, 5_000);
        YoutubeVideo fewViews = ShortFilterTest.video("few", "c3", 40, "public", true, null, null, 10);
        given(youtubeClient.getVideos(List.of("good", "long", "few"))).willReturn(List.of(good, tooLong, fewViews));
        given(channelRepository.findChannelIdsByStatus(any(), eq("REJECTED"))).willReturn(List.of());
        given(shortRepository.countGroupByChannelForHobby(eq(1L), anyList())).willReturn(List.of());
        given(classifier.classify("클라이밍", List.of(good)))
                .willReturn(Map.of("good", new ShortContentClassifier.Verdict(true, 0.9)));
        given(writer.save(eq(10L), eq(1L), anyList(), eq("NEXT"))).willReturn(new SaveResult(1, 0, 1));

        BatchRun run = service.collect();

        ArgumentCaptor<List<ShortDraft>> drafts = ArgumentCaptor.forClass(List.class);
        verify(writer).save(eq(10L), eq(1L), drafts.capture(), eq("NEXT"));
        assertThat(drafts.getValue())
                .extracting(d -> d.video().videoId(), d -> d.decision().status(), d -> d.decision().rejectReason())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("good", ShortStatus.CANDIDATE, null),
                        org.assertj.core.groups.Tuple.tuple("long", ShortStatus.REJECTED, ShortFilter.TOO_LONG));

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
        assertThat(run.getProcessed()).isEqualTo(4);
        assertThat(run.getAccepted()).isEqualTo(1);
        assertThat(run.getRejected()).isEqualTo(2); // TOO_LONG 저장 1 + 조회수 미달 건너뜀 1
        assertThat(run.getQuotaUsed()).isEqualTo(101);
    }

    @Test
    @DisplayName("새 영상이 없으면 상세 조회 없이 다음 위치만 기록")
    void noNewVideos() {
        givenTargets(mapping(10L, hobby(1L, "클라이밍"), "볼더링", null));
        given(youtubeClient.isConfigured()).willReturn(true);
        given(batchRunRepository.sumQuotaUsedSince(anyList(), any())).willReturn(0L);
        given(youtubeClient.searchShorts("볼더링", null)).willReturn(new YoutubeSearchPage(List.of("old"), null));
        given(shortRepository.findExistingVideoIds(anyList())).willReturn(List.of("old"));
        given(writer.save(10L, 1L, List.of(), null)).willReturn(new SaveResult(0, 0, 0));

        BatchRun run = service.collect();

        verify(youtubeClient, never()).getVideos(anyList());
        assertThat(run.getQuotaUsed()).isEqualTo(100);
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
    }

    @Test
    @DisplayName("할당량 초과 응답을 받으면 남은 키워드는 멈추고 PARTIAL")
    void quotaExceededStops() {
        givenTargets(mapping(10L, hobby(1L, "클라이밍"), "볼더링", null), mapping(20L, hobby(2L, "러닝"), "러닝 초보", null));
        given(youtubeClient.isConfigured()).willReturn(true);
        given(batchRunRepository.sumQuotaUsedSince(anyList(), any())).willReturn(0L);
        given(youtubeClient.searchShorts("볼더링", null)).willThrow(new YoutubeQuotaExceededException("quotaExceeded"));

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        verify(youtubeClient, never()).searchShorts(eq("러닝 초보"), any());
        verify(writer, never()).save(anyLong(), anyLong(), anyList(), any());
    }

    @Test
    @DisplayName("한 키워드가 실패해도 다음 키워드는 계속 수집")
    void oneKeywordFails() {
        givenTargets(mapping(10L, hobby(1L, "클라이밍"), "볼더링", null), mapping(20L, hobby(2L, "러닝"), "러닝 초보", null));
        given(youtubeClient.isConfigured()).willReturn(true);
        given(batchRunRepository.sumQuotaUsedSince(anyList(), any())).willReturn(0L);
        willThrow(new IllegalStateException("timeout")).given(youtubeClient).searchShorts("볼더링", null);
        willReturn(new YoutubeSearchPage(List.of(), null)).given(youtubeClient).searchShorts("러닝 초보", null);
        given(writer.save(20L, 2L, List.of(), null)).willReturn(new SaveResult(0, 0, 0));

        BatchRun run = service.collect();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        assertThat(run.getMessage()).contains("볼더링 실패");
        verify(writer).save(20L, 2L, List.of(), null);
    }

    @Test
    @DisplayName("목표 개수를 채운 취미는 빼고, 처음 수집하는 키워드부터, 취미당 하나씩 고름")
    void pickTargets() {
        Hobby climbing = hobby(1L, "클라이밍");
        Hobby running = hobby(2L, "러닝");
        Hobby yoga = hobby(3L, "요가");
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0);
        given(mappingRepository.findAllWithHobby(SourceTarget.SHORT_KEYWORD, HobbyStatus.PUBLISHED)).willReturn(List.of(
                mapping(1L, climbing, "볼더링", null),               // 목표 도달 → 제외
                mapping(2L, running, "러닝 초보", now.minusHours(1)),
                mapping(3L, running, "달리기 자세", null),
                mapping(4L, yoga, "요가 스트레칭", now.minusHours(5))));
        given(shortRepository.countGroupByHobby(anyList())).willReturn(List.<Object[]>of(new Object[]{1L, 40L}));

        List<HobbySourceMapping> picked = service.pickTargets();

        assertThat(picked).extracting(HobbySourceMapping::getKeyword).containsExactly("달리기 자세", "요가 스트레칭");
    }

    @Test
    @DisplayName("주간 반영은 CANDIDATE → ACTIVE 건수를 기록")
    void release() {
        given(writer.releaseCandidates()).willReturn(5);

        BatchRun run = service.release();

        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.SUCCESS);
        assertThat(run.getAccepted()).isEqualTo(5);
        verify(youtubeClient, never()).searchShorts(anyString(), isNull());
    }
}
