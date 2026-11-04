package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.config.ExplorationProperties;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ExplorationProgressResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ExplorationProgressResponse.TopInterestResponse;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.*;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ExplorationProgressTest {
    @Mock ExplorationSessionRepository sessions;
    @Mock ExplorationViewRepository viewRepository;
    @Mock ShortReactionRepository reactionRepository;
    @Mock ShortRepository shorts;
    @Mock RecommendationRepository recommendations;
    @Spy ExplorationProperties properties = new ExplorationProperties();
    @InjectMocks ExplorationService service;

    User user;
    ExplorationSession session;
    final List<ExplorationView> views = new ArrayList<>();
    final List<ShortReaction> reactions = new ArrayList<>();
    long nextShortId = 100;

    @BeforeEach void setUp() {
        user = User.builder().email("progress@example.com").nickname("progress").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        session = ExplorationSession.builder().user(user).maxCount((short) 12).build();
        ReflectionTestUtils.setField(session, "id", 50L);
    }

    @Test void emptySessionReturnsEmptyListsAndStoredProgress() {
        var result = progress();
        assertThat(result.getCurrentStep()).isZero();
        assertThat(result.getMaxCount()).isEqualTo(12);
        assertThat(result.getReactedHobbies()).isEmpty();
        assertThat(result.getTopInterests()).isEmpty();
    }

    @Test void returnsHobbyNamesTopThreeAndRelativePercentagesWithoutTags() {
        Hobby climbing = hobby(1L, "클라이밍");
        Hobby running = hobby(2L, "러닝");
        Hobby pottery = hobby(3L, "도예");
        expose(climbing, "1", ReactionType.LIKE, false); // 3 (반응이 있으면 시청 시간은 보지 않는다)
        expose(running, "0.5", ReactionType.LIKE, false); // 3
        expose(pottery, "0.9", null, false); // 0.5 (거의 끝까지 봄)
        expose(pottery, "0.9", null, false); // 0.5
        expose(hobby(4L, "뜨개질"), "0.6", null, false); // -0.3 (0초 기록 = 바로 넘김)

        var result = progress();

        assertThat(result.getCurrentStep()).isEqualTo(5);
        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getHobbyName)
                .containsExactly("클라이밍", "러닝", "도예");
        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getPercent).containsExactly(100, 100, 33);
    }

    @Test void sumsPositiveAndNegativeVideosForSameHobbyAndRoundsPercentages() {
        Hobby climbing = hobby(1L, "클라이밍");
        expose(climbing, "1", ReactionType.LIKE, false); // +3
        expose(climbing, "1", ReactionType.LIKE, false); // +3
        expose(climbing, "1", ReactionType.DISLIKE, false); // -3
        expose(hobby(2L, "러닝"), "0.5", ReactionType.LIKE, false); // +3
        expose(hobby(2L, "러닝"), "1", null, false); // +0.5

        var result = progress();

        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getHobbyName)
                .containsExactly("러닝", "클라이밍");
        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getPercent).containsExactly(100, 86);
        assertThat(result.getReactedHobbies()).extracting(ExplorationProgressResponse.ReactedHobbyResponse::getName)
                .containsExactly("클라이밍", "러닝");
        assertThat(result.getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.DISLIKE);
    }

    @Test void latestHobbyReactionUsesUpdateTimeRatherThanRepositoryOrExposureOrder() {
        Hobby hobby = hobby(1L, "클라이밍");
        expose(hobby, "1", ReactionType.LIKE, false);
        expose(hobby, "1", ReactionType.DISLIKE, false);
        ReflectionTestUtils.setField(reactions.get(0), "updatedAt", LocalDateTime.of(2026, 10, 29, 12, 0));
        ReflectionTestUtils.setField(reactions.get(1), "updatedAt", LocalDateTime.of(2026, 10, 29, 11, 0));
        Collections.reverse(reactions);

        assertThat(progress().getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.LIKE);
    }

    @Test void equalReactionTimestampsUseReactionIdAsTieBreaker() {
        Hobby hobby = hobby(1L, "클라이밍");
        expose(hobby, "1", ReactionType.LIKE, false);
        expose(hobby, "1", ReactionType.DISLIKE, false);
        for (ShortReaction r : reactions) ReflectionTestUtils.setField(r, "updatedAt", LocalDateTime.of(2026, 10, 29, 12, 0));
        ReflectionTestUtils.setField(reactions.get(0), "id", 200L);
        ReflectionTestUtils.setField(reactions.get(1), "id", 100L);

        assertThat(progress().getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.LIKE);
    }

    @Test void excludesIrrelevantFailedAndUnassignedVideosFromScoresAndReactionTags() {
        expose(hobby(1L, "관련 없음"), "1", ReactionType.IRRELEVANT, false);
        expose(hobby(2L, "실패"), "1", ReactionType.LIKE, true);
        expose(null, "1", ReactionType.LIKE, false);
        expose(hobby(3L, "도예"), "1", ReactionType.LIKE, false);

        var result = progress();
        assertThat(result.getCurrentStep()).isEqualTo(4);
        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getHobbyName).containsExactly("도예");
        assertThat(result.getReactedHobbies()).extracting(ExplorationProgressResponse.ReactedHobbyResponse::getName)
                .containsExactly("도예");
    }

    @Test void nonPositiveScoresReturnNoInterestsButKeepValidDislikeTag() {
        expose(hobby(1L, "클라이밍"), "0.5", null, false); // 0
        expose(hobby(2L, "러닝"), "1", ReactionType.DISLIKE, false); // -2
        var result = progress();
        assertThat(result.getTopInterests()).isEmpty();
        assertThat(result.getReactedHobbies()).hasSize(1);
        assertThat(result.getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.DISLIKE);
    }

    @Test void equalScoresUseHobbyIdForStableRanking() {
        expose(hobby(3L, "도예"), "1", null, false);
        expose(hobby(1L, "클라이밍"), "1", null, false);
        expose(hobby(2L, "러닝"), "1", null, false);
        assertThat(progress().getTopInterests()).extracting(TopInterestResponse::getHobbyName)
                .containsExactly("클라이밍", "러닝", "도예");
    }

    @Test void missingWatchRatioIsTreatedAsZero() {
        expose(hobby(1L, "클라이밍"), null, ReactionType.LIKE, false);
        assertThat(progress().getTopInterests().get(0).getPercent()).isEqualTo(100);
    }

    @Test void reactionChangeAndCancellationRecomputeScoresAndKeepWatchTime() {
        expose(hobby(1L, "클라이밍"), "1", ReactionType.LIKE, false);
        expose(hobby(2L, "러닝"), "1", null, false);
        assertThat(progress().getTopInterests()).extracting(TopInterestResponse::getPercent).containsExactly(100, 17);
        reactions.get(0).updateType(ReactionType.DISLIKE, 50L);
        assertThat(progress().getTopInterests()).extracting(TopInterestResponse::getHobbyName).containsExactly("러닝");
        reactions.clear();
        var canceled = progress();
        assertThat(canceled.getReactedHobbies()).isEmpty();
        assertThat(canceled.getTopInterests()).extracting(TopInterestResponse::getPercent).containsExactly(100, 100);
    }

    @Test void ignoresReactionsWithoutExposureInThisSession() {
        expose(hobby(1L, "과거 기록"), "1", ReactionType.LIKE, false);
        views.clear();
        expose(hobby(2L, "러닝"), "1", null, false);
        var result = progress();
        assertThat(result.getReactedHobbies()).isEmpty();
        assertThat(result.getTopInterests()).extracting(TopInterestResponse::getHobbyName).containsExactly("러닝");
    }

    @Test void deniesForeignOrMissingSessionBeforeReadingRecords() {
        assertThatThrownBy(() -> service.getProgress(50L, user)).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND));
        verifyNoInteractions(viewRepository, reactionRepository);
    }

    private ExplorationProgressResponse progress() {
        ReflectionTestUtils.setField(session, "viewedCount", (short) views.size());
        given(sessions.findByIdAndUser(50L, user)).willReturn(Optional.of(session));
        given(viewRepository.findBySessionOrderBySeqAsc(session)).willReturn(views);
        given(reactionRepository.findByUserAndSessionId(user, 50L)).willReturn(reactions);
        return service.getProgress(50L, user);
    }

    private void expose(Hobby hobby, String ratio, ReactionType type, boolean failed) {
        Short shortform = Short.builder().hobby(hobby).youtubeVideoId("progress" + nextShortId).title("영상").build();
        ReflectionTestUtils.setField(shortform, "id", nextShortId++);
        views.add(ExplorationView.builder().session(session).shortform(shortform).seq((short) (views.size() + 1))
                .watchRatio(ratio == null ? null : new BigDecimal(ratio)).playFailed(failed).build());
        if (type != null) reactions.add(ShortReaction.builder().user(user).sessionId(50L).shortform(shortform).type(type).build());
    }

    private Hobby hobby(Long id, String name) {
        Hobby hobby = Hobby.builder().name(name).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }
}
