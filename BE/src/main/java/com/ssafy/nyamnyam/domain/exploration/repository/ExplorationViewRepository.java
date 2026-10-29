package com.ssafy.nyamnyam.domain.exploration.repository;

import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationSession;
import com.ssafy.nyamnyam.domain.exploration.entity.ExplorationView;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExplorationViewRepository extends JpaRepository<ExplorationView, Long> {

    Optional<ExplorationView> findBySessionAndShortform(ExplorationSession session, Short shortform);

    boolean existsBySessionAndShortform(ExplorationSession session, Short shortform);

    List<ExplorationView> findBySessionOrderBySeqAsc(ExplorationSession session);

    @Query("SELECT ev.shortform.id FROM ExplorationView ev WHERE ev.session.id = :sessionId")
    List<Long> findViewedShortIdsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT DISTINCT ev.shortform.id FROM ExplorationView ev WHERE ev.session.user.id = :userId")
    List<Long> findAllViewedShortIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT ev.session.user.id) FROM ExplorationView ev WHERE ev.shortform.id = :shortId")
    long countDistinctViewUsers(@Param("shortId") Long shortId);

    @Query("SELECT COUNT(DISTINCT ev.session.user.id) FROM ExplorationView ev " +
           "WHERE ev.shortform.id = :shortId AND ev.playFailed = true")
    long countDistinctUserPlayFailed(@Param("shortId") Long shortId);
}
