package com.ssafy.nyamnyam.domain.plan.dto;

import com.ssafy.nyamnyam.domain.plan.entity.UserPlanItem;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public final class PlanResponses {
    private PlanResponses() {}
    public static OffsetDateTime time(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));
    }

    public record Progress(int done, int skipped, int total, double rate) {
        public boolean completed() { return total > 0 && done + skipped == total; }
    }
    public record Item(Long itemId, int weekNo, int seq, String title, String description,
                       int expectedMinutes, String supplies, String doneCriteria, String status,
                       String memo, boolean isCustom, OffsetDateTime completedAt) {
        public static Item from(UserPlanItem item) {
            return new Item(item.getId(), item.getWeekNo(), item.getSeq(), item.getTitle(), item.getDescription(),
                    item.getExpectedMinutes(), item.getSupplies(), item.getDoneCriteria(), item.getStatus().name(),
                    item.getMemo(), item.isCustom(), time(item.getCompletedAt()));
        }
    }
    public record Week(int weekNo, String title, String status, boolean current,
                       Progress progress, int expectedMinutes, int overdueCount,
                       boolean weekCompleted, boolean canUpdateItems, boolean canEditPlan) {}

    public record View(Long planId, Long userHobbyId, Long hobbyId, String title, String hobbyStatus,
                       String planStatus, String skillLevel, OffsetDateTime startedAt,
                       int totalWeeks, int currentWeek, Integer selectedWeek, String weekTitle,
                       int weekExpectedMinutes, Progress progress, Progress planProgress, Progress weekProgress,
                       int overdueCount, boolean canEditPlan, boolean canUpdateItems, boolean weekCompleted,
                       Integer nextWeek, boolean planCompleted, List<Week> weeks, List<Item> items,
                       List<Item> overdueItems, List<Item> skippedItems, List<String> supplies) {}

    public record Mutation(Long itemId, String status, String memo, OffsetDateTime completedAt, Item item,
                           Progress progress, Progress planProgress, Progress weekProgress,
                           boolean weekCompleted, Integer nextWeek, boolean planCompleted) {}

    public record Started(Long userHobbyId, Long planId, String status, String title) {}
    public record HobbyState(Long userHobbyId, Long planId, String status, int currentWeek,
                             OffsetDateTime startedAt, OffsetDateTime pausedAt) {}
    public record Summary(int remainingThisWeekCount, int overdueCount, int inProgressCount,
                          int maxInProgressCount, Map<String, Long> statusCounts, List<UserHobbyResponse> plans) {}
}
