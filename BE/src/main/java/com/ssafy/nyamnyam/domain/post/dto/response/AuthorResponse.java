package com.ssafy.nyamnyam.domain.post.dto.response;

import com.ssafy.nyamnyam.domain.user.entity.User;

public record AuthorResponse(Long id, String nickname, String profileImageUrl) {

    public static AuthorResponse from(User user) {
        return new AuthorResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}
