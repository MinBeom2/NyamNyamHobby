package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.stream.*;
import static org.assertj.core.api.Assertions.*;

class RecommendationPlannerTest {
    RecommendationPlanner planner = new RecommendationPlanner();
    @Test void threeLikesSwitchToInterestAndRelatedWithExclusions() {
        List<Hobby> hobbies = IntStream.rangeClosed(1,8).mapToObj(i -> hobby(i,i)).toList();
        var profile = new PreferenceProfileService.Profile(Map.of(1L,12.0,2L,6.0,3L,3.0),Map.of(),Map.of(1L,3L),Map.of(),3);
        var result = planner.plan(hobbies,profile,Set.of(2L),Set.of(4L),null);
        assertThat(result.exploratory()).isFalse();
        assertThat(result.choices()).extracting(c -> c.hobby().getId()).containsExactly(1L,3L,5L,6L,7L);
        assertThat(result.choices().get(0).fallbackReason()).contains("좋아요 3회");
    }
    @Test void fillsRecommendedHobbiesWithLikedOnesWhenFewerThanThreeArePositive() {
        List<Hobby> hobbies = IntStream.rangeClosed(1,6).mapToObj(i -> hobby(i,i)).toList();
        // 1만 양수, 2·3은 좋아요를 받았지만 넘긴 영상이 많아 0 이하, 4는 좋아요 없음
        var profile = new PreferenceProfileService.Profile(Map.of(1L,5.0,2L,-0.3,3L,-1.2,4L,-0.6),Map.of(),
                Map.of(1L,2L,2L,1L,3L,1L),Map.of(),4);
        var result = planner.plan(hobbies,profile,Set.of(),Set.of(),null);
        assertThat(result.choices()).filteredOn(c -> c.type() == com.ssafy.nyamnyam.domain.recommendation.entity.RecoItemType.INTEREST)
                .extracting(c -> c.hobby().getId()).containsExactly(1L,2L,3L);
    }
    @Test void insufficientLikesUsePopularDistinctCategoriesAndReturnFewerWhenNeeded() {
        Hobby duplicate = hobby(2,1);
        ReflectionTestUtils.setField(duplicate,"saveCount",100);
        var result = planner.plan(List.of(hobby(1,1),duplicate,hobby(3,2)),
                new PreferenceProfileService.Profile(Map.of(),Map.of(),Map.of(1L,2L),Map.of(),2),Set.of(),Set.of(),null);
        assertThat(result.exploratory()).isTrue();
        assertThat(result.choices()).extracting(c -> c.hobby().getId()).containsExactly(2L,3L);
        assertThat(result.choices().get(0).score()).isEqualTo(100.0);
    }
    @Test void emptyEligiblePoolDoesNotInventRecommendations() {
        var result = planner.plan(List.of(hobby(1,1)),PreferenceProfileService.Profile.empty(),Set.of(1L),Set.of(),null);
        assertThat(result.choices()).isEmpty();
    }
    @Test void conditionsApplyOneWarningPenaltyAndSeparatePreferenceBoosts() {
        Hobby h = hobby(1,1);
        UserCondition conditions = UserCondition.builder().weeklyHours((short)0).monthlyBudget(0)
                .placePref("INDOOR").socialPref("ALONE").build();
        var p = new PreferenceProfileService.Profile(Map.of(1L,10.0),Map.of(),Map.of(1L,3L),Map.of(),3);
        var result = planner.plan(List.of(h),p,Set.of(),Set.of(),conditions).choices().get(0);
        assertThat(result.warning()).isTrue();
        assertThat(result.score()).isCloseTo(9.68,within(0.00001));
    }
    @Test void existingWeightedSimilarityFormulaIsPreserved() {
        Hobby first = hobby(1,1), second = hobby(2,2);
        Tag subject = Tag.builder().name("스포츠").tagGroup(TagGroup.SUBJECT).build();
        first.getHobbyTags().add(new HobbyTag(first,subject));
        second.getHobbyTags().add(new HobbyTag(second,subject));
        assertThat(planner.calculateTagSimilarity(first,second)).isCloseTo(4.0/11,within(1e-9));
    }
    @Test void selectionOrdersPickedHobbiesByConditionFitAndKeepsPickOrderOnTies() {
        Hobby cheap1 = hobby(1,1), pricey = hobby(2,1), cheap3 = hobby(3,1), cheap4 = hobby(4,1);
        ReflectionTestUtils.setField(pricey,"monthlyCostMin",80000);
        UserCondition c = UserCondition.builder().monthlyBudget(50000).weeklyHours((short)3).build();
        var result = planner.planSelection(List.of(pricey,cheap4,cheap1,cheap3),List.of(cheap1,pricey,cheap3,cheap4),Set.of(),c);
        // 예산을 넘는 2는 뒤로 밀리고, 나머지는 고른 순서(4→1→3)를 지킨다. 3개만 고른다
        assertThat(result.choices()).extracting(ch -> ch.hobby().getId()).containsExactly(4L,1L,3L);
        assertThat(result.choices().get(0).fallbackReason()).isEqualTo("월 예산 5만 원 안에서 시작할 수 있어요");
        assertThat(RecommendationPlanner.fitReason(pricey,c)).isEqualTo("월 예산보다 비용이 들 수 있어요 (월 8만 원부터)");
        assertThat(RecommendationPlanner.fitReason(pricey,null)).isEqualTo("직접 고른 취미예요");
    }
    @Test void selectionAddsSimilarUnpickedHobbiesExceptExcludedAndUnrelated() {
        Tag nature = Tag.builder().name("자연").tagGroup(TagGroup.SUBJECT).build();
        Tag outdoor = Tag.builder().name("실외").tagGroup(TagGroup.PLACE).build();
        Hobby picked = hobby(1,1), near = hobby(2,2), alsoNear = hobby(3,3), disliked = hobby(4,4), unrelated = hobby(5,5);
        for (Hobby h : List.of(picked,near,disliked)) { h.getHobbyTags().add(new HobbyTag(h,nature)); h.getHobbyTags().add(new HobbyTag(h,outdoor)); }
        alsoNear.getHobbyTags().add(new HobbyTag(alsoNear,outdoor));
        var result = planner.planSelection(List.of(picked),List.of(picked,near,alsoNear,disliked,unrelated),Set.of(4L),null);
        assertThat(result.choices()).extracting(ch -> ch.hobby().getId()).containsExactly(1L,2L,3L);
        assertThat(result.choices().get(1).type()).isEqualTo(com.ssafy.nyamnyam.domain.recommendation.entity.RecoItemType.RELATED);
        assertThat(result.choices().get(1).fallbackReason()).contains("직접 고른 취미1");
    }
    private Hobby hobby(long id,long categoryId) {
        Category category = Category.builder().name("분류"+categoryId).build();
        ReflectionTestUtils.setField(category,"id",categoryId);
        Hobby h = Hobby.builder().category(category).name("취미"+id).placeType(PlaceType.INDOOR).socialType(SocialType.ALONE)
                .difficulty(Difficulty.EASY).sessionMinMinutes((short)60).sessionMaxMinutes((short)90)
                .monthlyCostMin(10000).monthlyCostMax(20000).build();
        ReflectionTestUtils.setField(h,"id",id); return h;
    }
}
