package com.ssafy.nyamnyam.domain.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String url;

    /** 0부터 시작하는 표시 순서. 0번이 목록 썸네일 */
    @Column(nullable = false, columnDefinition = "smallint")
    private Short seq;

    PostImage(Post post, String url, int seq) {
        this.post = post;
        this.url = url;
        this.seq = (short) seq;
    }
}
