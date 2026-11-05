package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCategoryClassifierTest {

    @ParameterizedTest
    @DisplayName("카카오 카테고리 문자열에 따라 알맞은 PlaceCategory로 분류한다")
    @CsvSource({
            "'스포츠,레저 > 스포츠시설 > 암벽등반', GYM",
            "'스포츠,레저 > 스포츠시설 > 헬스클럽', GYM",
            "'가정,생활 > 여가시설 > 도예공방', STUDIO",
            "'스포츠,레저 > 스포츠시설 > 요가,필라테스', STUDIO",
            "'교육,학문 > 학원 > 미술학원', CLASS",
            "'교육,학문 > 학원 > 요리학원', CLASS",
            "'스포츠,레저 > 스포츠시설 > 수영장', SPORTS",
            "'스포츠,레저 > 스포츠시설 > 배드민턴장', SPORTS",
            "'여행 > 공원 > 도시근린공원', PARK",
            "'음식점 > 카페 > 보드게임카페', ETC",
            "'', ETC"
    })
    void classify(String categoryName, PlaceCategory expected) {
        PlaceCategory result = PlaceCategoryClassifier.classify(categoryName);
        assertThat(result).isEqualTo(expected);
    }
}

