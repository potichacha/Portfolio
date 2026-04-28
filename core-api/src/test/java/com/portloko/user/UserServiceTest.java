package com.portloko.user;

import com.portloko.shared.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

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

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getByHandle
    // -------------------------------------------------------------------------

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
}
