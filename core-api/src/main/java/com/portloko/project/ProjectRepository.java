package com.portloko.project;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProjectRepository implements PanacheRepository<Project> {

    public List<Project> findPublicDeployedByOwnerId(UUID ownerId) {
        return list(
                "owner.id = ?1 AND visibility = ?2 AND liveUrl IS NOT NULL AND deletedAt IS NULL ORDER BY createdAt DESC",
                ownerId,
                "PUBLIC"
        );
    }
}
