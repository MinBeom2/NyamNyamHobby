package com.ssafy.nyamnyam.domain.notification.event;

/**
 * 댓글·답글이 저장됐다는 사실. CommentService가 트랜잭션 안에서 발행하고,
 * {@link CommentNotificationPublisher}가 커밋 뒤에 받아 알림 메시지로 바꾼다.
 *
 * @param postAuthorId      글 작성자
 * @param commenterId       댓글 작성자(본인에게는 알림을 보내지 않음)
 * @param replyTargetUserId 답글이면 답글을 단 대상 댓글의 작성자, 일반 댓글이면 null
 * @param content           저장 전 원문(HTML 이스케이프 전). 알림 본문 미리보기에 쓴다
 */
public record CommentCreatedEvent(
        Long commentId,
        Long postId,
        String postTitle,
        Long postAuthorId,
        Long commenterId,
        String commenterNickname,
        Long replyTargetUserId,
        String content
) {
}
