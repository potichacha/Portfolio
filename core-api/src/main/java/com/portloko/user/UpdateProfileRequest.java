package com.portloko.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateProfileRequest(
        String bio,
        String handle,
        @JsonProperty("avatar_url") String avatarUrl
) {}
