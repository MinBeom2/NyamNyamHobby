package com.ssafy.nyamnyam.domain.recommendation.model;

/** 순서 변경 금지: V3 마이그레이션의 dimension_index와 일치한다. */
public enum TagFeature {
    INDOOR, OUTDOOR, BOTH_PLACES,
    ALONE, TOGETHER, BOTH_SOCIAL,
    ACTIVE, LIGHT_ACTIVITY, STATIC,
    SPORTS, NATURE, CRAFT, ART_PHOTO, MUSIC, COOKING, BOOK_WRITING, PLANTS, GAME_PUZZLE,
    HEALTH, ACHIEVEMENT, CREATION, RELAXATION, FOCUS, KNOWLEDGE;

    public double weight() {
        return ordinal() < 9 ? 1.0 : 2.0;
    }
}
