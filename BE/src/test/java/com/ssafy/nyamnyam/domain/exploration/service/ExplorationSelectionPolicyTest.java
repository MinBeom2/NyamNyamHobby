package com.ssafy.nyamnyam.domain.exploration.service;

import com.ssafy.nyamnyam.domain.recommendation.service.PreferenceProfileService.Profile;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ExplorationSelectionPolicyTest {
    static class FixedPolicy extends ExplorationSelectionPolicy {
        boolean prefer;
        @Override protected boolean prefer(double ratio) { return prefer; }
        @Override protected int index(int bound) { return 0; }
    }
    FixedPolicy policy = new FixedPolicy();
    @Test void firstFourSpreadAcrossUnseenCategoriesAndHandleShortage() {
        Short a = shortform(1,1), b = shortform(2,2);
        assertThat(policy.order(List.of(a,b),Profile.empty(),true,Set.of(1L),0.7).get(0)).isSameAs(b);
        assertThat(policy.order(List.of(a),Profile.empty(),true,Set.of(1L),0.7).get(0)).isSameAs(a);
    }
    @Test void preferenceAndExplorationBranchesCanBeTestedWithoutRandomFlakiness() {
        Short preferred = shortform(1,1), unexplored = shortform(2,2);
        Profile profile = new Profile(Map.of(1L,4.0),Map.of(),Map.of(1L,1L),Map.of(1L,10L),1);
        policy.prefer = true;
        assertThat(policy.order(List.of(unexplored,preferred),profile,false,Set.of(),0.7).get(0)).isSameAs(preferred);
        policy.prefer = false;
        assertThat(policy.order(List.of(preferred,unexplored),profile,false,Set.of(),0.7).get(0)).isSameAs(unexplored);
        assertThat(policy.order(List.of(),profile,false,Set.of(),0.7)).isEmpty();
    }
    private Short shortform(long id,long categoryId) {
        Category c = Category.builder().name("분류").build(); ReflectionTestUtils.setField(c,"id",categoryId);
        Hobby h = Hobby.builder().name("취미").category(c).build(); ReflectionTestUtils.setField(h,"id",id);
        Short s = Short.builder().hobby(h).youtubeVideoId("v"+id).title("영상").build(); ReflectionTestUtils.setField(s,"id",id); return s;
    }
}
