package com.ssafy.nyamnyam.domain.recommendation.consumer;

import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.entity.ReasonStatus;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecommendationItem;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationItemRepository;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationReasonConsumer {

    private final GmsClient gmsClient;
    private final RecommendationItemRepository itemRepository;

    @RabbitListener(queues = RabbitConfig.RECO_REASON_QUEUE)
    @Transactional
    public void consumeReasonEvent(RecommendationReasonEvent event) {
        log.info("Received RecommendationReasonEvent for recommendationId: {}", event.getRecommendationId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }

        for (RecommendationReasonEvent.ItemReasonPayload payload : event.getItems()) {
            try {
                String reason = gmsClient.generateRecommendationReason(
                        payload.getHobbyName(),
                        payload.getSubjectTag(),
                        payload.getGainTag(),
                        payload.getUserReactionSummary()
                );

                RecommendationItem item = itemRepository.findById(payload.getItemId()).orElse(null);
                if (item != null) {
                    item.updateReason(reason, ReasonStatus.COMPLETED);
                    log.info("Updated AI recommendation reason for hobby {}: {}", payload.getHobbyName(), reason);
                }
            } catch (Exception e) {
                log.error("Failed to generate and update AI reason for itemId {}: {}", payload.getItemId(), e.getMessage());
            }
        }
    }
}
