package com.ssafy.nyamnyam.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateConditionsRequest {

    @Min(value = 0, message = "주당 활동 시간은 0 이상이어야 합니다.")
    @Max(value = 40, message = "주당 활동 시간은 40 이하여야 합니다.")
    private Short weeklyHours;

    @Min(value = 0, message = "월 예산은 0 이상이어야 합니다.")
    private Integer monthlyBudget;

    @Pattern(regexp = "^(INDOOR|OUTDOOR|ANY)$", message = "장소 선호는 INDOOR, OUTDOOR, ANY 중 하나여야 합니다.")
    private String placePref;

    @Pattern(regexp = "^(ALONE|TOGETHER|ANY)$", message = "소셜 선호는 ALONE, TOGETHER, ANY 중 하나여야 합니다.")
    private String socialPref;

    @Positive(message = "지역 ID는 양수여야 합니다.")
    private Long regionId;

    public UpdateConditionsRequest(Short weeklyHours, Integer monthlyBudget, String placePref, String socialPref, Long regionId) {
        this.weeklyHours = weeklyHours;
        this.monthlyBudget = monthlyBudget;
        this.placePref = placePref;
        this.socialPref = socialPref;
        this.regionId = regionId;
    }
}

