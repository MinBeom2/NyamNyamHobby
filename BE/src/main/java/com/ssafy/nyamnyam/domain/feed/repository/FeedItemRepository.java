package com.ssafy.nyamnyam.domain.feed.repository;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FeedItemRepository extends JpaRepository<FeedItem, Long>, JpaSpecificationExecutor<FeedItem> {

    @Override
    @EntityGraph(attributePaths = {"hobby", "region"})
    Page<FeedItem> findAll(Specification<FeedItem> spec, Pageable pageable);
}

