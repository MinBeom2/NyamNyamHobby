package com.ssafy.nyamnyam.domain.report;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.post.entity.Comment;
import com.ssafy.nyamnyam.domain.post.entity.CommentStatus;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostStatus;
import com.ssafy.nyamnyam.domain.post.entity.PostType;
import com.ssafy.nyamnyam.domain.post.repository.CommentRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostRepository;
import com.ssafy.nyamnyam.domain.report.entity.ReportTargetType;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.State;
import com.ssafy.nyamnyam.domain.report.service.ModerationTargets.Target;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModerationTargetsTest {

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;

    private ModerationTargets targets;
    private Post post;
    private Comment comment;

    @BeforeEach
    void setUp() {
        targets = new ModerationTargets(postRepository, commentRepository);
        Hobby hobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        User author = User.builder().email("a@b.c").nickname("벽타는펭귄").build();
        ReflectionTestUtils.setField(author, "id", 15L);
        post = Post.builder().hobby(hobby).user(author).type(PostType.QUESTION).title("제목").content("본문").build();
        ReflectionTestUtils.setField(post, "id", 12L);
        comment = Comment.builder().post(post).user(author).content("댓글").build();
        ReflectionTestUtils.setField(comment, "id", 7L);
    }

    @Test
    @DisplayName("대상 정보: 댓글이면 게시글 ID와 게시글 노출 여부를 함께")
    void find() {
        given(commentRepository.findWithRelationsById(7L)).willReturn(Optional.of(comment));
        post.delete();

        Target target = targets.find(ReportTargetType.COMMENT, 7L).orElseThrow();

        assertThat(target.postId()).isEqualTo(12L);
        assertThat(target.authorNickname()).isEqualTo("벽타는펭귄");
        assertThat(target.state()).isEqualTo(State.ACTIVE);
        assertThat(target.isReportable()).isFalse();
    }

    @Test
    @DisplayName("댓글 숨김·복구는 상태가 실제로 바뀐 경우에만 게시글 댓글 수를 고친다")
    void commentCount() {
        given(commentRepository.findWithRelationsById(7L)).willReturn(Optional.of(comment));
        willReturn(1, 0).given(commentRepository)
                .changeStatus(eq(7L), eq(CommentStatus.ACTIVE), eq(CommentStatus.HIDDEN), any());
        willReturn(1).given(commentRepository)
                .changeStatus(eq(7L), eq(CommentStatus.HIDDEN), eq(CommentStatus.ACTIVE), any());

        assertThat(targets.hide(ReportTargetType.COMMENT, 7L)).isTrue();
        assertThat(targets.hide(ReportTargetType.COMMENT, 7L)).isFalse(); // 이미 숨김(동시 요청)
        assertThat(targets.unhide(ReportTargetType.COMMENT, 7L)).isTrue();

        verify(postRepository).addCommentCount(12L, -1);
        verify(postRepository).addCommentCount(12L, 1);
    }

    @Test
    @DisplayName("게시글은 상태만 바꾸고, 사용자 대상은 아무것도 하지 않는다")
    void postAndUser() {
        given(postRepository.changeStatus(eq(12L), eq(PostStatus.ACTIVE), eq(PostStatus.HIDDEN), any())).willReturn(1);

        assertThat(targets.hide(ReportTargetType.POST, 12L)).isTrue();
        assertThat(targets.hide(ReportTargetType.USER, 1L)).isFalse();
        assertThat(targets.find(ReportTargetType.USER, 1L)).isEmpty();
        verify(postRepository, never()).addCommentCount(anyLong(), anyInt());
    }
}
