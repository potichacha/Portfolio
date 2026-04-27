package com.portloko.user;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public user profile returned by GET /v1/me and GET /v1/users/{handle}.
 * Fields match BACK-005 acceptance criteria.
 */
public record UserResponse(
        UUID id,
        String handle,
        String email,
        @JsonProperty("avatar_url") String avatarUrl,
        String bio,
        @JsonProperty("github_login") String githubLogin,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    static UserResponse from(User user) {
        return new UserResponse(
                user.id,
                user.handle,
                user.email,
                user.avatarUrl,
                user.bio,
                user.githubLogin,
                user.createdAt
        );
    }
}
