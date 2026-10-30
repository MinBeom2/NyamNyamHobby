package com.ssafy.nyamnyam.domain.plan.dto;

import jakarta.validation.constraints.*;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CreatePlanItemRequest(
        @JsonDeserialize(using = PlanIntegerDeserializer.class) @NotNull @Min(1) @Max(32767) Integer weekNo,
        @NotBlank @Size(max = 50) String title,
        @Size(max = 500) String description,
        @JsonDeserialize(using = PlanIntegerDeserializer.class) @NotNull @Min(1) @Max(1440) Integer expectedMinutes,
        @Size(max = 200) String supplies,
        @Size(max = 200) String doneCriteria) {}
