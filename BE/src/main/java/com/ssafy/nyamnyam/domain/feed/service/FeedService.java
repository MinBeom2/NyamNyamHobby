package com.ssafy.nyamnyam.domain.feed.service;

import com.ssafy.nyamnyam.domain.feed.dto.request.FeedSearchCondition;
import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
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
import java.time.ZoneId;
import java.util.ArrayList;
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
            if (condition.hobbyId() != null) {
                predicates.add(cb.equal(root.get("hobby").get("id"), condition.hobbyId()));
            }
            if (condition.type() != null) {
                predicates.add(cb.equal(root.get("type"), condition.type()));
            }
            // 종료된 행사 제외 필터 (기본 true). 서버 시간대와 상관없이 한국 날짜로 판단한다
            if (condition.excludeEnded()) {
                LocalDate today = LocalDate.now(KST);
                predicates.add(cb.or(
                        cb.isNull(root.get("eventEnd")),
                        cb.greaterThanOrEqualTo(root.get("eventEnd"), today)
                ));
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
