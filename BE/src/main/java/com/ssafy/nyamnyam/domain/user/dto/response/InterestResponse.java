package com.ssafy.nyamnyam.domain.user.dto.response;

import com.ssafy.nyamnyam.domain.user.entity.InterestSelection;

public record InterestResponse(Long hobbyId, String name, String thumbnailUrl) {
    public static InterestResponse from(InterestSelection selection) {
        var hobby = selection.getHobby();
        return new InterestResponse(hobby.getId(), hobby.getName(), hobby.getThumbnailUrl());
    }
}
