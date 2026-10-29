package com.ssafy.nyamnyam.domain.exploration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ExplorationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void defaultsToTwelve() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExplorationProperties.class).getSessionSize()).isEqualTo(12);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 32767})
    void bindsConfiguredSessionSize(int sessionSize) {
        contextRunner.withPropertyValues("exploration.session-size=" + sessionSize).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ExplorationProperties.class).getSessionSize()).isEqualTo(sessionSize);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 32768})
    void rejectsSizesOutsideDatabaseSmallintRange(int sessionSize) {
        contextRunner.withPropertyValues("exploration.session-size=" + sessionSize).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(BindValidationException.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ExplorationProperties.class)
    static class TestConfiguration {
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.1", "NaN"})
    void rejectsInvalidPreferenceRatio(String ratio) {
        contextRunner.withPropertyValues("exploration.prefer-ratio="+ratio).run(context->assertThat(context).hasFailed());
    }
}
