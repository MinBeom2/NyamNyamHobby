package com.ssafy.nyamnyam.domain.plan;

import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.plan.dto.*;
import com.ssafy.nyamnyam.domain.plan.entity.*;
import com.ssafy.nyamnyam.domain.plan.service.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.error.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** Opt in against a disposable PostgreSQL+pgvector database; never substitutes an in-memory lock. */
@EnabledIfEnvironmentVariable(named = "TODO_POSTGRES_TESTS", matches = "true")
@SpringBootTest(properties = {"app.short-collect.enabled=false", "recommendation.reason-outbox-enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.listener.direct.auto-startup=false"})
class PlanPostgresIntegrationTest {
    @Autowired PlanService service;
    @Autowired PlanQueryService query;
    @Autowired UserHobbyRepository hobbies;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean RabbitTemplate rabbit;
    Long userId;
    Long categoryId;
    List<Long> hobbyIds = new ArrayList<>();
    TransactionTemplate tx;

    @BeforeEach void setup() {
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            String token = UUID.randomUUID().toString().substring(0, 8);
            var owner = User.builder().email("todo-" + token + "@test.local").nickname("td" + token)
                    .status(UserStatus.ACTIVE).build();
            em.persist(owner); userId = owner.getId();
            var category = Category.builder().name("todo-" + token).build();
            em.persist(category); categoryId = category.getId();
            for (int i = 0; i < 4; i++) {
                var hobby = Hobby.builder().category(category).name("todo-" + token + i)
                        .placeType(PlaceType.INDOOR).socialType(SocialType.ALONE).difficulty(Difficulty.EASY)
                        .sessionMinMinutes((short) 15).sessionMaxMinutes((short) 30).status(HobbyStatus.PUBLISHED).build();
                em.persist(hobby); hobbyIds.add(hobby.getId());
                var template = new PlanTemplate(hobby.getId(), "BEGINNER", "DB 테스트 계획", (short) 2, 7,
                        "PUBLISHED", LocalDateTime.now(Clock.systemUTC()));
                template.addWeek((short) 1, "첫 주"); template.addWeek((short) 2, "둘째 주");
                template.addItem(new PlanTemplateItem(template, (short) 1, (short) 1, "첫 활동", "설명",
                        (short) 15, "종이", "한 번 수행"));
                em.persist(template);
            }
        });
    }

    @AfterEach void cleanup() {
        if (userId == null) return;
        tx.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM user_plan_items WHERE user_plan_id IN (SELECT p.id FROM user_plans p JOIN user_hobbies h ON p.user_hobby_id=h.id WHERE h.user_id=?)", userId);
            jdbc.update("DELETE FROM user_plan_weeks WHERE user_plan_id IN (SELECT p.id FROM user_plans p JOIN user_hobbies h ON p.user_hobby_id=h.id WHERE h.user_id=?)", userId);
            jdbc.update("DELETE FROM user_plans WHERE user_hobby_id IN (SELECT id FROM user_hobbies WHERE user_id=?)", userId);
            jdbc.update("DELETE FROM user_hobbies WHERE user_id=?", userId);
            jdbc.update("DELETE FROM users WHERE id=?", userId);
            for (var id : hobbyIds) {
                jdbc.update("DELETE FROM plan_template_items WHERE template_id IN (SELECT id FROM plan_templates WHERE hobby_id=?)", id);
                jdbc.update("DELETE FROM plan_template_weeks WHERE template_id IN (SELECT id FROM plan_templates WHERE hobby_id=?)", id);
                jdbc.update("DELETE FROM plan_templates WHERE hobby_id=?", id);
                jdbc.update("DELETE FROM hobbies WHERE id=?", id);
            }
            if (categoryId != null) jdbc.update("DELETE FROM categories WHERE id=?", categoryId);
        });
    }

    PlanResponses.Started start(int i) { return service.start(userId, hobbyIds.get(i), new StartPlanRequest("BEGINNER")); }

    List<String> race(Callable<String> left, Callable<String> right) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2); var go = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (var action : List.of(left, right)) futures.add(pool.submit(() -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Race did not start");
                try { return action.call(); }
                catch (CustomException ex) { return ex.getErrorCode().getCode(); }
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally { go.countDown(); pool.shutdownNow(); }
    }

    @Test void concurrentStartsCannotExceedThree() throws Exception {
        start(0); start(1);
        assertThat(race(() -> { start(2); return "OK"; }, () -> { start(3); return "OK"; }))
                .containsExactlyInAnyOrder("OK", "MY_HOBBY_LIMIT");
        assertThat(hobbies.countByUserIdAndStatus(userId, UserHobbyStatus.IN_PROGRESS)).isEqualTo(3);
        assertThat(query.summary(userId).plans()).hasSize(3);
    }

    @Test void concurrentResumeAndStartShareSameLimit() throws Exception {
        start(0); start(1); var paused = start(2);
        service.changeStatus(userId, hobbyIds.get(2), new HobbyStatusRequest("PAUSED"));
        assertThat(race(() -> {
            var state = service.changeStatus(userId, hobbyIds.get(2), new HobbyStatusRequest("IN_PROGRESS"));
            assertThat(state.planId()).isEqualTo(paused.planId()); return "OK";
        }, () -> { start(3); return "OK"; })).containsExactlyInAnyOrder("OK", "MY_HOBBY_LIMIT");
        assertThat(hobbies.countByUserIdAndStatus(userId, UserHobbyStatus.IN_PROGRESS)).isEqualTo(3);
    }

    @Test void concurrentDuplicateStartsCreateOneCopy() throws Exception {
        assertThat(race(() -> { start(0); return "OK"; }, () -> { start(0); return "OK"; }))
                .containsExactlyInAnyOrder("OK", "MY_HOBBY_IN_PROGRESS");
        assertThat(query.summary(userId).plans()).hasSize(1);
    }

    @Test void copiedCollectionsAndMutationsRoundTripThroughPostgres() {
        var started = start(0);
        var view = query.get(userId, started.planId(), null, false);
        assertThat(view.weeks()).hasSize(2);
        assertThat(view.weekTitle()).isEqualTo("첫 주");
        var memo = new UpdatePlanItemRequest(); memo.setMemo("DB 메모");
        service.update(userId, started.planId(), view.items().get(0).itemId(), memo);
        var done = new UpdatePlanItemRequest(); done.setStatus(ItemStatus.DONE);
        service.update(userId, started.planId(), view.items().get(0).itemId(), done);
        var updated = query.get(userId, started.planId(), 1, false);
        assertThat(updated.weekCompleted()).isTrue();
        assertThat(updated.items().get(0).memo()).isEqualTo("DB 메모");
        service.delete(userId, started.planId(), view.items().get(0).itemId());
        assertThat(query.get(userId, started.planId(), 1, false).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_plan_items WHERE user_plan_id=? AND deleted_at IS NOT NULL",
                Integer.class, started.planId())).isEqualTo(1);
    }
}
