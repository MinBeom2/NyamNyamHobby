package com.ssafy.nyamnyam.domain.post.dto.request;

import com.ssafy.nyamnyam.domain.post.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 게시글 등록. 제목 2~50자, 본문 10~5,000자, 이미지는 업로드 API로 받은 URL 최대 5개.
 */
public record PostCreateRequest(
        @NotNull(message = "취미를 선택해 주세요.")
        Long hobbyId,

        @NotNull(message = "글 유형을 선택해 주세요.")
        PostType type,

        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(min = 2, max = 50, message = "제목은 2~50자로 입력해 주세요.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(min = 10, max = 5000, message = "내용은 10~5,000자로 입력해 주세요.")
        String content,

        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있어요.")
        List<String> imageUrls
) {
}
