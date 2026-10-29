package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecoItemType;
import com.ssafy.nyamnyam.domain.recommendation.entity.Recommendation;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecommendationItem;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationItemRepository;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.user.entity.Role;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ExplorationSessionRepository sessionRepository;

    @Mock
    private ExplorationViewRepository viewRepository;

    @Mock
    private ShortReactionRepository reactionRepository;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationItemRepository itemRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private Hobby hobbyClimbing;
    private Hobby hobbyRunning;
    private Hobby hobbyPottery;
    private Short shortClimbing;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        Category catExercise = Category.builder().name("운동").build();
        Category catCraft = Category.builder().name("공예").build();

        Tag tagIndoor = Tag.builder().name("실내").tagGroup(TagGroup.PLACE).build();
        Tag tagSports = Tag.builder().name("스포츠").tagGroup(TagGroup.SUBJECT).build();
        Tag tagCraft = Tag.builder().name("공예").tagGroup(TagGroup.SUBJECT).build();

        hobbyClimbing = Hobby.builder()
                .category(catExercise)
                .name("클라이밍")
                .placeType(PlaceType.INDOOR)
                .socialType(SocialType.TOGETHER)
                .difficulty(Difficulty.NORMAL)
                .sessionMinMinutes((short) 60)
                .sessionMaxMinutes((short) 120)
                .status(HobbyStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(hobbyClimbing, "id", 1L);
        hobbyClimbing.getHobbyTags().add(new HobbyTag(hobbyClimbing, tagIndoor));
        hobbyClimbing.getHobbyTags().add(new HobbyTag(hobbyClimbing, tagSports));

        hobbyRunning = Hobby.builder()
                .category(catExercise)
                .name("러닝")
                .placeType(PlaceType.OUTDOOR)
                .socialType(SocialType.ALONE)
                .difficulty(Difficulty.EASY)
                .sessionMinMinutes((short) 30)
                .sessionMaxMinutes((short) 60)
                .status(HobbyStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(hobbyRunning, "id", 2L);
        hobbyRunning.getHobbyTags().add(new HobbyTag(hobbyRunning, tagSports));

        hobbyPottery = Hobby.builder()
                .category(catCraft)
                .name("도자기")
                .placeType(PlaceType.INDOOR)
                .socialType(SocialType.TOGETHER)
                .difficulty(Difficulty.NORMAL)
                .sessionMinMinutes((short) 90)
                .sessionMaxMinutes((short) 150)
                .status(HobbyStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(hobbyPottery, "id", 3L);
        hobbyPottery.getHobbyTags().add(new HobbyTag(hobbyPottery, tagIndoor));
        hobbyPottery.getHobbyTags().add(new HobbyTag(hobbyPottery, tagCraft));

        shortClimbing = Short.builder()
                .hobby(hobbyClimbing)
                .youtubeVideoId("aBcD123xYz0")
                .title("클라이밍 영상")
                .durationSeconds((short) 60)
                .status(ShortStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(shortClimbing, "id", 100L);
    }

    @ParameterizedTest
    @EnumSource(value = SessionStatus.class, names = {"IN_PROGRESS", "COMPLETED"})
    @DisplayName("탐색 세션 종료 시 피드백을 집계하여 관심/연관 취미를 추천하고 RabbitMQ 이벤트를 발행한다")
    void finishExploration_success(SessionStatus status) {
        // given
        ExplorationSession session = ExplorationSession.builder()
                .user(testUser)
                .maxCount((short) 12)
                .build();
        ReflectionTestUtils.setField(session, "id", 50L);
        if (status == SessionStatus.COMPLETED) session.complete();
        var originalEndedAt = session.getEndedAt();

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(shortClimbing)
                .seq((short) 1)
                .watchedSeconds((short) 60)
                .watchRatio(BigDecimal.valueOf(1.0))
                .playFailed(false)
                .build();

        ShortReaction reaction = ShortReaction.builder()
                .user(testUser)
                .shortform(shortClimbing)
                .sessionId(50L)
                .type(ReactionType.LIKE)
                .build();

        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser)).willReturn(Optional.of(session));
        given(viewRepository.findBySessionOrderBySeqAsc(session)).willReturn(List.of(view));
        given(reactionRepository.findByUser(testUser)).willReturn(List.of(reaction));
        given(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED))
                .willReturn(List.of(hobbyClimbing, hobbyRunning, hobbyPottery));

        // when
        var result = recommendationService.finishExploration(50L, testUser);
        RecommendationResponse response = result.response();

        // then
        assertThat(response).isNotNull();
        assertThat(result.created()).isTrue();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        if (originalEndedAt != null) assertThat(session.getEndedAt()).isEqualTo(originalEndedAt);
        assertThat(response.getItems()).isNotEmpty();

        // 클라이밍이 높은 점수로 관심 취미(INTEREST) 1위 선정 확인
        assertThat(response.getItems().get(0).getHobbyName()).isEqualTo("클라이밍");
        assertThat(response.getItems().get(0).getType()).isEqualTo(RecoItemType.INTEREST);

        // RabbitMQ 메시지 발행 검증
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq(RabbitConfig.RECO_REASON_ROUTING_KEY),
                any(RecommendationReasonEvent.class)
        );
    }

    @Test
    @DisplayName("종료 재요청은 저장된 추천·순위를 반환하고 재계산·저장·이벤트 발행을 하지 않는다")
    void repeatedFinishReturnsExistingRecommendation() {
        ExplorationSession session = ExplorationSession.builder().user(testUser).build();
        session.complete();
        var endedAt = session.getEndedAt();
        Recommendation recommendation = Recommendation.builder().user(testUser).session(session)
                .reactionCount((short) 3).isExploratory(false).build();
        ReflectionTestUtils.setField(recommendation, "id", 88L);
        RecommendationItem item = RecommendationItem.builder().recommendation(recommendation)
                .hobby(hobbyClimbing).type(RecoItemType.INTEREST).rankNo((short) 1)
                .score(BigDecimal.valueOf(4)).build();
        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser)).willReturn(Optional.of(session));
        given(recommendationRepository.findBySessionId(50L)).willReturn(Optional.of(recommendation));
        given(itemRepository.findByRecommendationOrderByRankNoAsc(recommendation)).willReturn(List.of(item));

        var result = recommendationService.finishExploration(50L, testUser);

        assertThat(result.created()).isFalse();
        assertThat(result.response().getRecommendationId()).isEqualTo(88L);
        assertThat(result.response().getReactionCount()).isEqualTo(3);
        assertThat(result.response().getItems()).extracting(RecommendationResponse.RecommendationItemDto::getHobbyId)
                .containsExactly(1L);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        verify(recommendationRepository, never()).save(any());
        verify(itemRepository, never()).save(any());
        verifyNoInteractions(viewRepository, reactionRepository, hobbyRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("타인 또는 없는 세션은 기존 추천 조회 전에 404로 거절한다")
    void rejectsMissingOrForeignSession() {
        assertError(() -> recommendationService.finishExploration(50L, testUser), ErrorCode.COMMON_NOT_FOUND);
        verifyNoInteractions(recommendationRepository, viewRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("포기한 세션에서는 새 추천을 생성하지 않는다")
    void rejectsAbandonedSession() {
        ExplorationSession session = ExplorationSession.builder().user(testUser).build();
        session.abandon();
        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser)).willReturn(Optional.of(session));

        assertError(() -> recommendationService.finishExploration(50L, testUser), ErrorCode.SHORT_SESSION_ENDED);
        verify(recommendationRepository, never()).save(any());
        verifyNoInteractions(viewRepository, hobbyRepository, rabbitTemplate);
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }
}
