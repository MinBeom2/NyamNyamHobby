package com.ssafy.nyamnyam.domain.feed.service;

import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedItemRepository feedItemRepository;

    /**
     * 취미 정보 피드 목록 조회 (공개, PUBLISHED 피드만 노출)
     */
    public PageResponse<FeedItemResponse> getFeeds(Long hobbyId, FeedType type, Boolean excludeEnded, int page, int size) {
        Specification<FeedItem> spec = feedSpec(hobbyId, type, excludeEnded);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
        Page<FeedItem> feedPage = feedItemRepository.findAll(spec, pageable);
        return PageResponse.of(feedPage, FeedItemResponse::from);
    }

    private static Specification<FeedItem> feedSpec(Long hobbyId, FeedType type, Boolean excludeEnded) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), FeedStatus.PUBLISHED));

            if (hobbyId != null) {
                predicates.add(cb.equal(root.get("hobby").get("id"), hobbyId));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 종료된 행사 제외 필터 (기본 true)
            if (excludeEnded == null || Boolean.TRUE.equals(excludeEnded)) {
                LocalDate today = LocalDate.now();
                predicates.add(cb.or(
                        cb.isNull(root.get("eventEnd")),
                        cb.greaterThanOrEqualTo(root.get("eventEnd"), today)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

