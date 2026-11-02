package com.ssafy.nyamnyam.domain.notification;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.dto.NotificationResponse;
import com.ssafy.nyamnyam.domain.notification.entity.Notification;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.repository.NotificationRepository;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 0, 0); // UTC
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(repository,
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    private static Notification notification(long id, long userId, LocalDateTime readAt) throws Exception {
        Constructor<Notification> c = Notification.class.getDeclaredConstructor();
        c.setAccessible(true);
        Notification n = c.newInstance();
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "userId", userId);
        ReflectionTestUtils.setField(n, "type", NotificationType.COMMENT);
        ReflectionTestUtils.setField(n, "title", "댓글");
        ReflectionTestUtils.setField(n, "linkPath", "/community/1");
        ReflectionTestUtils.setField(n, "dedupKey", "COMMENT:" + id);
        ReflectionTestUtils.setField(n, "readAt", readAt);
        ReflectionTestUtils.setField(n, "createdAt", NOW.minusHours(1));
        return n;
    }

    @Test
    @DisplayName("저장: 중복 무시 INSERT에 현재 UTC 시각을 넘기고, 실제로 넣었는지 돌려준다")
    void createInsertsIgnoringDuplicate() {
        given(repository.insertIgnoringDuplicate(7L, "COMMENT", "t", "b", "/l", "COMMENT:1", NOW)).willReturn(1, 0);
        NotificationMessage m = new NotificationMessage(7L, NotificationType.COMMENT, "t", "b", "/l", "COMMENT:1");

        assertThat(service.create(m)).isTrue();
        assertThat(service.create(m)).isFalse();
    }

    @Test
    @DisplayName("저장: 필수 값이 없는 메시지는 버린다")
    void createIgnoresBrokenMessage() {
        assertThat(service.create(null)).isFalse();
        assertThat(service.create(new NotificationMessage(null, NotificationType.COMMENT, "t", null, "/l", "K"))).isFalse();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("목록: 최신순 20개 기본, unreadOnly면 안 읽은 것만, 시각은 +09:00")
    void listNewestFirst() throws Exception {
        given(repository.findByUserIdAndReadAtIsNull(eq(7L), any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(List.of(notification(1L, 7L, null)), inv.getArgument(1), 1));

        PageResponse<NotificationResponse> page = service.list(7L, true, null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByUserIdAndReadAtIsNull(eq(7L), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        NotificationResponse item = page.content().get(0);
        assertThat(item.isRead()).isFalse();
        assertThat(item.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(item.createdAt().getHour()).isEqualTo(8); // 23:00 UTC = 08:00 KST
        verify(repository, never()).findByUserId(anyLong(), any());
    }

    @Test
    @DisplayName("목록: 음수 page·size 범위 밖은 400")
    void listRejectsInvalidPaging() {
        assertThatThrownBy(() -> service.list(7L, false, -1, 20)).isInstanceOf(CustomException.class);
        assertThatThrownBy(() -> service.list(7L, false, 0, 101)).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("단건 읽음: 처음 읽은 시각을 남기고, 남의 알림이면 404")
    void readOwnNotificationOnly() throws Exception {
        Notification n = notification(1L, 7L, null);
        given(repository.findByIdAndUserId(1L, 7L)).willReturn(Optional.of(n));
        given(repository.findByIdAndUserId(1L, 8L)).willReturn(Optional.empty());

        assertThat(service.read(7L, 1L).isRead()).isTrue();
        assertThat(n.getReadAt()).isEqualTo(NOW);
        n.markRead(NOW.plusHours(1));
        assertThat(n.getReadAt()).isEqualTo(NOW); // 다시 읽어도 처음 시각 유지
        assertThatThrownBy(() -> service.read(8L, 1L))
                .isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    @DisplayName("모두 읽음·안 읽은 수·90일 정리")
    void bulkOperations() {
        given(repository.markAllRead(7L, NOW)).willReturn(3);
        given(repository.countByUserIdAndReadAtIsNull(7L)).willReturn(5L);
        given(repository.deleteCreatedBefore(NOW.minusDays(90))).willReturn(2);

        assertThat(service.readAll(7L)).isEqualTo(3);
        assertThat(service.unreadCount(7L)).isEqualTo(5L);
        assertThat(service.purgeExpired()).isEqualTo(2);
        verify(repository, never()).insertIgnoringDuplicate(anyLong(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
    }
}
