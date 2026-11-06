package com.ssafy.nyamnyam.domain.post.controller;

import com.ssafy.nyamnyam.domain.post.dto.request.PostCreateRequest;
import com.ssafy.nyamnyam.domain.post.dto.request.PostUpdateRequest;
import com.ssafy.nyamnyam.domain.post.dto.response.PostDetailResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.PostLikeResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.PostSummaryResponse;
import com.ssafy.nyamnyam.domain.post.entity.PostType;
import com.ssafy.nyamnyam.domain.post.service.PostService;
import com.ssafy.nyamnyam.domain.post.service.PostService.PostSort;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 커뮤니티 게시판 - 글 (API 명세 3.7 1)~4)·7) 중 글 부분).
 * 목록·상세는 공개(SecurityConfig에서 GET 허용), 나머지는 로그인 필요.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 전체 취미 게시글 목록 */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getAllPosts(
            @RequestParam(value = "type", required = false) PostType type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "LATEST") PostSort sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getAllPosts(type, keyword, sort, page, size)));
    }

    /** 취미별 게시글 목록 */
    @GetMapping("/hobbies/{hobbyId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getPosts(
            @PathVariable("hobbyId") Long hobbyId,
            @RequestParam(value = "type", required = false) PostType type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "LATEST") PostSort sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPosts(hobbyId, type, keyword, sort, page, size)));
    }

    /** 게시글 등록 (201) */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PostDetailResponse response = postService.createPost(requireLogin(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 게시글 상세 (공개, 볼 때마다 조회수 +1) */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long viewerId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(postId, viewerId)));
    }

    /** 게시글 수정 (작성자만) */
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.updatePost(postId, requireLogin(principal), request)));
    }

    /** 게시글 삭제 (작성자만, 204) */
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        postService.deletePost(postId, requireLogin(principal));
        return ResponseEntity.noContent().build();
    }

    /** 게시글 좋아요 */
    @PutMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> likePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.likePost(postId, requireLogin(principal))));
    }

    /** 게시글 좋아요 취소 */
    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeResponse>> unlikePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.unlikePost(postId, requireLogin(principal))));
    }

    private Long requireLogin(UserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal.getUserId();
    }
}
