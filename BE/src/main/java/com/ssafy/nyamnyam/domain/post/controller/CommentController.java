package com.ssafy.nyamnyam.domain.post.controller;

import com.ssafy.nyamnyam.domain.post.dto.request.CommentCreateRequest;
import com.ssafy.nyamnyam.domain.post.dto.request.CommentUpdateRequest;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentLikeResponse;
import com.ssafy.nyamnyam.domain.post.dto.response.CommentResponse;
import com.ssafy.nyamnyam.domain.post.service.CommentService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
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
 * 커뮤니티 게시판 - 댓글 (API 명세 3.7 5)~7) 중 댓글 부분).
 * 목록은 공개(SecurityConfig에서 GET 허용), 나머지는 로그인 필요.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 댓글 목록: 부모 댓글 50개씩 + 각 답글 전부 */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long viewerId = principal != null ? principal.getUserId() : null;
        return ResponseEntity.ok(ApiResponse.success(commentService.getComments(postId, viewerId, page)));
    }

    /** 댓글·답글 작성 (201) */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CommentResponse response = commentService.createComment(postId, requireLogin(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 댓글 수정 (작성자만) */
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(commentId, requireLogin(principal), request)));
    }

    /** 댓글 삭제 (작성자만, 204) */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(commentId, requireLogin(principal));
        return ResponseEntity.noContent().build();
    }

    /** 댓글 좋아요 */
    @PutMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponse>> likeComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.likeComment(commentId, requireLogin(principal))));
    }

    /** 댓글 좋아요 취소 */
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentLikeResponse>> unlikeComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.unlikeComment(commentId, requireLogin(principal))));
    }

    private Long requireLogin(UserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return principal.getUserId();
    }
}
