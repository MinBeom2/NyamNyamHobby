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
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.upload.UploadProperties;
import com.ssafy.nyamnyam.global.upload.UploadPurpose;
import com.ssafy.nyamnyam.global.util.HtmlEscaper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 취미별 게시판의 글(FR-BOARD-01~04 중 글 부분). 댓글은 별도 작업.
 * 명세(FR-BOARD-02)대로 제목·본문은 HTML 이스케이프해 저장한다(XSS 방지).
 * 길이 제한(제목 2~50자, 본문 10~5,000자)은 사용자가 입력한 원문 기준으로 검사하고,
 * 이스케이프로 늘어난 제목이 DB 컬럼(50자)을 넘으면 400으로 안내한다.
 * 검색어도 같은 방식으로 이스케이프해 저장된 값과 비교한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_IMAGES = 5;

    public enum PostSort { LATEST, LIKES, COMMENTS }

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final ContentLikeRepository likeRepository;
    private final HobbyRepository hobbyRepository;
    private final UserRepository userRepository;
    private final UploadProperties uploadProperties;

    /** 취미별 게시판 목록. 숨김·삭제 글은 빠진다 */
    public PageResponse<PostSummaryResponse> getPosts(Long hobbyId, PostType type, String keyword,
                                                      PostSort sort, int page, int size) {
        findPublishedHobby(hobbyId);

        PageRequest pageable = PageRequest.of(Math.max(page, 0), clampSize(size), toSort(sort));
        Page<Post> posts = postRepository.findAll(boardSpec(hobbyId, type, keyword), pageable);

        Map<Long, String> thumbnails = new HashMap<>();
        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        if (!postIds.isEmpty()) {
            for (Object[] row : postImageRepository.findThumbnails(postIds)) {
                thumbnails.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        return PageResponse.of(posts, post -> PostSummaryResponse.of(post, thumbnails.get(post.getId())));
    }

    /** 글 상세. 볼 때마다 조회수 +1. viewerId는 비로그인이면 null */
    @Transactional
    public PostDetailResponse getPost(Long postId, Long viewerId) {
        // 조회수를 DB에서 먼저 올리고(동시 요청에도 정확), 올라간 값으로 다시 읽는다
        if (postRepository.increaseViewCount(postId) == 0) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없어요.");
        }
        Post post = findActivePost(postId);

        boolean isLiked = viewerId != null
                && likeRepository.existsByUserIdAndTargetTypeAndTargetId(viewerId, LikeTargetType.POST, postId);
        return PostDetailResponse.of(post, post.getViewCount(), isLiked, post.isWrittenBy(viewerId));
    }

    @Transactional
    public PostDetailResponse createPost(Long userId, PostCreateRequest request) {
        Hobby hobby = findPublishedHobby(request.hobbyId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .hobby(hobby)
                .user(user)
                .type(request.type())
                .title(normalizeTitle(request.title()))
                .content(normalizeContent(request.content()))
                .build();
        post.replaceImages(validateImageUrls(request.imageUrls()));
        postRepository.save(post);

        return PostDetailResponse.of(post, post.getViewCount(), false, true);
    }

    @Transactional
    public PostDetailResponse updatePost(Long postId, Long userId, PostUpdateRequest request) {
        Post post = findActivePost(postId);
        checkOwner(post, userId);

        post.update(
                request.type(),
                request.title() != null ? normalizeTitle(request.title()) : null,
                request.content() != null ? normalizeContent(request.content()) : null
        );
        if (request.imageUrls() != null) {
            post.replaceImages(validateImageUrls(request.imageUrls()));
        }

        boolean isLiked = likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, LikeTargetType.POST, postId);
        return PostDetailResponse.of(post, post.getViewCount(), isLiked, true);
    }

    /** soft delete: 목록에서 빠지고 상세는 404 */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findActivePost(postId);
        checkOwner(post, userId);
        post.delete();
    }

    /** 좋아요. 이미 눌렀으면 그대로 둔다 */
    @Transactional
    public PostLikeResponse likePost(Long postId, Long userId) {
        findActivePost(postId);
        int inserted = likeRepository.insertIfAbsent(userId, LikeTargetType.POST.name(), postId, LocalDateTime.now());
        if (inserted > 0) {
            postRepository.addLikeCount(postId, 1);
        }
        return new PostLikeResponse(postRepository.findLikeCount(postId), true);
    }

    /** 좋아요 취소. 누르지 않았으면 그대로 둔다 */
    @Transactional
    public PostLikeResponse unlikePost(Long postId, Long userId) {
        findActivePost(postId);
        int deleted = likeRepository.deleteLike(userId, LikeTargetType.POST, postId);
        if (deleted > 0) {
            postRepository.addLikeCount(postId, -1);
        }
        return new PostLikeResponse(postRepository.findLikeCount(postId), false);
    }

    // ───────── 내부 ─────────

    private Hobby findPublishedHobby(Long hobbyId) {
        return hobbyRepository.findById(hobbyId)
                .filter(h -> h.getStatus() == HobbyStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "취미를 찾을 수 없어요."));
    }

    private Post findActivePost(Long postId) {
        return postRepository.findWithUserAndHobbyById(postId)
                .filter(Post::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "게시글을 찾을 수 없어요."));
    }

    private void checkOwner(Post post, Long userId) {
        if (!post.isWrittenBy(userId)) {
            throw new CustomException(ErrorCode.BOARD_FORBIDDEN);
        }
    }

    /** 제목: 앞뒤 공백을 지운 원문이 2~50자인지 본 뒤 이스케이프. 이스케이프 결과가 컬럼(50자)을 넘으면 거부 */
    static String normalizeTitle(String title) {
        String trimmed = title == null ? "" : title.strip();
        if (trimmed.length() < 2 || trimmed.length() > 50) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "제목은 2~50자로 입력해 주세요.");
        }
        String escaped = HtmlEscaper.escape(trimmed);
        if (escaped.length() > 50) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT,
                    "제목에 특수문자(<, >, &, \", ')가 많아 저장할 수 없어요. 제목을 조금 줄여 주세요.");
        }
        return escaped;
    }

    /** 본문: 줄바꿈을 \n으로 통일하고 앞뒤 공백을 지운 원문이 10~5,000자인지 본 뒤 이스케이프 */
    static String normalizeContent(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.length() < 10 || normalized.length() > 5000) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "내용은 10~5,000자로 입력해 주세요.");
        }
        return HtmlEscaper.escape(normalized);
    }

    /** 이미지는 최대 5장, 우리 업로드 API에서 게시글 용도(post 폴더)로 올린 URL만 허용. 프로필 이미지는 첨부 불가 */
    List<String> validateImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        if (imageUrls.size() > MAX_IMAGES) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "이미지는 최대 5장까지 첨부할 수 있어요.");
        }
        String prefix = uploadProperties.imageUrlPrefix(UploadPurpose.POST);
        for (String url : imageUrls) {
            if (url == null || !url.startsWith(prefix) || url.contains("..") || url.length() > 500) {
                throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "업로드한 이미지만 첨부할 수 있어요.");
            }
        }
        return List.copyOf(imageUrls);
    }

    static int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    static Sort toSort(PostSort sort) {
        Sort latest = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        if (sort == PostSort.LIKES) {
            return Sort.by(Sort.Order.desc("likeCount")).and(latest);
        }
        if (sort == PostSort.COMMENTS) {
            return Sort.by(Sort.Order.desc("commentCount")).and(latest);
        }
        return latest;
    }

    /** 게시판 조건: 해당 취미 · ACTIVE · (유형) · (제목 또는 내용에 검색어, 대소문자 무시) */
    static Specification<Post> boardSpec(Long hobbyId, PostType type, String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.strip();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("hobby").get("id"), hobbyId));
            predicates.add(cb.equal(root.get("status"), PostStatus.ACTIVE));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (trimmedKeyword != null && !trimmedKeyword.isEmpty()) {
                // 저장된 제목·본문이 HTML 이스케이프돼 있으므로 검색어도 같게 바꿔 비교한다(예: Q&A → Q&amp;A)
                String pattern = "%" + escapeLike(HtmlEscaper.escape(trimmedKeyword).toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("content")), pattern, '\\')
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** LIKE 특수문자(%, _, \)를 글자 그대로 찾도록 이스케이프 */
    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
