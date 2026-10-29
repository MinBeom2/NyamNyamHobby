package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.*;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HobbyVideoService {
    private final HobbyRepository hobbies;
    private final HobbyDislikeRepository dislikes;
    private final ShortRepository shorts;
    private final UserRepository users;

    public record ShortCard(Long shortId, String youtubeVideoId, String title, Integer duration, String thumbnailUrl) {
        static ShortCard from(Short s) {
            return new ShortCard(s.getId(), s.getYoutubeVideoId(), s.getTitle(), (int) s.getDurationSeconds(), s.getThumbnailUrl());
        }
    }
    @Transactional
    public void dislike(Long userId, Long hobbyId, boolean excluded) {
        if (hobbyId <= 0) throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        Hobby hobby = hobbies.findById(hobbyId).orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        if (excluded && hobby.getStatus() != HobbyStatus.PUBLISHED) throw new CustomException(ErrorCode.COMMON_NOT_FOUND);
        if (!users.existsById(userId)) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        if (excluded) dislikes.save(userId, hobbyId); else dislikes.delete(userId, hobbyId);
    }
    public List<ShortCard> relatedShorts(Long hobbyId, int limit) {
        if (hobbyId <= 0) throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        if (limit < 1 || limit > 6) throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "limit은 1~6이어야 합니다.");
        Hobby hobby = hobbies.findById(hobbyId).orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        if (hobby.getStatus() != HobbyStatus.PUBLISHED) throw new CustomException(ErrorCode.COMMON_NOT_FOUND);
        return shorts.findByHobbyIdAndStatusOrderByReleasedAtDescIdDesc(hobbyId, ShortStatus.ACTIVE, PageRequest.of(0, limit))
                .stream().map(ShortCard::from).toList();
    }
}
