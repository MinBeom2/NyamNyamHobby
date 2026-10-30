package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition;
import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition.PlaceFilter;
import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition.SocialFilter;
import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition.SortType;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbySummaryResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyStatus;
import com.ssafy.nyamnyam.domain.hobby.entity.PlaceType;
import com.ssafy.nyamnyam.domain.hobby.entity.SocialType;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 취미 검색·상세(FR-HOBBY-01, 02). 공개(PUBLISHED) 취미만 다룬다.
 * 별로예요·관련 숏폼은 {@link HobbyVideoService}에 있다.
 *
 * <p>검색은 공개 취미 전체(수십 개 규모의 관리자 등록 카탈로그)를 분류·태그와 함께 한 번에 읽은 뒤
 * 메모리에서 필터·정렬·페이지를 적용한다. 추천순은 사용자의 숏폼 반응 점수(취미별)를 써야 해서
 * DB 정렬로 옮기기 어렵고, 카탈로그가 작아 이 방식이 단순하고 충분하다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final UserRepository userRepository;
    private final PreferenceProfileService preferenceProfileService;

    /**
     * 취미 검색. 비회원은 userId를 null로 넘긴다.
     * 정렬 기본값은 로그인 시 RECOMMEND(관심사 점수), 비회원은 POPULAR. 비회원이 RECOMMEND를 요청해도 POPULAR로 처리한다.
     */
    public PageResponse<HobbySummaryResponse> search(HobbySearchCondition condition, Long userId) {
        List<Hobby> matched = hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED).stream()
                .filter(h -> matchesKeyword(h, condition.keyword()))
                .filter(h -> condition.time() == null
                        || condition.time().overlaps(h.getSessionMinMinutes(), h.getSessionMaxMinutes()))
                .filter(h -> condition.cost() == null
                        || condition.cost().overlaps(h.getMonthlyCostMin(), h.getMonthlyCostMax()))
                .filter(h -> condition.difficulty() == null || h.getDifficulty() == condition.difficulty())
                .filter(h -> matchesPlace(h, condition.place()))
                .filter(h -> matchesSocial(h, condition.social()))
                .sorted(comparator(resolveSort(condition.sort(), userId), userId))
                .toList();

        PageRequest pageable = PageRequest.of(condition.page(), condition.size());
        int from = (int) Math.min(pageable.getOffset(), matched.size());
        int to = Math.min(from + condition.size(), matched.size());
        return PageResponse.of(new PageImpl<>(matched.subList(from, to), pageable, matched.size()),
                HobbySummaryResponse::from);
    }

    /**
     * 취미 상세 조회. 비회원은 userId를 null로 넘기면 myStatus가 "NONE"이 된다.
     * 없거나 공개되지 않은 취미는 404(COMMON_NOT_FOUND)로, 관련 숏폼 조회와 같은 기준이다.
     */
    public HobbyDetailResponse getDetail(Long hobbyId, Long userId) {
        if (hobbyId == null || hobbyId <= 0) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT);
        }
        Hobby hobby = hobbyRepository.findWithCategoryAndTagsById(hobbyId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        if (hobby.getStatus() != HobbyStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.COMMON_NOT_FOUND);
        }
        String myStatus = userId == null ? null
                : userHobbyRepository.findByUserIdAndHobbyId(userId, hobbyId)
                        .map(UserHobby::getStatus)
                        .map(Enum::name)
                        .orElse(null);
        return HobbyDetailResponse.of(hobby, myStatus);
    }

    /** 취미명 또는 태그 이름 부분 일치(대소문자 무시). 검색어가 없으면 전체 */
    private static boolean matchesKeyword(Hobby hobby, String keyword) {
        if (keyword == null) {
            return true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        if (hobby.getName().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        return hobby.getHobbyTags().stream()
                .anyMatch(ht -> ht.getTag().getName().toLowerCase(Locale.ROOT).contains(needle));
    }

    private static boolean matchesPlace(Hobby hobby, PlaceFilter place) {
        return switch (place) {
            case ANY -> true;
            case INDOOR -> hobby.getPlaceType() == PlaceType.INDOOR || hobby.getPlaceType() == PlaceType.BOTH;
            case OUTDOOR -> hobby.getPlaceType() == PlaceType.OUTDOOR || hobby.getPlaceType() == PlaceType.BOTH;
        };
    }

    private static boolean matchesSocial(Hobby hobby, SocialFilter social) {
        return switch (social) {
            case ANY -> true;
            case ALONE -> hobby.getSocialType() == SocialType.ALONE || hobby.getSocialType() == SocialType.BOTH;
            case TOGETHER -> hobby.getSocialType() == SocialType.TOGETHER || hobby.getSocialType() == SocialType.BOTH;
        };
    }

    private static SortType resolveSort(SortType requested, Long userId) {
        if (requested == null) {
            return userId == null ? SortType.POPULAR : SortType.RECOMMEND;
        }
        if (requested == SortType.RECOMMEND && userId == null) {
            return SortType.POPULAR;
        }
        return requested;
    }

    /** 동점이면 인기순(저장 수) → 이름 → ID 순으로 고정해 페이지가 바뀌어도 순서가 흔들리지 않게 한다 */
    private Comparator<Hobby> comparator(SortType sort, Long userId) {
        Comparator<Hobby> popular = Comparator.comparingInt(Hobby::getSaveCount).reversed()
                .thenComparing(Hobby::getName)
                .thenComparing(Hobby::getId);
        return switch (sort) {
            case POPULAR -> popular;
            case NAME -> Comparator.comparing(Hobby::getName).thenComparing(Hobby::getId);
            case COST_ASC -> Comparator.comparingInt(Hobby::getMonthlyCostMin)
                    .thenComparingInt(Hobby::getMonthlyCostMax)
                    .thenComparing(popular);
            case RECOMMEND -> {
                Map<Long, Double> scores = preferenceScores(userId);
                yield Comparator.<Hobby>comparingDouble(h -> scores.getOrDefault(h.getId(), 0.0)).reversed()
                        .thenComparing(popular);
            }
        };
    }

    /** 숏폼 탐색 반응으로 쌓인 취미별 관심사 점수. 사용자를 못 찾으면 점수 없음(=인기순과 같아짐) */
    private Map<Long, Double> preferenceScores(Long userId) {
        return userRepository.findById(userId)
                .map(user -> preferenceProfileService.load(user).hobbyScores())
                .orElse(Map.of());
    }
}
