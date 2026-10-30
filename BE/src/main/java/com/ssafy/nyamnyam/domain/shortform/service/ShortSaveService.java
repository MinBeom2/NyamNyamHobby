package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortSaveResponse;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortSaveService {
    private final ShortRepository shorts;
    private final ShortSaveRepository saves;

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
