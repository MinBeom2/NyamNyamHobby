package com.ssafy.nyamnyam.domain.user.service;

import com.ssafy.nyamnyam.domain.auth.repository.AuthTokenRepository;
import com.ssafy.nyamnyam.domain.plan.service.PlanQueryService;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.user.dto.request.OnboardingRequest;
import com.ssafy.nyamnyam.domain.user.dto.response.OnboardingResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.OnboardingOptionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.RegionResponse;
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
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final PlanQueryService planQueryService;

    @Transactional(readOnly = true)
    public OnboardingOptionsResponse getOnboardingOptions() {
        var hobbies = hobbyRepository.findByStatus(HobbyStatus.PUBLISHED).stream()
                .sorted(Comparator.comparing((Hobby h) -> h.getCategory().getId()).thenComparing(Hobby::getId))
                .map(h -> new OnboardingOptionsResponse.HobbyOption(
                        h.getId(), h.getName(), h.getCategory().getId(), h.getCategory().getName()))
                .toList();
        var regions = regionRepository.findAll().stream()
                .sorted(Comparator.comparing(Region::getSido).thenComparing(Region::getSigungu))
                .map(RegionResponse::from).toList();
        return new OnboardingOptionsResponse(hobbies, regions);
    }

    /** PUT 재시도 시 중복 없이 관심 취미 목록을 교체한다. 진행 중인 취미의 상태는 유지한다. */
    @Transactional
    public OnboardingResponse saveOnboarding(Long userId, OnboardingRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        var selected = request.getInitialHobbies() == null ? List.<OnboardingRequest.InitialHobby>of()
                : request.getInitialHobbies();
        Set<Long> hobbyIds = selected.stream().map(OnboardingRequest.InitialHobby::hobbyId)
                .collect(Collectors.toSet());
        if (hobbyIds.size() != selected.size()) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "관심 취미 ID는 중복될 수 없습니다.");
        }
        // 모든 선택값을 검증한 다음 변경하며, 이용 상황과 관심 취미는 같은 트랜잭션으로 저장한다.
        List<Hobby> hobbies = hobbyRepository.findAllById(hobbyIds);
        if (hobbies.size() != hobbyIds.size()
                || hobbies.stream().anyMatch(h -> h.getStatus() != HobbyStatus.PUBLISHED)) {
            throw new CustomException(ErrorCode.HOBBY_NOT_FOUND);
        }
        updateConditions(userId, request);

        List<UserHobby> existing = userHobbyRepository.findAllByUserIdWithHobby(userId);
        Set<Long> existingIds = new HashSet<>();
        int savedCount = 0;
        for (UserHobby entry : existing) {
            Long hobbyId = entry.getHobby().getId();
            existingIds.add(hobbyId);
            if (entry.getStatus() == UserHobbyStatus.SAVED) {
                if (hobbyIds.contains(hobbyId)) {
                    savedCount++;
                } else {
                    userHobbyRepository.delete(entry);
                    hobbyRepository.adjustSaveCount(hobbyId, -1);
                }
            }
        }
        for (Hobby hobby : hobbies.stream().sorted(Comparator.comparing(Hobby::getId)).toList()) {
            if (!existingIds.contains(hobby.getId())) {
                userHobbyRepository.save(UserHobby.builder().user(user).hobby(hobby).build());
                hobbyRepository.adjustSaveCount(hobby.getId(), 1);
                savedCount++;
            }
        }
        return new OnboardingResponse(true, savedCount);
    }

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
            condition = userConditionRepository.save(condition);
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
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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

        return planQueryService.enrich(userId, list);
    }

    /**
     * 관심 취미 저장
     */
    @Transactional
    public void saveHobby(Long userId, Long hobbyId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Hobby hobby = hobbyRepository.findById(hobbyId).filter(h -> h.getStatus() == HobbyStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));

        var existing = userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId);
        if (existing.isPresent()) {
            requireSaved(existing.get());
        } else {
            UserHobby userHobby = UserHobby.builder()
                    .user(user)
                    .hobby(hobby)
                    .status(UserHobbyStatus.SAVED)
                    .savedAt(LocalDateTime.now())
                    .build();
            userHobbyRepository.save(userHobby);
            hobbyRepository.adjustSaveCount(hobbyId, 1);
        }
    }

    /**
     * 관심 취미 해제
     */
    @Transactional
    public void unsaveHobby(Long userId, Long hobbyId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId)
                .ifPresent(entry -> {
                    requireSaved(entry);
                    userHobbyRepository.delete(entry);
                    hobbyRepository.adjustSaveCount(hobbyId, -1);
                });
    }

    private void requireSaved(UserHobby entry) {
        if (entry.getStatus() != UserHobbyStatus.SAVED)
            throw new CustomException(entry.getStatus() == UserHobbyStatus.IN_PROGRESS
                    || entry.getStatus() == UserHobbyStatus.PAUSED ? ErrorCode.MY_HOBBY_IN_PROGRESS
                    : ErrorCode.MY_HOBBY_INVALID_TRANSITION);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}

