package com.ssafy.nyamnyam.domain.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글·답글 작성. parentId가 없으면 댓글, 있으면 그 댓글의 답글.
 * 답글에 다시 답글을 달면(parentId가 답글) 원래 부모 댓글 아래로 붙고 내용 앞에 "@닉네임 "이 붙는다.
 */
public record CommentCreateRequest(
        @NotBlank(message = "댓글을 입력해 주세요.")
        @Size(max = 500, message = "댓글은 500자 이하로 입력해 주세요.")
        String content,

        Long parentId
) {
}
