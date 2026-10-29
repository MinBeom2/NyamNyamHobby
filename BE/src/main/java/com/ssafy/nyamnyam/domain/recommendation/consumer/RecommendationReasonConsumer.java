package com.ssafy.nyamnyam.domain.recommendation.consumer;

import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.service.RecommendationReasonWorker;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationReasonConsumer {
    private final RecommendationReasonWorker worker;
    @RabbitListener(queues = RabbitConfig.RECO_REASON_QUEUE)
    public void consumeReasonEvent(RecommendationReasonEvent event) {
        if (event == null || event.getRecommendationId() == null || event.getItems() == null) return;
        for (var item : event.getItems()) worker.process(event.getRecommendationId(), item);
    }
}
