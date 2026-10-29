package com.ssafy.nyamnyam.domain.exploration;

import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ExplorationProgressResponse;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.service.ExplorationService;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.FinishExplorationResult;
import com.ssafy.nyamnyam.domain.recommendation.service.RecommendationService;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 실제 PostgreSQL에서 실행. 외부 API·메시지 발송은 사용하지 않으며 생성한 fixture는 정리한다. */
@SpringBootTest(properties = {"app.short-collect.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.listener.direct.auto-startup=false"})
class ExplorationLifecycleIntegrationTest {
    @Autowired ExplorationService explorations;
    @Autowired RecommendationService recommendations;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean RabbitTemplate rabbit;

    TransactionTemplate transaction;
    Fixture fixture;
    record Fixture(Long userId, Long sessionId, Long shortId, Long hobbyId) {}

    @BeforeEach void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        fixture = transaction.execute(status -> {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            User user = User.builder().email(suffix + "@lifecycle.test").nickname("t" + suffix).build();
            em.persist(user);
            Hobby hobby = em.createQuery("SELECT h FROM Hobby h WHERE h.status = :status", Hobby.class)
                    .setParameter("status", HobbyStatus.PUBLISHED).setMaxResults(1).getSingleResult();
            Short shortform = Short.builder().hobby(hobby).youtubeVideoId("t" + suffix).title("test")
                    .durationSeconds((short) 60).status(ShortStatus.ACTIVE).build();
            em.persist(shortform);
            ExplorationSession session = ExplorationSession.builder().user(user).maxCount((short) 1).build();
            session.incrementViewedCount();
            em.persist(session);
            em.persist(ExplorationView.builder().session(session).shortform(shortform).seq((short) 1)
                    .watchedSeconds((short) 0).watchRatio(BigDecimal.ZERO).build());
            return new Fixture(user.getId(), session.getId(), shortform.getId(), hobby.getId());
        });
    }

    @AfterEach void cleanUp() {
        if (fixture == null) return;
        transaction.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM recommendation_items WHERE recommendation_id IN (SELECT id FROM recommendations WHERE session_id = ?)", fixture.sessionId());
            jdbc.update("DELETE FROM recommendations WHERE session_id = ?", fixture.sessionId());
            jdbc.update("DELETE FROM short_reactions WHERE user_id = ?", fixture.userId());
            jdbc.update("DELETE FROM exploration_views WHERE session_id IN (SELECT id FROM exploration_sessions WHERE user_id = ?)", fixture.userId());
            jdbc.update("DELETE FROM exploration_sessions WHERE user_id = ?", fixture.userId());
            jdbc.update("DELETE FROM shorts WHERE id = ?", fixture.shortId());
            jdbc.update("DELETE FROM users WHERE id = ?", fixture.userId());
        });
    }

    @Test void concurrentFinishCreatesAndPublishesOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<FinishExplorationResult> request = () -> {
            ready.countDown();
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return recommendations.finishExploration(fixture.sessionId(), user());
        };
        try {
            Future<FinishExplorationResult> first = executor.submit(request);
            Future<FinishExplorationResult> second = executor.submit(request);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(results).extracting(FinishExplorationResult::created).containsExactlyInAnyOrder(true, false);
            assertThat(results.get(0).response().getRecommendationId()).isEqualTo(results.get(1).response().getRecommendationId());
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendations WHERE session_id = ?", Long.class, fixture.sessionId())).isEqualTo(1);
            verify(rabbit, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void naturalCompletionAcceptsFinalRecordsAndThenFreezesThem() {
        User user = user();
        assertThat(explorations.getNextShort(fixture.sessionId(), user).getFinished()).isTrue();
        explorations.recordView(fixture.sessionId(), new RecordViewRequest(fixture.shortId(), 45, false), user);
        explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.LIKE, fixture.sessionId()), user);
        var first = recommendations.finishExploration(fixture.sessionId(), user);
        var repeated = recommendations.finishExploration(fixture.sessionId(), user);
        assertThat(first.created()).isTrue();
        assertThat(repeated.created()).isFalse();
        assertThat(repeated.response().getRecommendationId()).isEqualTo(first.response().getRecommendationId());
        assertStoredScore("3.500");
        assertEnded(() -> explorations.recordView(fixture.sessionId(), new RecordViewRequest(fixture.shortId(), 1, false), user));
        assertEnded(() -> explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.DISLIKE, fixture.sessionId()), user));
        assertEnded(() -> explorations.cancelReaction(fixture.shortId(), user));
        assertThat(jdbc.queryForObject("SELECT watched_seconds FROM exploration_views WHERE session_id = ?", Integer.class, fixture.sessionId())).isEqualTo(45);
        assertThat(jdbc.queryForObject("SELECT type FROM short_reactions WHERE user_id = ? AND short_id = ?", String.class, fixture.userId(), fixture.shortId())).isEqualTo("LIKE");
    }

    @Test void finishWaitsForInFlightViewAndUsesCommittedWatchTime() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch viewWritten = new CountDownLatch(1);
        CountDownLatch commitView = new CountDownLatch(1);
        CountDownLatch finishStarted = new CountDownLatch(1);
        try {
            Future<?> writer = executor.submit(() -> transaction.executeWithoutResult(status -> {
                explorations.recordView(fixture.sessionId(), new RecordViewRequest(fixture.shortId(), 45, false), user());
                viewWritten.countDown();
                await(commitView);
            }));
            assertThat(viewWritten.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> finisher = executor.submit(() -> {
                finishStarted.countDown();
                return recommendations.finishExploration(fixture.sessionId(), user());
            });
            assertThat(finishStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> finisher.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            commitView.countDown();
            writer.get(20, TimeUnit.SECONDS);
            finisher.get(20, TimeUnit.SECONDS);
            assertStoredScore("0.500");
        } finally {
            commitView.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test void progressReflectsReactionChangesCancellationAndFailedPlayback() {
        User user = user();
        explorations.recordView(fixture.sessionId(), new RecordViewRequest(fixture.shortId(), 60, false), user);
        var initial = explorations.getProgress(fixture.sessionId(), user);
        assertThat(initial.getCurrentStep()).isEqualTo(1);
        assertThat(initial.getMaxCount()).isEqualTo(1);
        assertThat(initial.getTopInterests()).hasSize(1);
        String hobbyName = jdbc.queryForObject("SELECT name FROM hobbies WHERE id = ?", String.class, fixture.hobbyId());
        assertThat(initial.getTopInterests().get(0).getHobbyName()).isEqualTo(hobbyName);
        assertThat(initial.getTopInterests().get(0).getPercent()).isEqualTo(100);
        assertThat(initial.getReactedHobbies()).isEmpty();

        explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.LIKE, fixture.sessionId()), user);
        assertThat(explorations.getProgress(fixture.sessionId(), user).getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.LIKE);
        explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.DISLIKE, fixture.sessionId()), user);
        var disliked = explorations.getProgress(fixture.sessionId(), user);
        assertThat(disliked.getTopInterests()).isEmpty();
        assertThat(disliked.getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.DISLIKE);

        explorations.cancelReaction(fixture.shortId(), user);
        var canceled = explorations.getProgress(fixture.sessionId(), user);
        assertThat(canceled.getReactedHobbies()).isEmpty();
        assertThat(canceled.getTopInterests()).hasSize(1); // 시청 점수는 취소되지 않는다.
        explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.IRRELEVANT, fixture.sessionId()), user);
        var irrelevant = explorations.getProgress(fixture.sessionId(), user);
        assertThat(irrelevant.getReactedHobbies()).isEmpty();
        assertThat(irrelevant.getTopInterests()).isEmpty();
        explorations.recordReaction(fixture.shortId(), new ReactionRequest(ReactionType.LIKE, fixture.sessionId()), user);
        explorations.recordView(fixture.sessionId(), new RecordViewRequest(fixture.shortId(), 60, true), user);
        var failed = explorations.getProgress(fixture.sessionId(), user);
        assertThat(failed.getCurrentStep()).isEqualTo(1);
        assertThat(failed.getReactedHobbies()).isEmpty();
        assertThat(failed.getTopInterests()).isEmpty();
    }

    @Test void progressDoesNotIncludeReactionFromAnotherSession() {
        transaction.executeWithoutResult(status -> {
            User user = em.getReference(User.class, fixture.userId());
            Short shortform = em.getReference(Short.class, fixture.shortId());
            ExplorationSession past = ExplorationSession.builder().user(user).build();
            past.incrementViewedCount();
            em.persist(past);
            em.persist(ExplorationView.builder().session(past).shortform(shortform).seq((short) 1)
                    .watchRatio(BigDecimal.ONE).build());
            em.persist(ShortReaction.builder().user(user).shortform(shortform).sessionId(past.getId())
                    .type(ReactionType.LIKE).build());
        });
        var result = explorations.getProgress(fixture.sessionId(), user());
        assertThat(result.getCurrentStep()).isEqualTo(1);
        assertThat(result.getReactedHobbies()).isEmpty();
        assertThat(result.getTopInterests()).isEmpty();
    }

    @Test void progressReadsOneSnapshotDuringConcurrentRecordUpdate() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch update = new CountDownLatch(1);
        try {
            Future<?> writer = executor.submit(() -> transaction.executeWithoutResult(status -> {
                // 진행 조회의 마지막 반응 읽기를 잠시 막고, 그 사이 시청·반응을 함께 갱신한다.
                jdbc.execute("LOCK TABLE short_reactions IN ACCESS EXCLUSIVE MODE");
                locked.countDown();
                await(update);
                jdbc.update("UPDATE exploration_views SET watched_seconds = 60, watch_ratio = 1 WHERE session_id = ?", fixture.sessionId());
                jdbc.update("INSERT INTO short_reactions(user_id,short_id,session_id,type,created_at,updated_at) VALUES (?,?,?,'LIKE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                        fixture.userId(), fixture.shortId(), fixture.sessionId());
            }));
            assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<ExplorationProgressResponse> reader = executor.submit(() -> explorations.getProgress(fixture.sessionId(), user()));
            awaitBlockedReactionRead();
            update.countDown();
            writer.get(20, TimeUnit.SECONDS);
            var beforeUpdate = reader.get(20, TimeUnit.SECONDS);
            assertThat(beforeUpdate.getReactedHobbies()).isEmpty();
            assertThat(beforeUpdate.getTopInterests()).isEmpty();
            var afterUpdate = explorations.getProgress(fixture.sessionId(), user());
            assertThat(afterUpdate.getReactedHobbies().get(0).getReaction()).isEqualTo(ReactionType.LIKE);
            assertThat(afterUpdate.getTopInterests().get(0).getPercent()).isEqualTo(100);
        } finally {
            update.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitBlockedReactionRead() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Boolean blocked = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid() AND wait_event_type = 'Lock' AND query LIKE '%short_reactions%')", Boolean.class);
            if (Boolean.TRUE.equals(blocked)) return;
            Thread.sleep(20);
        }
        throw new AssertionError("진행 조회가 반응 테이블을 읽을 때까지 대기하지 못했습니다.");
    }

    private User user() { return users.findById(fixture.userId()).orElseThrow(); }

    private void assertStoredScore(String score) {
        BigDecimal actual = jdbc.queryForObject("SELECT i.score FROM recommendation_items i JOIN recommendations r ON r.id = i.recommendation_id WHERE r.session_id = ? AND i.hobby_id = ?",
                BigDecimal.class, fixture.sessionId(), fixture.hobbyId());
        assertThat(actual).isEqualByComparingTo(score);
    }

    private void assertEnded(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.SHORT_SESSION_ENDED));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
