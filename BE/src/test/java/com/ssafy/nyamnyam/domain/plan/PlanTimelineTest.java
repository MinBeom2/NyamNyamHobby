package com.ssafy.nyamnyam.domain.plan;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.ssafy.nyamnyam.domain.plan.PlanTestFixture.*;

class PlanTimelineTest {
    @Test void weekChangesAtSeoulMidnightAndClampsToLastWeek() {
        var entry = entry(user(1), hobby(1), 12);
        assertThat(entry.currentWeek(3, START.plusDays(7).minusSeconds(1))).isEqualTo(1);
        assertThat(entry.currentWeek(3, START.plusDays(7))).isEqualTo(2);
        assertThat(entry.currentWeek(3, START.plusYears(1))).isEqualTo(3);
    }

    @Test void repeatPauseAndResumePreserveSubDayDurationsAndStartTime() {
        var entry = entry(user(1), hobby(1), 12);
        var pause = START.plusDays(6).plusHours(23);
        entry.pause(pause);
        entry.pause(pause.plusDays(4));
        assertThat(entry.getPausedAt()).isEqualTo(pause);
        assertThat(entry.currentWeek(3, pause.plusDays(50))).isEqualTo(1);
        entry.resume(pause.plusHours(2));
        entry.resume(pause.plusHours(3));
        assertThat(entry.getPausedSeconds()).isEqualTo(7200);
        assertThat(entry.getStartedAt()).isEqualTo(START);
        assertThat(entry.currentWeek(3, pause.plusHours(2))).isEqualTo(1);
        assertThat(entry.currentWeek(3, pause.plusHours(3))).isEqualTo(2);
        entry.pause(pause.plusHours(3));
        entry.resume(pause.plusDays(3).plusHours(3));
        assertThat(entry.getPausedSeconds()).isEqualTo(3 * 86400 + 7200);
    }

    @Test void stoppingPausedPlanFreezesWeekAndRetainsHistory() {
        var entry = entry(user(1), hobby(1), 12);
        entry.pause(START.plusDays(8));
        entry.stop(START.plusDays(30));
        assertThat(entry.currentWeek(3, START.plusYears(1))).isEqualTo(2);
        var ended = entry.getEndedAt();
        entry.stop(START.plusYears(1));
        assertThat(entry.getEndedAt()).isEqualTo(ended);
    }
}
