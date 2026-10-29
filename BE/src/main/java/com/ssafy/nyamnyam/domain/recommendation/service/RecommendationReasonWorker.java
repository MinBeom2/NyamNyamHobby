package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent.ItemReasonPayload;
import com.ssafy.nyamnyam.domain.recommendation.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationItemRepository;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationReasonWorker {
    private final RecommendationItemRepository items;
    private final GmsClient gms;
    @Transactional
    public void process(Long recommendationId, ItemReasonPayload payload) {
        if (payload == null || payload.getItemId() == null) return;
        RecommendationItem item = items.findByIdForUpdate(payload.getItemId()).orElse(null);
        if (item == null || !item.getRecommendation().getId().equals(recommendationId) || item.getReasonStatus() != ReasonStatus.PENDING) return;
        Optional<String> generated;
        try {
            generated = gms.tryGenerateRecommendationReason(payload.getHobbyName(), payload.getSubjectTag(),
                    payload.getGainTag(), payload.getUserReactionSummary());
        } catch (Exception e) { generated = Optional.empty(); }
        String reason = generated.map(s -> s.replaceAll("[\\r\\n]+", " ").trim().replaceAll("^\"|\"$", ""))
                .filter(s -> !s.isBlank()).orElse(null);
        if (reason != null) {
            java.util.regex.Matcher sentence = java.util.regex.Pattern.compile("^.*?[.!?。](?:\\s|$)").matcher(reason);
            if (sentence.find()) reason = sentence.group().trim();
            if (reason.codePointCount(0, reason.length()) > 60) reason = reason.substring(0, reason.offsetByCodePoints(0, 60));
        }
        item.updateReason(reason, reason == null ? ReasonStatus.FALLBACK : ReasonStatus.COMPLETED);
    }
}
