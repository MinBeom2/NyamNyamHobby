package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.config.ExplorationProperties;
import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.*;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExplorationService {

    private final ExplorationSessionRepository sessionRepository;
    private final ExplorationViewRepository viewRepository;
    private final ShortReactionRepository reactionRepository;
    private final ShortRepository shortRepository;
    private final ShortSaveRepository shortSaveRepository;
    private final ExplorationProperties explorationProperties;
    private final RecommendationRepository recommendationRepository;
    private final PreferenceProfileService profiles;
    private final ExplorationSelectionPolicy selectionPolicy;

    /**
     * 1) 숏폼 탐색 세션 시작 + 첫 번째 영상 반환
     */
    @Transactional
    public StartExplorationResponse startSession(User user) {
        Short firstShort = selectShort(user, null, List.of())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "노출 가능한 숏폼 영상이 없습니다."));

        ExplorationSession session = ExplorationSession.builder()
                .user(user)
                .maxCount((short) explorationProperties.getSessionSize())
                .build();
        sessionRepository.save(session);

        session.incrementViewedCount();

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(firstShort)
                .seq(session.getViewedCount())
                .watchedSeconds((short) 0)
                .watchRatio(BigDecimal.ZERO)
                .playFailed(false)
                .build();
        viewRepository.save(view);
        refreshStatistics(firstShort);

        ReactionType myReaction = reactionRepository.findByUserAndShortform(user, firstShort)
                .map(ShortReaction::getType)
                .orElse(null);

        ShortDetailResponse shortResponse = ShortDetailResponse.of(firstShort, myReaction,
                shortSaveRepository.exists(user.getId(), firstShort.getId()));

        return new StartExplorationResponse(
                session.getId(),
                (int) session.getMaxCount(),
                (int) session.getViewedCount(),
                shortResponse
        );
    }

    /**
     * 2) 다음 숏폼 영상 조회
     */
    @Transactional
    public NextShortResponse getNextShort(Long sessionId, User user) {
        ExplorationSession session = getSessionForUpdate(sessionId, user);

        if (session.getStatus() != SessionStatus.IN_PROGRESS || session.getViewedCount() >= session.getMaxCount()) {
            session.complete();
            return new NextShortResponse(true, (int) session.getViewedCount(), null);
        }

        List<Long> sessionViewedIds = viewRepository.findViewedShortIdsBySessionId(sessionId);
        Optional<Short> selected = selectShort(user, sessionId, sessionViewedIds);

        if (selected.isEmpty()) {
            session.complete();
            return new NextShortResponse(true, (int) session.getViewedCount(), null);
        }

        Short nextShort = selected.get();

        session.incrementViewedCount();

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(nextShort)
                .seq(session.getViewedCount())
                .watchedSeconds((short) 0)
                .watchRatio(BigDecimal.ZERO)
                .playFailed(false)
                .build();
        viewRepository.save(view);
        refreshStatistics(nextShort);

        ReactionType myReaction = reactionRepository.findByUserAndShortform(user, nextShort)
                .map(ShortReaction::getType)
                .orElse(null);

        ShortDetailResponse shortResponse = ShortDetailResponse.of(nextShort, myReaction,
                shortSaveRepository.exists(user.getId(), nextShort.getId()));

        return new NextShortResponse(false, (int) session.getViewedCount(), shortResponse);
    }

    /**
     * 3) 시청 시간 / 재생 실패 기록
     */
    @Transactional
    public void recordView(Long sessionId, RecordViewRequest request, User user) {
        ExplorationSession session = getSessionForUpdate(sessionId, user);
        requireMutable(session);
        Short shortform = getShortForUpdate(request.getShortId());
        ExplorationView view = viewRepository.findBySessionAndShortform(session, shortform)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "이 세션에 노출된 숏폼이 아닙니다."));

        int watchedSec = request.getWatchedSeconds() != null ? Math.max(0, request.getWatchedSeconds()) : 0;
        int duration = shortform.getDurationSeconds() != null && shortform.getDurationSeconds() > 0 ?
                shortform.getDurationSeconds() : 60;

        // 영상 길이를 초과하지 않도록 상한 적용
        watchedSec = Math.min(watchedSec, duration);

        BigDecimal watchRatio = BigDecimal.valueOf((double) watchedSec / duration)
                .setScale(3, RoundingMode.HALF_UP);

        // 동일 노출에서 보고된 실패는 뒤늦은 정상 보고나 재시도로 지우지 않는다.
        boolean playFailed = view.isPlayFailed() || Boolean.TRUE.equals(request.getPlayFailed());

        view.updateWatchTime((short) watchedSec, watchRatio, playFailed);

        refreshStatistics(shortform);
    }

    /**
     * 4) 숏폼 반응 등록 및 변경
     */
    @Transactional
    public ReactionResponse recordReaction(Long shortId, ReactionRequest request, User user) {
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "탐색 세션 ID는 필수이며 양수여야 합니다.");
        }
        ExplorationSession session = getSessionForUpdate(request.getSessionId(), user);
        requireMutable(session);
        Short shortform = getShortForUpdate(shortId);
        requireActive(shortform);
        requireExposed(session, shortform);

        Optional<ShortReaction> existing = reactionRepository.findByUserAndShortform(user, shortform);

        if (existing.isPresent()) {
            ShortReaction reaction = existing.get();
            reaction.updateType(request.getType(), request.getSessionId());
        } else {
            ShortReaction reaction = ShortReaction.builder()
                    .user(user)
                    .shortform(shortform)
                    .sessionId(request.getSessionId())
                    .type(request.getType())
                    .build();
            reactionRepository.save(reaction);

        }
        refreshStatistics(shortform);
        return new ReactionResponse(shortId, request.getType());
    }

    /**
     * 5) 숏폼 반응 취소
     */
    @Transactional
    public ReactionResponse cancelReaction(Long shortId, User user) {
        Optional<ShortReactionRepository.SessionReference> reference =
                reactionRepository.findSessionReferenceByUserAndShortformId(user, shortId);
        if (reference.isEmpty()) {
            Short shortform = shortRepository.findById(shortId)
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));
            requireActive(shortform);
            return new ReactionResponse(shortId, null);
        }

        Long sessionId = reference.get().getSessionId();
        if (sessionId == null) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND, "반응의 탐색 세션을 찾을 수 없습니다.");
        }
        ExplorationSession session = getSessionForUpdate(sessionId, user);
        requireMutable(session);
        Short shortform = getShortForUpdate(shortId);
        requireActive(shortform);
        requireExposed(session, shortform);
        Optional<ShortReaction> existing = reactionRepository.findByUserAndShortform(user, shortform);
        if (existing.isPresent()) {
            ShortReaction reaction = existing.get();
            if (!Objects.equals(reaction.getSessionId(), sessionId)) {
                throw new CustomException(ErrorCode.SHORT_SESSION_ENDED, "반응의 세션이 변경되었습니다. 다시 요청해 주세요.");
            }
            reactionRepository.delete(reaction);
            refreshStatistics(shortform);
        }
        return new ReactionResponse(shortId, null);
    }

    /**
     * 6) 탐색 세션 진행 현황 및 관심 주제 % 막대 조회
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ExplorationProgressResponse getProgress(Long sessionId, User user) {
        ExplorationSession session = getSessionOrThrow(sessionId, user);

        List<ExplorationView> views = viewRepository.findBySessionOrderBySeqAsc(session);
        List<ShortReaction> reactions = reactionRepository.findByUserAndSessionId(user, sessionId);

        Map<Long, ShortReaction> reactionMap = reactions.stream()
                .collect(Collectors.toMap(r -> r.getShortform().getId(), r -> r));
        Map<Long, Hobby> hobbies = new LinkedHashMap<>();
        Map<Long, BigDecimal> hobbyScores = new HashMap<>();
        Map<Long, ShortReaction> latestReactions = new HashMap<>();
        Comparator<ShortReaction> reactionOrder = Comparator.comparing(ShortReaction::getUpdatedAt)
                .thenComparing(r -> r.getId() != null ? r.getId() : 0L);

        for (ExplorationView v : views) {
            Short s = v.getShortform();
            Hobby h = s.getHobby();
            if (h == null || v.isPlayFailed()) continue;
            ShortReaction reaction = reactionMap.get(s.getId());
            ReactionType type = reaction != null ? reaction.getType() : null;
            if (type == ReactionType.IRRELEVANT) continue;

            hobbies.putIfAbsent(h.getId(), h);
            if (reaction != null) {
                latestReactions.merge(h.getId(), reaction,
                        (a, b) -> reactionOrder.compare(a, b) > 0 ? a : b);
            }
            int reactionScore = type == ReactionType.LIKE ? 3 : type == ReactionType.DISLIKE ? -3 : 0;
            BigDecimal ratio = v.getWatchRatio() != null ? v.getWatchRatio() : BigDecimal.ZERO;
            BigDecimal videoScore = ratio.multiply(BigDecimal.valueOf(2)).subtract(BigDecimal.ONE)
                    .add(BigDecimal.valueOf(reactionScore));
            hobbyScores.merge(h.getId(), videoScore, BigDecimal::add);
        }

        List<ExplorationProgressResponse.ReactedHobbyResponse> reactedHobbies = hobbies.values().stream()
                .filter(h -> latestReactions.containsKey(h.getId()))
                .map(h -> new ExplorationProgressResponse.ReactedHobbyResponse(
                        h.getId(), h.getName(), latestReactions.get(h.getId()).getType()))
                .toList();

        List<Map.Entry<Long, BigDecimal>> ranked = hobbyScores.entrySet().stream()
                .filter(e -> e.getValue().signum() > 0)
                .sorted((a, b) -> {
                    int scoreOrder = b.getValue().compareTo(a.getValue());
                    return scoreOrder != 0 ? scoreOrder : Long.compare(a.getKey(), b.getKey());
                })
                .limit(3)
                .toList();

        BigDecimal maxScore = ranked.isEmpty() ? BigDecimal.ONE : ranked.get(0).getValue();
        List<ExplorationProgressResponse.TopInterestResponse> topInterests = ranked.stream()
                .map(e -> new ExplorationProgressResponse.TopInterestResponse(hobbies.get(e.getKey()).getName(),
                        e.getValue().multiply(BigDecimal.valueOf(100))
                                .divide(maxScore, 0, RoundingMode.HALF_UP).intValueExact()))
                .toList();

        return ExplorationProgressResponse.builder()
                .currentStep((int) session.getViewedCount())
                .maxCount((int) session.getMaxCount())
                .reactedHobbies(reactedHobbies)
                .topInterests(topInterests)
                .build();
    }

    /** 첫 영상과 다음 영상에 같은 규칙을 적용한다. 새 영상이 없을 때만 과거 무반응 영상을 재사용한다. */
    private Optional<Short> selectShort(User user, Long sessionId, Collection<Long> sessionViewedIds) {
        Set<Long> viewedIds = new HashSet<>(viewRepository.findAllViewedShortIdsByUserId(user.getId()));
        Set<Long> excludedIds = new HashSet<>(sessionViewedIds);
        Set<Long> reactedIds = new HashSet<>();
        Set<Long> dislikedHobbies = new HashSet<>();
        for (ShortReaction reaction : reactionRepository.findByUser(user)) {
            Long shortId = reaction.getShortform().getId();
            reactedIds.add(shortId);
            if (reaction.getType() == ReactionType.IRRELEVANT) {
                excludedIds.add(shortId);
            }
            if (sessionId != null && sessionId.equals(reaction.getSessionId()) && reaction.getType() == ReactionType.DISLIKE
                    && reaction.getShortform().getHobby() != null) dislikedHobbies.add(reaction.getShortform().getHobby().getId());
        }

        List<Short> unseen = new ArrayList<>();
        List<Short> reusable = new ArrayList<>();
        for (Short candidate : shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE)) {
            Long shortId = candidate.getId();
            if (excludedIds.contains(shortId)) continue;
            if (candidate.getHobby() != null && dislikedHobbies.contains(candidate.getHobby().getId())) continue;
            if (!viewedIds.contains(shortId)) {
                unseen.add(candidate);
            } else if (!reactedIds.contains(shortId)) {
                reusable.add(candidate);
            }
        }

        if (unseen.isEmpty() && reusable.isEmpty()) return Optional.empty();
        var profile = profiles.load(user);
        var prior = sessionId == null ? profile : profiles.load(user, sessionId);
        boolean disperse = sessionViewedIds.size() < 4 && prior.hobbyScores().values().stream().noneMatch(score -> score > 0);
        Set<Long> seenCategories = sessionId == null ? Set.of() : viewRepository.findBySessionIdOrderBySeqAsc(sessionId).stream()
                .map(v -> v.getShortform().getHobby()).filter(Objects::nonNull).map(Hobby::getCategory)
                .filter(Objects::nonNull).map(c -> c.getId()).collect(Collectors.toSet());
        Optional<Short> selected = lockActiveCandidate(selectionPolicy.order(unseen, profile, disperse, seenCategories, explorationProperties.getPreferRatio()));
        return selected.isPresent() ? selected : lockActiveCandidate(selectionPolicy.order(reusable, profile, disperse, seenCategories, explorationProperties.getPreferRatio()));
    }

    private Optional<Short> lockActiveCandidate(List<Short> candidates) {
        while (!candidates.isEmpty()) {
            Short candidate = candidates.remove(0);
            Optional<Short> locked = shortRepository.findByIdAndStatusForUpdate(candidate.getId(), ShortStatus.ACTIVE);
            if (locked.isPresent()) return locked;
        }
        return Optional.empty();
    }

    private Short getShortForUpdate(Long shortId) {
        return shortRepository.findByIdForUpdate(shortId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));
    }

    private void refreshStatistics(Short shortform) {
        // 변경·삭제한 반응과 시청 기록까지 집계에 포함한다. 호출자는 영상 행 잠금을 보유한다.
        shortRepository.flush();
        shortform.updateStatistics(viewRepository.countDistinctViewUsers(shortform.getId()),
                viewRepository.countDistinctUserPlayFailed(shortform.getId()),
                reactionRepository.countByShortformIdAndType(shortform.getId(), ReactionType.IRRELEVANT));
    }

    private ExplorationSession getSessionOrThrow(Long sessionId, User user) {
        return sessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "탐색 세션을 찾을 수 없습니다."));
    }

    private ExplorationSession getSessionForUpdate(Long sessionId, User user) {
        return sessionRepository.findByIdAndUserForUpdate(sessionId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "탐색 세션을 찾을 수 없습니다."));
    }

    private void requireMutable(ExplorationSession session) {
        // COMPLETED는 영상 소진에 따른 자연 종료일 수도 있다. 추천 생성 전까지 마지막 기록을 받는다.
        if (session.getStatus() == SessionStatus.ABANDONED || recommendationRepository.existsBySessionId(session.getId())) {
            throw new CustomException(ErrorCode.SHORT_SESSION_ENDED);
        }
    }

    private void requireActive(Short shortform) {
        if (shortform.getStatus() != ShortStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SHORT_NOT_ACTIVE);
        }
    }

    private void requireExposed(ExplorationSession session, Short shortform) {
        if (!viewRepository.existsBySessionAndShortform(session, shortform)) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND, "이 세션에 노출된 숏폼이 아닙니다.");
        }
    }
}
