package com.ssafy.nyamnyam.domain.recommendation;

import com.ssafy.nyamnyam.domain.recommendation.model.TagFeature;
import com.ssafy.nyamnyam.domain.recommendation.repository.HobbyRecommendationRepository;
import com.ssafy.nyamnyam.domain.recommendation.service.HobbyRecommendationService;
import com.ssafy.nyamnyam.global.error.CustomException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HobbyRecommendationServiceTest {
    private final HobbyRecommendationRepository repository = mock(HobbyRecommendationRepository.class);
    private final HobbyRecommendationService service = new HobbyRecommendationService(repository);

    @Test void emptyOrNegativePreferenceDoesNotQuery() {
        assertThat(service.recommendByPreference(Map.of(), 3, Set.of())).isEmpty();
        assertThat(service.recommendByPreference(Map.of(TagFeature.SPORTS, -2.0), 3, Set.of())).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test void finiteExtremeScoresKeepNegativePreferenceAndWeights() {
        service.recommendByPreference(Map.of(TagFeature.SPORTS, Double.MAX_VALUE,
                TagFeature.INDOOR, -Double.MAX_VALUE), 3, Set.of(9L));
        var vector = ArgumentCaptor.forClass(String.class);
        verify(repository).findNearest(vector.capture(), eq(3), eq(Set.of(9L)));
        String[] values = vector.getValue().replace("[", "").replace("]", "").split(",");
        assertThat(values).hasSize(24);
        assertThat(Double.parseDouble(values[0])).isEqualTo(-1);
        assertThat(Double.parseDouble(values[9])).isEqualTo(2);
    }

    @Test void similarHobbyExcludesItselfWithoutMutatingInput() {
        when(repository.findPublishedHobbyVector(1)).thenReturn(Optional.of("[1,0]"));
        Set<Long> input = Set.of(7L);
        service.recommendSimilarToHobby(1, 3, input);
        verify(repository).findNearest("[1,0]", 3, Set.of(1L, 7L));
        assertThat(input).containsExactly(7L);
    }

    @Test void rejectsMalformedInputsAndUnavailableBase() {
        assertThatThrownBy(() -> service.recommendByPreference(Map.of(TagFeature.SPORTS, Double.NaN), 3, Set.of()))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.recommendByPreference(Map.of(), 0, Set.of()))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.recommendSimilarToHobby(1, 3, Set.of()))
                .isInstanceOf(CustomException.class);
    }
}
