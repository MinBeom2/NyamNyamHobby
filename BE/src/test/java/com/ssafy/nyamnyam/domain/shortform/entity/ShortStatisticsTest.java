package com.ssafy.nyamnyam.domain.shortform.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ShortStatisticsTest {
    @ParameterizedTest
    @CsvSource({"4,0,4,ACTIVE", "25,0,4,ACTIVE", "25,0,5,AUTO_HIDDEN",
            "26,0,5,ACTIVE", "26,0,6,AUTO_HIDDEN", "3,2,0,ACTIVE", "3,3,0,UNAVAILABLE",
            "25,3,5,UNAVAILABLE", "0,0,5,ACTIVE"})
    void appliesUserCountAndRatioBoundaries(long views, long failures, long irrelevant, ShortStatus expected) {
        Short shortform = shortform(ShortStatus.ACTIVE);
        shortform.updateStatistics(views, failures, irrelevant);
        assertThat(shortform.getStatus()).isEqualTo(expected);
        assertThat(shortform.getViewUserCount()).isEqualTo((int) views);
        assertThat(shortform.getFailUserCount()).isEqualTo((int) failures);
        assertThat(shortform.getIrrelevantCount()).isEqualTo((int) irrelevant);
    }

    @ParameterizedTest
    @EnumSource(value = ShortStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void doesNotOverrideInactiveStatesOrAutomaticallyRestore(ShortStatus status) {
        Short shortform = shortform(status);
        shortform.updateStatistics(25, 3, 5);
        assertThat(shortform.getStatus()).isEqualTo(status);
        shortform.updateStatistics(25, 0, 0);
        assertThat(shortform.getStatus()).isEqualTo(status);
    }

    @Test void comparesRatiosWithoutIntegerOverflow() {
        Short shortform = shortform(ShortStatus.ACTIVE);
        shortform.updateStatistics(Integer.MAX_VALUE, 0, 500_000_000);
        assertThat(shortform.getStatus()).isEqualTo(ShortStatus.AUTO_HIDDEN);
    }

    private Short shortform(ShortStatus status) {
        return Short.builder().youtubeVideoId("stats001").title("영상").status(status).build();
    }
}
