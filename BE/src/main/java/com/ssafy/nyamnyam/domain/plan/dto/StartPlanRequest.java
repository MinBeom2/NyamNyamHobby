package com.ssafy.nyamnyam.domain.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StartPlanRequest(
        @NotNull @Pattern(regexp = "BEGINNER|NOVICE|INTERMEDIATE") String skillLevel) {}
