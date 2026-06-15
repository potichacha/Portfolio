package com.portloko.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * BACK-7 — logout clears the JWT cookie; the endpoint requires authentication.
 */
@QuarkusTest
class AuthResourceTest {

    static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void logout_clearsJwtCookie_returns204() {
        given()
                .when().post("/v1/auth/logout")
                .then()
                .statusCode(204)
                // cleared cookie: empty value, Max-Age=0
                .cookie("jwt", equalTo(""))
                .header("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0"));
    }

    @Test
    void logout_withoutToken_returns401() {
        given()
                .when().post("/v1/auth/logout")
                .then()
                .statusCode(401);
    }
}
