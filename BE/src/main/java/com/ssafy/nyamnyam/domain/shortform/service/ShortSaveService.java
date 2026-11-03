package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortSaveResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortSummaryResponse;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
@RequiredArgsConstructor
public class ShortSaveService {
    private final ShortRepository shorts;
    private final ShortSaveRepository saves;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<SavedShortResponse> list(Long userId, Long hobbyId, int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || (hobbyId != null && hobbyId < 1)
                || !("recent".equals(sort) || "oldest".equals(sort))) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }
        long total = saves.count(userId, hobbyId);
        int totalPages = (int) Math.min(Integer.MAX_VALUE, (total + size - 1) / size);
        var content = saves.findSaved(userId, hobbyId, page, size, "oldest".equals(sort));
        return new PageResponse<>(content, new PageResponse.PageInfo(page, size, total, totalPages,
                (long) page + 1 < totalPages));
    }

    @Transactional(readOnly = true)
    public SavedShortSummaryResponse summary(Long userId) {
        var hobbies = saves.countByHobby(userId);
        return new SavedShortSummaryResponse(hobbies.stream().mapToLong(item -> item.count()).sum(), hobbies);
    }

    @Transactional
    public ShortSaveResponse save(Long userId, Long shortId) {
        Short shortform = findShort(shortId);
        if (shortform.getStatus() != ShortStatus.ACTIVE) {
            throw new CustomException(ErrorCode.SHORT_NOT_ACTIVE);
        }
        saves.save(userId, shortId);
        return new ShortSaveResponse(shortId, true);
    }

    @Transactional
    public ShortSaveResponse unsave(Long userId, Long shortId) {
        // 숨김/이용 불가 상태로 바뀌어도 본인의 보관 해제는 가능하다.
        findShort(shortId);
        saves.delete(userId, shortId);
        return new ShortSaveResponse(shortId, false);
    }

    private Short findShort(Long shortId) {
        // 같은 영상의 상태 변경 및 중복 저장 요청과 직렬화한다.
        return shorts.findByIdForUpdate(shortId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND, "숏폼을 찾을 수 없습니다."));
    }
}
