package com.portloko.auth;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;

/**
 * Tests pour l'endpoint GitHub OAuth login
 */
@QuarkusTest
public class GitHubAuthResourceTest {

    @BeforeEach
    public void setUp() {
        RestAssured.basePath = "/";
    }

    /**
     * Test: Le endpoint /v1/auth/github/login doit rediriger vers GitHub avec les bons paramètres
     */
    @Test
    public void testGitHubLoginRedirect() {
        given()
                .redirects().follow(false) // Don't follow redirects
                .when()
                .get("/v1/auth/github/login")
                .then()
                .statusCode(303) // See Other (redirect)
                .header("Location", containsString("github.com/login/oauth/authorize"))
                .header("Location", containsString("client_id="))
                .header("Location", containsString("scope="))
                .header("Location", containsString("state="))
                .header("Location", containsString("redirect_uri="));
    }

    /**
     * Test: Le state CSRF doit être présent et valide dans l'URL de redirection
     */
    @Test
    public void testCsrfStateIsGenerated() {
        String location = given()
                .when()
                .redirects()
                .follow(false)
                .get("/v1/auth/github/login")
                .then()
                .extract()
                .header("Location");

        // Vérifier que le state est présent et non vide
        String state = extractStateFromUrl(location);
        assert state != null && !state.isEmpty() : "State CSRF should not be null or empty";
        assert state.length() >= 32 : "State CSRF should have sufficient length";
    }

    /**
     * Test: L'URL de redirection doit contenir les bons scopes
     */
    @Test
    public void testGitHubScopeIsCorrect() {
        String location = given()
                .when()
                .redirects()
                .follow(false)
                .get("/v1/auth/github/login")
                .then()
                .extract()
                .header("Location");

        // Vérifier que les scopes sont corrects (read:user et repo)
        assert location.contains("scope=") : "Scope should be present in URL";
        // Les scopes doivent contenir read:user et repo
        assert location.contains("read%3Auser") || location.contains("read:user") : "Scope should contain read:user";
        assert location.contains("repo") : "Scope should contain repo";
    }

    /**
     * Test: L'URL doit être encodée correctement
     */
    @Test
    public void testUrlEncodingIsCorrect() {
        String location = given()
                .when()
                .redirects()
                .follow(false)
                .get("/v1/auth/github/login")
                .then()
                .extract()
                .header("Location");

        // Vérifier que l'URL commence par le bon domaine GitHub
        assert location.startsWith("https://github.com/login/oauth/authorize?") : "URL should start with GitHub authorize endpoint";
    }

    /**
     * Utilitaire pour extraire le state de l'URL
     */
    private String extractStateFromUrl(String url) {
        String[] params = url.split("[?&]");
        for (String param : params) {
            if (param.startsWith("state=")) {
                String state = param.substring("state=".length());
                try {
                    return URLDecoder.decode(state, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return state;
                }
            }
        }
        return null;
    }
}
