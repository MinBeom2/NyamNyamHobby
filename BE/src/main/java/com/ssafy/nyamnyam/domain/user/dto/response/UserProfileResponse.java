package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private boolean hasPassword;
    private UserConditionsResponse conditions;

    public UserProfileResponse(Long id, String email, String nickname, String profileImageUrl, String role, boolean hasPassword, UserConditionsResponse conditions) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role;
        this.hasPassword = hasPassword;
        this.conditions = conditions;
    }

    public static UserProfileResponse of(User user, UserCondition condition) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole().name(),
                user.getPasswordHash() != null,
                UserConditionsResponse.from(condition)
        );
    }
}

