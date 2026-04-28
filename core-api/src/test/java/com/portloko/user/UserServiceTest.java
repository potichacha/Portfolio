package com.portloko.user;

import com.portloko.project.Project;
import com.portloko.project.ProjectRepository;
import com.portloko.shared.BadRequestException;
import com.portloko.shared.ConflictException;
import com.portloko.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    UserService userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.id = userId;
        user.handle = "testdev";
        user.email = "testdev@example.com";
        user.avatarUrl = "https://avatars.githubusercontent.com/u/12345";
        user.bio = "Full-stack developer";
        user.githubUserId = 12345L;
        user.githubLogin = "testdev";
        user.createdAt = OffsetDateTime.now();
    }

    @Test
    void getById_returnsUserResponse_whenUserExists() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getById(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.handle()).isEqualTo("testdev");
        assertThat(response.email()).isEqualTo("testdev@example.com");
        assertThat(response.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");
        assertThat(response.bio()).isEqualTo("Full-stack developer");
        assertThat(response.githubLogin()).isEqualTo("testdev");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void getById_throwsNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getByHandle_returnsUserResponse_whenHandleExists() {
        when(userRepository.findActiveByHandle("testdev")).thenReturn(Optional.of(user));

        UserResponse response = userService.getByHandle("testdev");

        assertThat(response.handle()).isEqualTo("testdev");
    }

    @Test
    void getByHandle_throwsNotFoundException_whenHandleDoesNotExist() {
        when(userRepository.findActiveByHandle("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByHandle("unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateProfile_updatesEditableFields() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(userRepository.activeHandleExistsForAnotherUser("new-handle", userId)).thenReturn(false);

        UserResponse response = userService.updateProfile(
                userId,
                new UpdateProfileRequest("Updated bio", "new-handle", "https://cdn.example.com/avatar.png")
        );

        assertThat(response.handle()).isEqualTo("new-handle");
        assertThat(response.bio()).isEqualTo("Updated bio");
        assertThat(response.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void updateProfile_throwsBadRequestException_whenHandleIsInvalid() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(
                userId,
                new UpdateProfileRequest(null, "no", null)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Handle must match ^[a-zA-Z0-9-]{3,39}$");
    }

    @Test
    void updateProfile_throwsBadRequestException_whenBioIsTooLong() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateProfile(
                userId,
                new UpdateProfileRequest("a".repeat(301), null, null)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bio must be 300 characters or fewer");
    }

    @Test
    void updateProfile_throwsConflictException_whenHandleAlreadyExists() {
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(userRepository.activeHandleExistsForAnotherUser("taken-handle", userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(
                userId,
                new UpdateProfileRequest(null, "taken-handle", null)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Handle is already taken");
    }

    @Test
    void getPublicProfile_returnsPublicProfileWithProjects() {
        Project project = new Project();
        project.id = UUID.randomUUID();
        project.title = "Public project";
        project.liveUrl = "https://project.example.dev";
        project.createdAt = OffsetDateTime.now();

        when(userRepository.findActiveByHandle("testdev")).thenReturn(Optional.of(user));
        when(projectRepository.findPublicDeployedByOwnerId(userId)).thenReturn(List.of(project));

        PublicUserResponse response = userService.getPublicProfile("testdev");

        assertThat(response.handle()).isEqualTo("testdev");
        assertThat(response.projects()).hasSize(1);
        assertThat(response.projects().getFirst().liveUrl()).isEqualTo("https://project.example.dev");
    }
}
