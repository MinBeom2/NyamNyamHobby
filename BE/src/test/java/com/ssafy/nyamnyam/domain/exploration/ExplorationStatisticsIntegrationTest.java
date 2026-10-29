package com.ssafy.nyamnyam.domain.exploration;

import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.service.ExplorationService;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/** 실제 PostgreSQL의 영상 행 잠금·집계·상태 전환을 검증한다. 생성한 사용자와 영상만 정리한다. */
@SpringBootTest(properties = {"app.short-collect.enabled=false", "recommendation.reason-outbox-enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.listener.direct.auto-startup=false"})
class ExplorationStatisticsIntegrationTest {
    @Autowired ExplorationService service;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean RabbitTemplate rabbit;

    TransactionTemplate transaction;
    Long shortId;
    final List<Viewer> viewers = new ArrayList<>();
    record Viewer(Long userId, Long sessionId) {}

    @BeforeEach void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        shortId = transaction.execute(status -> {
            Short shortform = Short.builder().youtubeVideoId(token()).title("statistics test")
                    .durationSeconds((short) 60).status(ShortStatus.ACTIVE).build();
            em.persist(shortform);
            return shortform.getId();
        });
    }

    @AfterEach void cleanUp() {
        transaction.executeWithoutResult(status -> {
            for (Viewer viewer : viewers) {
                jdbc.update("DELETE FROM short_reactions WHERE user_id = ?", viewer.userId());
                jdbc.update("DELETE FROM exploration_views WHERE session_id IN (SELECT id FROM exploration_sessions WHERE user_id = ?)", viewer.userId());
                jdbc.update("DELETE FROM exploration_sessions WHERE user_id = ?", viewer.userId());
                jdbc.update("DELETE FROM users WHERE id = ?", viewer.userId());
            }
            if (shortId != null) jdbc.update("DELETE FROM shorts WHERE id = ?", shortId);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentExposuresCountUniqueUsers(boolean sameUser) throws Exception {
        Viewer first = viewer(false);
        Viewer second = sameUser ? first : viewer(false);
        concurrently(() -> assertThat(service.startSession(user(first)).getShortItem().getShortId()).isEqualTo(shortId),
                () -> assertThat(service.startSession(user(second)).getShortItem().getShortId()).isEqualTo(shortId));
        assertCounts(sameUser ? 1 : 2, 0, 0, "ACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exploration_views WHERE short_id = ?", Long.class, shortId)).isEqualTo(2);
        service.startSession(user(first));
        assertCounts(sameUser ? 1 : 2, 0, 0, "ACTIVE");
    }

    @Test void changesAndRepeatedCancellationsUseCurrentUniqueReactions() {
        Viewer viewer = viewer(true);
        react(viewer, ReactionType.IRRELEVANT);
        react(viewer, ReactionType.IRRELEVANT);
        assertCounts(1, 0, 1, "ACTIVE");
        react(viewer, ReactionType.LIKE);
        assertCounts(1, 0, 0, "ACTIVE");
        react(viewer, ReactionType.IRRELEVANT);
        service.cancelReaction(shortId, user(viewer));
        service.cancelReaction(shortId, user(viewer));
        assertCounts(1, 0, 0, "ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentReactionsDoNotLoseOrDuplicateCounts(boolean sameUser) throws Exception {
        Viewer first = viewer(true);
        Viewer second = sameUser ? anotherSession(first) : viewer(true);
        concurrently(() -> react(first, ReactionType.IRRELEVANT), () -> react(second, ReactionType.IRRELEVANT));
        assertCounts(sameUser ? 1 : 2, 0, sameUser ? 1 : 2, "ACTIVE");
        if (sameUser) {
            concurrently(() -> service.cancelReaction(shortId, user(first)), () -> service.cancelReaction(shortId, user(first)));
            assertCounts(1, 0, 0, "ACTIVE");
        }
    }

    @Test void fourIrrelevantUsersDoNotHideEvenAtOneHundredPercent() {
        seedViewers(4);
        for (Viewer viewer : viewers) react(viewer, ReactionType.IRRELEVANT);
        assertCounts(4, 0, 4, "ACTIVE");
    }

    @ParameterizedTest
    @ValueSource(ints = {25, 26})
    void hidesOnlyWhenBothUserAndRatioThresholdsAreMet(int total) {
        seedViewers(total);
        for (int i = 0; i < 5; i++) react(viewers.get(i), ReactionType.IRRELEVANT);
        assertCounts(total, 0, 5, total == 25 ? "AUTO_HIDDEN" : "ACTIVE");
        if (total == 26) react(viewers.get(5), ReactionType.IRRELEVANT);
        assertCounts(total, 0, total == 25 ? 5 : 6, "AUTO_HIDDEN");
        Viewer unaffected = viewers.get(6);
        assertError(() -> service.startSession(user(unaffected)), ErrorCode.COMMON_NOT_FOUND);
        assertThat(service.getNextShort(unaffected.sessionId(), user(unaffected)).getFinished()).isTrue();
        assertError(() -> react(unaffected, ReactionType.LIKE), ErrorCode.SHORT_NOT_ACTIVE);
    }

    @Test void repeatedFailuresAcrossSessionsCountOnceAndFailureEvidenceIsSticky() {
        seedViewers(3);
        Viewer first = viewers.get(0);
        Viewer repeated = anotherSession(first);
        watch(first, true);
        watch(first, true);
        watch(repeated, true);
        watch(first, false);
        assertCounts(3, 1, 0, "ACTIVE");
        assertThat(jdbc.queryForObject("SELECT play_failed FROM exploration_views WHERE session_id = ? AND short_id = ?",
                Boolean.class, first.sessionId(), shortId)).isTrue();
        watch(viewers.get(1), true);
        assertCounts(3, 2, 0, "ACTIVE");
        watch(viewers.get(2), true);
        assertCounts(3, 3, 0, "UNAVAILABLE");
        assertError(() -> service.startSession(user(first)), ErrorCode.COMMON_NOT_FOUND);
    }

    @Test void concurrentFailuresReachUnavailableWithoutLostUpdates() throws Exception {
        seedViewers(3);
        watch(viewers.get(0), true);
        concurrently(() -> watch(viewers.get(1), true), () -> watch(viewers.get(2), true));
        assertCounts(3, 3, 0, "UNAVAILABLE");
    }

    @Test void candidateHiddenWhileWaitingForLockIsNotExposed() throws Exception {
        seedViewers(25);
        for (int i = 0; i < 4; i++) react(viewers.get(i), ReactionType.IRRELEVANT);
        Viewer newcomer = viewer(false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch hidden = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch selecting = new CountDownLatch(1);
        try {
            Future<?> hider = executor.submit(() -> transaction.executeWithoutResult(status -> {
                react(viewers.get(4), ReactionType.IRRELEVANT);
                em.flush();
                hidden.countDown();
                await(release);
            }));
            assertThat(hidden.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> selection = executor.submit(() -> {
                selecting.countDown();
                assertError(() -> service.startSession(user(newcomer)), ErrorCode.COMMON_NOT_FOUND);
            });
            assertThat(selecting.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> selection.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            hider.get(20, TimeUnit.SECONDS);
            selection.get(20, TimeUnit.SECONDS);
            assertCounts(25, 0, 5, "AUTO_HIDDEN");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exploration_sessions WHERE user_id = ?", Long.class,
                    newcomer.userId())).isZero();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Viewer viewer(boolean expose) {
        Viewer viewer = transaction.execute(status -> {
            String token = token();
            User user = User.builder().email(token + "@statistics.test").nickname(token).build();
            em.persist(user);
            return new Viewer(user.getId(), expose ? expose(user) : null);
        });
        viewers.add(viewer);
        return viewer;
    }

    private Viewer anotherSession(Viewer viewer) {
        return transaction.execute(status -> new Viewer(viewer.userId(), expose(em.getReference(User.class, viewer.userId()))));
    }

    private Long expose(User user) {
        ExplorationSession session = ExplorationSession.builder().user(user).maxCount((short) 12).build();
        session.incrementViewedCount();
        em.persist(session);
        em.persist(ExplorationView.builder().session(session).shortform(em.getReference(Short.class, shortId))
                .seq((short) 1).watchRatio(BigDecimal.ZERO).build());
        return session.getId();
    }

    private void seedViewers(int count) { for (int i = 0; i < count; i++) viewer(true); }
    private User user(Viewer viewer) { return users.findById(viewer.userId()).orElseThrow(); }
    private void react(Viewer viewer, ReactionType type) {
        service.recordReaction(shortId, new ReactionRequest(type, viewer.sessionId()), user(viewer));
    }
    private void watch(Viewer viewer, boolean failed) {
        service.recordView(viewer.sessionId(), new RecordViewRequest(shortId, 10, failed), user(viewer));
    }
    private String token() { return "t" + UUID.randomUUID().toString().replace("-", "").substring(0, 10); }

    private void assertCounts(int viewers, int failures, int irrelevant, String status) {
        Map<String, Object> row = jdbc.queryForMap("SELECT view_user_count, fail_user_count, irrelevant_count, status FROM shorts WHERE id = ?", shortId);
        assertThat(row.get("view_user_count")).isEqualTo(viewers);
        assertThat(row.get("fail_user_count")).isEqualTo(failures);
        assertThat(row.get("irrelevant_count")).isEqualTo(irrelevant);
        assertThat(row.get("status")).isEqualTo(status);
    }

    private void assertError(Runnable operation, ErrorCode code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(CustomException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }

    private void concurrently(Runnable first, Runnable second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> a = executor.submit(() -> { ready.countDown(); await(start); first.run(); });
            Future<?> b = executor.submit(() -> { ready.countDown(); await(start); second.run(); });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            a.get(20, TimeUnit.SECONDS);
            b.get(20, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
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
