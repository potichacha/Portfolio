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

    public Optional<User> findActiveByGithubUserId(Long githubUserId) {
        return find("githubUserId = ?1 AND deletedAt IS NULL", githubUserId).firstResultOptional();
    }
}
