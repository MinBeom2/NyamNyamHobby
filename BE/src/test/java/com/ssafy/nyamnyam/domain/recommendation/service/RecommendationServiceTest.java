package com.ssafy.nyamnyam.domain.recommendation.service;

import com.ssafy.nyamnyam.domain.exploration.entity.*;
import com.ssafy.nyamnyam.domain.exploration.repository.ExplorationSessionRepository;
import com.ssafy.nyamnyam.domain.hobby.entity.*;
import com.ssafy.nyamnyam.domain.hobby.repository.*;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.recommendation.entity.*;
import com.ssafy.nyamnyam.domain.recommendation.repository.*;
import com.ssafy.nyamnyam.domain.user.entity.*;
import com.ssafy.nyamnyam.domain.user.repository.*;
import com.ssafy.nyamnyam.global.error.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.LongStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    @Mock ExplorationSessionRepository sessionRepository;
    @Mock PreferenceProfileService profiles;
    @Spy RecommendationPlanner planner = new RecommendationPlanner();
    @Mock HobbyRepository hobbyRepository;
    @Mock HobbyDislikeRepository dislikes;
    @Mock UserHobbyRepository userHobbies;
    @Mock UserConditionRepository conditions;
    @Mock RecommendationRepository recommendationRepository;
    @Mock RecommendationItemRepository itemRepository;
    @Mock RecommendationReasonOutbox outbox;
    @Spy ObjectMapper mapper = JsonMapper.builder().build();
    @InjectMocks RecommendationService service;
    User user;
    ExplorationSession session;
    @BeforeEach void setUp() {
        user = User.builder().email("test@example.com").nickname("test").build();
        ReflectionTestUtils.setField(user,"id",1L);
        session = ExplorationSession.builder().user(user).build();
        ReflectionTestUtils.setField(session,"id",50L);
    }
    @Test void createsSnapshotAndEnqueuesCommittedDeliveryRequest() {
        Hobby hobby = Hobby.builder().name("클라이밍").placeType(PlaceType.INDOOR).socialType(SocialType.ALONE)
                .difficulty(Difficulty.NORMAL).sessionMinMinutes((short)60).sessionMaxMinutes((short)120).build();
        ReflectionTestUtils.setField(hobby,"id",1L);
        when(sessionRepository.findByIdAndUserForUpdate(50L,user)).thenReturn(Optional.of(session));
        when(profiles.load(user)).thenReturn(new PreferenceProfileService.Profile(Map.of(1L,12.0),Map.of(),Map.of(1L,3L),Map.of(),3));
        when(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED)).thenReturn(List.of(hobby));
        List<RecommendationItem> stored = new ArrayList<>();
        when(recommendationRepository.save(any())).thenAnswer(call -> { Recommendation r = call.getArgument(0); ReflectionTestUtils.setField(r,"id",88L); return r; });
        when(itemRepository.save(any())).thenAnswer(call -> { RecommendationItem i = call.getArgument(0); ReflectionTestUtils.setField(i,"id",100L); stored.add(i); return i; });
        when(itemRepository.findByRecommendationOrderByRankNoAsc(any())).thenReturn(stored);
        var result = service.finishExploration(50L,user);
        assertThat(result.created()).isTrue();
        assertThat(result.response().getBasis()).isEqualTo("EXPLORATION_SESSION");
        assertThat(result.response().getItems().get(0).getHobbyName()).isEqualTo("클라이밍");
        assertThat(result.response().getItems().get(0).getFallbackReason()).contains("좋아요 3회");
        assertThat(stored.get(0).getHobbySnapshot()).contains("클라이밍");
        verify(outbox).enqueue(any());
    }
    @Test void repeatedFinishReturnsStoredItemsWithoutRecalculation() {
        session.complete();
        var endedAt = session.getEndedAt();
        Recommendation r = Recommendation.builder().user(user).session(session).reactionCount((short)3).build();
        ReflectionTestUtils.setField(r,"id",88L);
        Hobby h = Hobby.builder().name("클라이밍").sessionMinMinutes((short)60).sessionMaxMinutes((short)90).build();
        ReflectionTestUtils.setField(h,"id",1L);
        RecommendationItem item = RecommendationItem.builder().recommendation(r).hobby(h).type(RecoItemType.INTEREST)
                .rankNo((short)1).score(BigDecimal.ONE).build();
        when(sessionRepository.findByIdAndUserForUpdate(50L,user)).thenReturn(Optional.of(session));
        when(recommendationRepository.findBySessionId(50L)).thenReturn(Optional.of(r));
        when(itemRepository.findByRecommendationOrderByRankNoAsc(r)).thenReturn(List.of(item));
        var result = service.finishExploration(50L,user);
        assertThat(result.created()).isFalse();
        assertThat(result.response().getRecommendationId()).isEqualTo(88L);
        assertThat(session.getEndedAt()).isEqualTo(endedAt);
        verifyNoInteractions(profiles,outbox,hobbyRepository);
    }
    @Test void foreignSessionIsNotFound() {
        assertError(() -> service.finishExploration(50L,user),ErrorCode.COMMON_NOT_FOUND);
        verifyNoInteractions(recommendationRepository,profiles,outbox);
    }
    @Test void abandonedSessionCannotGenerateRecommendation() {
        session.abandon();
        when(sessionRepository.findByIdAndUserForUpdate(50L,user)).thenReturn(Optional.of(session));
        assertError(() -> service.finishExploration(50L,user),ErrorCode.SHORT_SESSION_ENDED);
        verifyNoInteractions(profiles,outbox);
    }
    @Test void foreignRecommendationIsNotFound() {
        assertError(() -> service.getRecommendation(88L,user),ErrorCode.COMMON_NOT_FOUND);
        verifyNoInteractions(itemRepository);
    }
    @Test void latestRecommendationIsEmptyBeforeFirstExploration() {
        when(recommendationRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.empty());
        assertThat(service.getLatestRecommendation(user)).isEmpty();
        verifyNoInteractions(itemRepository);
    }
    @Test void latestRecommendationReturnsMostRecentWithItems() {
        Recommendation latest = Recommendation.builder().user(user).session(session).basis(RecoBasis.REACTION).build();
        ReflectionTestUtils.setField(latest,"id",99L);
        when(recommendationRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(latest));
        when(itemRepository.findByRecommendationOrderByRankNoAsc(latest)).thenReturn(List.of());
        assertThat(service.getLatestRecommendation(user)).hasValueSatisfying(r -> assertThat(r.getRecommendationId()).isEqualTo(99L));
    }
    @Test void emptySnapshotThumbnailIsFilledFromSameHobby() {
        Recommendation latest = Recommendation.builder().user(user).session(session).basis(RecoBasis.REACTION).build();
        ReflectionTestUtils.setField(latest,"id",99L);
        Hobby camping = Hobby.builder().name("캠핑").thumbnailUrl("/assets/hobbies/camping.webp")
                .sessionMinMinutes((short)60).sessionMaxMinutes((short)120).build();
        ReflectionTestUtils.setField(camping,"id",5L);
        Hobby pottery = Hobby.builder().name("도자기").thumbnailUrl("/assets/hobbies/pottery-new.webp")
                .sessionMinMinutes((short)60).sessionMaxMinutes((short)120).build();
        ReflectionTestUtils.setField(pottery,"id",12L);
        RecommendationItem old = RecommendationItem.builder().recommendation(latest).hobby(camping).type(RecoItemType.INTEREST)
                .rankNo((short)1).score(BigDecimal.ONE).build();
        old.initializeSnapshot("{\"id\":5,\"name\":\"캠핑\",\"thumbnailUrl\":null}", null, false);
        RecommendationItem kept = RecommendationItem.builder().recommendation(latest).hobby(pottery).type(RecoItemType.RELATED)
                .rankNo((short)2).score(BigDecimal.ONE).build();
        kept.initializeSnapshot("{\"id\":12,\"name\":\"도자기\",\"thumbnailUrl\":\"/assets/hobbies/pottery.webp\"}", null, false);
        when(recommendationRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(latest));
        when(itemRepository.findByRecommendationOrderByRankNoAsc(latest)).thenReturn(List.of(old, kept));

        var items = service.getLatestRecommendation(user).orElseThrow().getItems();

        assertThat(items.get(0).getHobby().name()).isEqualTo("캠핑");
        assertThat(items.get(0).getHobby().thumbnailUrl()).isEqualTo("/assets/hobbies/camping.webp");
        assertThat(items.get(1).getHobby().thumbnailUrl()).isEqualTo("/assets/hobbies/pottery.webp");
    }
    @Test void selectionCreatesSelectionRecommendationWithoutGptAndSummarizesCommonTags() {
        com.ssafy.nyamnyam.domain.hobby.entity.Tag outdoor = com.ssafy.nyamnyam.domain.hobby.entity.Tag.builder().name("실외").tagGroup(TagGroup.PLACE).build();
        Hobby a = Hobby.builder().name("러닝").placeType(PlaceType.OUTDOOR).socialType(SocialType.ALONE).difficulty(Difficulty.EASY)
                .sessionMinMinutes((short)30).sessionMaxMinutes((short)60).monthlyCostMin(0).monthlyCostMax(10000).build();
        Hobby b = Hobby.builder().name("등산").placeType(PlaceType.OUTDOOR).socialType(SocialType.BOTH).difficulty(Difficulty.HARD)
                .sessionMinMinutes((short)120).sessionMaxMinutes((short)300).monthlyCostMin(0).monthlyCostMax(50000).build();
        ReflectionTestUtils.setField(a,"id",2L); ReflectionTestUtils.setField(b,"id",6L);
        a.getHobbyTags().add(new HobbyTag(a,outdoor)); b.getHobbyTags().add(new HobbyTag(b,outdoor));
        when(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED)).thenReturn(List.of(a,b));
        when(recommendationRepository.save(any())).thenAnswer(call -> { Recommendation r = call.getArgument(0); ReflectionTestUtils.setField(r,"id",90L); return r; });
        List<RecommendationItem> stored = new ArrayList<>();
        when(itemRepository.save(any())).thenAnswer(call -> { RecommendationItem i = call.getArgument(0); stored.add(i); return i; });
        when(itemRepository.findByRecommendationOrderByRankNoAsc(any())).thenReturn(stored);

        var response = service.createFromSelection(List.of(6L,2L,6L),user);

        assertThat(response.getBasis()).isEqualTo("DIRECT_SELECTION");
        assertThat(response.getBasisText()).isEqualTo("직접 고른 취미 2개에 #실외 성향이 많아요. 시작하기 좋은 순서로 정리했어요");
        assertThat(response.getItems()).extracting(RecommendationResponse.RecommendationItemDto::getHobbyName).containsExactly("등산","러닝");
        assertThat(response.getItems()).allSatisfy(i -> assertThat(i.getReasonStatus()).isEqualTo(ReasonStatus.FALLBACK));
        verifyNoInteractions(outbox,sessionRepository,profiles);
    }
    @Test void selectionRejectsUnknownOrTooManyHobbies() {
        when(hobbyRepository.findAllWithTagsByStatus(HobbyStatus.PUBLISHED)).thenReturn(List.of());
        assertError(() -> service.createFromSelection(List.of(99L),user),ErrorCode.COMMON_INVALID_INPUT);
        assertError(() -> service.createFromSelection(LongStream.rangeClosed(1,11).boxed().toList(),user),ErrorCode.COMMON_INVALID_INPUT);
        verifyNoInteractions(recommendationRepository);
    }
    private void assertError(Runnable r,ErrorCode code) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(CustomException.class,e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }
}