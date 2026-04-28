package com.portloko.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.portloko.project.PublicProjectResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record PublicUserResponse(
        String handle,
        @JsonProperty("avatar_url") String avatarUrl,
        String bio,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        List<PublicProjectResponse> projects
) {}
