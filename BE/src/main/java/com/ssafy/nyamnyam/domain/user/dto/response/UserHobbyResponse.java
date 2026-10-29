package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime startedAt;

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
        this.startedAt = startedAt;
    }

    public static UserHobbyResponse from(UserHobby userHobby) {
        Hobby hobby = userHobby.getHobby();
        return new UserHobbyResponse(
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
    }
}

