package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.config.ExplorationProperties;
import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.*;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.domain.user.entity.Role;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExplorationServiceTest {
    @Mock private ShortSaveRepository shortSaveRepository;
    @Mock private PreferenceProfileService profiles;
    @Spy private ExplorationSelectionPolicy selectionPolicy = new ExplorationSelectionPolicy();

    @Mock
    private ExplorationSessionRepository sessionRepository;

    @Mock
    private ExplorationViewRepository viewRepository;

    @Mock
    private ShortReactionRepository reactionRepository;

    @Mock
    private ShortRepository shortRepository;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Spy
    private ExplorationProperties explorationProperties = new ExplorationProperties();

    @InjectMocks
    private ExplorationService explorationService;

    private User testUser;
    private Hobby testHobby;
    private Short testShort;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(profiles.load(any())).thenReturn(PreferenceProfileService.Profile.empty());
        org.mockito.Mockito.lenient().when(profiles.load(any(), any())).thenReturn(PreferenceProfileService.Profile.empty());
        testUser = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        Category category = Category.builder().name("운동").build();
        ReflectionTestUtils.setField(category, "id", 1L);

        testHobby = Hobby.builder()
                .category(category)
                .name("클라이밍")
                .placeType(PlaceType.INDOOR)
                .socialType(SocialType.TOGETHER)
                .difficulty(Difficulty.NORMAL)
                .sessionMinMinutes((short) 60)
                .sessionMaxMinutes((short) 120)
                .status(HobbyStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(testHobby, "id", 10L);

        Tag subjectTag = Tag.builder().name("스포츠").tagGroup(TagGroup.SUBJECT).build();
        testHobby.getHobbyTags().add(new HobbyTag(testHobby, subjectTag));

        testShort = Short.builder()
                .hobby(testHobby)
                .youtubeVideoId("aBcD123xYz0")
                .title("퇴근 후 1시간 볼더링")
                .durationSeconds((short) 60)
                .status(ShortStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(testShort, "id", 100L);
    }

    @Test
    @DisplayName("탐색 세션 시작 시 세션 생성과 함께 첫 숏폼 영상을 반환한다")
    void startSession_success() {
        // given
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE))
                .willReturn(List.of(testShort));
        given(shortRepository.findByIdAndStatusForUpdate(100L, ShortStatus.ACTIVE)).willReturn(Optional.of(testShort));
        given(reactionRepository.findByUserAndShortform(testUser, testShort))
                .willReturn(Optional.empty());
        given(shortSaveRepository.exists(testUser.getId(), 100L)).willReturn(true);

        // when
        StartExplorationResponse response = explorationService.startSession(testUser);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMaxCount()).isEqualTo(12);
        assertThat(response.getViewedCount()).isEqualTo(1);
        assertThat(response.getShortItem().getShortId()).isEqualTo(100L);
        assertThat(response.getShortItem().getIsSaved()).isTrue();
        assertThat(response.getShortItem().getYoutubeVideoId()).isEqualTo("aBcD123xYz0");
        assertThat(response.getShortItem().getHobby().getName()).isEqualTo("클라이밍");
        verify(sessionRepository).save(any(ExplorationSession.class));
        verify(viewRepository).save(any(ExplorationView.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 24})
    @DisplayName("새 세션의 최대 노출 수는 설정값으로 저장하고 응답한다")
    void startSession_usesConfiguredSize(int sessionSize) {
        explorationProperties.setSessionSize(sessionSize);
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)).willReturn(List.of(testShort));
        given(shortRepository.findByIdAndStatusForUpdate(100L, ShortStatus.ACTIVE)).willReturn(Optional.of(testShort));
        given(reactionRepository.findByUserAndShortform(testUser, testShort)).willReturn(Optional.empty());

        StartExplorationResponse response = explorationService.startSession(testUser);

        ArgumentCaptor<ExplorationSession> sessionCaptor = ArgumentCaptor.forClass(ExplorationSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getMaxCount()).isEqualTo((short) sessionSize);
        assertThat(response.getMaxCount()).isEqualTo(sessionSize);
        assertThat(response.getViewedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다음 숏폼 영상 조회 시 아직 시청하지 않은 영상을 반환한다")
    void getNextShort_success() {
        // given
        ExplorationSession session = ExplorationSession.builder()
                .user(testUser)
                .maxCount((short) 12)
                .build();
        ReflectionTestUtils.setField(session, "id", 50L);
        ReflectionTestUtils.setField(session, "viewedCount", (short) 1);

        Short nextShort = Short.builder()
                .hobby(testHobby)
                .youtubeVideoId("kLmnOP12345")
                .title("다음 영상")
                .durationSeconds((short) 50)
                .status(ShortStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(nextShort, "id", 101L);

        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser))
                .willReturn(Optional.of(session));
        given(viewRepository.findViewedShortIdsBySessionId(50L))
                .willReturn(List.of(100L));
        given(viewRepository.findAllViewedShortIdsByUserId(testUser.getId()))
                .willReturn(List.of(100L));
        given(shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE))
                .willReturn(List.of(nextShort));
        given(shortRepository.findByIdAndStatusForUpdate(101L, ShortStatus.ACTIVE)).willReturn(Optional.of(nextShort));
        given(shortSaveRepository.exists(testUser.getId(), 101L)).willReturn(true);

        // when
        NextShortResponse response = explorationService.getNextShort(50L, testUser);

        // then
        assertThat(response.getFinished()).isFalse();
        assertThat(response.getViewedCount()).isEqualTo(2);
        assertThat(response.getShortItem().getShortId()).isEqualTo(101L);
        assertThat(response.getShortItem().getIsSaved()).isTrue();
        verify(viewRepository).save(any(ExplorationView.class));
    }

    @Test
    @DisplayName("현재 설정과 달라도 세션 생성 당시 최대 노출 수에 도달하면 종료한다")
    void getNextShort_finishedWhenMaxCountReached() {
        // given
        ExplorationSession session = ExplorationSession.builder()
                .user(testUser)
                .maxCount((short) 4)
                .build();
        ReflectionTestUtils.setField(session, "id", 50L);
        ReflectionTestUtils.setField(session, "viewedCount", (short) 4);
        explorationProperties.setSessionSize(24);

        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser))
                .willReturn(Optional.of(session));

        // when
        NextShortResponse response = explorationService.getNextShort(50L, testUser);

        // then
        assertThat(response.getFinished()).isTrue();
        assertThat(response.getViewedCount()).isEqualTo(4);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("진행 현황은 현재 설정 대신 세션 생성 당시 최대 노출 수를 반환한다")
    void getProgress_usesSessionSizeSnapshot() {
        ExplorationSession session = ExplorationSession.builder().user(testUser).maxCount((short) 5).build();
        session.incrementViewedCount();
        explorationProperties.setSessionSize(24);
        given(sessionRepository.findByIdAndUser(50L, testUser)).willReturn(Optional.of(session));
        given(viewRepository.findBySessionOrderBySeqAsc(session)).willReturn(List.of());
        given(reactionRepository.findByUserAndSessionId(testUser, 50L)).willReturn(List.of());

        ExplorationProgressResponse response = explorationService.getProgress(50L, testUser);

        assertThat(response.getMaxCount()).isEqualTo(5);
        assertThat(response.getCurrentStep()).isEqualTo(1);
    }

    @Test
    @DisplayName("시청 시간과 시청 비율을 성공적으로 기록한다")
    void recordView_success() {
        // given
        ExplorationSession session = ExplorationSession.builder()
                .user(testUser)
                .maxCount((short) 12)
                .build();
        ReflectionTestUtils.setField(session, "id", 50L);

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(testShort)
                .seq((short) 1)
                .watchedSeconds((short) 0)
                .watchRatio(BigDecimal.ZERO)
                .playFailed(false)
                .build();

        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser)).willReturn(Optional.of(session));
        given(shortRepository.findByIdForUpdate(100L)).willReturn(Optional.of(testShort));
        given(viewRepository.findBySessionAndShortform(session, testShort)).willReturn(Optional.of(view));

        RecordViewRequest request = new RecordViewRequest(100L, 30, false);

        // when
        explorationService.recordView(50L, request, testUser);

        // then
        assertThat(view.getWatchedSeconds()).isEqualTo((short) 30);
        assertThat(view.getWatchRatio()).isEqualByComparingTo(BigDecimal.valueOf(0.500));
    }

    @Test
    @DisplayName("숏폼에 좋아요 반응을 등록하면 정상적으로 저장된다")
    void recordReaction_like() {
        // given
        givenReactionSession();
        given(shortRepository.findByIdForUpdate(100L)).willReturn(Optional.of(testShort));
        given(reactionRepository.findByUserAndShortform(testUser, testShort)).willReturn(Optional.empty());

        ReactionRequest request = new ReactionRequest(ReactionType.LIKE, 50L);

        // when
        ReactionResponse response = explorationService.recordReaction(100L, request, testUser);

        // then
        assertThat(response.getShortId()).isEqualTo(100L);
        assertThat(response.getType()).isEqualTo(ReactionType.LIKE);
        verify(reactionRepository).save(any(ShortReaction.class));
    }

    @Test
    @DisplayName("관련 없음 반응 등록 시 원본 기록의 고유 사용자 집계를 반영한다")
    void recordReaction_irrelevantIncrementsCount() {
        // given
        givenReactionSession();
        given(shortRepository.findByIdForUpdate(100L)).willReturn(Optional.of(testShort));
        given(reactionRepository.countByShortformIdAndType(100L, ReactionType.IRRELEVANT)).willReturn(1L);
        given(reactionRepository.findByUserAndShortform(testUser, testShort)).willReturn(Optional.empty());

        ReactionRequest request = new ReactionRequest(ReactionType.IRRELEVANT, 50L);

        // when
        explorationService.recordReaction(100L, request, testUser);

        // then
        assertThat(testShort.getIrrelevantCount()).isEqualTo(1);
    }

    private void givenReactionSession() {
        ExplorationSession session = ExplorationSession.builder().user(testUser).maxCount((short) 12).build();
        ReflectionTestUtils.setField(session, "id", 50L);
        given(sessionRepository.findByIdAndUserForUpdate(50L, testUser)).willReturn(Optional.of(session));
        given(viewRepository.existsBySessionAndShortform(session, testShort)).willReturn(true);
    }
}
