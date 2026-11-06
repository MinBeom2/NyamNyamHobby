package com.ssafy.nyamnyam.domain.feed.service;

import com.ssafy.nyamnyam.domain.feed.dto.request.FeedSearchCondition;
import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedScope;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.repository.InterestSelectionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final List<UserHobbyStatus> RUNNING = List.of(UserHobbyStatus.IN_PROGRESS, UserHobbyStatus.PAUSED);
    private static final char LIKE_ESCAPE = '\\';

    private final FeedItemRepository feedItemRepository;
    private final InterestSelectionRepository interestSelectionRepository;
    private final UserHobbyRepository userHobbyRepository;

    /**
     * 취미 정보 피드 목록 조회 (공개, PUBLISHED 피드만 노출).
     * scope를 주지 않으면 회원은 MY, 비회원은 ALL. 비회원이 MY·SAVED를 요청해도 ALL로 본다.
     * scope에 해당하는 취미가 하나도 없으면 빈 페이지를 돌려주고, 화면이 전체 피드와 안내를 보여 준다.
     * 취미 칩(hobbyId)·검색어·기간·지역은 그 안에서 더 좁힌다.
     */
    public PageResponse<FeedItemResponse> getFeeds(Long userId, FeedSearchCondition condition, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
        Set<Long> scopeHobbyIds = scopeHobbyIds(userId, resolveScope(userId, condition.scope()));
        if (scopeHobbyIds != null && scopeHobbyIds.isEmpty()) {
            return PageResponse.of(Page.<FeedItem>empty(pageable), FeedItemResponse::from);
        }
        Page<FeedItem> feedPage = feedItemRepository.findAll(feedSpec(scopeHobbyIds, condition), pageable);
        return PageResponse.of(feedPage, FeedItemResponse::from);
    }

    /** 홈 '관심 취미 새 소식'에서 한 번에 보여 줄 최대 개수 */
    public static final int HIGHLIGHT_MAX = 10;
    /** 오늘 올라온 소식 중 마감일 순으로 고를 후보 수(이보다 많으면 최신 것부터 이만큼만 본다) */
    private static final int TODAY_CANDIDATES = 100;

    /**
     * 홈 '관심 취미 새 소식'. 내 취미 + 진행 중 취미 기준(없으면 전체 취미), 마감·종료된 항목은 뺀다.
     * 오늘(한국 날짜) 새로 올라온 소식이 있으면 마감일이 빠른 순으로 먼저 채우고(마감일 없는 것은 뒤로),
     * 모자란 자리는 최신순으로 채운다. 오늘 새 소식이 없으면 최신순 그대로다.
     */
    public List<FeedItemResponse> getHighlights(Long userId, int limit) {
        int size = Math.max(1, Math.min(limit, HIGHLIGHT_MAX));
        Set<Long> scopeHobbyIds = userId == null ? null : scopeHobbyIds(userId, FeedScope.MY);
        if (scopeHobbyIds != null && scopeHobbyIds.isEmpty()) scopeHobbyIds = null; // 내 취미가 없으면 전체 취미
        Specification<FeedItem> visible = feedSpec(scopeHobbyIds,
                new FeedSearchCondition(null, null, null, true, null, null, null, null, false));
        LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay(KST)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(); // DB 시각은 UTC
        Specification<FeedItem> today = visible.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("publishedAt"), todayStart));

        List<FeedItem> picked = new ArrayList<>(feedItemRepository.findAll(today,
                        PageRequest.of(0, TODAY_CANDIDATES, Sort.by(Sort.Direction.DESC, "publishedAt", "id")))
                .getContent().stream()
                .sorted(Comparator.comparing(FeedItem::getEventEnd, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FeedItem::getId, Comparator.reverseOrder()))
                .limit(size)
                .toList());
        if (picked.size() < size) {
            Set<Long> pickedIds = new HashSet<>();
            picked.forEach(item -> pickedIds.add(item.getId()));
            feedItemRepository.findAll(visible,
                            PageRequest.of(0, size + picked.size(), Sort.by(Sort.Direction.DESC, "publishedAt", "id")))
                    .getContent().stream()
                    .filter(item -> !pickedIds.contains(item.getId()))
                    .limit(size - picked.size())
                    .forEach(picked::add);
        }
        return picked.stream().map(FeedItemResponse::from).toList();
    }

    private static FeedScope resolveScope(Long userId, FeedScope requested) {
        if (userId == null) return FeedScope.ALL;
        return requested == null ? FeedScope.MY : requested;
    }

    /** ALL이면 null(취미 제한 없음) */
    private Set<Long> scopeHobbyIds(Long userId, FeedScope scope) {
        if (scope == FeedScope.ALL) return null;
        Set<Long> ids = new LinkedHashSet<>();
        if (scope == FeedScope.MY) {
            interestSelectionRepository.findAllByUserIdWithHobby(userId)
                    .forEach(selection -> ids.add(selection.getHobby().getId()));
            RUNNING.forEach(status -> userHobbyRepository.findByUserIdAndStatus(userId, status)
                    .forEach(entry -> ids.add(entry.getHobby().getId())));
        } else {
            userHobbyRepository.findByUserIdAndStatus(userId, UserHobbyStatus.SAVED)
                    .forEach(entry -> ids.add(entry.getHobby().getId()));
        }
        return ids;
    }

    private static Specification<FeedItem> feedSpec(Set<Long> scopeHobbyIds, FeedSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), FeedStatus.PUBLISHED));
            if (scopeHobbyIds != null) {
                predicates.add(root.get("hobby").get("id").in(scopeHobbyIds));
            }
            if (condition.uncategorized()) {
                predicates.add(cb.isNull(root.get("hobby")));
            } else if (condition.hobbyId() != null) {
                predicates.add(cb.equal(root.get("hobby").get("id"), condition.hobbyId()));
            }
            if (condition.type() != null) {
                predicates.add(cb.equal(root.get("type"), condition.type()));
            }
            // 마감·종료 제외 필터 (기본 true): 기간이 끝난 행사와 접수가 끝난 강좌를 뺀다.
            // 날짜는 서버 시간대와 상관없이 한국 날짜로, 접수 상태는 요약 맨 앞 글자로 판단한다(FeedItem.isReservationClosed와 같은 규칙)
            if (condition.excludeEnded()) {
                LocalDate today = LocalDate.now(KST);
                predicates.add(cb.or(
                        cb.isNull(root.get("eventEnd")),
                        cb.greaterThanOrEqualTo(root.get("eventEnd"), today)
                ));
                List<Predicate> closed = FeedItem.CLOSED_STATUSES.stream()
                        .map(status -> cb.like(root.get("summary"), escapeLike(status) + "%", LIKE_ESCAPE))
                        .toList();
                predicates.add(cb.not(cb.and(
                        cb.equal(root.get("origin"), FeedOrigin.SEOUL_RESERVATION),
                        cb.isNotNull(root.get("summary")),
                        cb.or(closed.toArray(new Predicate[0]))
                )));
            }
            if (condition.keyword() != null) {
                String like = "%" + escapeLike(condition.keyword().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("summary")), like, LIKE_ESCAPE)
                ));
            }
            // 진행 기간이 [from, to]와 겹치는 항목. 시작일·종료일 중 하나만 있으면 그 날짜 하루로 본다
            if (condition.hasPeriod()) {
                Expression<LocalDate> start = cb.coalesce(root.get("eventStart"), root.get("eventEnd"));
                Expression<LocalDate> end = cb.coalesce(root.get("eventEnd"), root.get("eventStart"));
                predicates.add(cb.isNotNull(start));
                if (condition.to() != null) predicates.add(cb.lessThanOrEqualTo(start, condition.to()));
                if (condition.from() != null) predicates.add(cb.greaterThanOrEqualTo(end, condition.from()));
            }
            if (!condition.regionIds().isEmpty()) {
                predicates.add(root.get("region").get("id").in(condition.regionIds()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 검색어의 %, _ 를 글자 그대로 찾도록 */
    public static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
