package com.portloko.user;

import com.portloko.project.Project;
import com.portloko.project.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
class UserFixture {

    @Inject
    UserRepository userRepository;

    @Inject
    ProjectRepository projectRepository;

    @Transactional
    public void createUser(UUID id) {
        projectRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.id = id;
        user.handle = "testdev";
        user.email = "testdev@example.com";
        user.avatarUrl = "https://avatars.githubusercontent.com/u/12345";
        user.bio = "Full-stack developer";
        user.githubUserId = 12345L;
        user.githubLogin = "testdev";
        user.createdAt = OffsetDateTime.now();
        userRepository.persist(user);
    }

    @Transactional
    public void createSecondUser(UUID id) {
        User user = new User();
        user.id = id;
        user.handle = "taken-handle";
        user.email = "taken@example.com";
        user.avatarUrl = "https://avatars.githubusercontent.com/u/67890";
        user.bio = "Backend developer";
        user.githubUserId = 67890L;
        user.githubLogin = "taken";
        user.createdAt = OffsetDateTime.now();
        userRepository.persist(user);
    }

    @Transactional
    public void createPublicProfileProjects(UUID ownerId) {
        User owner = userRepository.findActiveById(ownerId).orElseThrow();

        persistProject(owner, "Newest public project", "PUBLIC", "https://new.example.dev", OffsetDateTime.now());
        persistProject(owner, "Old public project", "PUBLIC", "https://old.example.dev", OffsetDateTime.now().minusDays(2));
        persistProject(owner, "Private project", "PRIVATE", "https://private.example.dev", OffsetDateTime.now().minusDays(1));
        persistProject(owner, "Public draft", "PUBLIC", null, OffsetDateTime.now().minusHours(2));
    }

    @Transactional
    public void softDeleteUser(UUID id) {
        User user = userRepository.findActiveById(id).orElseThrow();
        user.deletedAt = OffsetDateTime.now();
    }

    private void persistProject(User owner, String title, String visibility, String liveUrl, OffsetDateTime createdAt) {
        Project project = new Project();
        project.id = UUID.randomUUID();
        project.owner = owner;
        project.title = title;
        project.visibility = visibility;
        project.liveUrl = liveUrl;
        project.createdAt = createdAt;
        projectRepository.persist(project);
    }
}
