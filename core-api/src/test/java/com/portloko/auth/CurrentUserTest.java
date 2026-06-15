package com.portloko.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACK-7 — CurrentUser resolves the caller identity from the security context.
 */
@QuarkusTest
class CurrentUserTest {

    static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000042";

    @Inject
    CurrentUser currentUser;

    @Test
    @TestSecurity(user = TEST_USER_ID)
    void resolvesAuthenticatedId() {
        assertThat(currentUser.isAuthenticated()).isTrue();
        assertThat(currentUser.id()).contains(UUID.fromString(TEST_USER_ID));
        assertThat(currentUser.requireId()).isEqualTo(UUID.fromString(TEST_USER_ID));
    }

    @Test
    void anonymousHasNoId() {
        assertThat(currentUser.isAuthenticated()).isFalse();
        assertThat(currentUser.id()).isEmpty();
    }
}
