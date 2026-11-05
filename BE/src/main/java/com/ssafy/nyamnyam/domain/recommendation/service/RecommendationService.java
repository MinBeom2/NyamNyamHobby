package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.*;
import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.*;
import com.ssafy.nyamnyam.domain.recommendation.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.*;
import com.ssafy.nyamnyam.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    private final ExplorationSessionRepository sessionRepository;
    private final PreferenceProfileService profiles;
    private final RecommendationPlanner planner;
    private final HobbyRepository hobbyRepository;
    private final HobbyDislikeRepository dislikes;
    private final UserHobbyRepository userHobbies;
    private final UserConditionRepository conditions;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationItemRepository itemRepository;
    private final RecommendationReasonOutbox outbox;
    private final ObjectMapper mapper;

    @Transactional
    public FinishExplorationResult finishExploration(Long sessionId, User user) {
        ExplorationSession session = sessionRepository.findByIdAndUserForUpdate(sessionId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "탐색 세션을 찾을 수 없습니다."));
        Optional<Recommendation> existing = recommendationRepository.findBySessionId(sessionId);
        if (existing.isPresent()) return new FinishExplorationResult(toResponse(existing.get()), false);
        if (session.getStatus() == SessionStatus.ABANDONED) throw new CustomException(ErrorCode.SHORT_SESSION_ENDED);
        session.complete();

        PreferenceProfileService.Profile profile = profiles.load(user);
        List<UserHobby> storedHobbies = userHobbies.findByUserId(user.getId());
        Set<Long> saved = storedHobbies.stream().map(h -> h.getHobby().getId()).collect(Collectors.toSet());
        Set<Long> running = storedHobbies.stream().filter(h -> h.getStatus() == UserHobbyStatus.IN_PROGRESS)
                .map(h -> h.getHobby().getId()).collect(Collectors.toSet());
        UserCondition condition = conditions.findById(user.getId()).orElse(null);
        RecommendationPlanner.Plan plan = planner.plan(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED),
                profile, dislikes.findIds(user.getId()), running, condition);
        Recommendation recommendation = Recommendation.builder().user(user).session(session).basis(RecoBasis.REACTION)
                .isExploratory(plan.exploratory()).reactionCount((short) Math.min(Short.MAX_VALUE, profile.reactionCount())).build();
        String basisText = "숏폼 누적 반응 " + profile.reactionCount() + "개" + (condition == null ? "" : "와 이용 상황")
                + "을 바탕으로 " + (plan.exploratory() ? "탐색용 취미를 제안했어요" : "추천했어요");
        recommendation.initializeSnapshot(basisText, mapper.writeValueAsString(conditionSnapshot(condition)));
        recommendationRepository.save(recommendation);

        List<RecommendationReasonEvent.ItemReasonPayload> payloads = new ArrayList<>();
        int rank = 1;
        for (RecommendationPlanner.Choice choice : plan.choices()) {
            Hobby h = choice.hobby();
            RecommendationItem item = RecommendationItem.builder().recommendation(recommendation).hobby(h)
                    .type(choice.type()).rankNo((short) rank++)
                    .score(BigDecimal.valueOf(Math.min(choice.score(), 9999.999)).setScale(3, RoundingMode.HALF_UP))
                    .conditionWarning(choice.warning()).reasonStatus(ReasonStatus.PENDING).build();
            item.initializeSnapshot(mapper.writeValueAsString(RecommendationResponse.HobbySnapshot.from(h)), choice.fallbackReason(), saved.contains(h.getId()));
            itemRepository.save(item);
            payloads.add(RecommendationReasonEvent.ItemReasonPayload.builder().itemId(item.getId()).hobbyId(h.getId())
                    .hobbyName(h.getName()).subjectTag(tag(h, TagGroup.SUBJECT)).gainTag(tag(h, TagGroup.GAIN))
                    .userReactionSummary(choice.fallbackReason() + "; " + basisText).build());
        }
        if (!payloads.isEmpty()) outbox.enqueue(RecommendationReasonEvent.builder().recommendationId(recommendation.getId())
                .userId(user.getId()).items(payloads).build());
        return new FinishExplorationResult(toResponse(recommendation), true);
    }

    public RecommendationResponse getRecommendation(Long recommendationId, User user) {
        Recommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        return toResponse(recommendation);
    }

    /** 내 가장 최근 추천. 숏폼 탐색을 한 번도 끝내지 않았으면 비어 있다. */
    public Optional<RecommendationResponse> getLatestRecommendation(User user) {
        return recommendationRepository.findTopByUserIdOrderByIdDesc(user.getId()).map(this::toResponse);
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        return RecommendationResponse.builder().recommendationId(recommendation.getId()).basis("EXPLORATION_SESSION")
                .basisText(recommendation.getBasisText()).isExploratory(recommendation.isExploratory())
                .reactionCount((int) recommendation.getReactionCount())
                .items(itemRepository.findByRecommendationOrderByRankNoAsc(recommendation).stream().map(this::toItemDto).toList()).build();
    }
    private RecommendationResponse.RecommendationItemDto toItemDto(RecommendationItem item) {
        RecommendationResponse.HobbySnapshot hobby = item.getHobbySnapshot() == null
                ? RecommendationResponse.HobbySnapshot.from(item.getHobby())
                : withCurrentThumbnail(mapper.readValue(item.getHobbySnapshot(), RecommendationResponse.HobbySnapshot.class), item);
        String fallback = item.getFallbackReason() == null ? hobby.name() + " 취미를 추천해요" : item.getFallbackReason();
        return RecommendationResponse.RecommendationItemDto.builder().itemId(item.getId()).hobby(hobby).type(item.getType())
                .rank((int) item.getRankNo()).reason(item.getReason())
                .reasonStatus(item.getReasonStatus() == ReasonStatus.DONE ? ReasonStatus.COMPLETED : item.getReasonStatus())
                .fallbackReason(fallback).conditionWarning(item.isConditionWarning()).saved(item.isSaved()).build();
    }
    /**
     * 취미 대표 이미지(V14)를 넣기 전에 만든 추천은 사본에 이미지가 비어 있다.
     * 비어 있을 때만 같은 취미(hobby_id)의 지금 이미지로 채운다. 사본에 이미지가 있으면 그대로 둔다.
     */
    private RecommendationResponse.HobbySnapshot withCurrentThumbnail(RecommendationResponse.HobbySnapshot snapshot, RecommendationItem item) {
        if (snapshot.thumbnailUrl() != null && !snapshot.thumbnailUrl().isBlank()) return snapshot;
        Hobby hobby = item.getHobby();
        if (hobby == null || hobby.getThumbnailUrl() == null || hobby.getThumbnailUrl().isBlank()) return snapshot;
        return snapshot.withThumbnailUrl(hobby.getThumbnailUrl());
    }
    private String tag(Hobby hobby, TagGroup group) {
        return hobby.getHobbyTags().stream().map(HobbyTag::getTag).filter(t -> t.getTagGroup() == group)
                .map(Tag::getName).findFirst().orElse(null);
    }
    private Map<String, Object> conditionSnapshot(UserCondition c) {
        if (c == null) return Map.of();
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("weeklyHours", c.getWeeklyHours());
        snapshot.put("monthlyBudget", c.getMonthlyBudget());
        snapshot.put("placePref", c.getPlacePref());
        snapshot.put("socialPref", c.getSocialPref());
        return snapshot;
    }
}
