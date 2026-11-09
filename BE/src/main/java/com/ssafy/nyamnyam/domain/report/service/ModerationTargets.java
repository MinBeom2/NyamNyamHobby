package com.ssafy.nyamnyam.domain.report.service;

import com.ssafy.nyamnyam.domain.post.entity.Comment;
import com.ssafy.nyamnyam.domain.post.entity.CommentStatus;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostStatus;
import com.ssafy.nyamnyam.domain.post.repository.CommentRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostRepository;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 신고 대상(게시글·댓글)을 읽고 숨기거나 되살린다.
 * 숨김·복구는 "지금 상태가 맞을 때만 바꾸는" 조건부 UPDATE라서 동시에 여러 번 불려도 한 번만 바뀌고,
 * 댓글이면 게시글의 comment_count(노출 중인 댓글 수)도 그때만 함께 고친다.
 */
@Component
@RequiredArgsConstructor
public class ModerationTargets {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    /** 대상 상태. 화면 문구는 {@link #label()} */
    public enum State {
        ACTIVE("노출 중"),
        HIDDEN("숨김"),
        DELETED("작성자 삭제");

        private final String label;

        State(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * 알림·처리에 필요한 대상 정보. title·content는 저장된 그대로(HTML 이스케이프 상태)다.
     *
     * @param postId 게시글이면 자기 ID, 댓글이면 달린 게시글 ID(링크용)
     * @param title      게시글 제목. 댓글이면 null
     * @param postActive 댓글이 달린 게시글이 노출 중인지(게시글이면 자기 상태와 같다)
     */
    public record Target(
            ReportTargetType type,
            Long id,
            Long postId,
            Long authorId,
            String authorNickname,
            String title,
            String content,
            State state,
            boolean postActive
    ) {
        public boolean isActive() {
            return state == State.ACTIVE;
        }

        /** 사용자가 지금 볼 수 있는 대상만 신고할 수 있다(숨김·삭제된 게시글의 댓글 포함 제외) */
        public boolean isReportable() {
            return isActive() && postActive;
        }
    }

    public Optional<Target> find(ReportTargetType type, Long id) {
        return switch (type) {
            case POST -> postRepository.findById(id).map(ModerationTargets::of);
            case COMMENT -> commentRepository.findWithRelationsById(id).map(ModerationTargets::of);
            case USER -> Optional.empty();
        };
    }

    /** 노출 중이면 숨긴다. @return 이번 호출로 숨겼으면 true */
    public boolean hide(ReportTargetType type, Long id) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case POST -> postRepository.changeStatus(id, PostStatus.ACTIVE, PostStatus.HIDDEN, now) > 0;
            case COMMENT -> changeComment(id, CommentStatus.ACTIVE, CommentStatus.HIDDEN, -1, now);
            case USER -> false;
        };
    }

    /** 숨김 상태면 다시 노출한다. @return 이번 호출로 되살렸으면 true */
    public boolean unhide(ReportTargetType type, Long id) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case POST -> postRepository.changeStatus(id, PostStatus.HIDDEN, PostStatus.ACTIVE, now) > 0;
            case COMMENT -> changeComment(id, CommentStatus.HIDDEN, CommentStatus.ACTIVE, 1, now);
            case USER -> false;
        };
    }

    private boolean changeComment(Long id, CommentStatus from, CommentStatus to, int countDelta, LocalDateTime now) {
        Long postId = commentRepository.findWithRelationsById(id).map(c -> c.getPost().getId()).orElse(null);
        if (postId == null || commentRepository.changeStatus(id, from, to, now) == 0) {
            return false;
        }
        postRepository.addCommentCount(postId, countDelta);
        return true;
    }

    private static Target of(Post post) {
        return new Target(ReportTargetType.POST, post.getId(), post.getId(),
                post.getUser().getId(), post.getUser().getNickname(),
                post.getTitle(), post.getContent(), stateOf(post.getStatus().name()), post.isActive());
    }

    private static Target of(Comment comment) {
        return new Target(ReportTargetType.COMMENT, comment.getId(), comment.getPost().getId(),
                comment.getUser().getId(), comment.getUser().getNickname(),
                null, comment.getContent(), stateOf(comment.getStatus().name()), comment.getPost().isActive());
    }

    /** PostStatus·CommentStatus는 이름이 같다(ACTIVE·HIDDEN·DELETED) */
    private static State stateOf(String status) {
        return State.valueOf(status);
    }
}
