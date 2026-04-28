package com.portloko.project;

import com.portloko.user.User;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project extends PanacheEntityBase {

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    public User owner;

    @Column(name = "title", nullable = false, length = 120)
    public String title;

    @Column(name = "visibility", nullable = false, length = 20)
    public String visibility;

    @Column(name = "live_url")
    public String liveUrl;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    public OffsetDateTime deletedAt;
}
