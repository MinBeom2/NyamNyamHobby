package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.*;
import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationViewRepository;
import com.ssafy.nyamnyam.domain.exploration.repository.ShortReactionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyTag;
import com.ssafy.nyamnyam.domain.hobby.entity.Tag;
import com.ssafy.nyamnyam.domain.hobby.entity.TagGroup;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class ExplorationService {

    private final ExplorationSessionRepository sessionRepository;
    private final ExplorationViewRepository viewRepository;
    private final ShortReactionRepository reactionRepository;
    private final ShortRepository shortRepository;

    private static final int DEFAULT_SESSION_SIZE = 12;

    /**
     * 1) 숏폼 탐색 세션 시작 + 첫 번째 영상 반환
     */
    @Transactional
    public StartExplorationResponse startSession(User user) {
        ExplorationSession session = ExplorationSession.builder()
                .user(user)
                .maxCount((short) DEFAULT_SESSION_SIZE)
                .build();
        sessionRepository.save(session);

        // 제외 대상: 관련 없음 표시한 영상
        Set<Long> excludedIds = new HashSet<>(reactionRepository.findShortIdsByUserIdAndType(user.getId(), ReactionType.IRRELEVANT));

        List<Short> candidates = excludedIds.isEmpty() ?
                shortRepository.findWithHobbyByStatus(ShortStatus.ACTIVE) :
                shortRepository.findCandidatesExcluding(ShortStatus.ACTIVE, excludedIds);

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND, "노출 가능한 숏폼 영상이 없습니다.");
        }

        // 무작위 1개 선택
        Collections.shuffle(candidates);
        Short firstShort = candidates.get(0);

        session.incrementViewedCount();
        firstShort.incrementViewUserCount();

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(firstShort)
                .seq(session.getViewedCount())
                .watchedSeconds((short) 0)
                .watchRatio(BigDecimal.ZERO)
                .playFailed(false)
                .build();
        viewRepository.save(view);

        ReactionType myReaction = reactionRepository.findByUserAndShortform(user, firstShort)
                .map(ShortReaction::getType)
                .orElse(null);

        ShortDetailResponse shortResponse = ShortDetailResponse.of(firstShort, myReaction, false);

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
        ExplorationSession session = getSessionOrThrow(sessionId, user);

        if (session.getStatus() != SessionStatus.IN_PROGRESS || session.getViewedCount() >= session.getMaxCount()) {
            session.complete();
            return new NextShortResponse(true, (int) session.getViewedCount(), null);
        }

        // 이번 세션에서 이미 본 영상
        List<Long> sessionViewedIds = viewRepository.findViewedShortIdsBySessionId(sessionId);
        // 사용자가 관련 없음 누른 영상
        List<Long> irrelevantIds = reactionRepository.findShortIdsByUserIdAndType(user.getId(), ReactionType.IRRELEVANT);
        // 사용자가 과거에 본 영상 전체
        List<Long> allViewedIds = viewRepository.findAllViewedShortIdsByUserId(user.getId());

        Set<Long> excludedIds = new HashSet<>();
        excludedIds.addAll(sessionViewedIds);
        excludedIds.addAll(irrelevantIds);
        excludedIds.addAll(allViewedIds);

        List<Short> candidates = shortRepository.findCandidatesExcluding(ShortStatus.ACTIVE, excludedIds);

        // 후보가 부족하면 이번 세션에서 보지 않았고 관련 없음이 아닌 영상 중에서 선택
        if (candidates.isEmpty()) {
            Set<Long> fallbackExcluded = new HashSet<>();
            fallbackExcluded.addAll(sessionViewedIds);
            fallbackExcluded.addAll(irrelevantIds);
            candidates = shortRepository.findCandidatesExcluding(ShortStatus.ACTIVE, fallbackExcluded);
        }

        // 그래도 후보가 없으면 종료
        if (candidates.isEmpty()) {
            session.complete();
            return new NextShortResponse(true, (int) session.getViewedCount(), null);
        }

        Collections.shuffle(candidates);
        Short nextShort = candidates.get(0);

        session.incrementViewedCount();
        nextShort.incrementViewUserCount();

        ExplorationView view = ExplorationView.builder()
                .session(session)
                .shortform(nextShort)
                .seq(session.getViewedCount())
                .watchedSeconds((short) 0)
                .watchRatio(BigDecimal.ZERO)
                .playFailed(false)
                .build();
        viewRepository.save(view);

        ReactionType myReaction = reactionRepository.findByUserAndShortform(user, nextShort)
                .map(ShortReaction::getType)
                .orElse(null);

        ShortDetailResponse shortResponse = ShortDetailResponse.of(nextShort, myReaction, false);

        return new NextShortResponse(false, (int) session.getViewedCount(), shortResponse);
    }

    /**
     * 3) 시청 시간 / 재생 실패 기록
     */
    @Transactional
    public void recordView(Long sessionId, RecordViewRequest request, User user) {
        ExplorationSession session = getSessionOrThrow(sessionId, user);
        Short shortform = shortRepository.findById(request.getShortId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        int watchedSec = request.getWatchedSeconds() != null ? Math.max(0, request.getWatchedSeconds()) : 0;
        int duration = shortform.getDurationSeconds() != null && shortform.getDurationSeconds() > 0 ?
                shortform.getDurationSeconds() : 60;

        // 영상 길이를 초과하지 않도록 상한 적용
        watchedSec = Math.min(watchedSec, duration);

        BigDecimal watchRatio = BigDecimal.valueOf((double) watchedSec / duration)
                .setScale(3, RoundingMode.HALF_UP);

        boolean playFailed = Boolean.TRUE.equals(request.getPlayFailed());

        Optional<ExplorationView> existingView = viewRepository.findBySessionAndShortform(session, shortform);
        if (existingView.isPresent()) {
            existingView.get().updateWatchTime((short) watchedSec, watchRatio, playFailed);
        } else {
            ExplorationView view = ExplorationView.builder()
                    .session(session)
                    .shortform(shortform)
                    .seq(session.getViewedCount())
                    .watchedSeconds((short) watchedSec)
                    .watchRatio(watchRatio)
                    .playFailed(playFailed)
                    .build();
            viewRepository.save(view);
        }

        if (playFailed) {
            shortform.incrementFailUserCount();
            long failedUsers = viewRepository.countDistinctUserPlayFailed(shortform.getId());
            if (failedUsers >= 3) {
                log.warn("Short {} unavailable due to {} failed users", shortform.getId(), failedUsers);
            }
        }
    }

    /**
     * 4) 숏폼 반응 등록 및 변경
     */
    @Transactional
    public ReactionResponse recordReaction(Long shortId, ReactionRequest request, User user) {
        Short shortform = shortRepository.findById(shortId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        Optional<ShortReaction> existing = reactionRepository.findByUserAndShortform(user, shortform);

        if (existing.isPresent()) {
            ShortReaction reaction = existing.get();
            ReactionType oldType = reaction.getType();
            ReactionType newType = request.getType();

            if (oldType == ReactionType.IRRELEVANT && newType != ReactionType.IRRELEVANT) {
                shortform.incrementIrrelevantCount(-1);
            } else if (oldType != ReactionType.IRRELEVANT && newType == ReactionType.IRRELEVANT) {
                shortform.incrementIrrelevantCount(1);
            }

            reaction.updateType(newType, request.getSessionId());
        } else {
            ShortReaction reaction = ShortReaction.builder()
                    .user(user)
                    .shortform(shortform)
                    .sessionId(request.getSessionId())
                    .type(request.getType())
                    .build();
            reactionRepository.save(reaction);

            if (request.getType() == ReactionType.IRRELEVANT) {
                shortform.incrementIrrelevantCount(1);
            }
        }

        return new ReactionResponse(shortId, request.getType());
    }

    /**
     * 5) 숏폼 반응 취소
     */
    @Transactional
    public ReactionResponse cancelReaction(Long shortId, User user) {
        Short shortform = shortRepository.findById(shortId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));

        Optional<ShortReaction> existing = reactionRepository.findByUserAndShortform(user, shortform);
        if (existing.isPresent()) {
            ShortReaction reaction = existing.get();
            if (reaction.getType() == ReactionType.IRRELEVANT) {
                shortform.incrementIrrelevantCount(-1);
            }
            reactionRepository.delete(reaction);
        }

        return new ReactionResponse(shortId, null);
    }

    /**
     * 6) 탐색 세션 진행 현황 및 관심 주제 % 막대 조회
     */
    public ExplorationProgressResponse getProgress(Long sessionId, User user) {
        ExplorationSession session = getSessionOrThrow(sessionId, user);

        List<ExplorationView> views = viewRepository.findBySessionOrderBySeqAsc(session);
        List<ShortReaction> reactions = reactionRepository.findByUserAndSessionId(user, sessionId);

        Map<Long, ReactionType> reactionMap = reactions.stream()
                .collect(Collectors.toMap(r -> r.getShortform().getId(), ShortReaction::getType, (k1, k2) -> k1));

        // 1. 지금까지 반응한 취미 목록
        Map<Long, ExplorationProgressResponse.ReactedHobbyResponse> reactedHobbyMap = new LinkedHashMap<>();
        for (ShortReaction r : reactions) {
            Hobby h = r.getShortform().getHobby();
            if (h != null && !reactedHobbyMap.containsKey(h.getId())) {
                reactedHobbyMap.put(h.getId(), new ExplorationProgressResponse.ReactedHobbyResponse(
                        h.getId(), h.getName(), r.getType()
                ));
            }
        }

        // 2. 관심 주제 카드 (소재 SUBJECT 태그 상위 3개 점수 집계)
        // 영상 점수 = 반응 점수(LIKE +3, DISLIKE -3) + 시청 가중치(watchRatio * 2 - 1)
        Map<String, Double> subjectScoreMap = new HashMap<>();

        for (ExplorationView v : views) {
            Short s = v.getShortform();
            Hobby h = s.getHobby();
            if (h == null) continue;

            ReactionType rType = reactionMap.get(s.getId());
            if (rType == ReactionType.IRRELEVANT || v.isPlayFailed()) continue;

            double reactionScore = 0.0;
            if (rType == ReactionType.LIKE) reactionScore = 3.0;
            else if (rType == ReactionType.DISLIKE) reactionScore = -3.0;

            double ratio = v.getWatchRatio() != null ? v.getWatchRatio().doubleValue() : 0.0;
            double watchWeight = (ratio * 2.0) - 1.0;
            double videoScore = reactionScore + watchWeight;

            // 해당 취미의 태그 중 SUBJECT(소재) 태그에 점수 가산
            for (HobbyTag ht : h.getHobbyTags()) {
                Tag tag = ht.getTag();
                if (tag.getTagGroup() == TagGroup.SUBJECT) {
                    subjectScoreMap.put(tag.getName(), subjectScoreMap.getOrDefault(tag.getName(), 0.0) + videoScore);
                }
            }
        }

        // 상위 3개 소재 태그 산출 및 % 계산
        List<Map.Entry<String, Double>> sortedSubjects = subjectScoreMap.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .toList();

        double maxScore = sortedSubjects.isEmpty() ? 1.0 : sortedSubjects.get(0).getValue();
        List<ExplorationProgressResponse.TopInterestResponse> topInterests = sortedSubjects.stream()
                .map(e -> {
                    int percent = (int) Math.round((e.getValue() / maxScore) * 100);
                    return new ExplorationProgressResponse.TopInterestResponse(e.getKey(), percent);
                })
                .toList();

        return ExplorationProgressResponse.builder()
                .currentStep((int) session.getViewedCount())
                .maxCount((int) session.getMaxCount())
                .reactedHobbies(new ArrayList<>(reactedHobbyMap.values()))
                .topInterests(topInterests)
                .build();
    }

    private ExplorationSession getSessionOrThrow(Long sessionId, User user) {
        return sessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "탐색 세션을 찾을 수 없습니다."));
    }
}
