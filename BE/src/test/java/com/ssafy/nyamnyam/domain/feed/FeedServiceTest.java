package com.ssafy.nyamnyam.domain.feed;

import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.repository.FeedItemRepository;
import com.ssafy.nyamnyam.domain.feed.service.FeedService;
import com.ssafy.nyamnyam.domain.hobby.entity.Category;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    FeedItemRepository feedItemRepository;

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
                .type(FeedType.EVENT)
                .title("클라이밍 페스티벌")
                .sourceName("TourAPI")
                .sourceUrl("https://example.com")
                .eventStart(LocalDate.now().minusDays(1))
                .eventEnd(LocalDate.now().plusDays(1))
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
                .eventStart(LocalDate.now().minusDays(3))
                .eventEnd(LocalDate.now().minusDays(1))
                .origin(FeedOrigin.TOUR_API)
                .status(FeedStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .build();

        PageImpl<FeedItem> pageResult = new PageImpl<>(List.of(ongoingFeed, endedFeed));
        given(feedItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(pageResult);

        PageResponse<FeedItemResponse> response = feedService.getFeeds(1L, FeedType.EVENT, false, 0, 20);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0).isEnded()).isFalse();
        assertThat(response.content().get(1).isEnded()).isTrue();
    }
}

