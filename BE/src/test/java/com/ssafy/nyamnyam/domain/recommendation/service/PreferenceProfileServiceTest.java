package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.*;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import com.ssafy.nyamnyam.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreferenceProfileServiceTest {
    ExplorationViewRepository views = mock(ExplorationViewRepository.class);
    ShortReactionRepository reactions = mock(ShortReactionRepository.class);
    PreferenceProfileService service = new PreferenceProfileService(views,reactions);
    @Test void usesLatestViewOnceAndCurrentReactionAcrossSessions() {
        User user = User.builder().email("a@a.com").nickname("test").build();
        ReflectionTestUtils.setField(user,"id",1L);
        Hobby h = Hobby.builder().name("취미").build(); ReflectionTestUtils.setField(h,"id",1L);
        Short s = Short.builder().hobby(h).youtubeVideoId("p1").title("영상").build(); ReflectionTestUtils.setField(s,"id",10L);
        ExplorationSession old = ExplorationSession.builder().user(user).build(), current = ExplorationSession.builder().user(user).build();
        ReflectionTestUtils.setField(old,"id",1L); ReflectionTestUtils.setField(current,"id",2L);
        ExplorationView latest = ExplorationView.builder().session(current).shortform(s).watchRatio(new BigDecimal("0.25")).build();
        ExplorationView previous = ExplorationView.builder().session(old).shortform(s).watchRatio(BigDecimal.ONE).build();
        when(views.findHistoryByUserId(1L)).thenReturn(List.of(latest,previous));
        when(reactions.findByUser(user)).thenReturn(List.of(ShortReaction.builder().user(user).shortform(s).type(ReactionType.LIKE).build()));
        assertThat(service.load(user).hobbyScores().get(1L)).isEqualTo(2.5);
        assertThat(service.load(user).likeCount()).isEqualTo(1);
        assertThat(service.load(user,2L).hobbyScores().get(1L)).isEqualTo(4.0);
        ReflectionTestUtils.setField(latest,"playFailed",true);
        assertThat(service.load(user).hobbyScores()).isEmpty();
    }
}
