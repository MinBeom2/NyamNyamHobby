package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.recommendation.dto.event.RecommendationReasonEvent.ItemReasonPayload;
import com.ssafy.nyamnyam.domain.recommendation.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.RecommendationItemRepository;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationReasonWorkerTest {
    RecommendationItemRepository items = mock(RecommendationItemRepository.class);
    GmsClient gms = mock(GmsClient.class);
    RecommendationReasonWorker worker = new RecommendationReasonWorker(items,gms);
    @Test void boundsReasonLengthAndSkipsDuplicateDelivery() {
        RecommendationItem item = item();
        when(items.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        when(gms.tryGenerateRecommendationReason(any(),any(),any(),any())).thenReturn(Optional.of("가".repeat(80)));
        var payload = payload(); worker.process(88L,payload); worker.process(88L,payload);
        assertThat(item.getReasonStatus()).isEqualTo(ReasonStatus.COMPLETED);
        assertThat(item.getReason()).hasSize(60);
        verify(gms,times(1)).tryGenerateRecommendationReason(any(),any(),any(),any());
    }
    @Test void emptyGenerationUsesFallbackAndWrongRecommendationIsIgnored() {
        RecommendationItem item = item();
        when(items.findByIdForUpdate(1L)).thenReturn(Optional.of(item));
        worker.process(99L,payload()); verifyNoInteractions(gms);
        when(gms.tryGenerateRecommendationReason(any(),any(),any(),any())).thenReturn(Optional.empty());
        worker.process(88L,payload());
        assertThat(item.getReasonStatus()).isEqualTo(ReasonStatus.FALLBACK);
        assertThat(item.getReason()).isNull();
    }
    private RecommendationItem item() {
        Recommendation r = Recommendation.builder().build(); ReflectionTestUtils.setField(r,"id",88L);
        return RecommendationItem.builder().recommendation(r).build();
    }
    private ItemReasonPayload payload() { return ItemReasonPayload.builder().itemId(1L).hobbyName("취미").build(); }
}
