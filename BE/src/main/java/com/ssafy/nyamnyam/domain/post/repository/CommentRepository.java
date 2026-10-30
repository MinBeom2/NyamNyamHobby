package com.ssafy.nyamnyam.domain.post.repository;

import com.ssafy.nyamnyam.domain.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 게시글의 부모 댓글 한 페이지(오래된 순).
     * 노출 중(ACTIVE)이거나, 삭제·숨김됐어도 노출 중인 대댓글이 남아 있는 댓글("삭제된 댓글입니다" 자리 표시)만 나온다.
     */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.user " +
                   "WHERE c.post.id = :postId AND c.parent IS NULL " +
                   "AND (c.status = com.ssafy.nyamnyam.domain.post.entity.CommentStatus.ACTIVE " +
                   "  OR EXISTS (SELECT r.id FROM Comment r WHERE r.parent = c " +
                   "             AND r.status = com.ssafy.nyamnyam.domain.post.entity.CommentStatus.ACTIVE))",
           countQuery = "SELECT COUNT(c) FROM Comment c " +
                        "WHERE c.post.id = :postId AND c.parent IS NULL " +
                        "AND (c.status = com.ssafy.nyamnyam.domain.post.entity.CommentStatus.ACTIVE " +
                        "  OR EXISTS (SELECT r.id FROM Comment r WHERE r.parent = c " +
                        "             AND r.status = com.ssafy.nyamnyam.domain.post.entity.CommentStatus.ACTIVE))")
    Page<Comment> findVisibleParents(@Param("postId") Long postId, Pageable pageable);

    /** 부모 댓글들의 노출 중인 대댓글(오래된 순) */
    @Query("SELECT r FROM Comment r JOIN FETCH r.user " +
           "WHERE r.parent.id IN :parentIds " +
           "AND r.status = com.ssafy.nyamnyam.domain.post.entity.CommentStatus.ACTIVE " +
           "ORDER BY r.createdAt ASC, r.id ASC")
    List<Comment> findActiveReplies(@Param("parentIds") Collection<Long> parentIds);

    /** 작성자·게시글·부모 댓글까지 함께 읽는다(답글 대상·권한 확인용) */
    @Query("SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.post LEFT JOIN FETCH c.parent " +
           "WHERE c.id = :id")
    Optional<Comment> findWithRelationsById(@Param("id") Long id);

    /** 동시 요청에도 개수가 어긋나지 않도록 DB에서 더한다. 0 아래로는 내려가지 않는다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN c.likeCount + :delta < 0 THEN 0 ELSE c.likeCount + :delta END " +
           "WHERE c.id = :id")
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Query("SELECT c.likeCount FROM Comment c WHERE c.id = :id")
    int findLikeCount(@Param("id") Long id);
}
