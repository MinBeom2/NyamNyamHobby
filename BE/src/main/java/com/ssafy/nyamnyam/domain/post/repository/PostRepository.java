package com.ssafy.nyamnyam.domain.post.repository;

import com.ssafy.nyamnyam.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /** 목록: 작성자를 함께 읽어 N+1을 막는다 */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user JOIN FETCH p.hobby WHERE p.id = :id")
    Optional<Post> findWithUserAndHobbyById(@Param("id") Long id);

    /** 노출 중(ACTIVE)인 글만 조회수 +1. @return 올렸으면 1, 없거나 숨김·삭제면 0 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 " +
           "WHERE p.id = :id AND p.status = com.ssafy.nyamnyam.domain.post.entity.PostStatus.ACTIVE")
    int increaseViewCount(@Param("id") Long id);

    /** 동시 요청에도 개수가 어긋나지 않도록 DB에서 더한다. 0 아래로는 내려가지 않는다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount + :delta < 0 THEN 0 ELSE p.likeCount + :delta END " +
           "WHERE p.id = :id")
    int addLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Query("SELECT p.likeCount FROM Post p WHERE p.id = :id")
    int findLikeCount(@Param("id") Long id);

    /** 댓글 수(노출 중인 댓글+대댓글) 증감. 0 아래로는 내려가지 않는다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN p.commentCount + :delta < 0 THEN 0 ELSE p.commentCount + :delta END " +
           "WHERE p.id = :id")
    int addCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
