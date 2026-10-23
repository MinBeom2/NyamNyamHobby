package com.ssafy.nyamnyam.domain.exploration.repository;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import com.ssafy.nyamnyam.domain.exploration.entity.ShortReaction;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShortReactionRepository extends JpaRepository<ShortReaction, Long> {

    Optional<ShortReaction> findByUserAndShortform(User user, Short shortform);

    Optional<ShortReaction> findByUserAndShortformId(User user, Long shortId);

    List<ShortReaction> findByUserAndSessionId(User user, Long sessionId);

    List<ShortReaction> findByUser(User user);

    @Query("SELECT sr.shortform.id FROM ShortReaction sr WHERE sr.user.id = :userId AND sr.type = :type")
    List<Long> findShortIdsByUserIdAndType(@Param("userId") Long userId, @Param("type") ReactionType type);
}
