package com.ssafy.nyamnyam.domain.post.dto.request;

import com.ssafy.nyamnyam.domain.post.entity.PostType;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 게시글 수정. 보낸 항목만 바꾼다(null이면 그대로).
 * imageUrls는 보내면 전체 교체이고, 빈 배열이면 이미지를 모두 뺀다.
 */
public record PostUpdateRequest(
        PostType type,

        @Size(min = 2, max = 50, message = "제목은 2~50자로 입력해 주세요.")
        String title,

        @Size(min = 10, max = 5000, message = "내용은 10~5,000자로 입력해 주세요.")
        String content,

        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있어요.")
        List<String> imageUrls
) {
}
