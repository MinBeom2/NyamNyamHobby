package com.ssafy.nyamnyam.domain.post.entity;

/** ACTIVE 노출 · HIDDEN 신고 등으로 숨김 · DELETED 작성자 삭제(soft delete) */
public enum PostStatus {
    ACTIVE,
    HIDDEN,
    DELETED
}
