package com.ssafy.nyamnyam.domain.user.service;

import com.ssafy.nyamnyam.domain.auth.repository.AuthTokenRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.user.dto.request.ChangePasswordRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateConditionsRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateProfileRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.WithdrawRequest;
import com.ssafy.nyamnyam.domain.user.dto.response.UserConditionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserProfileResponse;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserConditionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserConditionRepository userConditionRepository;
    private final RegionRepository regionRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyRepository hobbyRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 내 프로필 및 환경 정보 조회
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = getUser(userId);
        UserCondition condition = userConditionRepository.findByUserIdWithRegion(userId).orElse(null);
        return UserProfileResponse.of(user, condition);
    }

    /**
     * 프로필 기본 정보 수정 (닉네임, 프로필 이미지 URL)
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            String newNickname = request.getNickname().trim();
            if (!newNickname.equals(user.getNickname())) {
                if (userRepository.existsByNickname(newNickname)) {
                    throw new CustomException(ErrorCode.AUTH_NICKNAME_DUPLICATED);
                }
                user.changeNickname(newNickname);
            }
        }

        if (request.getProfileImageUrl() != null) {
            user.changeProfileImageUrl(request.getProfileImageUrl());
        }

        UserCondition condition = userConditionRepository.findByUserIdWithRegion(userId).orElse(null);
        return UserProfileResponse.of(user, condition);
    }

    /**
     * 이용 상황 및 선호 지역 수정
     */
    @Transactional
    public UserConditionsResponse updateConditions(Long userId, UpdateConditionsRequest request) {
        User user = getUser(userId);

        Region region = null;
        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        }

        UserCondition condition = userConditionRepository.findByUserIdWithRegion(userId).orElse(null);
        if (condition != null) {
            condition.update(
                    request.getWeeklyHours(),
                    request.getMonthlyBudget(),
                    request.getPlacePref(),
                    request.getSocialPref(),
                    region
            );
        } else {
            condition = UserCondition.builder()
                    .user(user)
                    .weeklyHours(request.getWeeklyHours())
                    .monthlyBudget(request.getMonthlyBudget())
                    .placePref(request.getPlacePref())
                    .socialPref(request.getSocialPref())
                    .region(region)
                    .build();
            userConditionRepository.save(condition);
        }

        return UserConditionsResponse.from(condition);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);

        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new CustomException(ErrorCode.AUTH_PASSWORD_MISMATCH);
            }
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = getUser(userId);

        if (user.getPasswordHash() != null) {
            if (request == null || request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS, "비밀번호가 일치하지 않습니다.");
            }
        }

        user.withdraw(LocalDateTime.now());
        authTokenRepository.deleteAllByUserId(userId);
    }

    /**
     * 내 취미 목록 조회 (status 필터 가능)
     */
    @Transactional(readOnly = true)
    public List<UserHobbyResponse> getUserHobbies(Long userId, String status) {
        List<UserHobby> list;
        if (status != null && !status.isBlank()) {
            try {
                UserHobbyStatus hobbyStatus = UserHobbyStatus.valueOf(status.trim().toUpperCase());
                list = userHobbyRepository.findAllByUserIdAndStatusWithHobby(userId, hobbyStatus);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "유효하지 않은 취미 상태입니다: " + status);
            }
        } else {
            list = userHobbyRepository.findAllByUserIdWithHobby(userId);
        }

        return list.stream().map(UserHobbyResponse::from).toList();
    }

    /**
     * 관심 취미 저장
     */
    @Transactional
    public void saveHobby(Long userId, Long hobbyId) {
        User user = getUser(userId);
        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));

        var existing = userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId);
        if (existing.isPresent()) {
            existing.get().updateStatus(UserHobbyStatus.SAVED);
        } else {
            UserHobby userHobby = UserHobby.builder()
                    .user(user)
                    .hobby(hobby)
                    .status(UserHobbyStatus.SAVED)
                    .savedAt(LocalDateTime.now())
                    .build();
            userHobbyRepository.save(userHobby);
        }
    }

    /**
     * 관심 취미 해제
     */
    @Transactional
    public void unsaveHobby(Long userId, Long hobbyId) {
        userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId)
                .ifPresent(userHobbyRepository::delete);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}

