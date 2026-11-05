package com.ssafy.nyamnyam.domain.notification;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.repository.NewFeedTargetRepository;
import com.ssafy.nyamnyam.domain.notification.service.NewFeedNotifier;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewFeedNotifierTest {

    // 2026-11-06 09:00 KST
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneOffset.UTC);
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @Mock NewFeedTargetRepository targets;
    @Mock NotificationService notifications;

    NewFeedNotifier notifier;
    Hobby yoga, climbing, pottery, baking;

    @BeforeEach void setUp() {
        notifier = new NewFeedNotifier(targets, notifications, clock);
        yoga = hobby(3L, "요가");
        climbing = hobby(1L, "클라이밍");
        pottery = hobby(12L, "도자기");
        baking = hobby(21L, "베이킹");
    }

    @Test void bundlesNewFeedsPerUserForMyAndSavedAndRunningHobbies() {
        LocalDateTime now = LocalDateTime.now(clock);
        given(targets.findPublishedBetween(FeedStatus.PUBLISHED, now.minusHours(24), now)).willReturn(List.of(
                feed(yoga, null, null), feed(yoga, null, null), feed(climbing, null, null),
                feed(pottery, null, null), feed(baking, null, null),
                feed(climbing, today.minusDays(1), null),                    // 이미 끝난 행사
                feed(baking, null, "접수종료 · 무료 · 센터")));                 // 접수 마감 강좌
        given(targets.findInterestOwners(anyCollection(), eq(UserStatus.ACTIVE)))
                .willReturn(List.<Object[]>of(new Object[]{7L, 3L}, new Object[]{7L, 1L}));
        given(targets.findHobbyOwners(anyCollection(), any(), eq(UserStatus.ACTIVE)))
                .willReturn(List.<Object[]>of(new Object[]{7L, 12L}, new Object[]{7L, 21L}, new Object[]{8L, 21L}));
        given(notifications.create(any())).willReturn(true);

        int created = notifier.notifyNewFeeds();

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<NotificationMessage> sent = ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notifications, times(2)).create(sent.capture());
        NotificationMessage forSeven = sent.getAllValues().get(0);
        assertThat(forSeven.userId()).isEqualTo(7L);
        assertThat(forSeven.type()).isEqualTo(NotificationType.NEW_FEED);
        assertThat(forSeven.title()).isEqualTo("관심 취미 새 소식 5건");
        assertThat(forSeven.body()).isEqualTo("요가 · 도자기 · 베이킹 외 1개에 새 소식이 올라왔어요.");
        assertThat(forSeven.linkPath()).isEqualTo("/info-feed");
        assertThat(forSeven.dedupKey()).isEqualTo("NEW_FEED:2026-11-06");
        NotificationMessage forEight = sent.getAllValues().get(1);
        assertThat(forEight.title()).isEqualTo("관심 취미 새 소식 1건");
        assertThat(forEight.body()).isEqualTo("베이킹에 새 소식이 올라왔어요.");
    }

    @Test void searchesOwnersOnlyForSavedRunningAndPausedHobbies() {
        LocalDateTime now = LocalDateTime.now(clock);
        given(targets.findPublishedBetween(FeedStatus.PUBLISHED, now.minusHours(24), now))
                .willReturn(List.of(feed(yoga, null, null)));
        given(targets.findInterestOwners(anyCollection(), eq(UserStatus.ACTIVE))).willReturn(List.of());
        ArgumentCaptor<Collection<UserHobbyStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        given(targets.findHobbyOwners(anyCollection(), statuses.capture(), eq(UserStatus.ACTIVE))).willReturn(List.of());

        assertThat(notifier.notifyNewFeeds()).isZero();
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(
                UserHobbyStatus.SAVED, UserHobbyStatus.IN_PROGRESS, UserHobbyStatus.PAUSED);
        verify(notifications, never()).create(any());
    }

    @Test void noNewFeedsSendsNothing() {
        given(targets.findPublishedBetween(any(), any(), any())).willReturn(List.of());

        assertThat(notifier.notifyNewFeeds()).isZero();
        verify(targets, never()).findInterestOwners(any(), any());
        verify(notifications, never()).create(any());
    }

    private static Hobby hobby(Long id, String name) {
        Hobby hobby = Hobby.builder().name(name).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    private static FeedItem feed(Hobby hobby, LocalDate end, String summary) {
        return FeedItem.builder().hobby(hobby).type(FeedType.CLASS).title(hobby.getName() + " 소식")
                .summary(summary).sourceName("서울").sourceUrl("https://x").origin(FeedOrigin.SEOUL_RESERVATION)
                .status(FeedStatus.PUBLISHED).eventEnd(end).build();
    }
}
