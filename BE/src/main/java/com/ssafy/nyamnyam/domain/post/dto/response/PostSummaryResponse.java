package com.ssafy.nyamnyam.domain.post.dto.response;

import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostType;

import java.time.LocalDateTime;

/** 게시판 목록 한 줄 */
public record PostSummaryResponse(
        Long id,
    Long hobbyId,
    String hobbyName,
        PostType type,
        String title,
        String contentSummary,
        AuthorResponse author,
        String thumbnailUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
    private static final int SUMMARY_LENGTH = 100;

    public static PostSummaryResponse of(Post post, String thumbnailUrl) {
        return new PostSummaryResponse(
                post.getId(),
            post.getHobby().getId(),
            post.getHobby().getName(),
                post.getType(),
                post.getTitle(),
                summarize(post.getContent()),
                AuthorResponse.from(post.getUser()),
                thumbnailUrl,
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getCreatedAt()
        );
    }

    /**
     * 줄바꿈·연속 공백을 한 칸으로 줄이고 앞 100자만.
     * 본문은 HTML 이스케이프돼 저장돼 있으므로 &amp; 같은 엔티티가 중간에 잘리지 않게 엔티티 앞에서 자른다.
     */
    static String summarize(String content) {
        if (content == null) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        if (flat.length() <= SUMMARY_LENGTH) {
            return flat;
        }
        int cut = SUMMARY_LENGTH;
        int amp = flat.lastIndexOf('&', cut - 1);
        if (amp >= 0 && flat.indexOf(';', amp) >= cut) {
            cut = amp;
        }
        return flat.substring(0, cut) + "…";
    }
}
