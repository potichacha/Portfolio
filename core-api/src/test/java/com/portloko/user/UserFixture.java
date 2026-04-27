package com.portloko.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CDI bean de test — @Transactional est correctement intercepté car
 * l'appel passe par le proxy CDI, contrairement à @BeforeEach direct.
 */
@ApplicationScoped
class UserFixture {

    @Inject
    UserRepository userRepository;

    @Transactional
    public void createUser(UUID id) {
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
}
