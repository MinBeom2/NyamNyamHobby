package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.response.PlanPreviewResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.plan.repository.PlanTemplateRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 취미 TODO 플랜 미리보기(FR-HOBBY-02 P1, 명세 3.4-4). 시작하기(PlanService)와 같은 기준으로
 * 해당 (취미, 숙련도)의 공개(PUBLISHED) 원본을 읽기만 한다. 원본 데이터는 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanPreviewService {

    public static final String DEFAULT_SKILL_LEVEL = "BEGINNER";
    public static final int DEFAULT_WEEKS = 2;
    public static final int MAX_WEEKS = 3;
    private static final int SKILL_LEVEL_MAX_LENGTH = 12; // user_hobbies.skill_level VARCHAR(12)
    private static final String TEMPLATE_PUBLISHED = "PUBLISHED";

    private final HobbyRepository hobbyRepository;
    private final PlanTemplateRepository planTemplateRepository;

    /**
     * @param skillLevel 생략하면 BEGINNER. 대소문자는 구분하지 않는다
     * @param weeks      미리 보여 줄 주차 수. 생략하면 2, 1~3만 허용(화면표 04-4는 3주차까지)
     */
    public PlanPreviewResponse preview(Long hobbyId, String skillLevel, Integer weeks) {
        if (hobbyId == null || hobbyId <= 0) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }
        int previewWeeks = weeks == null ? DEFAULT_WEEKS : weeks;
        if (previewWeeks < 1 || previewWeeks > MAX_WEEKS) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "weeks는 1~" + MAX_WEEKS + "이어야 합니다.");
        }
        String level = normalizeSkillLevel(skillLevel);

        boolean published = hobbyRepository.findById(hobbyId)
                .map(h -> h.getStatus() == HobbyStatus.PUBLISHED)
                .orElse(false);
        if (!published) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND);
        }
        return planTemplateRepository.findByHobbyIdAndSkillLevelAndStatus(hobbyId, level, TEMPLATE_PUBLISHED)
                .map(template -> PlanPreviewResponse.of(template, previewWeeks))
                .orElseGet(() -> PlanPreviewResponse.notReady(level));
    }

    private static String normalizeSkillLevel(String skillLevel) {
        if (skillLevel == null || skillLevel.isBlank()) {
            return DEFAULT_SKILL_LEVEL;
        }
        String level = skillLevel.strip().toUpperCase(Locale.ROOT);
        if (level.length() > SKILL_LEVEL_MAX_LENGTH || !level.matches("[A-Z_]+")) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "skillLevel 값이 올바르지 않습니다: " + skillLevel);
        }
        return level;
    }
}
