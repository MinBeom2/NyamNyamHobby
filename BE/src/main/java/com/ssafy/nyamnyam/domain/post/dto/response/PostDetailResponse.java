package com.ssafy.nyamnyam.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostImage;
import com.ssafy.nyamnyam.domain.post.entity.PostType;

import java.time.LocalDateTime;
import java.util.List;

/** 게시글 상세. isLiked·isMine은 로그인하지 않았으면 false */
public record PostDetailResponse(
        Long id,
        HobbyRef hobby,
        PostType type,
        String title,
        String content,
        List<String> imageUrls,
        AuthorResponse author,
        int viewCount,
        int likeCount,
        int commentCount,
        @JsonProperty("isLiked") boolean isLiked,
        @JsonProperty("isMine") boolean isMine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record HobbyRef(Long id, String name) {
    }

    public static PostDetailResponse of(Post post, int viewCount, boolean isLiked, boolean isMine) {
        return new PostDetailResponse(
                post.getId(),
                new HobbyRef(post.getHobby().getId(), post.getHobby().getName()),
                post.getType(),
                post.getTitle(),
                post.getContent(),
                post.getImages().stream().map(PostImage::getUrl).toList(),
                AuthorResponse.from(post.getUser()),
                viewCount,
                post.getLikeCount(),
                post.getCommentCount(),
                isLiked,
                isMine,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
