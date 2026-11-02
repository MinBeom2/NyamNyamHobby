package com.ssafy.nyamnyam.domain.post.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.post.dto.request.CommentCreateRequest;
import com.ssafy.nyamnyam.domain.post.dto.request.CommentUpdateRequest;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentLikeResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentResponse;
import com.ssafy.nyamnyam.domain.post.entity.Comment;
import com.ssafy.nyamnyam.domain.post.entity.CommentStatus;
import com.ssafy.nyamnyam.domain.post.entity.LikeTargetType;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostType;
import com.ssafy.nyamnyam.domain.post.repository.CommentRepository;
import com.ssafy.nyamnyam.domain.post.repository.ContentLikeRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostRepository;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.domain.notification.event.CommentCreatedEvent;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private ContentLikeRepository likeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CommentService service;
    private Post post;
    private User penguin;
    private User rabbit;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentRepository, postRepository, likeRepository, userRepository, eventPublisher);
        Hobby climbing = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(climbing, "id", 1L);
        penguin = user(15L, "벽타는펭귄");
        rabbit = user(16L, "산토끼");
        post = Post.builder().hobby(climbing).user(penguin).type(PostType.QUESTION)
                .title("암벽화 추천").content("발볼이 넓어요").build();
        ReflectionTestUtils.setField(post, "id", 100L);
    }

    private static User user(long id, String nickname) {
        User user = User.builder().email(nickname + "@b.c").nickname(nickname).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Comment comment(long id, User author, Comment parent, String content) {
        Comment comment = Comment.builder().post(post).user(author).parent(parent).content(content).build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private void givenActivePost() {
        given(postRepository.findById(100L)).willReturn(Optional.of(post));
    }

    // ───────── 작성 ─────────

    @Test
    @DisplayName("댓글 작성: 줄바꿈 통일·공백 제거·HTML 이스케이프 후 저장하고 게시글 댓글 수 +1")
    void createComment() {
        givenActivePost();
        given(userRepository.findById(16L)).willReturn(Optional.of(rabbit));

        CommentResponse response = service.createComment(100L, 16L,
                new CommentCreateRequest("  저는 <b>라스포르티바</b>\r\n추천해요  ", null));

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(saved.capture());
        assertThat(saved.getValue().getContent()).isEqualTo("저는 &lt;b&gt;라스포르티바&lt;/b&gt;\n추천해요");
        assertThat(saved.getValue().getParent()).isNull();
        assertThat(response.isMine()).isTrue();
        assertThat(response.replies()).isEmpty();
        verify(postRepository).addCommentCount(100L, 1);
    }

    @Test
    @DisplayName("댓글에 답글: 그 댓글 아래에 붙고 내용은 그대로")
    void replyToComment() {
        Comment parent = comment(1L, penguin, null, "질문 있어요");
        givenActivePost();
        given(userRepository.findById(16L)).willReturn(Optional.of(rabbit));
        given(commentRepository.findWithRelationsById(1L)).willReturn(Optional.of(parent));

        CommentResponse response = service.createComment(100L, 16L, new CommentCreateRequest("답변이에요", 1L));

        assertThat(response.parentId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("답변이에요");
    }

    @Test
    @DisplayName("답글에 답글: 원래 부모 댓글 아래로 붙고 내용 앞에 \"@닉네임 \"")
    void replyToReply() {
        Comment parent = comment(1L, penguin, null, "질문 있어요");
        Comment reply = comment(2L, rabbit, parent, "답변이에요");
        givenActivePost();
        given(userRepository.findById(15L)).willReturn(Optional.of(penguin));
        given(commentRepository.findWithRelationsById(2L)).willReturn(Optional.of(reply));

        CommentResponse response = service.createComment(100L, 15L, new CommentCreateRequest("감사해요!", 2L));

        assertThat(response.parentId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("@산토끼 감사해요!");
    }

    @Test
    @DisplayName("작성하면 알림용 이벤트 발행: 글 작성자와 답글 대상(답글에 단 답글이면 그 답글 작성자)을 담는다")
    void createCommentPublishesNotificationEvent() {
        Comment parent = comment(1L, penguin, null, "질문 있어요");
        Comment reply = comment(2L, rabbit, parent, "답변이에요");
        givenActivePost();
        given(userRepository.findById(15L)).willReturn(Optional.of(penguin));
        given(commentRepository.findWithRelationsById(2L)).willReturn(Optional.of(reply));

        service.createComment(100L, 15L, new CommentCreateRequest("감사해요!", 2L));

        ArgumentCaptor<CommentCreatedEvent> captor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CommentCreatedEvent event = captor.getValue();
        assertThat(event.postId()).isEqualTo(100L);
        assertThat(event.postTitle()).isEqualTo("암벽화 추천");
        assertThat(event.postAuthorId()).isEqualTo(15L);
        assertThat(event.commenterId()).isEqualTo(15L);
        assertThat(event.replyTargetUserId()).isEqualTo(16L);
        assertThat(event.content()).isEqualTo("감사해요!");
    }

    @Test
    @DisplayName("다른 글의 댓글이나 삭제된 댓글에는 답글을 달 수 없음(404)")
    void replyTargetMustBeActiveAndSamePost() {
        Post other = Post.builder().hobby(post.getHobby()).user(penguin).type(PostType.INFO).title("다른 글").content("본문").build();
        ReflectionTestUtils.setField(other, "id", 200L);
        Comment otherPostComment = Comment.builder().post(other).user(penguin).content("다른 글 댓글").build();
        ReflectionTestUtils.setField(otherPostComment, "id", 9L);
        Comment deleted = comment(3L, penguin, null, "지운 댓글");
        deleted.delete();

        givenActivePost();
        given(userRepository.findById(16L)).willReturn(Optional.of(rabbit));
        willReturn(Optional.of(otherPostComment)).given(commentRepository).findWithRelationsById(9L);
        willReturn(Optional.of(deleted)).given(commentRepository).findWithRelationsById(3L);

        assertThatThrownBy(() -> service.createComment(100L, 16L, new CommentCreateRequest("답글", 9L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        assertThatThrownBy(() -> service.createComment(100L, 16L, new CommentCreateRequest("답글", 3L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(commentRepository, never()).save(any());
        verify(postRepository, never()).addCommentCount(anyLong(), anyInt());
    }

    @Test
    @DisplayName("삭제된 게시글에는 댓글을 달 수 없음(404)")
    void cannotCommentOnDeletedPost() {
        post.delete();
        givenActivePost();

        assertThatThrownBy(() -> service.createComment(100L, 16L, new CommentCreateRequest("댓글", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    // ───────── 목록 ─────────

    @Test
    @DisplayName("목록: 부모 50개 단위 페이지 + 답글을 부모별로 묶고, 좋아요 여부는 한 번에 조회")
    void getComments() {
        Comment first = comment(1L, penguin, null, "첫 댓글");
        Comment second = comment(2L, rabbit, null, "두번째 댓글");
        Comment replyA = comment(3L, rabbit, first, "답글A");
        Comment replyB = comment(4L, penguin, first, "답글B");
        givenActivePost();
        given(commentRepository.findVisibleParents(eq(100L), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(first, second), inv.getArgument(1), 51));
        given(commentRepository.findActiveReplies(List.of(1L, 2L))).willReturn(List.of(replyA, replyB));
        given(likeRepository.findLikedTargetIds(eq(16L), eq(LikeTargetType.COMMENT), any()))
                .willReturn(List.of(2L, 4L));

        PageResponse<CommentResponse> result = service.getComments(100L, 16L, 0);

        assertThat(result.page().size()).isEqualTo(CommentService.PAGE_SIZE);
        assertThat(result.page().totalElements()).isEqualTo(51);
        assertThat(result.page().hasNext()).isTrue();
        assertThat(result.content()).extracting(CommentResponse::id).containsExactly(1L, 2L);

        CommentResponse firstRes = result.content().get(0);
        assertThat(firstRes.replies()).extracting(CommentResponse::id).containsExactly(3L, 4L);
        assertThat(firstRes.replies()).extracting(CommentResponse::isLiked).containsExactly(false, true);
        assertThat(firstRes.replies()).extracting(CommentResponse::isMine).containsExactly(true, false);
        assertThat(result.content().get(1).isLiked()).isTrue();
        assertThat(result.content().get(1).replies()).isEmpty();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findVisibleParents(eq(100L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("목록: 지워졌지만 답글이 남은 댓글은 작성자·내용 없이 isDeleted=true")
    void deletedParentPlaceholder() {
        Comment parent = comment(1L, penguin, null, "지울 댓글");
        parent.delete();
        Comment reply = comment(2L, rabbit, parent, "남은 답글");
        givenActivePost();
        given(commentRepository.findVisibleParents(eq(100L), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(parent), inv.getArgument(1), 1));
        given(commentRepository.findActiveReplies(List.of(1L))).willReturn(List.of(reply));

        CommentResponse res = service.getComments(100L, null, 0).content().get(0);

        assertThat(res.isDeleted()).isTrue();
        assertThat(res.author()).isNull();
        assertThat(res.content()).isNull();
        assertThat(res.replies()).hasSize(1);
        assertThat(res.replies().get(0).content()).isEqualTo("남은 답글");
        verify(likeRepository, never()).findLikedTargetIds(any(), any(), any());
    }

    @Test
    @DisplayName("목록: 음수 페이지는 0으로")
    void negativePage() {
        givenActivePost();
        given(commentRepository.findVisibleParents(eq(100L), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1), 0));

        PageResponse<CommentResponse> result = service.getComments(100L, null, -3);

        assertThat(result.page().number()).isZero();
        assertThat(result.content()).isEmpty();
        verify(commentRepository, never()).findActiveReplies(any());
    }

    // ───────── 수정·삭제 ─────────

    @Test
    @DisplayName("수정: 본인 댓글만, 이스케이프해 저장")
    void updateComment() {
        Comment mine = comment(1L, rabbit, null, "원래 내용");
        given(commentRepository.findWithRelationsById(1L)).willReturn(Optional.of(mine));

        CommentResponse res = service.updateComment(1L, 16L, new CommentUpdateRequest("바뀐 \"내용\""));

        assertThat(res.content()).isEqualTo("바뀐 &quot;내용&quot;");
        assertThat(mine.getContent()).isEqualTo("바뀐 &quot;내용&quot;");
    }

    @Test
    @DisplayName("남의 댓글 수정·삭제는 403")
    void forbidden() {
        Comment others = comment(1L, penguin, null, "펭귄 댓글");
        given(commentRepository.findWithRelationsById(1L)).willReturn(Optional.of(others));

        assertThatThrownBy(() -> service.updateComment(1L, 16L, new CommentUpdateRequest("고침")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOARD_FORBIDDEN);
        assertThatThrownBy(() -> service.deleteComment(1L, 16L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOARD_FORBIDDEN);
        assertThat(others.isActive()).isTrue();
    }

    @Test
    @DisplayName("삭제: soft delete 후 게시글 댓글 수 -1, 이미 지운 댓글은 404")
    void deleteComment() {
        Comment mine = comment(1L, rabbit, null, "내 댓글");
        given(commentRepository.findWithRelationsById(1L)).willReturn(Optional.of(mine));

        service.deleteComment(1L, 16L);

        assertThat(mine.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(postRepository).addCommentCount(100L, -1);
        assertThatThrownBy(() -> service.deleteComment(1L, 16L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    // ───────── 좋아요 ─────────

    @Test
    @DisplayName("좋아요: 처음 누를 때만 +1, 취소도 눌렀던 경우에만 -1")
    void likes() {
        Comment target = comment(1L, penguin, null, "댓글");
        given(commentRepository.findWithRelationsById(1L)).willReturn(Optional.of(target));
        given(likeRepository.insertIfAbsent(eq(16L), eq("COMMENT"), eq(1L), any())).willReturn(1, 0);
        given(likeRepository.deleteLike(16L, LikeTargetType.COMMENT, 1L)).willReturn(0);
        given(commentRepository.findLikeCount(1L)).willReturn(1);

        CommentLikeResponse liked = service.likeComment(1L, 16L);
        service.likeComment(1L, 16L);
        CommentLikeResponse unliked = service.unlikeComment(1L, 16L);

        assertThat(liked.isLiked()).isTrue();
        assertThat(liked.likeCount()).isEqualTo(1);
        assertThat(unliked.isLiked()).isFalse();
        verify(commentRepository).addLikeCount(1L, 1);
        verify(commentRepository, never()).addLikeCount(1L, -1);
    }

    // ───────── 내용 규칙 ─────────

    @Test
    @DisplayName("내용 정리: 비었거나 500자 초과면 400")
    void normalize() {
        assertThat(CommentService.normalize(" a\r\nb\r c ")).isEqualTo("a\nb\n c");
        assertThat(CommentService.normalize("가".repeat(500))).hasSize(500);
        assertThatThrownBy(() -> CommentService.normalize("   "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
        assertThatThrownBy(() -> CommentService.normalize(null)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> CommentService.normalize("가".repeat(501))).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("멘션: 이미 같은 @닉네임으로 시작하면 다시 붙이지 않음")
    void withMention() {
        assertThat(CommentService.withMention("산토끼", "감사해요")).isEqualTo("@산토끼 감사해요");
        assertThat(CommentService.withMention("산토끼", "@산토끼 감사해요")).isEqualTo("@산토끼 감사해요");
        assertThat(CommentService.withMention("산토끼", "@산토끼\n감사해요")).isEqualTo("@산토끼\n감사해요");
        assertThat(CommentService.withMention("산토끼", "@산토끼")).isEqualTo("@산토끼");
        assertThat(CommentService.withMention("산토끼", "@산토끼님 감사해요")).isEqualTo("@산토끼 @산토끼님 감사해요");
    }

    @Test
    @DisplayName("이스케이프 후 500자를 넘으면 400")
    void escapeWithinLimit() {
        assertThat(CommentService.escapeWithinLimit("a<b")).isEqualTo("a&lt;b");
        assertThat(CommentService.escapeWithinLimit("가".repeat(496) + "<")).hasSize(500);
        assertThatThrownBy(() -> CommentService.escapeWithinLimit("가".repeat(497) + "<"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }
}
