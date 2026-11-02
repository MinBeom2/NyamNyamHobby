package com.ssafy.nyamnyam.domain.post.service;

import com.ssafy.nyamnyam.domain.notification.event.CommentCreatedEvent;
import com.ssafy.nyamnyam.domain.post.dto.request.CommentCreateRequest;
import com.ssafy.nyamnyam.domain.post.dto.request.CommentUpdateRequest;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentLikeResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentResponse;
import com.ssafy.nyamnyam.domain.post.entity.Comment;
import com.ssafy.nyamnyam.domain.post.entity.LikeTargetType;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.repository.CommentRepository;
import com.ssafy.nyamnyam.domain.post.repository.ContentLikeRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.util.HtmlEscaper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 게시글 댓글(FR-BOARD-03·04).
 * <ul>
 *   <li>목록: 부모 댓글 50개씩(오래된 순) + 각 댓글의 답글 전부. 대화 묶음이 페이지 사이에서 잘리지 않는다.</li>
 *   <li>답글은 1단계만. 답글에 다시 답글을 달면 원래 부모 댓글 아래로 붙고 내용 앞에 "@닉네임 "을 단다.</li>
 *   <li>삭제: 답글이 남은 댓글은 "삭제된 댓글입니다" 자리로 남고, 남은 답글이 모두 지워지면 목록에서 사라진다.</li>
 *   <li>게시글의 comment_count = 노출 중인 댓글+답글 수. 작성 +1, 삭제 -1.</li>
 *   <li>내용은 게시글과 같이 HTML 이스케이프해 저장한다(길이는 원문 기준 1~500자).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    public static final int PAGE_SIZE = 50;
    private static final int MAX_LENGTH = 500;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ContentLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 댓글 목록(공개). viewerId는 비로그인이면 null */
    public PageResponse<CommentResponse> getComments(Long postId, Long viewerId, int page) {
        findActivePost(postId);

        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        Page<Comment> parents = commentRepository.findVisibleParents(postId, pageable);

        List<Long> parentIds = parents.getContent().stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParent = new LinkedHashMap<>();
        if (!parentIds.isEmpty()) {
            for (Comment reply : commentRepository.findActiveReplies(parentIds)) {
                repliesByParent.computeIfAbsent(reply.getParent().getId(), k -> new ArrayList<>()).add(reply);
            }
        }

        Set<Long> liked = likedIds(viewerId, parents.getContent(), repliesByParent);
        return PageResponse.of(parents, parent -> CommentResponse.of(
                parent,
                liked.contains(parent.getId()),
                viewerId,
                repliesByParent.getOrDefault(parent.getId(), List.of()).stream()
                        .map(reply -> CommentResponse.of(reply, liked.contains(reply.getId()), viewerId, List.of()))
                        .toList()
        ));
    }

    @Transactional
    public CommentResponse createComment(Long postId, Long userId, CommentCreateRequest request) {
        Post post = findActivePost(postId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String raw = normalize(request.content());
        Comment parent = null;
        User replyTarget = null;
        if (request.parentId() != null) {
            Comment target = commentRepository.findWithRelationsById(request.parentId())
                    .filter(Comment::isActive)
                    .filter(c -> c.getPost().getId().equals(postId))
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "답글을 달 댓글을 찾을 수 없어요."));
            replyTarget = target.getUser();
            if (target.isReply()) {
                // 답글의 답글: 원래 부모 아래로 붙이고 누구에게 단 답글인지 @닉네임으로 남긴다
                parent = target.getParent();
                raw = withMention(target.getUser().getNickname(), raw);
            } else {
                parent = target;
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(escapeWithinLimit(raw))
                .build();
        commentRepository.save(comment);
        CommentResponse response = CommentResponse.of(comment, false, userId, List.of());

        postRepository.addCommentCount(postId, 1);
        // 커밋된 뒤 글 작성자·답글 대상에게 알림을 보낸다(notification 도메인)
        eventPublisher.publishEvent(new CommentCreatedEvent(comment.getId(), postId, post.getTitle(),
                post.getUser().getId(), userId, user.getNickname(),
                replyTarget != null ? replyTarget.getId() : null, request.content()));
        return response;
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, CommentUpdateRequest request) {
        Comment comment = findActiveComment(commentId);
        checkOwner(comment, userId);
        comment.updateContent(escapeWithinLimit(normalize(request.content())));

        boolean isLiked = likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.COMMENT, commentId);
        return CommentResponse.of(comment, isLiked, userId, List.of());
    }

    /** soft delete. 답글이 남아 있으면 목록에 "삭제된 댓글입니다"로 남는다 */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findActiveComment(commentId);
        checkOwner(comment, userId);
        comment.delete();
        postRepository.addCommentCount(comment.getPost().getId(), -1);
    }

    /** 좋아요. 이미 눌렀으면 그대로 둔다 */
    @Transactional
    public CommentLikeResponse likeComment(Long commentId, Long userId) {
        findActiveComment(commentId);
        int inserted = likeRepository.insertIfAbsent(userId, LikeTargetType.COMMENT.name(), commentId, LocalDateTime.now());
        if (inserted > 0) {
            commentRepository.addLikeCount(commentId, 1);
        }
        return new CommentLikeResponse(commentRepository.findLikeCount(commentId), true);
    }

    /** 좋아요 취소. 누르지 않았으면 그대로 둔다 */
    @Transactional
    public CommentLikeResponse unlikeComment(Long commentId, Long userId) {
        findActiveComment(commentId);
        int deleted = likeRepository.deleteLike(userId, LikeTargetType.COMMENT, commentId);
        if (deleted > 0) {
            commentRepository.addLikeCount(commentId, -1);
        }
        return new CommentLikeResponse(commentRepository.findLikeCount(commentId), false);
    }

    // ───────── 내부 ─────────

    private Post findActivePost(Long postId) {
        return postRepository.findById(postId)
                .filter(Post::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없어요."));
    }

    /** 노출 중인 댓글이면서 게시글도 노출 중이어야 한다 */
    private Comment findActiveComment(Long commentId) {
        return commentRepository.findWithRelationsById(commentId)
                .filter(Comment::isActive)
                .filter(c -> c.getPost().isActive())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "댓글을 찾을 수 없어요."));
    }

    private void checkOwner(Comment comment, Long userId) {
        if (!comment.isWrittenBy(userId)) {
            throw new CustomException(ErrorCode.BOARD_FORBIDDEN);
        }
    }

    private Set<Long> likedIds(Long viewerId, List<Comment> parents, Map<Long, List<Comment>> repliesByParent) {
        if (viewerId == null) {
            return Set.of();
        }
        List<Long> ids = new ArrayList<>();
        parents.stream().filter(Comment::isActive).forEach(c -> ids.add(c.getId()));
        repliesByParent.values().forEach(list -> list.forEach(c -> ids.add(c.getId())));
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(likeRepository.findLikedTargetIds(viewerId, LikeTargetType.COMMENT, ids));
    }

    /** 줄바꿈 통일·앞뒤 공백 제거 후 원문 1~500자 */
    static String normalize(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "댓글은 1~500자로 입력해 주세요.");
        }
        return normalized;
    }

    /** "@닉네임 " 을 앞에 붙인다. 사용자가 이미 같은 멘션으로 시작했으면 중복으로 붙이지 않는다 */
    static String withMention(String nickname, String content) {
        String mention = "@" + nickname;
        if (content.equals(mention) || content.startsWith(mention + " ") || content.startsWith(mention + "\n")) {
            return content;
        }
        return mention + " " + content;
    }

    /** HTML 이스케이프 후 DB 컬럼(500자)을 넘으면 안내 */
    static String escapeWithinLimit(String raw) {
        String escaped = HtmlEscaper.escape(raw);
        if (escaped.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT,
                    "특수문자(<, >, &, \", ')나 @닉네임을 포함하면 500자를 넘어요. 댓글을 조금 줄여 주세요.");
        }
        return escaped;
    }
}
