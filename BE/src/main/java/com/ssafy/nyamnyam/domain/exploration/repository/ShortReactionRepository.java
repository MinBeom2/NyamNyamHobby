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

    interface SessionReference {
        Long getSessionId();
    }

    // 취소 시 세션 잠금을 먼저 잡고, 반응 엔티티는 영상 잠금을 획득한 후 새로 조회한다.
    Optional<SessionReference> findSessionReferenceByUserAndShortformId(User user, Long shortId);

    @Query("SELECT COUNT(DISTINCT sr.user.id) FROM ShortReaction sr WHERE sr.shortform.id = :shortId AND sr.type = :type")
    long countByShortformIdAndType(@Param("shortId") Long shortId, @Param("type") ReactionType type);

    Optional<ShortReaction> findByUserAndShortform(User user, Short shortform);

    Optional<ShortReaction> findByUserAndShortformId(User user, Long shortId);

    List<ShortReaction> findByUserAndSessionId(User user, Long sessionId);

    List<ShortReaction> findByUser(User user);

    @Query("SELECT sr.shortform.id FROM ShortReaction sr WHERE sr.user.id = :userId AND sr.type = :type")
    List<Long> findShortIdsByUserIdAndType(@Param("userId") Long userId, @Param("type") ReactionType type);

    /** 비회원 이관용: guestKey로 연결된 반응 목록 */
    List<ShortReaction> findByGuestKey(String guestKey);

    /** 비회원 이관용: 회원이 이미 반응한 숏폼 ID 집합 */
    @Query("SELECT sr.shortform.id FROM ShortReaction sr WHERE sr.user.id = :userId")
    List<Long> findShortIdsByUserId(@Param("userId") Long userId);
}
