package com.ssafy.nyamnyam.domain.hobby.entity;

/** 데이터 매핑표(hobby_source_mappings)의 대상. 취미별로 어떤 검색어를 어디에 쓸지 구분한다. */
public enum SourceTarget {
    KAKAO_KEYWORD,
    PUBLIC_SPORTS,
    PUBLIC_PARK,
    EVENT_KEYWORD,
    SHORT_KEYWORD
}
