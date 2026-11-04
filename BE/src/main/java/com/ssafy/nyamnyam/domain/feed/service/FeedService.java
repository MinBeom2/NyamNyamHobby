package com.ssafy.nyamnyam.domain.feed.service;

import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedScope;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.repository.InterestSelectionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
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

    private final FeedItemRepository feedItemRepository;
    private final InterestSelectionRepository interestSelectionRepository;
    private final UserHobbyRepository userHobbyRepository;

    /**
     * 취미 정보 피드 목록 조회 (공개, PUBLISHED 피드만 노출).
     * scope를 주지 않으면 회원은 MY, 비회원은 ALL. 비회원이 MY·SAVED를 요청해도 ALL로 본다.
     * scope에 해당하는 취미가 하나도 없으면 빈 페이지를 돌려주고, 화면이 전체 피드와 안내를 보여 준다.
     * hobbyId(취미 칩)는 scope 안에서 한 취미로 더 좁힐 때 쓴다.
     */
    public PageResponse<FeedItemResponse> getFeeds(Long userId, FeedScope scope, Long hobbyId, FeedType type,
                                                   Boolean excludeEnded, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
        Set<Long> scopeHobbyIds = scopeHobbyIds(userId, resolveScope(userId, scope));
        if (scopeHobbyIds != null && scopeHobbyIds.isEmpty()) {
            return PageResponse.of(Page.<FeedItem>empty(pageable), FeedItemResponse::from);
        }
        Specification<FeedItem> spec = feedSpec(scopeHobbyIds, hobbyId, type, excludeEnded);
        Page<FeedItem> feedPage = feedItemRepository.findAll(spec, pageable);
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

    private static Specification<FeedItem> feedSpec(Set<Long> scopeHobbyIds, Long hobbyId, FeedType type,
                                                   Boolean excludeEnded) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), FeedStatus.PUBLISHED));
            if (scopeHobbyIds != null) {
                predicates.add(root.get("hobby").get("id").in(scopeHobbyIds));
            }
            if (hobbyId != null) {
                predicates.add(cb.equal(root.get("hobby").get("id"), hobbyId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            // 종료된 행사 제외 필터 (기본 true). 서버 시간대와 상관없이 한국 날짜로 판단한다
            if (excludeEnded == null || Boolean.TRUE.equals(excludeEnded)) {
                LocalDate today = LocalDate.now(KST);
                predicates.add(cb.or(
                        cb.isNull(root.get("eventEnd")),
                        cb.greaterThanOrEqualTo(root.get("eventEnd"), today)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
