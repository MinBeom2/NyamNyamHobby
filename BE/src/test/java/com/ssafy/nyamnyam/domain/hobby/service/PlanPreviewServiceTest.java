package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.response.PlanPreviewResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.plan.entity.PlanTemplate;
import com.ssafy.nyamnyam.domain.plan.entity.PlanTemplateItem;
import com.ssafy.nyamnyam.domain.plan.repository.PlanTemplateRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** 플랜 미리보기(명세 3.4-4): 공개 원본의 앞 주차만 읽어 보여 준다 */
@ExtendWith(MockitoExtension.class)
class PlanPreviewServiceTest {

    @Mock HobbyRepository hobbyRepository;
    @Mock PlanTemplateRepository planTemplateRepository;
    @InjectMocks PlanPreviewService service;

    private static Hobby publishedHobby(long id) {
        Hobby hobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    /** 3주짜리 원본. 1주차는 활동 2개(seq를 거꾸로 넣어 정렬 확인), 2·3주차는 1개 */
    private static PlanTemplate template() {
        PlanTemplate template = new PlanTemplate(3L, "BEGINNER", "클라이밍 · 입문 3주 계획", (short) 3, 1,
                "PUBLISHED", LocalDateTime.of(2026, 10, 1, 0, 0));
        ReflectionTestUtils.setField(template, "id", 50L);
        template.addWeek((short) 1, "환경과 안전 수칙 익히기");
        template.addWeek((short) 2, "기본 동작 익히기");
        template.addWeek((short) 3, "기초 동작 복습");
        template.addItem(item(template, 1, 2, "강사와 장비 및 안전 수칙 확인하기", 30, 102L));
        template.addItem(item(template, 1, 1, "체험 수업 안내 확인하기", 15, 101L));
        template.addItem(item(template, 2, 1, "강사와 기본 홀드 잡기 연습", 40, 103L));
        template.addItem(item(template, 3, 1, "지도받은 기초 동작 복습", 40, 104L));
        return template;
    }

    private static PlanTemplateItem item(PlanTemplate t, int week, int seq, String title, int minutes, Long id) {
        PlanTemplateItem item = new PlanTemplateItem(t, (short) week, (short) seq, title, "설명", (short) minutes,
                "암벽화(대여)", "완료 기준");
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    @Test
    @DisplayName("기본값: BEGINNER 공개 원본의 1~2주차를 주차 제목·활동(seq 순)과 함께 돌려준다")
    void previewDefault() {
        given(hobbyRepository.findById(3L)).willReturn(Optional.of(publishedHobby(3L)));
        given(planTemplateRepository.findByHobbyIdAndSkillLevelAndStatus(3L, "BEGINNER", "PUBLISHED"))
                .willReturn(Optional.of(template()));

        PlanPreviewResponse response = service.preview(3L, null, null);

        assertThat(response.ready()).isTrue();
        assertThat(response.skillLevel()).isEqualTo("BEGINNER");
        assertThat(response.title()).isEqualTo("클라이밍 · 입문 3주 계획");
        assertThat(response.totalWeeks()).isEqualTo(3);
        assertThat(response.previewWeeks()).isEqualTo(2);
        assertThat(response.weeks()).hasSize(2);
        assertThat(response.weeks().get(0).weekNo()).isEqualTo(1);
        assertThat(response.weeks().get(0).title()).isEqualTo("환경과 안전 수칙 익히기");
        assertThat(response.weeks().get(0).items()).extracting(PlanPreviewResponse.Item::seq).containsExactly(1, 2);
        assertThat(response.weeks().get(0).items().get(0).title()).isEqualTo("체험 수업 안내 확인하기");
        assertThat(response.weeks().get(0).items().get(0).expectedMinutes()).isEqualTo(15);
        assertThat(response.weeks().get(0).items().get(0).supplies()).isEqualTo("암벽화(대여)");
        assertThat(response.weeks().get(1).items()).hasSize(1);
    }

    @Test
    @DisplayName("weeks=3이면 3주차까지, 원본이 그보다 짧으면 총 주차까지만")
    void previewWeeksClamped() {
        given(hobbyRepository.findById(3L)).willReturn(Optional.of(publishedHobby(3L)));
        given(planTemplateRepository.findByHobbyIdAndSkillLevelAndStatus(3L, "BEGINNER", "PUBLISHED"))
                .willReturn(Optional.of(template()));

        assertThat(service.preview(3L, "beginner", 3).weeks()).hasSize(3);
        assertThat(service.preview(3L, "BEGINNER", 1).weeks()).hasSize(1);

        PlanTemplate twoWeeks = new PlanTemplate(3L, "BEGINNER", "짧은 계획", (short) 2, 1, "PUBLISHED",
                LocalDateTime.of(2026, 10, 1, 0, 0));
        twoWeeks.addWeek((short) 1, "1주");
        twoWeeks.addItem(item(twoWeeks, 1, 1, "활동", 10, 1L));
        given(planTemplateRepository.findByHobbyIdAndSkillLevelAndStatus(3L, "BEGINNER", "PUBLISHED"))
                .willReturn(Optional.of(twoWeeks));
        PlanPreviewResponse response = service.preview(3L, null, 3);
        assertThat(response.previewWeeks()).isEqualTo(2);
        assertThat(response.weeks()).hasSize(2);
        assertThat(response.weeks().get(1).title()).isEqualTo("2주차"); // 주차 제목이 없으면 기본 제목
        assertThat(response.weeks().get(1).items()).isEmpty();
    }

    @Test
    @DisplayName("공개 원본이 없으면 404가 아니라 ready=false로 200")
    void previewNotReady() {
        given(hobbyRepository.findById(3L)).willReturn(Optional.of(publishedHobby(3L)));
        given(planTemplateRepository.findByHobbyIdAndSkillLevelAndStatus(3L, "INTERMEDIATE", "PUBLISHED"))
                .willReturn(Optional.empty());

        PlanPreviewResponse response = service.preview(3L, "INTERMEDIATE", null);

        assertThat(response.ready()).isFalse();
        assertThat(response.skillLevel()).isEqualTo("INTERMEDIATE");
        assertThat(response.title()).isNull();
        assertThat(response.totalWeeks()).isZero();
        assertThat(response.weeks()).isEmpty();
    }

    @Test
    @DisplayName("없거나 비공개 취미는 404 COMMON_NOT_FOUND이고 원본은 조회하지 않는다")
    void previewHobbyNotFound() {
        given(hobbyRepository.findById(99L)).willReturn(Optional.empty());
        Hobby draft = Hobby.builder().name("초안").status(HobbyStatus.DRAFT).build();
        given(hobbyRepository.findById(4L)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.preview(99L, null, null)).satisfies(t -> hasCode(t, ErrorCode.COMMON_NOT_FOUND));
        assertThatThrownBy(() -> service.preview(4L, null, null)).satisfies(t -> hasCode(t, ErrorCode.COMMON_NOT_FOUND));
        verify(planTemplateRepository, never()).findByHobbyIdAndSkillLevelAndStatus(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("잘못된 입력은 400: 양수가 아닌 ID, weeks 0·4, 형식에 맞지 않는 skillLevel")
    void previewInvalidInput() {
        assertThatThrownBy(() -> service.preview(0L, null, null)).satisfies(t -> hasCode(t, ErrorCode.COMMON_INVALID_INPUT));
        assertThatThrownBy(() -> service.preview(3L, null, 0)).satisfies(t -> hasCode(t, ErrorCode.COMMON_INVALID_INPUT));
        assertThatThrownBy(() -> service.preview(3L, null, 4)).satisfies(t -> hasCode(t, ErrorCode.COMMON_INVALID_INPUT));
        assertThatThrownBy(() -> service.preview(3L, "입문", null)).satisfies(t -> hasCode(t, ErrorCode.COMMON_INVALID_INPUT));
        assertThatThrownBy(() -> service.preview(3L, "VERY_LONG_LEVEL_NAME", null)).satisfies(t -> hasCode(t, ErrorCode.COMMON_INVALID_INPUT));
        verifyNoInteractions(hobbyRepository, planTemplateRepository);
    }

    private static void hasCode(Throwable t, ErrorCode code) {
        assertThat(t).isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(code);
    }
}
