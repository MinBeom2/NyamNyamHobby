package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.config.ExplorationProperties;
import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExplorationLifecycleTest {
    @Mock ExplorationSessionRepository sessions;
    @Mock ExplorationViewRepository views;
    @Mock ShortReactionRepository reactions;
    @Mock ShortRepository shorts;
    @Mock RecommendationRepository recommendations;
    @Spy ExplorationProperties properties = new ExplorationProperties();
    @InjectMocks ExplorationService service;

    User user;
    ExplorationSession session;
    Short shortform;
    ExplorationView view;
    ShortReaction reaction;

    @BeforeEach void setUp() {
        user = User.builder().email("lifecycle@example.com").nickname("lifecycle").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        session = ExplorationSession.builder().user(user).build();
        ReflectionTestUtils.setField(session, "id", 50L);
        shortform = Short.builder().youtubeVideoId("lifecycle01").title("영상")
                .durationSeconds((short) 60).status(ShortStatus.ACTIVE).build();
        ReflectionTestUtils.setField(shortform, "id", 100L);
        view = ExplorationView.builder().session(session).shortform(shortform).seq((short) 1)
                .watchRatio(BigDecimal.ZERO).build();
        reaction = ShortReaction.builder().user(user).shortform(shortform).sessionId(50L)
                .type(ReactionType.IRRELEVANT).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"next", "view", "reaction", "cancel", "progress"})
    void rejectsMissingOrForeignSession(String operation) {
        if (operation.equals("cancel")) givenCancellation();
        assertError(() -> invoke(operation), ErrorCode.COMMON_NOT_FOUND);
        verify(views, never()).save(any());
        verify(reactions, never()).save(any());
        verify(reactions, never()).delete(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "reaction", "cancel"})
    void rejectsUnexposedShort(String operation) {
        givenSession();
        givenShort(operation);
        assertError(() -> invoke(operation), ErrorCode.COMMON_NOT_FOUND);
        verify(views, never()).save(any());
        verify(reactions, never()).save(any());
        verify(reactions, never()).delete(any());
        assertThat(view.getWatchedSeconds()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "reaction", "cancel"})
    void rejectsChangesAfterRecommendation(String operation) {
        givenSession();
        given(recommendations.existsBySessionId(50L)).willReturn(true);
        if (operation.equals("cancel")) givenCancellation();
        assertError(() -> invoke(operation), ErrorCode.SHORT_SESSION_ENDED);
        verify(reactions, never()).save(any());
        verify(reactions, never()).delete(any());
        assertThat(view.getWatchedSeconds()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "reaction", "cancel"})
    void rejectsChangesToAbandonedSession(String operation) {
        session.abandon();
        givenSession();
        if (operation.equals("cancel")) givenCancellation();
        assertError(() -> invoke(operation), ErrorCode.SHORT_SESSION_ENDED);
        verifyNoInteractions(recommendations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "reaction", "cancel"})
    void acceptsFinalRecordsAfterNaturalCompletion(String operation) {
        session.complete();
        var endedAt = session.getEndedAt();
        givenSession();
        givenShort(operation);
        if (operation.equals("view")) {
            given(views.findBySessionAndShortform(session, shortform)).willReturn(Optional.of(view));
        } else {
            given(views.existsBySessionAndShortform(session, shortform)).willReturn(true);
            if (operation.equals("cancel")) {
                given(reactions.findByUserAndShortform(user, shortform)).willReturn(Optional.of(reaction));
            }
        }

        invoke(operation);

        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        switch (operation) {
            case "view" -> assertThat(view.getWatchedSeconds()).isEqualTo((short) 30);
            case "reaction" -> verify(reactions).save(any(ShortReaction.class));
            case "cancel" -> verify(reactions).delete(reaction);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ShortStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsReactionOnInactiveShort(ShortStatus status) {
        ReflectionTestUtils.setField(shortform, "status", status);
        givenSession();
        given(shorts.findByIdForUpdate(100L)).willReturn(Optional.of(shortform));
        assertError(() -> invoke("reaction"), ErrorCode.SHORT_NOT_ACTIVE);
        verifyNoInteractions(reactions);
    }

    @ParameterizedTest
    @EnumSource(value = ShortStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void rejectsCancellationOnInactiveShort(ShortStatus status) {
        ReflectionTestUtils.setField(shortform, "status", status);
        given(shorts.findById(100L)).willReturn(Optional.of(shortform));
        assertError(() -> invoke("cancel"), ErrorCode.SHORT_NOT_ACTIVE);
        verifyNoInteractions(sessions);
        verify(reactions, never()).delete(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void requiresPositiveSessionId(Long sessionId) {
        assertError(() -> service.recordReaction(100L, new ReactionRequest(ReactionType.LIKE, sessionId), user),
                ErrorCode.COMMON_INVALID_INPUT);
        verifyNoInteractions(sessions, shorts, reactions);
    }

    @Test void cancellationWithoutReactionIsIdempotent() {
        given(shorts.findById(100L)).willReturn(Optional.of(shortform));
        assertThat(service.cancelReaction(100L, user).getType()).isNull();
        verifyNoInteractions(sessions);
        verify(reactions, never()).delete(any());
    }

    @ParameterizedTest
    @EnumSource(value = SessionStatus.class, names = {"COMPLETED", "ABANDONED"})
    void repeatedNextPreservesTerminalStateAndTimestamp(SessionStatus status) {
        if (status == SessionStatus.COMPLETED) session.complete(); else session.abandon();
        var endedAt = session.getEndedAt();
        givenSession();
        var result = service.getNextShort(50L, user);
        assertThat(result.getFinished()).isTrue();
        assertThat(result.getShortItem()).isNull();
        assertThat(session.getStatus()).isEqualTo(status);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        verifyNoInteractions(views, shorts, recommendations);
    }

    private void givenSession() {
        given(sessions.findByIdAndUserForUpdate(50L, user)).willReturn(Optional.of(session));
    }

    private void givenShort(String operation) {
        if (operation.equals("cancel")) givenCancellation();
        given(shorts.findByIdForUpdate(100L)).willReturn(Optional.of(shortform));
    }

    private void givenCancellation() {
        given(reactions.findSessionReferenceByUserAndShortformId(user, 100L)).willReturn(Optional.of(() -> 50L));
    }

    private void invoke(String operation) {
        switch (operation) {
            case "next" -> service.getNextShort(50L, user);
            case "view" -> service.recordView(50L, new RecordViewRequest(100L, 30, false), user);
            case "reaction" -> service.recordReaction(100L, new ReactionRequest(ReactionType.LIKE, 50L), user);
            case "cancel" -> service.cancelReaction(100L, user);
            case "progress" -> service.getProgress(50L, user);
            default -> throw new IllegalArgumentException(operation);
        }
    }

    private void assertError(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }
}
