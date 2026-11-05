package com.ssafy.nyamnyam.domain.notification.service;

import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedStatus;
import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.repository.NewFeedTargetRepository;
import com.ssafy.nyamnyam.domain.user.entity.UserHobbyStatus;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * NOTI-B03 새 피드 알림(매일 09:00). 지난 24시간 안에 공개된 정보 피드를 취미별로 세고,
 * 그 취미를 내 취미·관심 취미·진행 중(일시중지 포함) 취미로 가진 회원에게 하루 1건으로 묶어 보낸다.
 * 05:00·05:30 수집과 06:00 GMS 분류가 끝난 뒤라 그날 아침 공개된 소식까지 들어간다.
 * 마감·종료된 항목과 '그 외'(취미 없음) 피드는 세지 않는다. 같은 날 다시 돌아도 dedupKey로 한 번만 저장된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewFeedNotifier {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 본문에 이름을 적는 취미 수. 나머지는 '외 N개' */
    static final int NAMED_HOBBIES = 3;
    static final String LINK = "/info-feed";
    private static final List<UserHobbyStatus> OWNED = List.of(
            UserHobbyStatus.SAVED, UserHobbyStatus.IN_PROGRESS, UserHobbyStatus.PAUSED);

    private final NewFeedTargetRepository targetRepository;
    private final NotificationService notificationService;
    private final Clock planClock; // UTC 시계(PlanTimeConfig). DB 시각도 UTC다

    /** @return 새로 만든 알림 수 */
    @Transactional(readOnly = true)
    public int notifyNewFeeds() {
        LocalDateTime now = LocalDateTime.now(planClock);
        Map<Long, HobbyCount> counts = new HashMap<>();
        for (FeedItem item : targetRepository.findPublishedBetween(FeedStatus.PUBLISHED, now.minusHours(24), now)) {
            if (item.computeIsEnded() || item.isReservationClosed()) continue;
            counts.computeIfAbsent(item.getHobby().getId(), id -> new HobbyCount(item.getHobby().getName())).count++;
        }
        if (counts.isEmpty()) {
            log.info("[NEW_FEED] no new feeds");
            return 0;
        }

        Map<Long, Set<Long>> hobbiesByUser = new LinkedHashMap<>();
        addOwners(hobbiesByUser, targetRepository.findInterestOwners(counts.keySet(), UserStatus.ACTIVE));
        addOwners(hobbiesByUser, targetRepository.findHobbyOwners(counts.keySet(), OWNED, UserStatus.ACTIVE));

        String today = LocalDate.now(planClock.withZone(KST)).toString();
        int created = 0;
        for (Map.Entry<Long, Set<Long>> entry : hobbiesByUser.entrySet()) {
            if (notificationService.create(messageFor(entry.getKey(), entry.getValue(), counts, today))) created++;
        }
        log.info("[NEW_FEED] hobbies={} users={} notifications created={}", counts.size(), hobbiesByUser.size(), created);
        return created;
    }

    private static void addOwners(Map<Long, Set<Long>> hobbiesByUser, List<Object[]> rows) {
        for (Object[] row : rows) {
            hobbiesByUser.computeIfAbsent((Long) row[0], id -> new LinkedHashSet<>()).add((Long) row[1]);
        }
    }

    /** 새 소식이 많은 취미부터 최대 3개 이름을 적는다. 예: '요가 · 클라이밍 외 1개에 새 소식이 올라왔어요' */
    static NotificationMessage messageFor(Long userId, Set<Long> hobbyIds, Map<Long, HobbyCount> counts, String today) {
        List<HobbyCount> mine = hobbyIds.stream().map(counts::get)
                .sorted(Comparator.comparingInt((HobbyCount c) -> c.count).reversed().thenComparing(c -> c.name))
                .toList();
        int total = mine.stream().mapToInt(c -> c.count).sum();
        String names = mine.stream().limit(NAMED_HOBBIES).map(c -> c.name).collect(Collectors.joining(" · "));
        String more = mine.size() > NAMED_HOBBIES ? " 외 " + (mine.size() - NAMED_HOBBIES) + "개" : "";
        return new NotificationMessage(userId, NotificationType.NEW_FEED,
                "관심 취미 새 소식 " + total + "건", names + more + "에 새 소식이 올라왔어요.",
                LINK, "NEW_FEED:" + today);
    }

    static final class HobbyCount {
        final String name;
        int count;

        HobbyCount(String name) {
            this.name = name;
        }
    }
}
