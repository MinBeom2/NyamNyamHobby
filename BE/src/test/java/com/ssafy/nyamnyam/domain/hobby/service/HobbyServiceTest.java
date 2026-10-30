package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HobbyServiceTest {

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private UserHobbyRepository userHobbyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PreferenceProfileService preferenceProfileService;

    @InjectMocks
    private HobbyService hobbyService;

    private Hobby hobby;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().name("운동·야외").build();
        ReflectionTestUtils.setField(category, "id", 1L);
        hobby = Hobby.builder()
                .category(category)
                .name("클라이밍")
                .summary("온몸의 근육과 집중력을 깨우는 실내 암벽 등반")
                .description("클라이밍은 홀드를 잡고 오르는 운동입니다.")
                .placeType(PlaceType.INDOOR)
                .socialType(SocialType.BOTH)
                .difficulty(Difficulty.NORMAL)
                .sessionMinMinutes((short) 60)
                .sessionMaxMinutes((short) 120)
                .initialCostMin(30000)
                .initialCostMax(80000)
                .monthlyCostMin(100000)
                .monthlyCostMax(150000)
                .supplies("암벽화, 초크백")
                .status(HobbyStatus.PUBLISHED)
                .saveCount(420)
                .build();
        ReflectionTestUtils.setField(hobby, "id", 3L);
        // 태그는 id 순으로 정렬되어 나가야 하므로 일부러 거꾸로 넣는다
        hobby.getHobbyTags().add(new HobbyTag(hobby, tag(20L, "활동적")));
        hobby.getHobbyTags().add(new HobbyTag(hobby, tag(10L, "실내")));
    }

    private static Tag tag(Long id, String name) {
        Tag tag = Tag.builder().name(name).tagGroup(TagGroup.SUBJECT).build();
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    @Test
    @DisplayName("비회원 상세 조회: 명세 필드가 채워지고 myStatus는 NONE")
    void getDetail_guest() {
        given(hobbyRepository.findWithCategoryAndTagsById(3L)).willReturn(Optional.of(hobby));

        HobbyDetailResponse response = hobbyService.getDetail(3L, null);

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("클라이밍");
        assertThat(response.category().id()).isEqualTo(1L);
        assertThat(response.category().name()).isEqualTo("운동·야외");
        assertThat(response.placeType()).isEqualTo("INDOOR");
        assertThat(response.socialType()).isEqualTo("BOTH");
        assertThat(response.difficulty()).isEqualTo("NORMAL");
        assertThat(response.sessionMinutes().min()).isEqualTo(60);
        assertThat(response.sessionMinutes().max()).isEqualTo(120);
        assertThat(response.initialCost().min()).isEqualTo(30000);
        assertThat(response.monthlyCost().max()).isEqualTo(150000);
        assertThat(response.supplies()).isEqualTo("암벽화, 초크백");
        assertThat(response.environment()).isNull();
        assertThat(response.tags()).containsExactly("실내", "활동적");
        assertThat(response.saveCount()).isEqualTo(420);
        assertThat(response.myStatus()).isEqualTo("NONE");
        verifyNoInteractions(userHobbyRepository);
    }

    @Test
    @DisplayName("회원 상세 조회: 관심 저장한 취미면 myStatus가 SAVED")
    void getDetail_memberWithSavedHobby() {
        given(hobbyRepository.findWithCategoryAndTagsById(3L)).willReturn(Optional.of(hobby));
        UserHobby saved = UserHobby.builder().hobby(hobby).status(UserHobbyStatus.SAVED).build();
        given(userHobbyRepository.findByUserIdAndHobbyId(7L, 3L)).willReturn(Optional.of(saved));

        assertThat(hobbyService.getDetail(3L, 7L).myStatus()).isEqualTo("SAVED");
    }

    @Test
    @DisplayName("회원 상세 조회: 기록이 없으면 myStatus는 NONE")
    void getDetail_memberWithoutRecord() {
        given(hobbyRepository.findWithCategoryAndTagsById(3L)).willReturn(Optional.of(hobby));
        given(userHobbyRepository.findByUserIdAndHobbyId(7L, 3L)).willReturn(Optional.empty());

        assertThat(hobbyService.getDetail(3L, 7L).myStatus()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("없는 취미는 404 COMMON_NOT_FOUND")
    void getDetail_notFound() {
        given(hobbyRepository.findWithCategoryAndTagsById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> hobbyService.getDetail(99L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    @DisplayName("공개되지 않은(DRAFT) 취미도 404")
    void getDetail_notPublished() {
        ReflectionTestUtils.setField(hobby, "status", HobbyStatus.DRAFT);
        given(hobbyRepository.findWithCategoryAndTagsById(3L)).willReturn(Optional.of(hobby));

        assertThatThrownBy(() -> hobbyService.getDetail(3L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verifyNoInteractions(userHobbyRepository);
    }

    @Test
    @DisplayName("양수가 아닌 취미 ID는 400 COMMON_INVALID_INPUT")
    void getDetail_invalidId() {
        assertThatThrownBy(() -> hobbyService.getDetail(0L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
        verifyNoInteractions(hobbyRepository);
    }
}
