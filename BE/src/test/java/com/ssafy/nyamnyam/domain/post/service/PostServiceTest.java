package com.ssafy.nyamnyam.domain.post.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.post.dto.request.PostCreateRequest;
import com.ssafy.nyamnyam.domain.post.dto.request.PostUpdateRequest;
import com.ssafy.nyamnyam.domain.post.dto.response.PostDetailResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.PostLikeResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.PostSummaryResponse;
import com.ssafy.nyamnyam.domain.post.entity.LikeTargetType;
import com.ssafy.nyamnyam.domain.post.entity.Post;
import com.ssafy.nyamnyam.domain.post.entity.PostStatus;
import com.ssafy.nyamnyam.domain.post.entity.PostType;
import com.ssafy.nyamnyam.domain.post.repository.ContentLikeRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostImageRepository;
import com.ssafy.nyamnyam.domain.post.repository.PostRepository;
import com.ssafy.nyamnyam.domain.post.service.PostService.PostSort;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.upload.UploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final String IMG = "https://k15d104.p.ssafy.io/api/v1/uploads/images/post/2026/10/";
    private static final String PROFILE_IMG = "https://k15d104.p.ssafy.io/api/v1/uploads/images/profile/2026/10/";

    @Mock private PostRepository postRepository;
    @Mock private PostImageRepository postImageRepository;
    @Mock private ContentLikeRepository likeRepository;
    @Mock private HobbyRepository hobbyRepository;
    @Mock private UserRepository userRepository;

    private PostService service;
    private Hobby climbing;
    private User writer;

    @BeforeEach
    void setUp() {
        service = new PostService(postRepository, postImageRepository, likeRepository, hobbyRepository, userRepository,
                new UploadProperties("./uploads", "https://k15d104.p.ssafy.io"));
        climbing = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(climbing, "id", 1L);
        writer = User.builder().email("a@b.c").nickname("벽타는펭귄").build();
        ReflectionTestUtils.setField(writer, "id", 15L);
    }

    private Post post(long id, User author) {
        Post post = Post.builder().hobby(climbing).user(author).type(PostType.REVIEW)
                .title("입문 한 달 후기").content("처음에는 악력도 약하고 힘들었어요").build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    // ───────── 등록 ─────────

    @Test
    @DisplayName("등록: 제목·본문을 정리해 저장하고, 이미지는 순서대로 seq 0부터")
    void create() {
        given(hobbyRepository.findById(1L)).willReturn(Optional.of(climbing));
        given(userRepository.findById(15L)).willReturn(Optional.of(writer));

        PostDetailResponse response = service.createPost(15L, new PostCreateRequest(1L, PostType.QUESTION,
                "  암벽화 추천해 주세요  ", "발볼이 넓은 편이라\r\n대여화가 아파요", List.of(IMG + "a.jpg", IMG + "b.png")));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());
        Post post = saved.getValue();
        assertThat(post.getTitle()).isEqualTo("암벽화 추천해 주세요");
        assertThat(post.getContent()).isEqualTo("발볼이 넓은 편이라\n대여화가 아파요");
        assertThat(post.getStatus()).isEqualTo(PostStatus.ACTIVE);
        assertThat(post.getImages()).extracting(i -> i.getUrl(), i -> i.getSeq())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(IMG + "a.jpg", (short) 0),
                        org.assertj.core.groups.Tuple.tuple(IMG + "b.png", (short) 1));
        assertThat(response.isMine()).isTrue();
        assertThat(response.imageUrls()).hasSize(2);
        assertThat(response.hobby().name()).isEqualTo("클라이밍");
    }

    @Test
    @DisplayName("등록: 비공개 취미, 다른 사이트 이미지 URL, 공백만 있는 제목은 거부")
    void createRejects() {
        Hobby draft = Hobby.builder().name("초안").status(HobbyStatus.DRAFT).build();
        given(hobbyRepository.findById(2L)).willReturn(Optional.of(draft));
        assertThatThrownBy(() -> service.createPost(15L,
                new PostCreateRequest(2L, PostType.INFO, "제목입니다", "본문은 열 글자 이상입니다", null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND);

        given(hobbyRepository.findById(1L)).willReturn(Optional.of(climbing));
        given(userRepository.findById(15L)).willReturn(Optional.of(writer));
        assertThatThrownBy(() -> service.createPost(15L, new PostCreateRequest(1L, PostType.INFO,
                "제목입니다", "본문은 열 글자 이상입니다", List.of("https://evil.example.com/x.jpg"))))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("업로드한 이미지만");

        assertThatThrownBy(() -> service.createPost(15L, new PostCreateRequest(1L, PostType.INFO,
                "   a  ", "본문은 열 글자 이상입니다", null)))
                .hasMessageContaining("제목은 2~50자");
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록: 제목·본문은 HTML 이스케이프해 저장(XSS 방지)")
    void createEscapesHtml() {
        given(hobbyRepository.findById(1L)).willReturn(Optional.of(climbing));
        given(userRepository.findById(15L)).willReturn(Optional.of(writer));

        service.createPost(15L, new PostCreateRequest(1L, PostType.QUESTION,
                "Q&A <b>질문</b>", "<script>alert('x')</script> 본문입니다", null));

        ArgumentCaptor<Post> saved = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Q&amp;A &lt;b&gt;질문&lt;/b&gt;");
        assertThat(saved.getValue().getContent())
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; 본문입니다")
                .doesNotContain("<script>");
    }

    @Test
    @DisplayName("제목: 원문 기준 2~50자, 이스케이프로 50자를 넘으면 안내 후 거부")
    void titleLengthAfterEscape() {
        String fifty = "가".repeat(50);
        assertThat(PostService.normalizeTitle(fifty)).isEqualTo(fifty);
        String withAmp = "&" + "가".repeat(49);   // 원문 50자 → 이스케이프 후 54자
        assertThatThrownBy(() -> PostService.normalizeTitle(withAmp))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("특수문자");
        // 본문 길이는 원문 기준: 10자 미만이면 거부(이스케이프로 늘어나도 통과시키지 않음)
        assertThatThrownBy(() -> PostService.normalizeContent("<<<<<"))
                .hasMessageContaining("10~5,000자");
    }

    @Test
    @DisplayName("이미지 URL: 5장 초과·경로 조작은 거부, 비어 있으면 빈 목록")
    void validateImageUrls() {
        assertThat(service.validateImageUrls(null)).isEmpty();
        assertThatThrownBy(() -> service.validateImageUrls(List.of(IMG + "1.jpg", IMG + "2.jpg", IMG + "3.jpg",
                IMG + "4.jpg", IMG + "5.jpg", IMG + "6.jpg"))).hasMessageContaining("최대 5장");
        assertThatThrownBy(() -> service.validateImageUrls(List.of(IMG + "../../etc/passwd")))
                .isInstanceOf(CustomException.class);
        // 프로필 폴더 이미지는 게시글에 첨부할 수 없음
        assertThatThrownBy(() -> service.validateImageUrls(List.of(PROFILE_IMG + "me.jpg")))
                .hasMessageContaining("업로드한 이미지만");
    }

    // ───────── 수정·삭제 ─────────

    @Test
    @DisplayName("수정: 작성자가 아니면 BOARD_FORBIDDEN")
    void updateForbidden() {
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post(120L, writer)));

        assertThatThrownBy(() -> service.updatePost(120L, 99L, new PostUpdateRequest(null, "새 제목", null, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.BOARD_FORBIDDEN);
    }

    @Test
    @DisplayName("수정: 보낸 항목만 바뀌고, imageUrls를 보내면 전체 교체")
    void updatePartial() {
        Post post = post(120L, writer);
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndTargetTypeAndTargetId(15L, LikeTargetType.POST, 120L)).willReturn(true);

        PostDetailResponse response = service.updatePost(120L, 15L,
                new PostUpdateRequest(PostType.INFO, null, null, List.of(IMG + "new.webp")));

        assertThat(post.getType()).isEqualTo(PostType.INFO);
        assertThat(post.getTitle()).isEqualTo("입문 한 달 후기");
        assertThat(response.imageUrls()).containsExactly(IMG + "new.webp");
        assertThat(response.isLiked()).isTrue();
    }

    @Test
    @DisplayName("삭제: 작성자는 soft delete(DELETED), 이미 삭제된 글은 404")
    void delete() {
        Post post = post(120L, writer);
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post));

        service.deletePost(120L, 15L);
        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);

        assertThatThrownBy(() -> service.deletePost(120L, 15L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    // ───────── 상세 ─────────

    @Test
    @DisplayName("상세: 조회수를 올리고(올라간 값으로 응답), 로그인 사용자의 좋아요·본인 여부를 채움")
    void getPost() {
        Post post = post(120L, writer);
        ReflectionTestUtils.setField(post, "viewCount", 241);
        given(postRepository.increaseViewCount(120L)).willReturn(1);
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post));
        given(likeRepository.existsByUserIdAndTargetTypeAndTargetId(15L, LikeTargetType.POST, 120L)).willReturn(true);

        PostDetailResponse mine = service.getPost(120L, 15L);
        assertThat(mine.viewCount()).isEqualTo(241);
        assertThat(mine.isMine()).isTrue();
        assertThat(mine.isLiked()).isTrue();

        PostDetailResponse guest = service.getPost(120L, null);
        assertThat(guest.isMine()).isFalse();
        assertThat(guest.isLiked()).isFalse();
    }

    @Test
    @DisplayName("상세: 없거나 숨김·삭제된 글은 조회수가 안 올라가고 404")
    void getPostNotFound() {
        given(postRepository.increaseViewCount(999L)).willReturn(0);

        assertThatThrownBy(() -> service.getPost(999L, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        verify(postRepository, never()).findWithUserAndHobbyById(anyLong());
    }

    // ───────── 좋아요 ─────────

    @Test
    @DisplayName("좋아요: 새로 눌렀을 때만 개수 +1, 이미 눌렀으면 그대로")
    void like() {
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post(120L, writer)));
        given(likeRepository.insertIfAbsent(eq(7L), eq("POST"), eq(120L), any())).willReturn(1, 0);
        given(postRepository.findLikeCount(120L)).willReturn(19, 19);

        PostLikeResponse first = service.likePost(120L, 7L);
        PostLikeResponse again = service.likePost(120L, 7L);

        verify(postRepository, org.mockito.Mockito.times(1)).addLikeCount(120L, 1);
        assertThat(first).isEqualTo(new PostLikeResponse(19, true));
        assertThat(again).isEqualTo(new PostLikeResponse(19, true));
    }

    @Test
    @DisplayName("좋아요 취소: 지웠을 때만 개수 -1")
    void unlike() {
        given(postRepository.findWithUserAndHobbyById(120L)).willReturn(Optional.of(post(120L, writer)));
        given(likeRepository.deleteLike(7L, LikeTargetType.POST, 120L)).willReturn(0);
        given(postRepository.findLikeCount(120L)).willReturn(18);

        PostLikeResponse response = service.unlikePost(120L, 7L);

        verify(postRepository, never()).addLikeCount(anyLong(), anyInt());
        assertThat(response).isEqualTo(new PostLikeResponse(18, false));
    }

    // ───────── 목록 ─────────

    @Test
    @DisplayName("목록: 페이지 크기는 최대 50, 썸네일은 글별 첫 이미지, 본문은 100자 요약")
    @SuppressWarnings("unchecked")
    void getPosts() {
        given(hobbyRepository.findById(1L)).willReturn(Optional.of(climbing));
        Post a = post(1L, writer);
        Post b = post(2L, writer);
        given(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(a, b), inv.getArgument(1), 2));
        given(postImageRepository.findThumbnails(List.of(1L, 2L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, IMG + "thumb.jpg"}));

        PageResponse<PostSummaryResponse> page = service.getPosts(1L, PostType.REVIEW, "후기", PostSort.LIKES, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(page.content()).extracting(PostSummaryResponse::thumbnailUrl).containsExactly(IMG + "thumb.jpg", null);
        assertThat(page.content().get(0).author().nickname()).isEqualTo("벽타는펭귄");
        assertThat(page.page().totalElements()).isEqualTo(2);
    }

        @Test
        @DisplayName("전체 게시판 목록: 취미 제한 없이 최신순 페이징하고 썸네일을 포함")
        @SuppressWarnings("unchecked")
        void getAllPosts() {
                Post post = post(1L, writer);
                given(postRepository.findAll(any(Specification.class), any(Pageable.class)))
                                .willAnswer(inv -> new PageImpl<>(List.of(post), inv.getArgument(1), 1));
                given(postImageRepository.findThumbnails(List.of(1L)))
                                .willReturn(List.<Object[]>of(new Object[]{1L, IMG + "thumb.jpg"}));

                PageResponse<PostSummaryResponse> page = service.getAllPosts(null, null, PostSort.LATEST, 0, 20);

                ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
                verify(postRepository).findAll(any(Specification.class), pageable.capture());
                verify(hobbyRepository, never()).findById(anyLong());
                assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
                assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
                assertThat(page.content()).singleElement().satisfies(item -> {
                        assertThat(item.hobbyId()).isEqualTo(1L);
                        assertThat(item.hobbyName()).isEqualTo("클라이밍");
                        assertThat(item.thumbnailUrl()).isEqualTo(IMG + "thumb.jpg");
                        assertThat(item.id()).isEqualTo(1L);
                });
        }

    @Test
    @DisplayName("정렬·검색어 처리")
    void helpers() {
        List<String> likes = new ArrayList<>();
        PostService.toSort(PostSort.LIKES).forEach(o -> likes.add(o.getProperty()));
        assertThat(likes).containsExactly("likeCount", "createdAt", "id");
        List<String> latest = new ArrayList<>();
        PostService.toSort(PostSort.LATEST).forEach(o -> latest.add(o.getProperty()));
        assertThat(latest).containsExactly("createdAt", "id");

        assertThat(PostService.escapeLike("100%_a\\b")).isEqualTo("100\\%\\_a\\\\b");
        assertThat(PostService.clampSize(0)).isEqualTo(20);
        assertThat(PostService.clampSize(30)).isEqualTo(30);
        assertThat(PostSummaryResponse.of(post(1L, writer), null).contentSummary()).isEqualTo("처음에는 악력도 약하고 힘들었어요");
        // 요약이 100자에서 잘릴 때 &amp; 같은 엔티티 중간에서 자르지 않음
        Post longPost = post(3L, writer);
        ReflectionTestUtils.setField(longPost, "content", "가".repeat(97) + "&amp;뒤");
        assertThat(PostSummaryResponse.of(longPost, null).contentSummary()).isEqualTo("가".repeat(97) + "…");
    }

    @Test
    @DisplayName("내가 쓴 글 목록: ACTIVE 상태 글만 최신순으로 페이징 조회")
    void getMyPosts() {
        Post p1 = post(1L, writer);
        Post p2 = post(2L, writer);
        org.springframework.data.domain.Page<Post> postPage = new org.springframework.data.domain.PageImpl<>(
                List.of(p2, p1), PageRequest.of(0, 20), 2);
        given(postRepository.findByUserIdAndStatus(eq(15L), eq(PostStatus.ACTIVE), any(Pageable.class)))
                .willReturn(postPage);

        PageResponse<PostSummaryResponse> result = service.getMyPosts(15L, 0, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByUserIdAndStatus(eq(15L), eq(PostStatus.ACTIVE), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");

        assertThat(result.content()).hasSize(2);
        assertThat(result.page().totalElements()).isEqualTo(2);
    }
}
