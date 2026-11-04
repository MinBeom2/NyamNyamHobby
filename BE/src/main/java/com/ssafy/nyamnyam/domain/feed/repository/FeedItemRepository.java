package com.ssafy.nyamnyam.domain.feed.repository;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long>, JpaSpecificationExecutor<FeedItem> {

    @Override
    @EntityGraph(attributePaths = {"hobby"})
    Page<FeedItem> findAll(Specification<FeedItem> spec, Pageable pageable);

    Optional<FeedItem> findByOriginAndSourceRef(FeedOrigin origin, String sourceRef);

    /** 출처가 다른 같은 행사인지 보려고 시작일이 같은 항목을 꺼낸다(제목 유사도는 코드에서 비교) */
    List<FeedItem> findByEventStart(LocalDate eventStart);
}

