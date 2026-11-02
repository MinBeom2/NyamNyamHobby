package com.ssafy.nyamnyam.domain.notification;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.event.CommentCreatedEvent;
import com.ssafy.nyamnyam.domain.notification.event.CommentNotificationPublisher;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 댓글 알림 대상 규칙과 큐 발행·실패 시 직접 저장 */
@ExtendWith(MockitoExtension.class)
class CommentNotificationPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock NotificationService notificationService;
    @InjectMocks CommentNotificationPublisher publisher;

    private static final long AUTHOR = 1L, COMMENTER = 2L, OTHER = 3L;

    private static CommentCreatedEvent event(Long commenter, Long replyTarget) {
        return new CommentCreatedEvent(500L, 100L, "암벽화 추천", AUTHOR, commenter, "산토끼", replyTarget, "  저는\n라스포르티바 추천해요  ");
    }

    private List<NotificationMessage> sent(CommentCreatedEvent event) {
        publisher.onCommentCreated(event);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, atLeast(0))
                .convertAndSend(eq(RabbitConfig.EXCHANGE_NAME), eq(RabbitConfig.NOTIFICATION_ROUTING_KEY), captor.capture());
        List<NotificationMessage> messages = new ArrayList<>();
        captor.getAllValues().forEach(v -> messages.add((NotificationMessage) v));
        return messages;
    }

    @Test
    @DisplayName("남의 글에 댓글: 글 작성자에게 COMMENT 한 건, 링크·중복키·미리보기")
    void commentNotifiesPostAuthor() {
        List<NotificationMessage> messages = sent(event(COMMENTER, null));

        assertThat(messages).hasSize(1);
        NotificationMessage m = messages.get(0);
        assertThat(m.userId()).isEqualTo(AUTHOR);
        assertThat(m.type()).isEqualTo(NotificationType.COMMENT);
        assertThat(m.title()).isEqualTo("'암벽화 추천'에 댓글이 달렸어요");
        assertThat(m.body()).isEqualTo("산토끼: 저는 라스포르티바 추천해요");
        assertThat(m.linkPath()).isEqualTo("/community/100");
        assertThat(m.dedupKey()).isEqualTo("COMMENT:500");
    }

    @Test
    @DisplayName("내 글에 내가 단 댓글은 알리지 않는다")
    void ownCommentIsSilent() {
        assertThat(sent(event(AUTHOR, null))).isEmpty();
    }

    @Test
    @DisplayName("다른 사람 댓글에 답글: 대상에게 REPLY, 글 작성자에게 COMMENT")
    void replyNotifiesTargetAndAuthor() {
        List<NotificationMessage> messages = sent(event(COMMENTER, OTHER));

        assertThat(messages).extracting(NotificationMessage::userId, NotificationMessage::type, NotificationMessage::dedupKey)
                .containsExactly(
                        tuple(OTHER, NotificationType.REPLY, "REPLY:500"),
                        tuple(AUTHOR, NotificationType.COMMENT, "COMMENT:500"));
    }

    @Test
    @DisplayName("글 작성자 댓글에 답글: 작성자에게 REPLY 한 건만(중복 안 보냄)")
    void replyToAuthorSendsOnce() {
        List<NotificationMessage> messages = sent(event(COMMENTER, AUTHOR));

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).type()).isEqualTo(NotificationType.REPLY);
        assertThat(messages.get(0).userId()).isEqualTo(AUTHOR);
    }

    @Test
    @DisplayName("글 작성자가 다른 사람 댓글에 답글: 대상에게 REPLY만")
    void authorRepliesNotifiesTargetOnly() {
        List<NotificationMessage> messages = sent(event(AUTHOR, OTHER));

        assertThat(messages).extracting(NotificationMessage::userId).containsExactly(OTHER);
    }

    @Test
    @DisplayName("브로커에 못 보내면 그 자리에서 바로 저장한다")
    void fallsBackToDirectSaveWhenBrokerDown() {
        willThrow(new AmqpConnectException(new ConnectException("down")))
                .given(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE_NAME), eq(RabbitConfig.NOTIFICATION_ROUTING_KEY), any(Object.class));

        publisher.onCommentCreated(event(COMMENTER, null));

        verify(notificationService).create(any(NotificationMessage.class));
    }

    @Test
    @DisplayName("보낼 대상이 없으면 브로커도 저장도 부르지 않는다")
    void nothingToSend() {
        publisher.onCommentCreated(event(AUTHOR, AUTHOR));

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        verify(notificationService, never()).create(any());
    }

    @Test
    @DisplayName("제목·본문은 컬럼 길이(100·200)를 넘으면 잘린다")
    void messageIsTrimmedToColumnLength() {
        NotificationMessage m = new NotificationMessage(1L, NotificationType.COMMENT, "가".repeat(150), "나".repeat(300), "/x", "K");
        assertThat(m.title()).hasSize(100).endsWith("…");
        assertThat(m.body()).hasSize(200).endsWith("…");
    }
}
