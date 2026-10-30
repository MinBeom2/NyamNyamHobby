package com.ssafy.nyamnyam.domain.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record HobbyStatusRequest(
        @NotNull @Pattern(regexp = "IN_PROGRESS|PAUSED|STOPPED") String status) {}
