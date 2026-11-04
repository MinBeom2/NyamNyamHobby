package com.ssafy.nyamnyam.domain.feed.entity;

/**
 * 정보 피드를 어떤 취미 기준으로 모아 볼지.
 * <ul>
 *   <li>MY: 내 취미(마이페이지 '내 취미 선택', 최대 5개) + 진행 중·일시중지 취미. 회원 기본값</li>
 *   <li>SAVED: 취미 찾기에서 관심 저장한 취미(즐겨찾기)</li>
 *   <li>ALL: 전체 취미. 비회원은 항상 ALL</li>
 * </ul>
 */
public enum FeedScope {
    MY,
    SAVED,
    ALL
}
