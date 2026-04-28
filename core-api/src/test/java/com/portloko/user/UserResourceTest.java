package com.portloko.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class UserResourceTest {

    static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Inject
    UserFixture userFixture;

    @BeforeEach
    void setUp() {
        userFixture.createUser(UUID.fromString(TEST_USER_ID));
    }

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

    @Test
    void getMe_withoutToken_returns401() {
        given()
                .when().get("/v1/me")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void updateProfile_updatesEditableFields() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "handle": "new-handle",
                          "bio": "Updated bio",
                          "avatar_url": "https://cdn.example.com/avatar.png"
                        }
                        """)
                .when().patch("/v1/me/profile")
                .then()
                .statusCode(200)
                .body("id", equalTo(TEST_USER_ID))
                .body("handle", equalTo("new-handle"))
                .body("bio", equalTo("Updated bio"))
                .body("avatar_url", equalTo("https://cdn.example.com/avatar.png"));
    }

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void updateProfile_withInvalidHandle_returns400() {
        given()
                .contentType("application/json")
                .body("""
                        { "handle": "no" }
                        """)
                .when().patch("/v1/me/profile")
                .then()
                .statusCode(400)
                .body("code", equalTo("BAD_REQUEST"))
                .body("error", equalTo("Handle must match ^[a-zA-Z0-9-]{3,39}$"));
    }

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void updateProfile_withTooLongBio_returns400() {
        String bio = "a".repeat(301);

        given()
                .contentType("application/json")
                .body("{\"bio\":\"" + bio + "\"}")
                .when().patch("/v1/me/profile")
                .then()
                .statusCode(400)
                .body("code", equalTo("BAD_REQUEST"))
                .body("error", equalTo("Bio must be 300 characters or fewer"));
    }

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void updateProfile_withExistingHandle_returns409() {
        userFixture.createSecondUser(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        given()
                .contentType("application/json")
                .body("""
                        { "handle": "taken-handle" }
                        """)
                .when().patch("/v1/me/profile")
                .then()
                .statusCode(409)
                .body("code", equalTo("CONFLICT"))
                .body("error", equalTo("Handle is already taken"));
    }

    @Test
    void updateProfile_withoutToken_returns401() {
        given()
                .contentType("application/json")
                .body("""
                        { "bio": "Updated bio" }
                        """)
                .when().patch("/v1/me/profile")
                .then()
                .statusCode(401);
    }

    @Test
    void getPublicProfile_returnsPublicUserAndPublicDeployedProjects() {
        userFixture.createPublicProfileProjects(UUID.fromString(TEST_USER_ID));

        given()
                .when().get("/v1/users/testdev")
                .then()
                .statusCode(200)
                .body("handle", equalTo("testdev"))
                .body("avatar_url", notNullValue())
                .body("bio", equalTo("Full-stack developer"))
                .body("created_at", notNullValue())
                .body("projects", hasSize(2))
                .body("projects.title", contains("Newest public project", "Old public project"))
                .body("projects.live_url", contains("https://new.example.dev", "https://old.example.dev"));
    }

    @Test
    void getPublicProfile_withUnknownHandle_returns404() {
        given()
                .when().get("/v1/users/unknown")
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void getPublicProfile_withSoftDeletedUser_returns404() {
        userFixture.softDeleteUser(UUID.fromString(TEST_USER_ID));

        given()
                .when().get("/v1/users/testdev")
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }
}
