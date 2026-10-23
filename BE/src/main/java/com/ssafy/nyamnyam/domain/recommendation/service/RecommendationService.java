package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.recommendation.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationItemRepository;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final ExplorationSessionRepository sessionRepository;
    private final ExplorationViewRepository viewRepository;
    private final ShortReactionRepository reactionRepository;
    private final HobbyRepository hobbyRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationItemRepository itemRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 탐색 세션 종료 및 추천 생성 (비동기 AI 추천 사유 이벤트 발행)
     */
    @Transactional
    public RecommendationResponse finishExploration(Long sessionId, User user) {
        ExplorationSession session = sessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "탐색 세션을 찾을 수 없습니다."));

        session.complete();

        List<ExplorationView> views = viewRepository.findBySessionOrderBySeqAsc(session);
        List<ShortReaction> reactions = reactionRepository.findByUser(user);

        Map<Long, ReactionType> reactionMap = reactions.stream()
                .collect(Collectors.toMap(r -> r.getShortform().getId(), ShortReaction::getType, (k1, k2) -> k1));

        // 1. 취미별 / 태그별 점수 계산
        Map<Long, Double> hobbyScoreMap = new HashMap<>();
        Map<String, Double> tagScoreMap = new HashMap<>();
        Map<Long, String> hobbyReactionSummaryMap = new HashMap<>();

        for (ExplorationView v : views) {
            Short s = v.getShortform();
            Hobby h = s.getHobby();
            if (h == null || v.isPlayFailed()) continue;

            ReactionType rType = reactionMap.get(s.getId());
            if (rType == ReactionType.IRRELEVANT) continue;

            double reactionScore = 0.0;
            String reactionDesc = "영상 시청";
            if (rType == ReactionType.LIKE) {
                reactionScore = 3.0;
                reactionDesc = "좋아요";
            } else if (rType == ReactionType.DISLIKE) {
                reactionScore = -3.0;
                reactionDesc = "별로예요";
            }

            double ratio = v.getWatchRatio() != null ? v.getWatchRatio().doubleValue() : 0.0;
            double watchWeight = (ratio * 2.0) - 1.0;
            double videoScore = reactionScore + watchWeight;

            hobbyScoreMap.put(h.getId(), hobbyScoreMap.getOrDefault(h.getId(), 0.0) + videoScore);
            hobbyReactionSummaryMap.put(h.getId(), reactionDesc);

            for (HobbyTag ht : h.getHobbyTags()) {
                String tagName = ht.getTag().getName();
                tagScoreMap.put(tagName, tagScoreMap.getOrDefault(tagName, 0.0) + (videoScore * 0.3));
            }
        }

        List<Hobby> allHobbies = hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED);

        // 2. 관심 취미(INTEREST) 상위 3개 선정
        List<Hobby> interestHobbies = allHobbies.stream()
                .filter(h -> hobbyScoreMap.getOrDefault(h.getId(), 0.0) > 0)
                .sorted((a, b) -> Double.compare(hobbyScoreMap.get(b.getId()), hobbyScoreMap.get(a.getId())))
                .limit(3)
                .toList();

        boolean isExploratory = false;
        if (interestHobbies.isEmpty()) {
            isExploratory = true;
            // 점수가 없으면 임의의 서로 다른 분류 취미들로 탐색용 제안
            interestHobbies = allHobbies.stream().limit(3).toList();
        }

        Set<Long> interestHobbyIds = interestHobbies.stream().map(Hobby::getId).collect(Collectors.toSet());

        // 3. 연관 취미(RELATED) 3개 선정 (취미태그 유사도 공식 적용)
        List<Hobby> candidateRelated = allHobbies.stream()
                .filter(h -> !interestHobbyIds.contains(h.getId()))
                .toList();

        List<Map.Entry<Hobby, Double>> rankedRelated = new ArrayList<>();
        double maxTagScore = tagScoreMap.values().stream().max(Double::compareTo).orElse(1.0);
        if (maxTagScore <= 0) maxTagScore = 1.0;

        for (Hobby candidate : candidateRelated) {
            double maxSimilarity = 0.0;
            for (Hobby interest : interestHobbies) {
                double sim = calculateTagSimilarity(interest, candidate);
                if (sim > maxSimilarity) maxSimilarity = sim;
            }

            // 후보 취미 태그 점수 합산
            double candTagSum = candidate.getHobbyTags().stream()
                    .mapToDouble(ht -> tagScoreMap.getOrDefault(ht.getTag().getName(), 0.0))
                    .sum();
            double normalizedTagScore = Math.max(0.0, candTagSum / maxTagScore);

            double totalScore = (maxSimilarity * 0.7) + (normalizedTagScore * 0.3);
            rankedRelated.add(Map.entry(candidate, totalScore));
        }

        rankedRelated.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Hobby> relatedHobbies = rankedRelated.stream().limit(3).map(Map.Entry::getKey).toList();

        // 4. Recommendation 엔티티 및 RecommendationItem 엔티티 저장
        Recommendation recommendation = Recommendation.builder()
                .user(user)
                .session(session)
                .basis(RecoBasis.REACTION)
                .isExploratory(isExploratory)
                .reactionCount((short) reactions.size())
                .build();
        recommendationRepository.save(recommendation);

        List<RecommendationResponse.RecommendationItemDto> itemDtos = new ArrayList<>();
        List<RecommendationReasonEvent.ItemReasonPayload> eventPayloads = new ArrayList<>();

        int rank = 1;
        // 관심 취미 등록
        for (Hobby h : interestHobbies) {
            double scoreVal = hobbyScoreMap.getOrDefault(h.getId(), 3.0);
            RecommendationItem item = RecommendationItem.builder()
                    .recommendation(recommendation)
                    .hobby(h)
                    .type(RecoItemType.INTEREST)
                    .rankNo((short) rank++)
                    .score(BigDecimal.valueOf(scoreVal).setScale(3, RoundingMode.HALF_UP))
                    .reasonStatus(ReasonStatus.PENDING)
                    .conditionWarning(false)
                    .build();
            itemRepository.save(item);

            itemDtos.add(toItemDto(item, h));
            eventPayloads.add(toEventPayload(item, h, hobbyReactionSummaryMap.get(h.getId())));
        }

        // 연관 취미 등록
        for (Hobby h : relatedHobbies) {
            RecommendationItem item = RecommendationItem.builder()
                    .recommendation(recommendation)
                    .hobby(h)
                    .type(RecoItemType.RELATED)
                    .rankNo((short) rank++)
                    .score(BigDecimal.valueOf(2.0).setScale(3, RoundingMode.HALF_UP))
                    .reasonStatus(ReasonStatus.PENDING)
                    .conditionWarning(false)
                    .build();
            itemRepository.save(item);

            itemDtos.add(toItemDto(item, h));
            eventPayloads.add(toEventPayload(item, h, "관심 취미와의 연관성"));
        }

        // 5. RabbitMQ 비동기 이벤트 발행 (reco.reason 큐)
        RecommendationReasonEvent event = RecommendationReasonEvent.builder()
                .recommendationId(recommendation.getId())
                .userId(user.getId())
                .items(eventPayloads)
                .build();

        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.RECO_REASON_ROUTING_KEY, event);
            log.info("Sent RecommendationReasonEvent for recommendationId: {}", recommendation.getId());
        } catch (Exception e) {
            log.error("Failed to publish RecommendationReasonEvent to RabbitMQ: {}", e.getMessage());
        }

        return RecommendationResponse.builder()
                .recommendationId(recommendation.getId())
                .basis(recommendation.getBasis())
                .isExploratory(recommendation.isExploratory())
                .reactionCount((int) recommendation.getReactionCount())
                .items(itemDtos)
                .build();
    }

    /**
     * 취미태그 유사도 기준표 공식:
     * (성향 겹침 수 + 4 * 주제 겹침 수) / 11.0
     */
    private double calculateTagSimilarity(Hobby h1, Hobby h2) {
        Set<String> h1Tendency = new HashSet<>();
        Set<String> h1Subject = new HashSet<>();

        for (HobbyTag ht : h1.getHobbyTags()) {
            Tag t = ht.getTag();
            if (t.getTagGroup() == TagGroup.SUBJECT || t.getTagGroup() == TagGroup.GAIN) {
                h1Subject.add(t.getName());
            } else {
                h1Tendency.add(t.getName());
            }
        }

        int tendencyOverlap = 0;
        int subjectOverlap = 0;

        for (HobbyTag ht : h2.getHobbyTags()) {
            Tag t = ht.getTag();
            if (t.getTagGroup() == TagGroup.SUBJECT || t.getTagGroup() == TagGroup.GAIN) {
                if (h1Subject.contains(t.getName())) subjectOverlap++;
            } else {
                if (h1Tendency.contains(t.getName())) tendencyOverlap++;
            }
        }

        return (tendencyOverlap + (4.0 * subjectOverlap)) / 11.0;
    }

    private RecommendationResponse.RecommendationItemDto toItemDto(RecommendationItem item, Hobby h) {
        String catName = h.getCategory() != null ? h.getCategory().getName() : null;
        String sessionM = String.format("%d~%d분", h.getSessionMinMinutes(), h.getSessionMaxMinutes());
        String monthlyC = String.format("%d~%d만원", h.getMonthlyCostMin() / 10000, h.getMonthlyCostMax() / 10000);

        return RecommendationResponse.RecommendationItemDto.builder()
                .itemId(item.getId())
                .hobbyId(h.getId())
                .hobbyName(h.getName())
                .category(catName)
                .type(item.getType())
                .rankNo((int) item.getRankNo())
                .score(item.getScore())
                .reason(item.getReason())
                .reasonStatus(item.getReasonStatus())
                .difficulty(h.getDifficulty())
                .sessionMinutes(sessionM)
                .monthlyCost(monthlyC)
                .placeType(h.getPlaceType())
                .socialType(h.getSocialType())
                .conditionWarning(item.isConditionWarning())
                .build();
    }

    private RecommendationReasonEvent.ItemReasonPayload toEventPayload(RecommendationItem item, Hobby h, String summary) {
        String subject = null;
        String gain = null;
        for (HobbyTag ht : h.getHobbyTags()) {
            if (ht.getTag().getTagGroup() == TagGroup.SUBJECT) subject = ht.getTag().getName();
            if (ht.getTag().getTagGroup() == TagGroup.GAIN) gain = ht.getTag().getName();
        }

        return RecommendationReasonEvent.ItemReasonPayload.builder()
                .itemId(item.getId())
                .hobbyId(h.getId())
                .hobbyName(h.getName())
                .subjectTag(subject)
                .gainTag(gain)
                .userReactionSummary(summary)
                .build();
    }
}
