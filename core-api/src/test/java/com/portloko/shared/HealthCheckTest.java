package com.portloko.shared;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * BACK-1 — the Core API must start with a valid config and expose a health
 * endpoint reporting UP. The readiness probe also covers the datasource.
 */
@QuarkusTest
class HealthCheckTest {

    @Test
    void health_reportsUp() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void readiness_reportsUp() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }
}
