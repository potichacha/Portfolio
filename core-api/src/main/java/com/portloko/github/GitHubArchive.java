package com.portloko.github;

import java.nio.file.Path;

public record GitHubArchive(
        Path path,
        String owner,
        String repository,
        String sha,
        long sizeBytes
) {}
