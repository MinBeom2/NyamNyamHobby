package com.ssafy.nyamnyam.domain.user.service;

import com.ssafy.nyamnyam.domain.auth.repository.AuthTokenRepository;
import com.ssafy.nyamnyam.domain.plan.service.PlanQueryService;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.user.dto.request.ChangePasswordRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateConditionsRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.UpdateProfileRequest;
import com.ssafy.nyamnyam.domain.user.dto.request.WithdrawRequest;
import com.ssafy.nyamnyam.domain.user.dto.response.UserConditionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserProfileResponse;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.RegionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserConditionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConditionRepository userConditionRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserHobbyRepository userHobbyRepository;

    @Mock
    private HobbyRepository hobbyRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock private PlanQueryService planQueryService;
    @Mock private com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository shortSaveRepository;
    @Mock private com.ssafy.nyamnyam.domain.post.repository.PostRepository postRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Region testRegion;
    private UserCondition testCondition;
    private Hobby testHobby;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .passwordHash("encoded_pwd")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        testRegion = Region.builder()
                .code("11110")
                .sido("서울특별시")
                .sigungu("종로구")
                .build();

        testCondition = UserCondition.builder()
                .user(testUser)
                .weeklyHours((short) 5)
                .monthlyBudget(150000)
                .placePref("INDOOR")
                .socialPref("TOGETHER")
                .region(testRegion)
                .build();

        Category category = Category.builder().name("스포츠").build();
        testHobby = Hobby.builder()
                .category(category)
                .name("클라이밍")
                .placeType(PlaceType.INDOOR)
                .socialType(SocialType.BOTH)
                .difficulty(Difficulty.NORMAL)
                .sessionMinMinutes((short) 60)
                .sessionMaxMinutes((short) 120)
                .initialCostMin(50000)
                .initialCostMax(100000)
                .monthlyCostMin(30000)
                .monthlyCostMax(60000)
                .build();
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void getUserProfile_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userConditionRepository.findByUserIdWithRegion(1L)).willReturn(Optional.of(testCondition));

        UserProfileResponse response = userService.getUserProfile(1L);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getConditions()).isNotNull();
        assertThat(response.getConditions().getWeeklyHours()).isEqualTo((short) 5);
        assertThat(response.getConditions().getRegion().getSido()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("프로필 닉네임 수정 성공")
    void updateProfile_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);
        given(userConditionRepository.findByUserIdWithRegion(1L)).willReturn(Optional.of(testCondition));

        UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", "http://image.url");
        UserProfileResponse response = userService.updateProfile(1L, request);

        assertThat(response.getNickname()).isEqualTo("새닉네임");
        assertThat(testUser.getNickname()).isEqualTo("새닉네임");
        assertThat(testUser.getProfileImageUrl()).isEqualTo("http://image.url");
    }

    @Test
    @DisplayName("프로필 수정 시 중복된 닉네임이면 예외 발생")
    void updateProfile_duplicateNickname_throwsException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("이미있는닉")).willReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("이미있는닉", null);

        assertThatThrownBy(() -> userService.updateProfile(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.AUTH_NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("활동 조건 수정 성공")
    void updateConditions_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(regionRepository.findById(10L)).willReturn(Optional.of(testRegion));
        given(userConditionRepository.findByUserIdWithRegion(1L)).willReturn(Optional.of(testCondition));

        UpdateConditionsRequest request = new UpdateConditionsRequest((short) 10, 300000, "OUTDOOR", "ALONE", 10L);
        UserConditionsResponse response = userService.updateConditions(1L, request);

        assertThat(response.getWeeklyHours()).isEqualTo((short) 10);
        assertThat(response.getMonthlyBudget()).isEqualTo(300000);
        assertThat(response.getPlacePref()).isEqualTo("OUTDOOR");
        assertThat(response.getSocialPref()).isEqualTo("ALONE");
    }

    @Test
    @DisplayName("활동 조건 신규 생성 성공 - 기존 조건이 없을 때 신규 저장")
    void updateConditions_createNew_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userConditionRepository.findByUserIdWithRegion(1L)).willReturn(Optional.empty());
        given(userConditionRepository.save(any(UserCondition.class))).willAnswer(invocation -> invocation.getArgument(0));

        UpdateConditionsRequest request = new UpdateConditionsRequest((short) 3, 150000, "ANY", "ANY", null);
        UserConditionsResponse response = userService.updateConditions(1L, request);

        assertThat(response.getWeeklyHours()).isEqualTo((short) 3);
        assertThat(response.getMonthlyBudget()).isEqualTo(150000);
        assertThat(response.getPlacePref()).isEqualTo("ANY");
        assertThat(response.getSocialPref()).isEqualTo("ANY");
        assertThat(response.getRegion()).isNull();
        verify(userConditionRepository).save(any(UserCondition.class));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("OldPassword123!", "encoded_pwd")).willReturn(true);
        given(passwordEncoder.encode("NewPassword123!")).willReturn("new_encoded_pwd");

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword123!");
        userService.changePassword(1L, request);

        assertThat(testUser.getPasswordHash()).isEqualTo("new_encoded_pwd");
    }

    @Test
    @DisplayName("비밀번호 변경 시 현재 비밀번호 불일치 시 예외")
    void changePassword_mismatch_throwsException() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("WrongPassword!", "encoded_pwd")).willReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "NewPassword123!");

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.AUTH_PASSWORD_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("Password123!", "encoded_pwd")).willReturn(true);

        WithdrawRequest request = new WithdrawRequest("Password123!");
        userService.withdraw(1L, request);

        assertThat(testUser.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(testUser.getWithdrawnAt()).isNotNull();
        verify(authTokenRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("취미 관심 저장 성공")
    void saveHobby_success() {
        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(testUser));
        testHobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        given(hobbyRepository.findById(5L)).willReturn(Optional.of(testHobby));
        given(userHobbyRepository.findByUserIdAndHobbyId(1L, 5L)).willReturn(Optional.empty());

        userService.saveHobby(1L, 5L);

        verify(userHobbyRepository).save(any(UserHobby.class));
        verify(hobbyRepository).adjustSaveCount(5L, 1);
    }

    @ParameterizedTest
    @EnumSource(value = UserHobbyStatus.class, names = {"IN_PROGRESS", "PAUSED", "STOPPED", "COMPLETED"})
    void saveAndUnsaveCannotOverwriteOrDeletePlannedHobby(UserHobbyStatus status) {
        var hobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        var entry = UserHobby.builder().user(testUser).hobby(hobby).status(status).build();
        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(testUser));
        given(hobbyRepository.findById(5L)).willReturn(Optional.of(hobby));
        given(userHobbyRepository.findByUserIdAndHobbyId(1L, 5L)).willReturn(Optional.of(entry));
        assertThatThrownBy(() -> userService.saveHobby(1L, 5L)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> userService.unsaveHobby(1L, 5L)).isInstanceOf(CustomException.class);
        assertThat(entry.getStatus()).isEqualTo(status);
        verify(userHobbyRepository, never()).delete(any());
        verify(userHobbyRepository, never()).save(any());
        verify(hobbyRepository, never()).adjustSaveCount(any(), anyInt());
    }

    @Test void repeatedSaveAndUnsaveAreIdempotent() {
        var hobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        var entry = UserHobby.builder().user(testUser).hobby(hobby).build();
        given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(testUser));
        given(hobbyRepository.findById(5L)).willReturn(Optional.of(hobby));
        given(userHobbyRepository.findByUserIdAndHobbyId(1L, 5L)).willReturn(Optional.of(entry), Optional.of(entry), Optional.empty());
        userService.saveHobby(1L, 5L);
        userService.unsaveHobby(1L, 5L);
        userService.unsaveHobby(1L, 5L);
        verify(userHobbyRepository, times(1)).delete(entry);
        verify(hobbyRepository, times(1)).adjustSaveCount(5L, -1);
        verify(hobbyRepository, never()).adjustSaveCount(5L, 1);
    }

    @Test
    @DisplayName("취미 목록 조회 성공")
    void getUserHobbies_success() {
        UserHobby userHobby = UserHobby.builder()
                .user(testUser)
                .hobby(testHobby)
                .status(UserHobbyStatus.SAVED)
                .build();
        given(userHobbyRepository.findAllByUserIdAndStatusWithHobby(1L, UserHobbyStatus.SAVED))
                .willReturn(List.of(userHobby));
        given(planQueryService.enrich(1L, List.of(userHobby))).willReturn(List.of(UserHobbyResponse.from(userHobby)));

        List<UserHobbyResponse> result = userService.getUserHobbies(1L, "SAVED");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("클라이밍");
        assertThat(result.get(0).getStatus()).isEqualTo("SAVED");
    }

    @Test
    @DisplayName("마이페이지 요약 통계 조회 성공")
    void getMySummary_success() {
        given(userHobbyRepository.countByUserIdAndStatus(1L, UserHobbyStatus.SAVED)).willReturn(3L);
        given(userHobbyRepository.countByUserIdAndStatusIn(eq(1L), any())).willReturn(2L);
        given(shortSaveRepository.countByUserId(1L)).willReturn(5L);
        given(postRepository.countByUserIdAndStatus(1L, com.ssafy.nyamnyam.domain.post.entity.PostStatus.ACTIVE)).willReturn(4L);

        com.ssafy.nyamnyam.domain.user.dto.response.MySummaryResponse summary = userService.getMySummary(1L);

        assertThat(summary.savedHobbyCount()).isEqualTo(3L);
        assertThat(summary.inProgressHobbyCount()).isEqualTo(2L);
        assertThat(summary.savedShortCount()).isEqualTo(5L);
        assertThat(summary.myPostCount()).isEqualTo(4L);
    }
}
