package com.ssafy.nyamnyam.domain.notification.event;

import com.ssafy.nyamnyam.domain.notification.dto.NotificationMessage;
import com.ssafy.nyamnyam.domain.notification.entity.NotificationType;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.global.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 댓글이 커밋된 뒤 알림 메시지를 만들어 RabbitMQ notification.create 큐로 보낸다(FR-NOTI-01 생성 대상 1).
 * <ul>
 *   <li>답글: 대상 댓글 작성자에게 REPLY</li>
 *   <li>글 작성자에게 COMMENT. 단, 이미 REPLY를 받는 사람이면 한 번만 보낸다</li>
 *   <li>자기 글·자기 댓글에 단 것은 알리지 않는다</li>
 * </ul>
 * 브로커에 보내지 못하면 알림이 사라지지 않도록 그 자리에서 바로 저장한다(dedup 키로 중복은 막힘).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationPublisher {

    static final int PREVIEW_LENGTH = 60;

    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        for (NotificationMessage message : messagesFor(event)) {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.NOTIFICATION_ROUTING_KEY, message);
            } catch (AmqpException e) {
                log.warn("[NOTIFICATION] publish failed, saving directly: key={}, error={}",
                        message.dedupKey(), e.getClass().getSimpleName());
                notificationService.create(message);
            }
        }
    }

    static List<NotificationMessage> messagesFor(CommentCreatedEvent e) {
        List<NotificationMessage> messages = new ArrayList<>();
        String link = "/community/" + e.postId();
        String preview = e.commenterNickname() + ": " + preview(e.content());

        Long replyTarget = e.replyTargetUserId();
        boolean notifyReply = replyTarget != null && !replyTarget.equals(e.commenterId());
        if (notifyReply) {
            messages.add(new NotificationMessage(replyTarget, NotificationType.REPLY,
                    "내 댓글에 답글이 달렸어요", preview, link, "REPLY:" + e.commentId()));
        }
        boolean notifyAuthor = e.postAuthorId() != null && !e.postAuthorId().equals(e.commenterId())
                && !(notifyReply && Objects.equals(e.postAuthorId(), replyTarget));
        if (notifyAuthor) {
            messages.add(new NotificationMessage(e.postAuthorId(), NotificationType.COMMENT,
                    "'" + e.postTitle() + "'에 댓글이 달렸어요", preview, link, "COMMENT:" + e.commentId()));
        }
        return messages;
    }

    private static String preview(String content) {
        String oneLine = content == null ? "" : content.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= PREVIEW_LENGTH ? oneLine : oneLine.substring(0, PREVIEW_LENGTH) + "…";
    }
}
