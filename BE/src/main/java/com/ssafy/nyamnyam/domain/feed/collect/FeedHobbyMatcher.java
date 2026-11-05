package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 행사 제목을 취미 행사 검색어(EVENT_KEYWORD)와 대조해 취미를 정한다.
 * 여러 검색어가 걸리면 더 긴 검색어가 이긴다('보드게임'이 '게임'보다, '캘리그라피'가 '그라피'보다 구체적).
 * 걸리는 검색어가 없으면 비어 있고, 수집기는 그 항목을 저장하지 않는다(GMS 분류는 FEED-B04).
 */
public class FeedHobbyMatcher {

    private record Keyword(String normalized, Hobby hobby) {
    }

    private final List<Keyword> keywords;

    public FeedHobbyMatcher(List<HobbySourceMapping> mappings) {
        this.keywords = mappings.stream()
                .map(m -> new Keyword(normalize(m.getKeyword()), m.getHobby()))
                .filter(k -> !k.normalized().isEmpty())
                .sorted(Comparator.comparingInt((Keyword k) -> k.normalized().length()).reversed())
                .toList();
    }

    public Optional<Hobby> match(String title) {
        if (title == null) return Optional.empty();
        String text = normalize(title);
        return keywords.stream().filter(k -> text.contains(k.normalized())).map(Keyword::hobby).findFirst();
    }

    public boolean isEmpty() {
        return keywords.isEmpty();
    }

    /** 공백·대소문자 차이를 없앤다('K-POP'과 'k-pop', '보드 게임'과 '보드게임') */
    static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
