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
    private Hobby hobby(long id,long categoryId) {
        Category category = Category.builder().name("분류"+categoryId).build();
        ReflectionTestUtils.setField(category,"id",categoryId);
        Hobby h = Hobby.builder().category(category).name("취미"+id).placeType(PlaceType.INDOOR).socialType(SocialType.ALONE)
                .difficulty(Difficulty.EASY).sessionMinMinutes((short)60).sessionMaxMinutes((short)90)
                .monthlyCostMin(10000).monthlyCostMax(20000).build();
        ReflectionTestUtils.setField(h,"id",id); return h;
    }
}
