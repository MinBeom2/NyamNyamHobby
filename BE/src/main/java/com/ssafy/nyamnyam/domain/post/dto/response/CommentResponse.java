package com.ssafy.nyamnyam.domain.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.nyamnyam.domain.post.entity.Comment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 한 개. 댓글이면 replies에 답글이 들어 있고, 답글이면 replies는 빈 목록이다.
 * 삭제됐지만 답글이 남아 있는 댓글은 isDeleted=true이고 author·content는 null("삭제된 댓글입니다"로 표시).
 */
public record CommentResponse(
        Long id,
        Long parentId,
        AuthorResponse author,
        String content,
        @JsonProperty("isDeleted") boolean isDeleted,
        int likeCount,
        @JsonProperty("isLiked") boolean isLiked,
        @JsonProperty("isMine") boolean isMine,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies
) {
    public static CommentResponse of(Comment comment, boolean isLiked, Long viewerId, List<CommentResponse> replies) {
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        if (!comment.isActive()) {
            return new CommentResponse(comment.getId(), parentId, null, null, true, 0, false, false,
                    comment.getCreatedAt(), comment.getUpdatedAt(), replies);
        }
        return new CommentResponse(
                comment.getId(),
                parentId,
                AuthorResponse.from(comment.getUser()),
                comment.getContent(),
                false,
                comment.getLikeCount(),
                isLiked,
                comment.isWrittenBy(viewerId),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
