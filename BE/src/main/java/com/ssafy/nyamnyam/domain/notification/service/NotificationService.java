package com.ssafy.nyamnyam.domain.notification.service;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.dto.NotificationResponse;
import com.ssafy.nyamnyam.domain.notification.entity.Notification;
import com.ssafy.nyamnyam.domain.notification.repository.NotificationRepository;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 웹 알림 저장·조회·읽음 처리(FR-NOTI-01, 명세 3.9-1·2). 생성은 큐 소비자·주차 알림 배치가 부른다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final int RETENTION_DAYS = 90;

    private final NotificationRepository notificationRepository;
    private final Clock planClock; // UTC 시계(PlanTimeConfig). 테스트에서 시각을 고정할 수 있다

    /**
     * 알림 1건 저장. 같은 사용자에게 같은 dedupKey가 이미 있으면 저장하지 않는다.
     * 커밋 이후(AFTER_COMMIT) 리스너에서도 부를 수 있도록 항상 새 트랜잭션에서 쓴다.
     *
     * @return 새로 저장했으면 true
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean create(NotificationMessage message) {
        if (message == null || message.userId() == null || message.type() == null || message.dedupKey() == null) {
            return false;
        }
        return notificationRepository.insertIgnoringDuplicate(message.userId(), message.type().name(),
                message.title(), message.body(), message.linkPath(), message.dedupKey(), now()) > 0;
    }

    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, Integer page, Integer size) {
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;
        if (pageValue < 0 || sizeValue < 1 || sizeValue > MAX_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "page는 0 이상, size는 1~" + MAX_SIZE + "이어야 합니다.");
        }
        PageRequest pageable = PageRequest.of(pageValue, sizeValue,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNull(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable);
        return PageResponse.of(result, NotificationResponse::from);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /** 단건 읽음. 남의 알림이거나 없으면 404 */
    @Transactional
    public NotificationResponse read(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        notification.markRead(now());
        return NotificationResponse.from(notification);
    }

    /** 안 읽은 알림 전부 읽음. 바꾼 개수를 돌려준다 */
    @Transactional
    public int readAll(Long userId) {
        return notificationRepository.markAllRead(userId, now());
    }

    /** 보관 기간(90일)이 지난 알림 삭제 */
    @Transactional
    public int purgeExpired() {
        return notificationRepository.deleteCreatedBefore(now().minusDays(RETENTION_DAYS));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(planClock);
    }
}
