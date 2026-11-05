package com.ssafy.nyamnyam.domain.feed.repository;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long>, JpaSpecificationExecutor<FeedItem> {

    @Override
    @EntityGraph(attributePaths = {"hobby", "region"})
    Page<FeedItem> findAll(Specification<FeedItem> spec, Pageable pageable);

    Optional<FeedItem> findByOriginAndSourceRef(FeedOrigin origin, String sourceRef);

    /**
     * GMS 분류 대상: 아직 분류하지 않은 항목. 화면에 안 보이는 분류 대기(PENDING_REVIEW)를 먼저,
     * 그다음 검색어 후보로 먼저 공개된 항목을 오래된 순으로 꺼낸다.
     */
    @Query("SELECT f FROM FeedItem f LEFT JOIN FETCH f.hobby WHERE f.classifiedAt IS NULL "
            + "ORDER BY CASE WHEN f.status = com.ssafy.nyamnyam.domain.feed.entity.FeedStatus.PENDING_REVIEW THEN 0 ELSE 1 END, f.id")
    List<FeedItem> findClassifyTargets(Pageable pageable);

    /** 출처가 다른 같은 행사인지 보려고 시작일이 같은 항목을 꺼낸다(제목 유사도는 코드에서 비교) */
    List<FeedItem> findByEventStart(LocalDate eventStart);
}

