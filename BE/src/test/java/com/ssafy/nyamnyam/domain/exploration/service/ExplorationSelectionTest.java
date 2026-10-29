package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.config.ExplorationProperties;
import com.ssafy.nyamnyam.domain.exploration.dto.response.NextShortResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ShortDetailResponse;
import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import com.ssafy.nyamnyam.domain.exploration.entity.SessionStatus;
import com.ssafy.nyamnyam.domain.exploration.entity.ShortReaction;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExplorationSelectionTest {

    @Mock private ExplorationSessionRepository sessionRepository;
    @Mock private ExplorationViewRepository viewRepository;
    @Mock private ShortReactionRepository reactionRepository;
    @Mock private ShortRepository shortRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Spy private ExplorationProperties properties = new ExplorationProperties();
    @InjectMocks private ExplorationService service;

    private User user;
    private ExplorationSession session;

    @BeforeEach
    void setUp() {
        user = User.builder().email("selection@example.com").nickname("selection").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        session = ExplorationSession.builder().user(user).maxCount((short) 12).build();
        ReflectionTestUtils.setField(session, "id", 50L);
        session.incrementViewedCount();
        // 모의 ACTIVE 조회 결과 중 실제로 잠금 가능한 항목을 반환한다. 잠금을 시도하지 않는 종료 테스트도 있다.
        lenient().when(shortRepository.findByIdAndStatusForUpdate(any(), any())).thenAnswer(invocation ->
                shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE).stream()
                        .filter(candidate -> candidate.getId().equals(invocation.getArgument(0))).findFirst());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("첫 영상과 다음 영상은 재사용 가능한 영상보다 새 영상을 우선한다")
    void prefersUnseenShort(boolean next) {
        Short old = shortform(101L);
        Short unseen = shortform(102L);
        given(viewRepository.findAllViewedShortIdsByUserId(1L)).willReturn(List.of(100L, 101L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(old, unseen));

        assertThat(select(next).getShortId()).isEqualTo(102L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("새 영상이 없으면 이전 세션에서 본 영상 중 현재 반응이 없는 영상만 재사용한다")
    void reusesOnlyUnreactedShort(boolean next) {
        Short unreacted = shortform(101L);
        Short liked = shortform(102L);
        Short disliked = shortform(103L);
        Short irrelevant = shortform(104L);
        given(viewRepository.findAllViewedShortIdsByUserId(1L)).willReturn(List.of(100L, 101L, 102L, 103L, 104L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE))
                .willReturn(List.of(unreacted, liked, disliked, irrelevant));
        given(reactionRepository.findByUser(user)).willReturn(List.of(
                reaction(liked, ReactionType.LIKE), reaction(disliked, ReactionType.DISLIKE),
                reaction(irrelevant, ReactionType.IRRELEVANT)));

        assertThat(select(next).getShortId()).isEqualTo(101L);
    }

    static Stream<Arguments> reactedShorts() {
        return Stream.of(false, true).flatMap(next -> Arrays.stream(ReactionType.values())
                .map(type -> Arguments.of(next, type)));
    }

    @ParameterizedTest
    @MethodSource("reactedShorts")
    @DisplayName("반응이 남은 과거 영상만 있으면 시작은 404, 다음 조회는 종료한다")
    void neverReusesReactedShort(boolean next, ReactionType type) {
        Short reacted = shortform(101L);
        given(viewRepository.findAllViewedShortIdsByUserId(1L)).willReturn(List.of(101L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(reacted));
        given(reactionRepository.findByUser(user)).willReturn(List.of(reaction(reacted, type)));

        if (next) {
            givenNextSession(List.of(100L));
            assertFinishedWithoutExposure(service.getNextShort(50L, user));
        } else {
            assertNoStartingCandidate();
        }
        verify(viewRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("시청 기록이 없어도 관련 없음 반응 영상은 노출하지 않는다")
    void excludesIrrelevantShortWithoutViewRecord(boolean next) {
        Short irrelevant = shortform(101L);
        Short unseen = shortform(102L);
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(irrelevant, unseen));
        given(reactionRepository.findByUser(user)).willReturn(List.of(reaction(irrelevant, ReactionType.IRRELEVANT)));

        assertThat(select(next).getShortId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("재사용 시에도 현재 세션에서 노출한 영상은 제외한다")
    void excludesCurrentSessionFromReuse() {
        Short current = shortform(100L);
        Short reusable = shortform(101L);
        givenNextSession(List.of(100L));
        given(viewRepository.findAllViewedShortIdsByUserId(1L)).willReturn(List.of(100L, 101L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(current, reusable));

        assertThat(service.getNextShort(50L, user).getShortItem().getShortId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("현재 세션에서 본 영상만 남으면 노출 수를 늘리지 않고 종료한다")
    void finishesWhenOnlyCurrentSessionShortRemains() {
        givenNextSession(List.of(100L));
        given(viewRepository.findAllViewedShortIdsByUserId(1L)).willReturn(List.of(100L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(shortform(100L)));

        assertFinishedWithoutExposure(service.getNextShort(50L, user));
        verify(viewRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("ACTIVE 영상이 없으면 시작은 404, 다음 조회는 종료한다")
    void noActiveShorts(boolean next) {
        // 조회는 ACTIVE로 한정되므로 후보·숨김·이용 불가·제외 영상은 선택 대상이 아니다.
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of());
        if (next) {
            givenNextSession(List.of(100L));
            assertFinishedWithoutExposure(service.getNextShort(50L, user));
        } else {
            assertNoStartingCandidate();
        }
        verify(viewRepository, never()).save(any());
    }

    @Test
    @DisplayName("무작위 선택은 조회 결과 목록을 변경하지 않는다")
    void selectsWithoutMutatingCandidateList() {
        List<Short> candidates = List.of(shortform(101L), shortform(102L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(candidates);

        assertThat(service.startSession(user).getShortItem().getShortId()).isIn(101L, 102L);
        assertThat(candidates).extracting(Short::getId).containsExactly(101L, 102L);
    }

    private ShortDetailResponse select(boolean next) {
        if (next) {
            givenNextSession(List.of(100L));
            NextShortResponse response = service.getNextShort(50L, user);
            assertThat(response.getFinished()).isFalse();
            assertThat(response.getViewedCount()).isEqualTo(2);
            return response.getShortItem();
        }
        var response = service.startSession(user);
        assertThat(response.getViewedCount()).isEqualTo(1);
        return response.getShortItem();
    }

    private void givenNextSession(List<Long> viewedIds) {
        given(sessionRepository.findByIdAndUserForUpdate(50L, user)).willReturn(Optional.of(session));
        given(viewRepository.findViewedShortIdsBySessionId(50L)).willReturn(viewedIds);
    }

    private void assertNoStartingCandidate() {
        assertThatThrownBy(() -> service.startSession(user)).isInstanceOfSatisfying(CustomException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND));
        verify(sessionRepository, never()).save(any());
    }

    private void assertFinishedWithoutExposure(NextShortResponse response) {
        assertThat(response.getFinished()).isTrue();
        assertThat(response.getShortItem()).isNull();
        assertThat(response.getViewedCount()).isEqualTo(1);
        assertThat(session.getViewedCount()).isEqualTo((short) 1);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    private Short shortform(Long id) {
        Short shortform = Short.builder().youtubeVideoId("video" + id).title("영상 " + id)
                .durationSeconds((short) 60).status(ShortStatus.ACTIVE).build();
        ReflectionTestUtils.setField(shortform, "id", id);
        return shortform;
    }

    private ShortReaction reaction(Short shortform, ReactionType type) {
        return ShortReaction.builder().user(user).shortform(shortform).type(type).build();
    }
}
