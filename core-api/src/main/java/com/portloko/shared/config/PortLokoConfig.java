package com.portloko.shared.config;

import io.smallrye.config.ConfigMapping;
import jakarta.validation.constraints.NotBlank;

/**
 * Typed, validated application configuration (BACK-1).
 *
 * <p>Values are bound from environment variables at startup. Quarkus validates
 * every {@code @NotBlank} field eagerly, so a missing required variable makes
 * the application fail to boot with a clear message instead of failing later at
 * runtime.
 *
 * <p>Mapping (env var → property):
 * <ul>
 *   <li>{@code GITHUB_CLIENT_ID}     → {@code portloko.github.client-id}</li>
 *   <li>{@code GITHUB_CLIENT_SECRET} → {@code portloko.github.client-secret}</li>
 *   <li>{@code CLAUDE_API_KEY}       → {@code portloko.claude.api-key}</li>
 *   <li>{@code RUNNER_URL}           → {@code portloko.runner.url}</li>
 * </ul>
 */
@ConfigMapping(prefix = "portloko")
public interface PortLokoConfig {

    GitHub github();

    Claude claude();

    Runner runner();

    interface GitHub {
        @NotBlank
        String clientId();

        @NotBlank
        String clientSecret();
    }

    interface Claude {
        @NotBlank
        String apiKey();
    }

    interface Runner {
        @NotBlank
        String url();
    }
}
