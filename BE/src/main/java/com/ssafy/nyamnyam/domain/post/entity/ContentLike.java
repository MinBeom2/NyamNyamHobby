package com.ssafy.nyamnyam.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글·댓글 좋아요(likes). 회원당 대상별 1회(UNIQUE user_id, target_type, target_id).
 * 엔티티 이름을 Like로 하면 JPQL 예약어(LIKE)와 겹쳐 ContentLike로 둔다.
 * 등록은 동시 요청에도 안전하도록 리포지토리의 INSERT ... ON CONFLICT DO NOTHING으로 한다.
 */
@Entity
@Table(name = "likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private LikeTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
