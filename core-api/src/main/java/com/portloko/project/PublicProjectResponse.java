package com.portloko.project;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicProjectResponse(
        UUID id,
        String title,
        @JsonProperty("live_url") String liveUrl,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    public static PublicProjectResponse from(Project project) {
        return new PublicProjectResponse(
                project.id,
                project.title,
                project.liveUrl,
                project.createdAt
        );
    }
}
