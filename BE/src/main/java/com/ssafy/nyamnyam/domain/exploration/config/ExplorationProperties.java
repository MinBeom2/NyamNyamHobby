package com.ssafy.nyamnyam.domain.exploration.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "exploration")
public class ExplorationProperties {

    @Min(1)
    @Max(Short.MAX_VALUE)
    private int sessionSize = 12;
}
