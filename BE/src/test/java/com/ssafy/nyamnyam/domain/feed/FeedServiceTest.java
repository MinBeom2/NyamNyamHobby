package com.ssafy.nyamnyam.domain.feed;

import com.ssafy.nyamnyam.domain.feed.dto.request.FeedSearchCondition;
import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedScope;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.feed.service.FeedService;
import com.ssafy.nyamnyam.domain.hobby.entity.Category;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.user.entity.InterestSelection;
import com.ssafy.nyamnyam.domain.user.entity.Region;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserHobby;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.repository.InterestSelectionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserHobbyRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    FeedItemRepository feedItemRepository;

    @Mock
    InterestSelectionRepository interestSelectionRepository;

    @Mock
    UserHobbyRepository userHobbyRepository;

    @InjectMocks
    FeedService feedService;

    @Test
    @DisplayName("피드 목록 조회 시 isEnded 계산이 정상 동작한다")
    void getFeedsCalculatesIsEnded() {
        Category category = Category.builder().name("운동").build();
        Hobby hobby = Hobby.builder().category(category).name("클라이밍").build();

        // 1) 진행 중인 행사 (종료일이 내일) -> isEnded = false
        FeedItem ongoingFeed = FeedItem.builder()
                .hobby(hobby)
                .region(Region.builder().code("11710").sido("서울특별시").sigungu("송파구").build())
                .type(FeedType.EVENT)
                .title("클라이밍 페스티벌")
                .sourceName("TourAPI")
                .sourceUrl("https://example.com")
                .eventStart(LocalDate.now(KST).minusDays(1))
                .eventEnd(LocalDate.now(KST).plusDays(1))
                .origin(FeedOrigin.TOUR_API)
                .status(FeedStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build();

        // 2) 종료된 행사 (종료일이 어제) -> isEnded = true
        FeedItem endedFeed = FeedItem.builder()
                .hobby(hobby)
                .type(FeedType.EVENT)
                .title("지난 대회")
                .sourceName("TourAPI")
                .sourceUrl("https://example.com")
                .eventStart(LocalDate.now(KST).minusDays(3))
                .eventEnd(LocalDate.now(KST).minusDays(1))
                .origin(FeedOrigin.TOUR_API)
                .status(FeedStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build();

        PageImpl<FeedItem> pageResult = new PageImpl<>(List.of(ongoingFeed, endedFeed));
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(pageResult);

        // 비회원은 scope와 상관없이 전체 취미 기준
        PageResponse<FeedItemResponse> response = feedService.getFeeds(null, new FeedSearchCondition(FeedScope.MY, 1L, FeedType.EVENT, false, null, null, null, null, false), 0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).isEnded()).isFalse();
        assertThat(response.content().get(1).isEnded()).isTrue();
        assertThat(response.content().get(0).regionName()).isEqualTo("서울 송파구");
        assertThat(response.content().get(1).regionName()).isNull();
    }

    @Test
    @DisplayName("회원이 내 취미를 하나도 고르지 않았으면 피드를 조회하지 않고 빈 페이지를 준다")
    void memberWithoutMyHobbiesGetsEmptyPage() {
        given(interestSelectionRepository.findAllByUserIdWithHobby(7L)).willReturn(List.of());
        given(userHobbyRepository.findByUserIdAndStatus(any(), any())).willReturn(List.of());

        PageResponse<FeedItemResponse> response = feedService.getFeeds(7L, new FeedSearchCondition(null, null, null, true, null, null, null, null, false), 0, 20);

        assertThat(response.content()).isEmpty();
        verifyNoInteractions(feedItemRepository);
    }

    @Test
    @DisplayName("MY는 내 취미와 진행 중·일시중지 취미를, SAVED는 관심 저장한 취미를 모은다")
    void scopeCollectsHobbiesFromTheRightSource() {
        User user = User.builder().email("feed@test.com").nickname("feed").build();
        ReflectionTestUtils.setField(user, "id", 7L);
        Hobby selected = hobby(1L), running = hobby(2L), saved = hobby(3L);
        given(interestSelectionRepository.findAllByUserIdWithHobby(7L))
                .willReturn(List.of(InterestSelection.builder().user(user).hobby(selected).build()));
        given(userHobbyRepository.findByUserIdAndStatus(7L, UserHobbyStatus.IN_PROGRESS))
                .willReturn(List.of(UserHobby.builder().user(user).hobby(running).status(UserHobbyStatus.IN_PROGRESS).build()));
        given(userHobbyRepository.findByUserIdAndStatus(7L, UserHobbyStatus.PAUSED)).willReturn(List.of());
        given(userHobbyRepository.findByUserIdAndStatus(7L, UserHobbyStatus.SAVED))
                .willReturn(List.of(UserHobby.builder().user(user).hobby(saved).build()));
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        feedService.getFeeds(7L, new FeedSearchCondition(FeedScope.MY, null, null, true, null, null, null, null, false), 0, 20);
        verify(userHobbyRepository, never()).findByUserIdAndStatus(7L, UserHobbyStatus.SAVED);

        feedService.getFeeds(7L, new FeedSearchCondition(FeedScope.SAVED, null, null, true, null, null, null, null, false), 0, 20);
        verify(userHobbyRepository).findByUserIdAndStatus(7L, UserHobbyStatus.SAVED);
        verify(feedItemRepository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("ALL은 내 취미를 조회하지 않는다")
    void allScopeDoesNotReadMyHobbies() {
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<>(List.of()));
        feedService.getFeeds(7L, new FeedSearchCondition(FeedScope.ALL, null, null, true, null, null, null, null, false), 0, 20);
        verifyNoInteractions(interestSelectionRepository, userHobbyRepository);
    }

    private Hobby hobby(Long id) {
        Hobby hobby = Hobby.builder().name("취미" + id).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    @Test
    @DisplayName("검색어는 2자 이상만 쓰고 %, _ 는 글자 그대로 찾는다")
    void keywordIsTrimmedAndEscaped() {
        assertThat(new FeedSearchCondition(null, null, null, true, " 요", null, null, null, false).keyword()).isNull();
        assertThat(new FeedSearchCondition(null, null, null, true, " 요가 ", null, null, null, false).keyword()).isEqualTo("요가");
        assertThat(FeedService.escapeLike("100%_달성")).isEqualTo("100\\%\\_달성");
    }

    @Test
    @DisplayName("서울 공공서비스예약 강좌는 요약 맨 앞 접수 상태로 마감 여부를 알린다")
    void reservationClosedComesFromStatusInSummary() {
        Hobby hobby = Hobby.builder().name("요가").build();
        FeedItem closed = FeedItem.builder().hobby(hobby).type(FeedType.CLASS).title("요가").sourceName("서울")
                .sourceUrl("https://x").origin(FeedOrigin.SEOUL_RESERVATION).summary("접수종료 · 무료 · 센터").build();
        FeedItem open = FeedItem.builder().hobby(hobby).type(FeedType.CLASS).title("요가").sourceName("서울")
                .sourceUrl("https://x").origin(FeedOrigin.SEOUL_RESERVATION).summary("접수중 · 무료 · 센터").build();
        FeedItem show = FeedItem.builder().hobby(hobby).type(FeedType.SHOW).title("전시").sourceName("문화포털")
                .sourceUrl("https://x").origin(FeedOrigin.CULTURE_PORTAL).summary("마감 임박 전시").build();
        assertThat(closed.isReservationClosed()).isTrue();
        assertThat(open.isReservationClosed()).isFalse();
        assertThat(show.isReservationClosed()).isFalse();
    }

    @Test
    @DisplayName("홈 새 소식: 오늘 올라온 소식을 마감일 순으로 먼저, 모자라면 최신순으로 채운다")
    void highlightsPutTodayByDeadlineThenFillWithLatest() {
        given(interestSelectionRepository.findAllByUserIdWithHobby(7L)).willReturn(List.of());
        given(userHobbyRepository.findByUserIdAndStatus(any(), any())).willReturn(List.of());
        LocalDate today = LocalDate.now(KST);
        FeedItem lateDeadline = feed(1L, today.plusDays(20));
        FeedItem noDeadline = feed(2L, null);
        FeedItem soonDeadline = feed(3L, today.plusDays(2));
        FeedItem older = feed(4L, today.plusDays(1));
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(lateDeadline, noDeadline, soonDeadline)))   // 오늘 올라온 것
                .willReturn(new PageImpl<>(List.of(lateDeadline, older, noDeadline, soonDeadline))); // 최신순 전체

        List<FeedItemResponse> result = feedService.getHighlights(7L, 5);

        assertThat(result).extracting(FeedItemResponse::id).containsExactly(3L, 1L, 2L, 4L);
    }

    @Test
    @DisplayName("홈 새 소식: 오늘 소식이 개수보다 많으면 마감일 순으로 잘라 최신순 조회는 하지 않는다")
    void highlightsCutTodayAtLimit() {
        LocalDate today = LocalDate.now(KST);
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<>(List.of(
                feed(1L, today.plusDays(9)), feed(2L, today.plusDays(1)), feed(3L, today.plusDays(5)))));

        List<FeedItemResponse> result = feedService.getHighlights(null, 2);

        assertThat(result).extracting(FeedItemResponse::id).containsExactly(2L, 3L);
        verify(feedItemRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verifyNoInteractions(interestSelectionRepository, userHobbyRepository); // 비회원은 전체 취미
    }

    private FeedItem feed(Long id, LocalDate end) {
        Hobby hobby = Hobby.builder().name("요가").build();
        FeedItem item = FeedItem.builder().hobby(hobby).type(FeedType.CLASS).title("피드" + id).sourceName("서울")
                .sourceUrl("https://x").origin(FeedOrigin.SEOUL_RESERVATION).status(FeedStatus.PUBLISHED)
                .eventStart(end == null ? null : end.minusDays(3)).eventEnd(end).publishedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
