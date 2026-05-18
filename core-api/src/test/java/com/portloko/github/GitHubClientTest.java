package com.portloko.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubClientTest {

    private static final String TOKEN = "gho_test-token";

    private final Map<String, Queue<StubResponse>> stubs = new ConcurrentHashMap<>();
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private GitHubClient client;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();
        client = newClient(1024);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void listRepositories_paginatesWithOneHundredPerPageAndFiltersPushPermission() {
        stubJson(
                "/user/repos?per_page=100&page=1",
                200,
                Map.of("Link", List.of("<" + baseUrl() + "/user/repos?per_page=100&page=2>; rel=\"next\"")),
                """
                        [
                          {
                            "name": "deployable",
                            "full_name": "octo/deployable",
                            "html_url": "https://github.com/octo/deployable",
                            "language": "Java",
                            "description": "Ready to ship",
                            "default_branch": "main",
                            "permissions": { "push": true }
                          },
                          {
                            "name": "readonly",
                            "full_name": "octo/readonly",
                            "html_url": "https://github.com/octo/readonly",
                            "language": "TypeScript",
                            "description": "Read only",
                            "default_branch": "main",
                            "permissions": { "push": false }
                          }
                        ]
                        """
        );
        stubJson(
                "/user/repos?per_page=100&page=2",
                200,
                """
                        [
                          {
                            "name": "portfolio",
                            "full_name": "octo/portfolio",
                            "html_url": "https://github.com/octo/portfolio",
                            "language": "Vue",
                            "description": null,
                            "default_branch": "develop",
                            "permissions": { "push": true }
                          }
                        ]
                        """
        );

        List<GitHubRepositoryResponse> repositories = client.listRepositories(TOKEN);

        assertThat(repositories).hasSize(2);
        assertThat(repositories).extracting(GitHubRepositoryResponse::name)
                .containsExactly("deployable", "portfolio");
        assertThat(repositories.getFirst().fullName()).isEqualTo("octo/deployable");
        assertThat(repositories.getFirst().htmlUrl()).isEqualTo("https://github.com/octo/deployable");
        assertThat(repositories.getFirst().language()).isEqualTo("Java");
        assertThat(repositories.getFirst().description()).isEqualTo("Ready to ship");
        assertThat(repositories.getFirst().defaultBranch()).isEqualTo("main");
        assertThat(requests).extracting(RecordedRequest::pathAndQuery)
                .containsExactly("/user/repos?per_page=100&page=1", "/user/repos?per_page=100&page=2");
        assertThat(requests).allSatisfy(request -> assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN));
    }

    @Test
    void resolveCommitSha_usesHeadsRefAndReturnsFullSha() {
        String sha = "a".repeat(40);
        stubJson(
                "/repos/octo/demo/git/ref/heads/feature/deploy",
                200,
                """
                        {
                          "object": {
                            "type": "commit",
                            "sha": "%s"
                          }
                        }
                        """.formatted(sha)
        );

        String resolvedSha = client.resolveCommitSha(TOKEN, "octo", "demo", "feature/deploy");

        assertThat(resolvedSha).isEqualTo(sha);
        assertThat(requests).extracting(RecordedRequest::pathAndQuery)
                .containsExactly("/repos/octo/demo/git/ref/heads/feature/deploy");
    }

    @Test
    void resolveCommitSha_fallsBackToTagAndDereferencesAnnotatedTag() {
        String tagSha = "b".repeat(40);
        String commitSha = "c".repeat(40);
        stubJson("/repos/octo/demo/git/ref/heads/v1.0.0", 404, "{}");
        stubJson(
                "/repos/octo/demo/git/ref/tags/v1.0.0",
                200,
                """
                        {
                          "object": {
                            "type": "tag",
                            "sha": "%s"
                          }
                        }
                        """.formatted(tagSha)
        );
        stubJson(
                "/repos/octo/demo/git/tags/%s".formatted(tagSha),
                200,
                """
                        {
                          "object": {
                            "type": "commit",
                            "sha": "%s"
                          }
                        }
                        """.formatted(commitSha)
        );

        String resolvedSha = client.resolveCommitSha(TOKEN, "octo", "demo", "v1.0.0");

        assertThat(resolvedSha).isEqualTo(commitSha);
        assertThat(requests).extracting(RecordedRequest::pathAndQuery)
                .containsExactly(
                        "/repos/octo/demo/git/ref/heads/v1.0.0",
                        "/repos/octo/demo/git/ref/tags/v1.0.0",
                        "/repos/octo/demo/git/tags/%s".formatted(tagSha)
                );
    }

    @Test
    void resolveCommitSha_throwsNotFoundWhenBranchAndTagAreMissing() {
        stubJson("/repos/octo/demo/git/ref/heads/missing", 404, "{}");
        stubJson("/repos/octo/demo/git/ref/tags/missing", 404, "{}");

        assertThatThrownBy(() -> client.resolveCommitSha(TOKEN, "octo", "demo", "missing"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(exception -> {
                    GitHubApiException githubException = (GitHubApiException) exception;
                    assertThat(githubException.statusCode()).isEqualTo(404);
                    assertThat(githubException.code()).isEqualTo("GITHUB_REF_NOT_FOUND");
                });
    }

    @Test
    void resolveCommitSha_throwsForbiddenWhenRepositoryIsInaccessible() {
        stubJson("/repos/octo/private/git/ref/heads/main", 403, "{}");

        assertThatThrownBy(() -> client.resolveCommitSha(TOKEN, "octo", "private", "main"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(exception -> {
                    GitHubApiException githubException = (GitHubApiException) exception;
                    assertThat(githubException.statusCode()).isEqualTo(403);
                    assertThat(githubException.code()).isEqualTo("GITHUB_FORBIDDEN");
                });
        assertThat(requests).extracting(RecordedRequest::pathAndQuery)
                .containsExactly("/repos/octo/private/git/ref/heads/main");
    }

    @Test
    void downloadTarball_storesArchiveAndCleanupDeletesIt() throws IOException {
        String sha = "d".repeat(40);
        byte[] archiveBytes = "tarball-content".getBytes(StandardCharsets.UTF_8);
        stubBinary("/repos/octo/demo/tarball/" + sha, 200, archiveBytes);

        GitHubArchive archive = client.downloadTarball(TOKEN, "octo", "demo", sha);

        assertThat(archive.owner()).isEqualTo("octo");
        assertThat(archive.repository()).isEqualTo("demo");
        assertThat(archive.sha()).isEqualTo(sha);
        assertThat(archive.sizeBytes()).isEqualTo(archiveBytes.length);
        assertThat(archive.path()).exists();
        assertThat(Files.readAllBytes(archive.path())).isEqualTo(archiveBytes);

        client.cleanupArchive(archive);

        assertThat(archive.path()).doesNotExist();
    }

    @Test
    void downloadTarball_deletesPartialFileWhenArchiveIsTooLarge() throws IOException {
        String sha = "e".repeat(40);
        client = newClient(4);
        stubBinary("/repos/octo/demo/tarball/" + sha, 200, "12345".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> client.downloadTarball(TOKEN, "octo", "demo", sha))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(exception -> {
                    GitHubApiException githubException = (GitHubApiException) exception;
                    assertThat(githubException.statusCode()).isEqualTo(413);
                    assertThat(githubException.code()).isEqualTo("GITHUB_ARCHIVE_TOO_LARGE");
                });
        try (var paths = Files.list(tempDir)) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    @Test
    void downloadTarball_throwsForbiddenForPrivateRepositoryWithoutScope() throws IOException {
        String sha = "f".repeat(40);
        stubJson("/repos/octo/private/tarball/" + sha, 403, "{}");

        assertThatThrownBy(() -> client.downloadTarball(TOKEN, "octo", "private", sha))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(exception -> {
                    GitHubApiException githubException = (GitHubApiException) exception;
                    assertThat(githubException.statusCode()).isEqualTo(403);
                    assertThat(githubException.code()).isEqualTo("GITHUB_FORBIDDEN");
                });
        try (var paths = Files.list(tempDir)) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    private GitHubClient newClient(long maxArchiveBytes) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new GitHubClient(httpClient, new ObjectMapper(), baseUrl(), tempDir, maxArchiveBytes);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void stubJson(String pathAndQuery, int statusCode, String body) {
        stubJson(pathAndQuery, statusCode, Map.of(), body);
    }

    private void stubJson(String pathAndQuery, int statusCode, Map<String, List<String>> headers, String body) {
        stub(pathAndQuery, statusCode, withContentType(headers, "application/json"), body.getBytes(StandardCharsets.UTF_8));
    }

    private void stubBinary(String pathAndQuery, int statusCode, byte[] body) {
        stub(pathAndQuery, statusCode, Map.of("Content-Type", List.of("application/octet-stream")), body);
    }

    private void stub(String pathAndQuery, int statusCode, Map<String, List<String>> headers, byte[] body) {
        stubs.computeIfAbsent(pathAndQuery, ignored -> new ArrayDeque<>())
                .add(new StubResponse(statusCode, headers, body));
    }

    private Map<String, List<String>> withContentType(Map<String, List<String>> headers, String contentType) {
        if (headers.containsKey("Content-Type")) {
            return headers;
        }
        Map<String, List<String>> merged = new ConcurrentHashMap<>(headers);
        merged.put("Content-Type", List.of(contentType));
        return merged;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String pathAndQuery = exchange.getRequestURI().getRawPath();
        if (exchange.getRequestURI().getRawQuery() != null) {
            pathAndQuery += "?" + exchange.getRequestURI().getRawQuery();
        }
        requests.add(new RecordedRequest(
                exchange.getRequestMethod(),
                pathAndQuery,
                exchange.getRequestHeaders().getFirst("Authorization")
        ));

        StubResponse response = stubs.getOrDefault(pathAndQuery, new ArrayDeque<>()).poll();
        if (response == null) {
            response = new StubResponse(500, Map.of("Content-Type", List.of("text/plain")), ("No stub for " + pathAndQuery).getBytes(StandardCharsets.UTF_8));
        }

        response.headers().forEach((name, values) -> values.forEach(value -> exchange.getResponseHeaders().add(name, value)));
        exchange.sendResponseHeaders(response.statusCode(), response.body().length);
        try (OutputStream responseBody = exchange.getResponseBody()) {
            responseBody.write(response.body());
        }
    }

    record StubResponse(int statusCode, Map<String, List<String>> headers, byte[] body) {}

    record RecordedRequest(String method, String pathAndQuery, String authorization) {}
}
