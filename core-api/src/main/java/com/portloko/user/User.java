package com.portloko.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends PanacheEntityBase {

    @Id
    public UUID id;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @Column(name = "handle", nullable = false, unique = true, length = 39)
    public String handle;

    @Column(name = "email", nullable = false, unique = true)
    public String email;

    @Column(name = "avatar_url")
    public String avatarUrl;

    @Column(name = "bio")
    public String bio;

    @Column(name = "github_user_id", nullable = false, unique = true)
    public Long githubUserId;

    @Column(name = "github_login", nullable = false)
    public String githubLogin;

    // scopes added by BACK-001 (auth module) as @ElementCollection
    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
