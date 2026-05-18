package com.portloko.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryResponse(
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("html_url") String htmlUrl,
        String language,
        String description,
        @JsonProperty("default_branch") String defaultBranch
) {}
