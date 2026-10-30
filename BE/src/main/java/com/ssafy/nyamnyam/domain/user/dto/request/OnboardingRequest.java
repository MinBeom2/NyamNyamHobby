package com.ssafy.nyamnyam.domain.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** 01-6A 관심 취미와 01-6B 이용 상황을 한 번에 저장한다. */
@Getter
@NoArgsConstructor
public class OnboardingRequest extends UpdateConditionsRequest {
    @Valid
    @Size(max = 5, message = "관심 취미는 최대 5개까지 선택할 수 있습니다.")
    private List<@NotNull InitialHobby> initialHobbies = List.of();

    public OnboardingRequest(Short weeklyHours, Integer monthlyBudget, String placePref,
                             String socialPref, Long regionId, List<InitialHobby> initialHobbies) {
        super(weeklyHours, monthlyBudget, placePref, socialPref, regionId);
        this.initialHobbies = initialHobbies;
    }

    public record InitialHobby(@NotNull @Positive Long hobbyId) {}
}
