package com.portloko.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class GitHubClient {

    private static final int REPOSITORIES_PER_PAGE = 100;
    private static final TypeReference<List<RepositoryPayload>> REPOSITORY_LIST_TYPE = new TypeReference<>() {};

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "github.api.base-url", defaultValue = "https://api.github.com")
    String apiBaseUrl;

    @ConfigProperty(name = "github.archive.temp-dir", defaultValue = "")
    Optional<String> archiveTempDir;

    @ConfigProperty(name = "github.archive.max-bytes", defaultValue = "52428800")
    long maxArchiveBytes;

    private HttpClient httpClient;
    private Path archiveDirectory;

    public GitHubClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    GitHubClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    GitHubClient(HttpClient httpClient, ObjectMapper objectMapper, String apiBaseUrl, Path archiveDirectory, long maxArchiveBytes) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = normalizeBaseUrl(apiBaseUrl);
        this.archiveDirectory = archiveDirectory;
        this.maxArchiveBytes = maxArchiveBytes;
    }

    @PostConstruct
    void init() {
        apiBaseUrl = normalizeBaseUrl(apiBaseUrl);
        archiveDirectory = archiveTempDir
                .filter(value -> !value.isBlank())
                .map(Path::of)
                .orElseGet(() -> Path.of(System.getProperty("java.io.tmpdir"), "portloko-github-archives"));
    }

    public List<GitHubRepositoryResponse> listRepositories(String oauthToken) {
        validateToken(oauthToken);

        List<GitHubRepositoryResponse> repositories = new ArrayList<>();
        int page = 1;
        boolean hasNextPage;

        do {
            HttpResponse<String> response = sendJson(request("/user/repos?per_page=" + REPOSITORIES_PER_PAGE + "&page=" + page, oauthToken)
                    .GET()
                    .build());
            ensureSuccess(response, "Unable to list GitHub repositories");

            List<RepositoryPayload> payloads = readRepositoryPayloads(response.body());
            payloads.stream()
                    .filter(RepositoryPayload::canPush)
                    .map(RepositoryPayload::toResponse)
                    .forEach(repositories::add);

            hasNextPage = hasNextPage(response) || payloads.size() == REPOSITORIES_PER_PAGE;
            page++;
        } while (hasNextPage);

        return repositories;
    }

    public String resolveCommitSha(String oauthToken, String owner, String repository, String ref) {
        validateToken(oauthToken);
        validateOwnerRepositoryAndRef(owner, repository, ref);

        GitReference reference = getReference(oauthToken, owner, repository, "heads", ref);
        if (reference == null) {
            reference = getReference(oauthToken, owner, repository, "tags", ref);
        }
        if (reference == null) {
            throw new GitHubApiException("GitHub branch or tag not found", 404, "GITHUB_REF_NOT_FOUND");
        }

        if ("tag".equals(reference.object().type())) {
            GitTag tag = getTag(oauthToken, owner, repository, reference.object().sha());
            return validateCommitSha(tag.object().sha());
        }

        return validateCommitSha(reference.object().sha());
    }

    public GitHubArchive downloadTarball(String oauthToken, String owner, String repository, String sha) {
        validateToken(oauthToken);
        validateOwnerRepositoryAndRef(owner, repository, sha);
        validateCommitSha(sha);

        try {
            Files.createDirectories(archiveDirectory);
            String prefix = safeFilePart(owner) + "-" + safeFilePart(repository) + "-" + sha.substring(0, 12) + "-";
            Path archivePath = Files.createTempFile(archiveDirectory, prefix, ".tar.gz");
            long sizeBytes = downloadTarballTo(oauthToken, owner, repository, sha, archivePath);
            return new GitHubArchive(archivePath, owner, repository, sha, sizeBytes);
        } catch (GitHubApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new GitHubApiException("Unable to store GitHub archive", 500, "GITHUB_ARCHIVE_STORAGE_ERROR");
        }
    }

    public void cleanupArchive(GitHubArchive archive) {
        if (archive == null || archive.path() == null) {
            return;
        }
        try {
            Files.deleteIfExists(archive.path());
        } catch (IOException exception) {
            throw new GitHubApiException("Unable to clean GitHub archive", 500, "GITHUB_ARCHIVE_CLEANUP_ERROR");
        }
    }

    private long downloadTarballTo(String oauthToken, String owner, String repository, String sha, Path archivePath) throws IOException {
        HttpRequest request = request("/repos/" + encodePathSegment(owner)
                + "/" + encodePathSegment(repository)
                + "/tarball/" + encodePathSegment(sha), oauthToken)
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                closeQuietly(response.body());
                Files.deleteIfExists(archivePath);
                throw mapError(response.statusCode(), "Unable to download GitHub archive");
            }

            long totalBytes = copyArchive(response.body(), archivePath);
            if (totalBytes > maxArchiveBytes) {
                Files.deleteIfExists(archivePath);
                throw new GitHubApiException("GitHub archive is too large", 413, "GITHUB_ARCHIVE_TOO_LARGE");
            }
            return totalBytes;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            Files.deleteIfExists(archivePath);
            throw new GitHubApiException("GitHub archive download interrupted", 500, "GITHUB_ARCHIVE_DOWNLOAD_INTERRUPTED");
        } catch (GitHubApiException exception) {
            Files.deleteIfExists(archivePath);
            throw exception;
        }
    }

    private long copyArchive(InputStream responseBody, Path archivePath) throws IOException {
        try (InputStream input = responseBody;
             var output = Files.newOutputStream(archivePath)) {
            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes <= maxArchiveBytes) {
                    output.write(buffer, 0, read);
                }
            }
            return totalBytes;
        }
    }

    private GitReference getReference(String oauthToken, String owner, String repository, String namespace, String ref) {
        HttpResponse<String> response = sendJson(request("/repos/" + encodePathSegment(owner)
                + "/" + encodePathSegment(repository)
                + "/git/ref/" + namespace + "/" + encodeGitRef(ref), oauthToken)
                .GET()
                .build());

        if (response.statusCode() == 404) {
            return null;
        }
        ensureSuccess(response, "Unable to resolve GitHub ref");
        return readValue(response.body(), GitReference.class);
    }

    private GitTag getTag(String oauthToken, String owner, String repository, String tagSha) {
        HttpResponse<String> response = sendJson(request("/repos/" + encodePathSegment(owner)
                + "/" + encodePathSegment(repository)
                + "/git/tags/" + encodePathSegment(tagSha), oauthToken)
                .GET()
                .build());
        ensureSuccess(response, "Unable to resolve GitHub tag");
        return readValue(response.body(), GitTag.class);
    }

    private HttpRequest.Builder request(String pathAndQuery, String oauthToken) {
        return HttpRequest.newBuilder(URI.create(apiBaseUrl + pathAndQuery))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + oauthToken)
                .header("User-Agent", "PortLoko-Core-API")
                .header("X-GitHub-Api-Version", "2022-11-28");
    }

    private HttpResponse<String> sendJson(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new GitHubApiException("Unable to reach GitHub API", 502, "GITHUB_API_UNAVAILABLE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitHubApiException("GitHub API request interrupted", 500, "GITHUB_API_INTERRUPTED");
        }
    }

    private List<RepositoryPayload> readRepositoryPayloads(String body) {
        try {
            return objectMapper.readValue(body, REPOSITORY_LIST_TYPE);
        } catch (IOException exception) {
            throw new GitHubApiException("Unable to parse GitHub repositories response", 502, "GITHUB_API_INVALID_RESPONSE");
        }
    }

    private <T> T readValue(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException exception) {
            throw new GitHubApiException("Unable to parse GitHub response", 502, "GITHUB_API_INVALID_RESPONSE");
        }
    }

    private void ensureSuccess(HttpResponse<?> response, String message) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw mapError(response.statusCode(), message);
        }
    }

    private GitHubApiException mapError(int statusCode, String message) {
        return switch (statusCode) {
            case 401 -> new GitHubApiException("GitHub token is invalid or expired", 401, "GITHUB_UNAUTHORIZED");
            case 403 -> new GitHubApiException("GitHub repository is inaccessible or the token lacks scope", 403, "GITHUB_FORBIDDEN");
            case 404 -> new GitHubApiException("GitHub resource not found", 404, "GITHUB_NOT_FOUND");
            case 413 -> new GitHubApiException("GitHub archive is too large", 413, "GITHUB_ARCHIVE_TOO_LARGE");
            default -> new GitHubApiException(message, statusCode, "GITHUB_API_ERROR");
        };
    }

    private boolean hasNextPage(HttpResponse<?> response) {
        return response.headers()
                .firstValue("Link")
                .map(link -> link.contains("rel=\"next\""))
                .orElse(false);
    }

    private String validateCommitSha(String sha) {
        if (sha == null || sha.length() != 40 || !sha.chars().allMatch(this::isHexDigit)) {
            throw new GitHubApiException("GitHub commit SHA is invalid", 502, "GITHUB_INVALID_SHA");
        }
        return sha.toLowerCase(Locale.ROOT);
    }

    private boolean isHexDigit(int character) {
        return (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
    }

    private void validateToken(String oauthToken) {
        if (oauthToken == null || oauthToken.isBlank()) {
            throw new GitHubApiException("GitHub OAuth token is required", 401, "GITHUB_TOKEN_REQUIRED");
        }
    }

    private void validateOwnerRepositoryAndRef(String owner, String repository, String ref) {
        if (owner == null || owner.isBlank() || repository == null || repository.isBlank() || ref == null || ref.isBlank()) {
            throw new GitHubApiException("GitHub owner, repository and reference are required", 400, "GITHUB_INVALID_REQUEST");
        }
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodeGitRef(String value) {
        return List.of(value.split("/", -1)).stream()
                .map(this::encodePathSegment)
                .reduce((left, right) -> left + "/" + right)
                .orElse("");
    }

    private String safeFilePart(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private void closeQuietly(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ignored) {
            // Best effort cleanup for error responses.
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RepositoryPayload(
            String name,
            @JsonProperty("full_name") String fullName,
            @JsonProperty("html_url") String htmlUrl,
            String language,
            String description,
            @JsonProperty("default_branch") String defaultBranch,
            RepositoryPermissions permissions
    ) {
        boolean canPush() {
            return permissions != null && permissions.push();
        }

        GitHubRepositoryResponse toResponse() {
            return new GitHubRepositoryResponse(name, fullName, htmlUrl, language, description, defaultBranch);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RepositoryPermissions(boolean push) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitReference(GitObject object) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitTag(GitObject object) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitObject(String sha, String type) {}
}
