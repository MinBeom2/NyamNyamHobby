package com.ssafy.nyamnyam.domain.user.service;

import com.ssafy.nyamnyam.domain.auth.repository.AuthTokenRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.user.dto.request.OnboardingRequest;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.*;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {
    @Mock UserRepository users;
    @Mock UserConditionRepository conditions;
    @Mock RegionRepository regions;
    @Mock UserHobbyRepository userHobbies;
    @Mock InterestSelectionRepository interests;
    @Mock HobbyRepository hobbies;
    @Mock AuthTokenRepository tokens;
    @Mock PasswordEncoder encoder;
    @InjectMocks UserService service;
    User user;
    Hobby hobby;

    @BeforeEach void setUp() {
        user = User.builder().email("onboarding@test.com").nickname("onboarding").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        hobby = Hobby.builder().name("클라이밍").status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(hobby, "id", 3L);
        lenient().when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    }

    @Test void savesConditionsAndHobbiesWithDefaultSkillLevel() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        given(hobbies.findAllById(Set.of(3L))).willReturn(List.of(hobby));
        Region region = Region.builder().code("11440").sido("서울특별시").sigungu("마포구").build();
        given(regions.findById(10L)).willReturn(Optional.of(region));
        var request = new OnboardingRequest((short) 3, 150000, "INDOOR", "ANY", 10L,
                List.of(new OnboardingRequest.InitialHobby(3L)));

        assertThat(service.saveOnboarding(1L, request).savedHobbyCount()).isEqualTo(1);

        var capturedCondition = ArgumentCaptor.forClass(UserCondition.class);
        verify(conditions).save(capturedCondition.capture());
        assertThat(capturedCondition.getValue().getWeeklyHours()).isEqualTo((short) 3);
        assertThat(capturedCondition.getValue().getMonthlyBudget()).isEqualTo(150000);
        assertThat(capturedCondition.getValue().getPlacePref()).isEqualTo("INDOOR");
        assertThat(capturedCondition.getValue().getSocialPref()).isEqualTo("ANY");
        assertThat(capturedCondition.getValue().getRegion()).isSameAs(region);
        var captured = ArgumentCaptor.forClass(InterestSelection.class);
        verify(interests).save(captured.capture());
        assertThat(captured.getValue().getHobby()).isSameAs(hobby);
        // 내 취미 선택은 관심 저장(user_hobbies)과 별개라 저장 수를 건드리지 않는다
        verifyNoInteractions(userHobbies);
        verify(hobbies, never()).adjustSaveCount(any(), anyInt());
    }

    @Test void repeatedSubmissionDoesNotDuplicateOrResetProgress() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        given(hobbies.findAllById(Set.of(3L))).willReturn(List.of(hobby));
        var selected = InterestSelection.builder().user(user).hobby(hobby).build();
        given(interests.findAllByUserIdWithHobby(1L)).willReturn(List.of(selected));
        var request = request(3L);

        service.saveOnboarding(1L, request);
        service.saveOnboarding(1L, request);

        verify(interests, never()).save(any());
        verify(interests, never()).delete(any());
        verifyNoInteractions(userHobbies);
        verify(hobbies, never()).adjustSaveCount(any(), anyInt());
    }

    @Test void retryOfAlreadySavedSelectionDoesNotIncrementCountOrInsert() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        given(hobbies.findAllById(Set.of(3L))).willReturn(List.of(hobby));
        var selected = InterestSelection.builder().user(user).hobby(hobby).build();
        given(interests.findAllByUserIdWithHobby(1L)).willReturn(List.of(selected));
        assertThat(service.saveOnboarding(1L, request(3L)).savedHobbyCount()).isEqualTo(1);
        assertThat(service.saveOnboarding(1L, request(3L)).savedHobbyCount()).isEqualTo(1);
        verify(interests, never()).save(any());
        verify(interests, never()).delete(any());
    }

    @Test void optionResponseIncludesCategoryAndDatabaseIds() {
        Category category = Category.builder().name("운동").build();
        ReflectionTestUtils.setField(category, "id", 7L);
        ReflectionTestUtils.setField(hobby, "category", category);
        Region region = Region.builder().code("11440").sido("서울특별시").sigungu("마포구").build();
        ReflectionTestUtils.setField(region, "id", 10L);
        given(hobbies.findByStatus(HobbyStatus.PUBLISHED)).willReturn(List.of(hobby));
        given(regions.findAll()).willReturn(List.of(region));
        var response = service.getOnboardingOptions();
        assertThat(response.hobbies()).singleElement().satisfies(option -> {
            assertThat(option.hobbyId()).isEqualTo(3L);
            assertThat(option.categoryId()).isEqualTo(7L);
            assertThat(option.categoryName()).isEqualTo("운동");
        });
        assertThat(response.regions()).singleElement().satisfies(option -> {
            assertThat(option.getId()).isEqualTo(10L);
            assertThat(option.getSido()).isEqualTo("서울특별시");
            assertThat(option.getSigungu()).isEqualTo("마포구");
        });
    }

    @Test void emptySelectionReplacesSavedHobbiesAndClearsOptionalConditions() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        UserCondition old = UserCondition.builder().user(user).weeklyHours((short) 5)
                .monthlyBudget(200000).placePref("INDOOR").socialPref("ALONE").build();
        given(conditions.findByUserIdWithRegion(1L)).willReturn(Optional.of(old));
        var selected = InterestSelection.builder().user(user).hobby(hobby).build();
        given(interests.findAllByUserIdWithHobby(1L)).willReturn(List.of(selected));

        assertThat(service.saveOnboarding(1L, new OnboardingRequest()).savedHobbyCount()).isZero();

        verify(interests).delete(selected);
        verifyNoInteractions(userHobbies);
        assertThat(old.getWeeklyHours()).isNull();
        assertThat(old.getMonthlyBudget()).isNull();
        assertThat(old.getPlacePref()).isNull();
        assertThat(old.getSocialPref()).isNull();
        assertThat(old.getRegion()).isNull();
    }

    @Test void acceptsEntirelySkippedOnboardingAndCreatesCompletionMarker() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        var result = service.saveOnboarding(1L, new OnboardingRequest(null, null, null, null, null, null));
        assertThat(result.onboarded()).isTrue();
        assertThat(result.savedHobbyCount()).isZero();
        verify(conditions).save(any(UserCondition.class));
        verify(interests, never()).save(any());
        verify(interests, never()).delete(any());
    }

    @Test void rejectsDuplicateHobbiesBeforeChangingAnything() {
        assertThatThrownBy(() -> service.saveOnboarding(1L, request(3L, 3L)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT));
        verifyNoInteractions(conditions, interests, hobbies);
    }

    @Test void rejectsMissingOrUnpublishedHobbyBeforeChangingAnything() {
        assertThatThrownBy(() -> service.saveOnboarding(1L, request(404L)))
                .isInstanceOf(CustomException.class);
        ReflectionTestUtils.setField(hobby, "status", HobbyStatus.DRAFT);
        given(hobbies.findAllById(Set.of(3L))).willReturn(List.of(hobby));
        assertThatThrownBy(() -> service.saveOnboarding(1L, request(3L)))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(conditions, interests);
    }

    @Test void invalidRegionDoesNotWriteEitherScreen() {
        given(users.findById(1L)).willReturn(Optional.of(user));
        given(hobbies.findAllById(Set.of(3L))).willReturn(List.of(hobby));
        assertThatThrownBy(() -> service.saveOnboarding(1L,
                new OnboardingRequest(null, null, null, null, 404L, request(3L).getInitialHobbies())))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND));
        verifyNoInteractions(conditions, interests);
    }

    @Test void rejectsMoreThanFiveInterestsBeforeChangingAnything() {
        assertThatThrownBy(() -> service.replaceInterests(1L, List.of(1L, 2L, 3L, 4L, 5L, 6L)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_INPUT));
        verifyNoInteractions(interests, hobbies);
    }

    @Test void replaceInterestsKeepsExistingAndAddsNewInRequestedOrder() {
        Hobby other = Hobby.builder().name("러닝").status(HobbyStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(other, "id", 4L);
        given(hobbies.findAllById(Set.of(3L, 4L))).willReturn(List.of(hobby, other));
        var kept = InterestSelection.builder().user(user).hobby(hobby).build();
        given(interests.findAllByUserIdWithHobby(1L)).willReturn(List.of(kept));

        var result = service.replaceInterests(1L, List.of(3L, 4L));

        assertThat(result).extracting("hobbyId").containsExactly(3L, 4L);
        verify(interests, never()).delete(any());
        var captured = ArgumentCaptor.forClass(InterestSelection.class);
        verify(interests).save(captured.capture());
        assertThat(captured.getValue().getHobby()).isSameAs(other);
    }

    private OnboardingRequest request(Long... ids) {
        return new OnboardingRequest(null, null, null, null, null,
                java.util.Arrays.stream(ids).map(OnboardingRequest.InitialHobby::new).toList());
    }
}
