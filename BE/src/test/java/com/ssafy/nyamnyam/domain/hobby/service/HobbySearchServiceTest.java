package com.ssafy.nyamnyam.domain.hobby.service;

import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbySummaryResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.HobbyRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

/** 취미 검색(FR-HOBBY-01): 필터 5종 AND, 정렬 4종, 페이지 */
@ExtendWith(MockitoExtension.class)
class HobbySearchServiceTest {

    @Mock HobbyRepository hobbyRepository;
    @Mock UserHobbyRepository userHobbyRepository;
    @Mock UserRepository userRepository;
    @Mock PreferenceProfileService preferenceProfileService;
    @InjectMocks HobbyService hobbyService;

    private Hobby climbing;   // 실내·함께·보통·60~120분·월 10~15만·저장 420·태그 실내,활동적
    private Hobby running;    // 실외·혼자·쉬움·30~60분·월 0~3만·저장 900·태그 활동적
    private Hobby reading;    // BOTH·혼자·쉬움·30~90분·월 1~3만·저장 50·태그 정적

    @BeforeEach
    void setUp() {
        Category sports = category(1L, "운동·야외");
        Category culture = category(2L, "문화·교양");
        climbing = hobby(1L, sports, "클라이밍", PlaceType.INDOOR, SocialType.TOGETHER, Difficulty.NORMAL,
                60, 120, 100_000, 150_000, 420, tag(10L, "실내"), tag(20L, "활동적"));
        running = hobby(2L, sports, "러닝", PlaceType.OUTDOOR, SocialType.ALONE, Difficulty.EASY,
                30, 60, 0, 30_000, 900, tag(20L, "활동적"));
        reading = hobby(3L, culture, "독서", PlaceType.BOTH, SocialType.ALONE, Difficulty.EASY,
                30, 90, 10_000, 30_000, 50, tag(30L, "정적"));
        // 조건 파서만 검사하는 테스트는 저장소를 부르지 않으므로 lenient
        lenient().when(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED))
                .thenReturn(List.of(climbing, running, reading));
    }

    private static HobbySearchCondition cond(String keyword, String time, String cost, String difficulty,
                                             String place, String social, String sort) {
        return HobbySearchCondition.of(keyword, time, cost, difficulty, place, social, sort, null, null);
    }

    private static List<String> names(PageResponse<HobbySummaryResponse> page) {
        return page.content().stream().map(HobbySummaryResponse::name).toList();
    }

    @Test
    @DisplayName("조건 없음·비회원: 전체를 인기순(저장 수)으로, 명세의 페이지 정보와 카드 필드를 돌려준다")
    void searchAll_guestPopular() {
        PageResponse<HobbySummaryResponse> page = hobbyService.search(cond(null, null, null, null, null, null, null), null);

        assertThat(names(page)).containsExactly("러닝", "클라이밍", "독서");
        assertThat(page.page().number()).isZero();
        assertThat(page.page().size()).isEqualTo(20);
        assertThat(page.page().totalElements()).isEqualTo(3);
        assertThat(page.page().totalPages()).isEqualTo(1);
        assertThat(page.page().hasNext()).isFalse();

        HobbySummaryResponse card = page.content().get(1);
        assertThat(card.id()).isEqualTo(1L);
        assertThat(card.category().name()).isEqualTo("운동·야외");
        assertThat(card.sessionMinutes().max()).isEqualTo(120);
        assertThat(card.monthlyCost().min()).isEqualTo(100_000);
        assertThat(card.tags()).containsExactly("실내", "활동적");
        verifyNoInteractions(userRepository, preferenceProfileService);
    }

    @Test
    @DisplayName("검색어는 취미명 또는 태그 부분 일치")
    void searchByKeyword() {
        assertThat(names(hobbyService.search(cond("클라", null, null, null, null, null, null), null)))
                .containsExactly("클라이밍");
        assertThat(names(hobbyService.search(cond("활동적", null, null, null, null, null, null), null)))
                .containsExactly("러닝", "클라이밍");
        assertThat(names(hobbyService.search(cond("없는말", null, null, null, null, null, null), null))).isEmpty();
    }

    @Test
    @DisplayName("필터는 모두 AND: 시간·비용은 범위가 겹치면 포함, 장소·참여는 BOTH 취미를 포함")
    void searchWithFilters() {
        assertThat(names(hobbyService.search(cond(null, "1H_LESS", null, null, null, null, null), null)))
                .containsExactly("러닝", "클라이밍", "독서");          // 클라이밍 60~120도 60에서 겹침
        assertThat(names(hobbyService.search(cond(null, "2H_MORE", null, null, null, null, null), null)))
                .containsExactly("클라이밍");
        assertThat(names(hobbyService.search(cond(null, null, "100K_MORE", null, null, null, null), null)))
                .containsExactly("클라이밍");
        assertThat(names(hobbyService.search(cond(null, null, "30K_LESS", "EASY", null, null, null), null)))
                .containsExactly("러닝", "독서");
        assertThat(names(hobbyService.search(cond(null, null, null, null, "INDOOR", null, null), null)))
                .containsExactly("클라이밍", "독서");                  // 독서는 BOTH
        assertThat(names(hobbyService.search(cond(null, null, null, null, "OUTDOOR", "ALONE", null), null)))
                .containsExactly("러닝", "독서");
        assertThat(names(hobbyService.search(cond(null, null, null, "HARD", null, null, null), null))).isEmpty();
    }

    @Test
    @DisplayName("정렬: 이름순·비용 낮은순")
    void searchSorted() {
        assertThat(names(hobbyService.search(cond(null, null, null, null, null, null, "NAME"), null)))
                .containsExactly("독서", "러닝", "클라이밍");
        assertThat(names(hobbyService.search(cond(null, null, null, null, null, null, "COST_ASC"), null)))
                .containsExactly("러닝", "독서", "클라이밍");
    }

    @Test
    @DisplayName("로그인 기본 정렬은 추천순: 관심사 점수가 높은 취미가 앞, 동점은 인기순")
    void searchRecommendForMember() {
        User user = User.builder().email("u@test.com").nickname("유저").build();
        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(preferenceProfileService.load(user)).willReturn(new PreferenceProfileService.Profile(
                Map.of(3L, 4.5, 1L, 1.0), Map.of(), Map.of(), Map.of(), 2));

        assertThat(names(hobbyService.search(cond(null, null, null, null, null, null, null), 7L)))
                .containsExactly("독서", "클라이밍", "러닝");
    }

    @Test
    @DisplayName("비회원이 RECOMMEND를 요청하면 인기순으로 처리한다")
    void searchRecommendForGuestFallsBackToPopular() {
        assertThat(names(hobbyService.search(cond(null, null, null, null, null, null, "RECOMMEND"), null)))
                .containsExactly("러닝", "클라이밍", "독서");
        verifyNoInteractions(userRepository, preferenceProfileService);
    }

    @Test
    @DisplayName("페이지: size 2면 2쪽, 범위를 넘는 page는 빈 목록")
    void searchPaging() {
        PageResponse<HobbySummaryResponse> first = hobbyService.search(
                HobbySearchCondition.of(null, null, null, null, null, null, "NAME", 0, 2), null);
        PageResponse<HobbySummaryResponse> second = hobbyService.search(
                HobbySearchCondition.of(null, null, null, null, null, null, "NAME", 1, 2), null);
        PageResponse<HobbySummaryResponse> beyond = hobbyService.search(
                HobbySearchCondition.of(null, null, null, null, null, null, "NAME", 5, 2), null);

        assertThat(names(first)).containsExactly("독서", "러닝");
        assertThat(first.page().totalPages()).isEqualTo(2);
        assertThat(first.page().hasNext()).isTrue();
        assertThat(names(second)).containsExactly("클라이밍");
        assertThat(second.page().hasNext()).isFalse();
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.page().totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("잘못된 조건은 400 COMMON_INVALID_INPUT: 1자 검색어, 모르는 필터 값, 음수 page, size 101")
    void invalidCondition() {
        assertThatThrownBy(() -> cond("클", null, null, null, null, null, null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, "3H", null, null, null, null, null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, null, "FREE", null, null, null, null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, null, null, "VERY_HARD", null, null, null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, null, null, null, "SEA", null, null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, null, null, null, null, "FAMILY", null)).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> cond(null, null, null, null, null, null, "RANDOM")).satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> HobbySearchCondition.of(null, null, null, null, null, null, null, -1, 20))
                .satisfies(this::isInvalidInput);
        assertThatThrownBy(() -> HobbySearchCondition.of(null, null, null, null, null, null, null, 0, 101))
                .satisfies(this::isInvalidInput);
    }

    @Test
    @DisplayName("빈 검색어·소문자 필터 값은 허용한다")
    void lenientCondition() {
        HobbySearchCondition condition = HobbySearchCondition.of("  ", "1h_less", null, "easy", "indoor", null, "name", null, null);
        assertThat(condition.keyword()).isNull();
        assertThat(condition.time()).isEqualTo(HobbySearchCondition.TimeRange.H1_LESS);
        assertThat(condition.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(condition.place()).isEqualTo(HobbySearchCondition.PlaceFilter.INDOOR);
        assertThat(condition.social()).isEqualTo(HobbySearchCondition.SocialFilter.ANY);
        assertThat(condition.sort()).isEqualTo(HobbySearchCondition.SortType.NAME);
    }

    private void isInvalidInput(Throwable t) {
        assertThat(t).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }

    private static Category category(Long id, String name) {
        Category category = Category.builder().name(name).build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private static Tag tag(Long id, String name) {
        Tag tag = Tag.builder().name(name).tagGroup(TagGroup.SUBJECT).build();
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    private static Hobby hobby(Long id, Category category, String name, PlaceType place, SocialType social,
                               Difficulty difficulty, int sessionMin, int sessionMax, int costMin, int costMax,
                               int saveCount, Tag... tags) {
        Hobby hobby = Hobby.builder()
                .category(category).name(name).summary(name + " 요약")
                .placeType(place).socialType(social).difficulty(difficulty)
                .sessionMinMinutes((short) sessionMin).sessionMaxMinutes((short) sessionMax)
                .monthlyCostMin(costMin).monthlyCostMax(costMax)
                .status(HobbyStatus.PUBLISHED).saveCount(saveCount)
                .build();
        ReflectionTestUtils.setField(hobby, "id", id);
        for (Tag tag : tags) {
            hobby.getHobbyTags().add(new HobbyTag(hobby, tag));
        }
        return hobby;
    }
}
