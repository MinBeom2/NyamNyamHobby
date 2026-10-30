package com.ssafy.nyamnyam.domain.post.repository;

import com.ssafy.nyamnyam.domain.post.entity.ContentLike;
import com.ssafy.nyamnyam.domain.post.entity.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {

    /**
     * 좋아요 등록. 이미 있으면 아무것도 하지 않는다(중복 요청·동시 요청에도 예외 없이 안전).
     * @return 새로 등록했으면 1, 이미 있었으면 0
     */
    @Modifying
    @Query(nativeQuery = true, value =
            "INSERT INTO likes (user_id, target_type, target_id, created_at) " +
            "VALUES (:userId, :targetType, :targetId, :now) " +
            "ON CONFLICT (user_id, target_type, target_id) DO NOTHING")
    int insertIfAbsent(@Param("userId") Long userId,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId,
                       @Param("now") LocalDateTime now);

    /** @return 지웠으면 1, 없었으면 0 */
    @Modifying
    @Query("DELETE FROM ContentLike l WHERE l.userId = :userId AND l.targetType = :targetType AND l.targetId = :targetId")
    int deleteLike(@Param("userId") Long userId,
                   @Param("targetType") LikeTargetType targetType,
                   @Param("targetId") Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, LikeTargetType targetType, Long targetId);

    /** 대상들 중 사용자가 좋아요한 대상 ID(댓글 목록의 isLiked를 한 번에 채운다) */
    @Query("SELECT l.targetId FROM ContentLike l " +
           "WHERE l.userId = :userId AND l.targetType = :targetType AND l.targetId IN :targetIds")
    List<Long> findLikedTargetIds(@Param("userId") Long userId,
                                  @Param("targetType") LikeTargetType targetType,
                                  @Param("targetIds") Collection<Long> targetIds);
}
