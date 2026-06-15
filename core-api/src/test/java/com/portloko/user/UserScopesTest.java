package com.portloko.user;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACK-5 — the {@code scopes} column persists and round-trips as a list.
 */
@QuarkusTest
class UserScopesTest {

    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    void scopes_persistAndRoundTrip() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.id = id;
        user.handle = "scopeuser";
        user.email = "scopeuser@example.com";
        user.githubUserId = 999001L;
        user.githubLogin = "scopeuser";
        user.scopes = List.of("read:user", "user:email");
        user.createdAt = OffsetDateTime.now();
        userRepository.persist(user);
        userRepository.getEntityManager().flush();
        userRepository.getEntityManager().clear();

        User reloaded = userRepository.findActiveById(id).orElseThrow();
        assertThat(reloaded.scopes).containsExactlyInAnyOrder("read:user", "user:email");
    }

    @Test
    @Transactional
    void scopes_defaultToEmpty() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.id = id;
        user.handle = "noscope";
        user.email = "noscope@example.com";
        user.githubUserId = 999002L;
        user.githubLogin = "noscope";
        user.createdAt = OffsetDateTime.now();
        userRepository.persist(user);
        userRepository.getEntityManager().flush();
        userRepository.getEntityManager().clear();

        User reloaded = userRepository.findActiveById(id).orElseThrow();
        assertThat(reloaded.scopes).isEmpty();
    }
}
