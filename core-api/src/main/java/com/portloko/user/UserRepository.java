package com.portloko.user;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findActiveById(UUID id) {
        return find("id = ?1 AND deletedAt IS NULL", id).firstResultOptional();
    }

    public Optional<User> findActiveByHandle(String handle) {
        return find("handle = ?1 AND deletedAt IS NULL", handle).firstResultOptional();
    }

    public boolean activeHandleExistsForAnotherUser(String handle, UUID userId) {
        return count("handle = ?1 AND id <> ?2 AND deletedAt IS NULL", handle, userId) > 0;
    }

    public Optional<User> findActiveByGithubUserId(Long githubUserId) {
        return find("githubUserId = ?1 AND deletedAt IS NULL", githubUserId).firstResultOptional();
    }
}
