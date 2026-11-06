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

    /**
     * 직접 선택 추천(basis=SELECTION). 숏폼 없이 고른 취미 1~10개를 이용 상황에 맞는 순으로 3개 정리하고,
     * 고른 취미들과 성향이 비슷한 취미 3개를 붙인다. 공통 성향 한 줄 요약은 basisText에 담는다.
     * 이유 문장은 이용 상황으로 바로 만들 수 있어 GPT를 부르지 않는다(reasonStatus=FALLBACK).
     * 홈은 가장 최근 추천을 보여주므로 숏폼 탐색 추천과 번갈아 덮인다. '내 취미'(interest_selections)는 건드리지 않는다.
     */
    @Transactional
    public RecommendationResponse createFromSelection(List<Long> hobbyIds, User user) {
        List<Long> ids = hobbyIds.stream().distinct().toList();
        if (ids.isEmpty() || ids.size() > 10)
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "직접 선택은 1~10개까지 가능합니다.");
        List<Hobby> published = hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED);
        Map<Long, Hobby> byId = published.stream().collect(Collectors.toMap(Hobby::getId, h -> h));
        List<Hobby> selected = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        if (selected.size() != ids.size())
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "선택할 수 없는 취미가 포함되어 있습니다.");

        List<UserHobby> storedHobbies = userHobbies.findByUserId(user.getId());
        Set<Long> saved = storedHobbies.stream().map(h -> h.getHobby().getId()).collect(Collectors.toSet());
        Set<Long> excluded = new HashSet<>(dislikes.findIds(user.getId()));
        storedHobbies.stream().filter(h -> h.getStatus() == UserHobbyStatus.IN_PROGRESS)
                .forEach(h -> excluded.add(h.getHobby().getId()));
        UserCondition condition = conditions.findById(user.getId()).orElse(null);
        RecommendationPlanner.Plan plan = planner.planSelection(selected, published, excluded, condition);

        Recommendation recommendation = Recommendation.builder().user(user).basis(RecoBasis.SELECTION).build();
        recommendation.initializeSnapshot(selectionSummary(selected, condition), mapper.writeValueAsString(conditionSnapshot(condition)));
        recommendationRepository.save(recommendation);
        int rank = 1;
        for (RecommendationPlanner.Choice choice : plan.choices()) {
            Hobby h = choice.hobby();
            RecommendationItem item = RecommendationItem.builder().recommendation(recommendation).hobby(h)
                    .type(choice.type()).rankNo((short) rank++)
                    .score(BigDecimal.valueOf(Math.min(choice.score(), 9999.999)).setScale(3, RoundingMode.HALF_UP))
                    .conditionWarning(choice.warning()).reasonStatus(ReasonStatus.FALLBACK).build();
            item.initializeSnapshot(mapper.writeValueAsString(RecommendationResponse.HobbySnapshot.from(h)), choice.fallbackReason(), saved.contains(h.getId()));
            itemRepository.save(item);
        }
        return toResponse(recommendation);
    }

    /** 고른 취미들에 2개 이상 겹치는 성향 태그를 많은 순으로 최대 3개. 예: '직접 고른 취미 5개에 #실외 #혼자 성향이 많아요.' */
    static String selectionSummary(List<Hobby> selected, UserCondition condition) {
        Map<String, Long> counts = selected.stream().flatMap(h -> h.getHobbyTags().stream())
                .map(ht -> ht.getTag().getName())
                .collect(Collectors.groupingBy(name -> name, LinkedHashMap::new, Collectors.counting()));
        List<String> common = counts.entrySet().stream().filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3).map(e -> "#" + e.getKey()).toList();
        String head = "직접 고른 취미 " + selected.size() + "개"
                + (common.isEmpty() ? "를 바탕으로 " : "에 " + String.join(" ", common) + " 성향이 많아요. ");
        return head + (condition == null ? "시작하기 좋은 순서로 정리했어요" : "이용 상황에 맞춰 시작하기 좋은 순서로 정리했어요");
    }

    public RecommendationResponse getRecommendation(Long recommendationId, User user) {
        Recommendation recommendation = recommendationRepository.findByIdAndUserId(recommendationId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        return toResponse(recommendation);
    }

    /** 내 가장 최근 추천(숏폼 탐색 또는 직접 선택). 둘 다 한 번도 하지 않았으면 비어 있다. */
    public Optional<RecommendationResponse> getLatestRecommendation(User user) {
        return recommendationRepository.findTopByUserIdOrderByIdDesc(user.getId()).map(this::toResponse);
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        return RecommendationResponse.builder().recommendationId(recommendation.getId())
                .basis(recommendation.getBasis() == RecoBasis.SELECTION ? "DIRECT_SELECTION" : "EXPLORATION_SESSION")
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
