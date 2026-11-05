package com.ssafy.nyamnyam.domain.place.collect;

import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;

/**
 * 카카오 로컬 API의 카테고리 정보(category_name)를 서비스 도메인 PlaceCategory로 매핑하는 유틸
 */
public final class PlaceCategoryClassifier {

    private PlaceCategoryClassifier() {
    }

    public static PlaceCategory classify(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return PlaceCategory.ETC;
        }
        String text = categoryName.toLowerCase();

        if (text.contains("공원") || text.contains("휴양림") || text.contains("캠핑장") || text.contains("산책로") || text.contains("자연")) {
            return PlaceCategory.PARK;
        }
        if (text.contains("헬스") || text.contains("피트니스") || text.contains("암벽등반") || text.contains("클라이밍")) {
            return PlaceCategory.GYM;
        }
        if (text.contains("공방") || text.contains("스튜디오") || text.contains("요가") || text.contains("필라테스") || text.contains("연습실") || text.contains("도예")) {
            return PlaceCategory.STUDIO;
        }
        if (text.contains("학원") || text.contains("클래스") || text.contains("문화센터") || text.contains("교육")) {
            return PlaceCategory.CLASS;
        }
        if (text.contains("스포츠") || text.contains("수영") || text.contains("테니스") || text.contains("배드민턴")
                || text.contains("볼링") || text.contains("체육관") || text.contains("운동장") || text.contains("골프")) {
            return PlaceCategory.SPORTS;
        }
        return PlaceCategory.ETC;
    }
}

