package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserConditionsResponse {

    private Short weeklyHours;
    private Integer monthlyBudget;
    private String placePref;
    private String socialPref;
    private RegionResponse region;

    public UserConditionsResponse(Short weeklyHours, Integer monthlyBudget, String placePref, String socialPref, RegionResponse region) {
        this.weeklyHours = weeklyHours;
        this.monthlyBudget = monthlyBudget;
        this.placePref = placePref;
        this.socialPref = socialPref;
        this.region = region;
    }

    public static UserConditionsResponse from(UserCondition condition) {
        if (condition == null) {
            return null;
        }
        return new UserConditionsResponse(
                condition.getWeeklyHours(),
                condition.getMonthlyBudget(),
                condition.getPlacePref(),
                condition.getSocialPref(),
                RegionResponse.from(condition.getRegion())
        );
    }
}

