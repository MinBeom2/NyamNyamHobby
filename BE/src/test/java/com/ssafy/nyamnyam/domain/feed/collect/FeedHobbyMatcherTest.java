package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbySourceMapping;
import com.ssafy.nyamnyam.domain.hobby.entity.SourceTarget;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedHobbyMatcherTest {

    private final Hobby boardGame = hobby(1L, "보드게임");
    private final Hobby puzzle = hobby(2L, "퍼즐");
    private final Hobby dance = hobby(3L, "K-팝 댄스");

    private final FeedHobbyMatcher matcher = new FeedHobbyMatcher(List.of(
            mapping(puzzle, "게임"), mapping(boardGame, "보드게임"), mapping(dance, "K-POP")));

    @Test void longerKeywordWins() {
        assertThat(matcher.match("서울 보드게임 페스티벌")).contains(boardGame);
        assertThat(matcher.match("레트로 게임 전시")).contains(puzzle);
    }

    @Test void ignoresSpacesAndCase() {
        assertThat(matcher.match("보드 게임 축제")).contains(boardGame);
        assertThat(matcher.match("k-pop 랜덤플레이댄스")).contains(dance);
    }

    @Test void unrelatedTitleHasNoHobby() {
        assertThat(matcher.match("가락 옥토버페스트")).isEmpty();
        assertThat(matcher.match(null)).isEmpty();
    }

    @Test void titleSimilarityTreatsNearlyEqualTitlesAsSame() {
        assertThat(FeedWriter.similarity("2026 서울 국제 보드게임 페스티벌", "2026 서울국제보드게임페스티벌")).isEqualTo(1.0);
        assertThat(FeedWriter.similarity("서울 보드게임 페스티벌", "서울 퍼즐 박람회")).isLessThan(0.8);
    }

    private static Hobby hobby(Long id, String name) {
        Hobby hobby = Hobby.builder().name(name).build();
        ReflectionTestUtils.setField(hobby, "id", id);
        return hobby;
    }

    private static HobbySourceMapping mapping(Hobby hobby, String keyword) {
        return HobbySourceMapping.builder().hobby(hobby).target(SourceTarget.EVENT_KEYWORD).keyword(keyword).build();
    }
}
