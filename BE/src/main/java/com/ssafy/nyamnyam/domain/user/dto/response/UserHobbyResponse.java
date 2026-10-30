package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import com.ssafy.nyamnyam.domain.plan.dto.PlanResponses;

@Getter
@NoArgsConstructor
public class UserHobbyResponse {

    private Long userHobbyId;
    private Long hobbyId;
    private String name;
    private String thumbnailUrl;
    private String status;
    private String skillLevel;
    private Long planId;
    private Double progressRate;
    private OffsetDateTime startedAt;
    private String title;
    private Integer totalWeeks;
    private Integer currentWeek;
    private String weekTitle;
    private PlanResponses.Progress planProgress;
    private PlanResponses.Progress weekProgress;
    private int remainingThisWeekCount;
    private int overdueCount;
    private boolean planReady;
    private List<String> availableSkillLevels = List.of();
    private OffsetDateTime pausedAt;

    public void setPlanOptions(List<String> levels) {
        availableSkillLevels = levels;
        planReady = levels.contains(skillLevel);
    }

    public void attachPlan(PlanResponses.View plan) {
        planId = plan.planId();
        title = plan.title();
        totalWeeks = plan.totalWeeks();
        currentWeek = plan.currentWeek();
        weekTitle = plan.weekTitle();
        planProgress = plan.planProgress();
        weekProgress = plan.weekProgress();
        progressRate = planProgress.rate();
        remainingThisWeekCount = weekProgress.total() - weekProgress.done() - weekProgress.skipped();
        overdueCount = plan.overdueCount();
    }


    public UserHobbyResponse(Long userHobbyId, Long hobbyId, String name, String thumbnailUrl,
                             String status, String skillLevel, Long planId, Double progressRate,
                             LocalDateTime startedAt) {
        this.userHobbyId = userHobbyId;
        this.hobbyId = hobbyId;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.skillLevel = skillLevel;
        this.planId = planId;
        this.progressRate = progressRate;
        this.startedAt = PlanResponses.time(startedAt);
    }

    public static UserHobbyResponse from(UserHobby userHobby) {
        Hobby hobby = userHobby.getHobby();
        var response = new UserHobbyResponse(
                userHobby.getId(),
                hobby.getId(),
                hobby.getName(),
                hobby.getThumbnailUrl(),
                userHobby.getStatus().name(),
                userHobby.getSkillLevel(),
                null,
                0.0,
                userHobby.getStartedAt()
        );
        response.pausedAt = PlanResponses.time(userHobby.getPausedAt());
        return response;
    }
}

