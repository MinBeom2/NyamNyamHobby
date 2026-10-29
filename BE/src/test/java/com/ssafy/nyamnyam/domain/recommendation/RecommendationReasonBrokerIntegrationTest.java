package com.ssafy.nyamnyam.domain.recommendation;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.recommendation.consumer.RecommendationReasonConsumer;
import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.entity.ReasonStatus;
import com.ssafy.nyamnyam.domain.recommendation.service.*;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** CI의 임시 PostgreSQL·RabbitMQ에서 실행하며 실제 GMS 호출은 하지 않는다. */
@SpringBootTest(properties={"app.short-collect.enabled=false","recommendation.reason-outbox-enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false","spring.rabbitmq.listener.direct.auto-startup=false"})
class RecommendationReasonBrokerIntegrationTest {
    @Autowired RecommendationService service;
    @Autowired RecommendationReasonOutbox outbox;
    @Autowired RecommendationReasonConsumer consumer;
    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean GmsClient gms;
    Long userId, sessionId;
    @BeforeEach void setup() {
        new TransactionTemplate(manager).executeWithoutResult(status->{
            String token=UUID.randomUUID().toString().substring(0,8);
            User user=User.builder().email(token+"@broker.test").nickname("r"+token).build();em.persist(user);userId=user.getId();
            ExplorationSession session=ExplorationSession.builder().user(user).build();em.persist(session);sessionId=session.getId();
        });
    }
    @AfterEach void cleanup() {
        if(userId==null)return;
        new TransactionTemplate(manager).executeWithoutResult(status->{
            jdbc.update("DELETE FROM recommendation_reason_outbox WHERE recommendation_id IN (SELECT id FROM recommendations WHERE user_id=?)",userId);
            jdbc.update("DELETE FROM recommendation_items WHERE recommendation_id IN (SELECT id FROM recommendations WHERE user_id=?)",userId);
            jdbc.update("DELETE FROM recommendations WHERE user_id=?",userId);
            jdbc.update("DELETE FROM exploration_sessions WHERE user_id=?",userId);
            jdbc.update("DELETE FROM users WHERE id=?",userId);
        });
    }
    @Test void confirmedPublishAndDuplicateDeliveryProduceOneReasonPerItem() {
        User user=users.findById(userId).orElseThrow();
        var generated=service.finishExploration(sessionId,user).response();
        assertThat(generated.getItems()).isNotEmpty();
        assertThat(generated.getItems()).allSatisfy(i->assertThat(i.getReasonStatus()).isEqualTo(ReasonStatus.PENDING));
        outbox.dispatchBatch();
        Object message=rabbit.receiveAndConvert(RabbitConfig.RECO_REASON_QUEUE,5000);
        assertThat(message).isInstanceOf(RecommendationReasonEvent.class);
        RecommendationReasonEvent event=(RecommendationReasonEvent)message;
        assertThat(event.getRecommendationId()).isEqualTo(generated.getRecommendationId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recommendation_reason_outbox WHERE recommendation_id=?",Long.class,event.getRecommendationId())).isZero();
        when(gms.tryGenerateRecommendationReason(any(),any(),any(),any())).thenReturn(Optional.of("즐겁게 시작할 수 있는 취미예요."));
        consumer.consumeReasonEvent(event);
        consumer.consumeReasonEvent(event);
        var completed=service.getRecommendation(event.getRecommendationId(),user);
        assertThat(completed.getItems()).allSatisfy(i->{assertThat(i.getReasonStatus()).isEqualTo(ReasonStatus.COMPLETED);assertThat(i.getReason()).hasSizeLessThanOrEqualTo(60);});
        verify(gms,times(event.getItems().size())).tryGenerateRecommendationReason(any(),any(),any(),any());
    }
}
