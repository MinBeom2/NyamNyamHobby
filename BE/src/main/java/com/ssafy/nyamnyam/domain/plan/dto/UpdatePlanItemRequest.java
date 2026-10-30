package com.ssafy.nyamnyam.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.ssafy.nyamnyam.domain.plan.entity.ItemStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Presence flags distinguish an omitted field from explicit JSON null. */
@Getter
public class UpdatePlanItemRequest {
    private ItemStatus status;
    @Size(max = 500) private String memo;
    @JsonDeserialize(using = PlanIntegerDeserializer.class)
    @Min(1) @Max(32767) private Integer weekNo;
    @JsonDeserialize(using = PlanIntegerDeserializer.class)
    @Min(1) @Max(1440) private Integer expectedMinutes;
    private boolean statusPresent;
    private boolean memoPresent;
    private boolean weekNoPresent;
    private boolean expectedMinutesPresent;

    @JsonSetter public void setStatus(ItemStatus value) { status = value; statusPresent = true; }
    @JsonSetter public void setMemo(String value) { memo = value; memoPresent = true; }
    @JsonSetter public void setWeekNo(Integer value) { weekNo = value; weekNoPresent = true; }
    @JsonSetter public void setExpectedMinutes(Integer value) { expectedMinutes = value; expectedMinutesPresent = true; }

    @AssertTrue(message = "변경할 필드를 지정하고 status/weekNo/expectedMinutes에 null을 보내지 마세요.")
    public boolean isValidPatch() {
        return (statusPresent || memoPresent || weekNoPresent || expectedMinutesPresent)
                && (!statusPresent || status != null) && (!weekNoPresent || weekNo != null)
                && (!expectedMinutesPresent || expectedMinutes != null);
    }
}
