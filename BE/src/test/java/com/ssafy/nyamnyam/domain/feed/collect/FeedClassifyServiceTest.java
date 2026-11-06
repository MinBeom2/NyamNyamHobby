package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchJob;
import com.ssafy.nyamnyam.global.batchrun.BatchRun;
import com.ssafy.nyamnyam.global.batchrun.BatchRunRepository;
import com.ssafy.nyamnyam.global.batchrun.BatchRunStatus;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedClassifyServiceTest {

    @Mock FeedItemRepository feeds;
    @Mock HobbyRepository hobbies;
    @Mock GmsClient gms;
    @Mock FeedWriter writer;
    @Mock BatchRunRepository runs;

    FeedClassifyProperties properties = new FeedClassifyProperties(60, 20, "gpt-4o-mini", 3);
    FeedHobbyClassifier classifier;
    FeedClassifyService service;
    Hobby yoga, cafe;

    @BeforeEach void setUp() {
        classifier = new FeedHobbyClassifier(gms, JsonMapper.builder().build(), properties);
        service = new FeedClassifyService(feeds, hobbies, classifier, writer, runs, properties);
        yoga = hobby(3L, "요가");
        cafe = hobby(23L, "홈카페");
        lenient().when(runs.save(any(BatchRun.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void appliesGmsAnswersAndSendsCandidateAndCategoryToTheCheapModel() {
        FeedItem keep = item(1L, "저녁 힐링 요가 교실", yoga, "체육");
        FeedItem wrong = item(2L, "커피 한잔과 함께하는 역사 강연", cafe, "역사");
        FeedItem none = item(3L, "가락 옥토버페스트", null, null);
        FeedItem silent = item(4L, "코바늘 소품 만들기", null, "공예");
        given(gms.isConfigured()).willReturn(true);
        given(hobbies.findByStatus(HobbyStatus.PUBLISHED)).willReturn(List.of(yoga, cafe));
        given(feeds.findClassifyTargets(any(Pageable.class))).willReturn(List.of(keep, wrong, none, silent));
        given(gms.completeJson(eq("gpt-4o-mini"), anyString(), anyString(), anyInt())).willReturn(Optional.of("""
                {"results":[{"id":1,"hobby":"요가"},{"id":2,"hobby":"그 외"},{"id":3,"hobby":"그 외"},{"id":99,"hobby":"요가"}]}"""));

        BatchRun run = service.classify();

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gms).completeJson(eq("gpt-4o-mini"), anyString(), prompt.capture(), anyInt());
        assertThat(prompt.getValue()).contains("취미 목록: 요가, 홈카페")
                .contains("id=2 | 커피 한잔과 함께하는 역사 강연 | 유형: 강좌·클래스 | 분류: 역사 | 후보: 홈카페")
                .contains("id=3 | 가락 옥토버페스트 | 유형: 강좌·클래스 | 후보: 없음");
        verify(writer).applyClassification(1L, true, yoga, 3);
        verify(writer).applyClassification(2L, true, null, 3); // 후보가 틀려 '그 외'
        verify(writer).applyClassification(3L, true, null, 3);
        verify(writer).applyClassification(4L, false, null, 3); // 답이 없으면 다음에 다시
        assertThat(run.getJob()).isEqualTo(BatchJob.FEED_CLASSIFY);
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.PARTIAL);
        assertThat(run.getQuotaUsed()).isEqualTo(1);
        assertThat(run.getMessage()).isEqualTo("취미 1건, 그 외 2건, 답 없음 1건");
    }

    @Test void dailyLimitAndChunkSizeBoundGmsCalls() {
        FeedClassifyProperties small = new FeedClassifyProperties(3, 2, "gpt-4o-mini", 3);
        service = new FeedClassifyService(feeds, hobbies, new FeedHobbyClassifier(gms, JsonMapper.builder().build(), small),
                writer, runs, small);
        given(gms.isConfigured()).willReturn(true);
        given(hobbies.findByStatus(HobbyStatus.PUBLISHED)).willReturn(List.of(yoga));
        given(feeds.findClassifyTargets(any(Pageable.class))).willReturn(List.of(
                item(1L, "요가 1", yoga, null), item(2L, "요가 2", yoga, null), item(3L, "요가 3", yoga, null)));
        given(gms.completeJson(anyString(), anyString(), anyString(), anyInt())).willReturn(Optional.empty());

        BatchRun run = service.classify();

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(feeds).findClassifyTargets(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(3);
        verify(gms, org.mockito.Mockito.times(2)).completeJson(anyString(), anyString(), anyString(), anyInt());
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
    }

    @Test void withoutGmsKeyNothingIsTouched() {
        given(gms.isConfigured()).willReturn(false);

        BatchRun run = service.classify();

        verify(feeds, never()).findClassifyTargets(any());
        verify(writer, never()).applyClassification(any(), anyBooleanArg(), isNull(), anyInt());
        assertThat(run.getStatus()).isEqualTo(BatchRunStatus.FAILED);
        assertThat(run.getMessage()).contains("GMS 키 없음");
    }

    @Test void unknownHobbyNamesAreIgnored() {
        Map<Long, String> parsed = classifier.parse("""
                {"results":[{"id":1,"hobby":"요가"},{"id":2,"hobby":"스카이다이빙"},{"id":3,"hobby":" 그 외 "}]}""",
                Set.of(1L, 2L, 3L), Set.of("요가"));
        assertThat(parsed).containsOnly(Map.entry(1L, "요가"), Map.entry(3L, "그 외"));
        assertThat(classifier.parse("not json", Set.of(1L), Set.of("요가"))).isEmpty();
    }

    @Test void writerConfirmsCandidateAfterTooManySilentAttempts() {
        FeedItem item = item(5L, "요가 교실", yoga, null);
        ReflectionTestUtils.setField(item, "classifyAttempts", (short) 2);
        given(feeds.findById(5L)).willReturn(Optional.of(item));
        FeedWriter realWriter = new FeedWriter(feeds, null);

        assertThat(realWriter.applyClassification(5L, false, null, 3)).isTrue();
        assertThat(item.getHobby()).isSameAs(yoga);
        assertThat(item.getStatus()).isEqualTo(FeedStatus.PUBLISHED);
        assertThat(item.getClassifiedAt()).isNotNull();
    }

    private static boolean anyBooleanArg() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }

    private static Hobby hobby(Long id, String name) {
        Hobby hobby = Hobby.builder().name(name).status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    private static FeedItem item(Long id, String title, Hobby candidate, String category) {
        FeedItem item = FeedItem.builder().hobby(candidate).type(FeedType.CLASS).title(title).sourceName("서울")
                .sourceUrl("https://x").origin(FeedOrigin.SEOUL_RESERVATION)
                .status(candidate == null ? FeedStatus.PENDING_REVIEW : FeedStatus.PUBLISHED)
                .createdAt(LocalDateTime.now()).build();
        item.prepareClassification(category);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
