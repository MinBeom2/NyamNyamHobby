package com.ssafy.nyamnyam.domain.hobby.dto.request;

import com.ssafy.nyamnyam.domain.hobby.entity.Difficulty;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;

import java.util.Arrays;

/**
 * 취미 검색 조건(API 명세 3.4-1). 쿼리 문자열을 검증해 담는다.
 * 명세의 값("1H_LESS" 등)은 Java 식별자가 될 수 없어 각 enum이 code로 들고 {@code from}으로 바꾼다.
 */
public record HobbySearchCondition(
        String keyword,
        TimeRange time,
        CostRange cost,
        Difficulty difficulty,
        PlaceFilter place,
        SocialFilter social,
        SortType sort,
        int page,
        int size
) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final int MIN_KEYWORD_LENGTH = 2;

    /** 1회 소요 시간(분). 취미의 시간 범위가 구간과 겹치면 포함 */
    public enum TimeRange {
        H1_LESS("1H_LESS", 0, 60),
        H1_TO_2H("1H_TO_2H", 60, 120),
        H2_MORE("2H_MORE", 120, Integer.MAX_VALUE);

        private final String code;
        private final int min;
        private final int max;

        TimeRange(String code, int min, int max) {
            this.code = code;
            this.min = min;
            this.max = max;
        }

        public boolean overlaps(int hobbyMin, int hobbyMax) {
            return hobbyMin <= max && hobbyMax >= min;
        }

        static TimeRange from(String code) {
            return byCode(values(), t -> t.code, code, "time");
        }
    }

    /** 월 비용(원). 취미의 월 비용 범위가 구간과 겹치면 포함 */
    public enum CostRange {
        K30_LESS("30K_LESS", 0, 30_000),
        K30_TO_100K("30K_TO_100K", 30_000, 100_000),
        K100_MORE("100K_MORE", 100_000, Integer.MAX_VALUE);

        private final String code;
        private final int min;
        private final int max;

        CostRange(String code, int min, int max) {
            this.code = code;
            this.min = min;
            this.max = max;
        }

        public boolean overlaps(int hobbyMin, int hobbyMax) {
            return hobbyMin <= max && hobbyMax >= min;
        }

        static CostRange from(String code) {
            return byCode(values(), c -> c.code, code, "cost");
        }
    }

    /** 장소. ANY는 조건 없음, INDOOR/OUTDOOR는 BOTH 취미도 포함 */
    public enum PlaceFilter {
        INDOOR, OUTDOOR, ANY;

        static PlaceFilter from(String code) {
            return byCode(values(), Enum::name, code, "place");
        }
    }

    /** 참여 형태. ANY는 조건 없음, ALONE/TOGETHER는 BOTH 취미도 포함 */
    public enum SocialFilter {
        ALONE, TOGETHER, ANY;

        static SocialFilter from(String code) {
            return byCode(values(), Enum::name, code, "social");
        }
    }

    /** 정렬. RECOMMEND는 로그인 시 기본, POPULAR는 비회원 기본 */
    public enum SortType {
        RECOMMEND, POPULAR, NAME, COST_ASC;

        static SortType from(String code) {
            return byCode(values(), Enum::name, code, "sort");
        }
    }

    /** 컨트롤러가 받은 문자열을 검증해 조건으로 만든다. 잘못된 값은 400 COMMON_INVALID_INPUT */
    public static HobbySearchCondition of(String keyword, String time, String cost, String difficulty,
                                          String place, String social, String sort, Integer page, Integer size) {
        String trimmedKeyword = keyword == null ? null : keyword.strip();
        if (trimmedKeyword != null && trimmedKeyword.isEmpty()) {
            trimmedKeyword = null;
        }
        if (trimmedKeyword != null && trimmedKeyword.length() < MIN_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "검색어는 2자 이상이어야 합니다.");
        }
        int pageValue = page == null ? 0 : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;
        if (pageValue < 0 || sizeValue < 1 || sizeValue > MAX_SIZE) {
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "page는 0 이상, size는 1~" + MAX_SIZE + "이어야 합니다.");
        }
        return new HobbySearchCondition(
                trimmedKeyword,
                isBlank(time) ? null : TimeRange.from(time),
                isBlank(cost) ? null : CostRange.from(cost),
                isBlank(difficulty) ? null : byCode(Difficulty.values(), Enum::name, difficulty, "difficulty"),
                isBlank(place) ? PlaceFilter.ANY : PlaceFilter.from(place),
                isBlank(social) ? SocialFilter.ANY : SocialFilter.from(social),
                isBlank(sort) ? null : SortType.from(sort),
                pageValue,
                sizeValue
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <E extends Enum<E>> E byCode(E[] values, java.util.function.Function<E, String> code,
                                                 String input, String paramName) {
        return Arrays.stream(values)
                .filter(v -> code.apply(v).equalsIgnoreCase(input.strip()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_INVALID_INPUT,
                        paramName + " 값이 올바르지 않습니다: " + input));
    }
}
