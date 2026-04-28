package com.portloko.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class UserResourceTest {

    // Fixed UUID used as JWT principal in @TestSecurity
    static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Inject
    UserFixture userFixture;

    @BeforeEach
    void setUp() {
        // Appel via proxy CDI → @Transactional dans UserFixture est bien intercepté
        userFixture.createUser(UUID.fromString(TEST_USER_ID));
    }

    // -------------------------------------------------------------------------
    // Happy path — authenticated user gets their own profile
    // -------------------------------------------------------------------------

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void getMe_returnsProfileOfAuthenticatedUser() {
        given()
                .when().get("/v1/me")
                .then()
                .statusCode(200)
                .body("id", equalTo(TEST_USER_ID))
                .body("handle", equalTo("testdev"))
                .body("email", equalTo("testdev@example.com"))
                .body("github_login", equalTo("testdev"))
                .body("bio", equalTo("Full-stack developer"))
                .body("avatar_url", notNullValue())
                .body("created_at", notNullValue());
    }

    // -------------------------------------------------------------------------
    // 401 — no credentials at all
    // -------------------------------------------------------------------------

    @Test
    void getMe_withoutToken_returns401() {
        given()
                .when().get("/v1/me")
                .then()
                .statusCode(401);
    }
}
