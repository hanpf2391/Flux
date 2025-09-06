package com.flux.entropia.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Maps all custom application properties from application.yml under the 'flux' prefix.
 * This provides a type-safe way to access configuration.
 */
@Component
@ConfigurationProperties(prefix = "flux")
@Data
@Validated
public class FluxProperties {

    /**
     * Configuration for rate limiting.
     */
    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        /**
         * The duration in milliseconds for which a user (by IP) is blocked after a request.
         */
        @NotNull
        private Long durationMs;
    }
}
