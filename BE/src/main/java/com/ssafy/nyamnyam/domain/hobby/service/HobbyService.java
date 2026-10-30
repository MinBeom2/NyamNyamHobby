package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 취미 검색·상세(FR-HOBBY-01, 02). 공개(PUBLISHED) 취미만 다룬다.
 * 별로예요·관련 숏폼은 {@link HobbyVideoService}에 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;

    /**
     * 취미 상세 조회. 비회원은 userId를 null로 넘기면 myStatus가 "NONE"이 된다.
     * 없거나 공개되지 않은 취미는 404(COMMON_NOT_FOUND)로, 관련 숏폼 조회와 같은 기준이다.
     */
    public HobbyDetailResponse getDetail(Long hobbyId, Long userId) {
        if (hobbyId == null || hobbyId <= 0) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }
        Hobby hobby = hobbyRepository.findWithCategoryAndTagsById(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        if (hobby.getStatus() != HobbyStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND);
        }
        String myStatus = userId == null ? null
                : userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId)
                        .map(UserHobby::getStatus)
                        .map(Enum::name)
                        .orElse(null);
        return HobbyDetailResponse.of(hobby, myStatus);
    }
}
