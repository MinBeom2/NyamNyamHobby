package com.ssafy.nyamnyam.domain.post.entity;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 취미별 게시판의 글(posts). 좋아요·댓글 수는 비정규화 컬럼으로 관리한다. */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostType type;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<PostImage> images = new ArrayList<>();

    @Builder
    public Post(Hobby hobby, User user, PostType type, String title, String content) {
        this.hobby = hobby;
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.status = PostStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isWrittenBy(Long userId) {
        return userId != null && user != null && userId.equals(user.getId());
    }

    public boolean isActive() {
        return status == PostStatus.ACTIVE;
    }

    /** null인 항목은 바꾸지 않는다 */
    public void update(PostType type, String title, String content) {
        if (type != null) {
            this.type = type;
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /** 이미지 전체 교체. 순서대로 seq 0, 1, 2… 를 붙인다(0번이 썸네일) */
    public void replaceImages(List<String> urls) {
        this.images.clear();
        for (int i = 0; i < urls.size(); i++) {
            this.images.add(new PostImage(this, urls.get(i), i));
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = PostStatus.DELETED;
        this.updatedAt = LocalDateTime.now();
    }
}
