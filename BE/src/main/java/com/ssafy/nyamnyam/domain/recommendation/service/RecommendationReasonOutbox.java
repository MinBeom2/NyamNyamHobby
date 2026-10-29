package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.List;

/** 추천과 함께 저장한 발행 요청을 커밋 이후 전달한다. 중복 전달은 소비자가 처리한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationReasonOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final RabbitTemplate rabbit;
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void enqueue(RecommendationReasonEvent event) {
        jdbc.update("INSERT INTO recommendation_reason_outbox(recommendation_id,payload) VALUES (?,CAST(? AS jsonb))",
                event.getRecommendationId(), mapper.writeValueAsString(event));
    }
    private record Pending(long id, long recommendationId, String payload, int attempts) {}

    @Transactional
    public void dispatchBatch() {
        List<Pending> pending = jdbc.query("SELECT id,recommendation_id,payload::text,attempts FROM recommendation_reason_outbox WHERE next_attempt_at <= CURRENT_TIMESTAMP ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED",
                (rs, row) -> new Pending(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getInt(4)));
        for (Pending p : pending) {
            try {
                RecommendationReasonEvent event = mapper.readValue(p.payload(), RecommendationReasonEvent.class);
                Boolean confirmed = rabbit.invoke(operations -> {
                    operations.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.RECO_REASON_ROUTING_KEY, event);
                    operations.waitForConfirmsOrDie(5000);
                    return true;
                });
                if (!Boolean.TRUE.equals(confirmed)) throw new IllegalStateException("publisher confirm missing");
                jdbc.update("DELETE FROM recommendation_reason_outbox WHERE id = ?", p.id());
            } catch (Exception e) {
                int attempts = p.attempts() + 1;
                log.warn("Recommendation reason publish failed: id={}, attempt={}, error={}", p.recommendationId(), attempts, e.getClass().getSimpleName());
                if (attempts >= 5) {
                    jdbc.update("UPDATE recommendation_items SET reason_status='FALLBACK' WHERE recommendation_id=? AND reason_status='PENDING'", p.recommendationId());
                    jdbc.update("DELETE FROM recommendation_reason_outbox WHERE id = ?", p.id());
                } else {
                    jdbc.update("UPDATE recommendation_reason_outbox SET attempts=?, next_attempt_at=CURRENT_TIMESTAMP + (? * INTERVAL '1 second') WHERE id=?",
                            attempts, Math.min(300, 5 * (1 << attempts)), p.id());
                }
            }
        }
    }
}
