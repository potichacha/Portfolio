package com.portloko.user;

import com.portloko.project.ProjectRepository;
import com.portloko.project.PublicProjectResponse;
import com.portloko.shared.BadRequestException;
import com.portloko.shared.ConflictException;
import com.portloko.shared.NotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class UserService {

    private static final int BIO_MAX_LENGTH = 300;
    private static final Pattern HANDLE_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{3,39}$");

    @Inject
    UserRepository userRepository;

    @Inject
    ProjectRepository projectRepository;

    public UserResponse getById(UUID id) {
        return userRepository.findActiveById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public UserResponse getByHandle(String handle) {
        return userRepository.findActiveByHandle(handle)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        if (request == null) {
            request = new UpdateProfileRequest(null, null, null);
        }

        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (request.bio() != null) {
            validateBio(request.bio());
            user.bio = request.bio();
        }

        if (request.handle() != null) {
            validateHandle(request.handle());
            if (!request.handle().equals(user.handle)
                    && userRepository.activeHandleExistsForAnotherUser(request.handle(), user.id)) {
                throw new ConflictException("Handle is already taken");
            }
            user.handle = request.handle();
        }

        if (request.avatarUrl() != null) {
            user.avatarUrl = request.avatarUrl();
        }

        return UserResponse.from(user);
    }

    public PublicUserResponse getPublicProfile(String handle) {
        User user = userRepository.findActiveByHandle(handle)
                .orElseThrow(() -> new NotFoundException("User not found"));

        List<PublicProjectResponse> projects = projectRepository.findPublicDeployedByOwnerId(user.id)
                .stream()
                .map(PublicProjectResponse::from)
                .toList();

        return new PublicUserResponse(user.handle, user.avatarUrl, user.bio, user.createdAt, projects);
    }

    private void validateBio(String bio) {
        if (bio.length() > BIO_MAX_LENGTH) {
            throw new BadRequestException("Bio must be 300 characters or fewer");
        }
    }

    private void validateHandle(String handle) {
        if (!HANDLE_PATTERN.matcher(handle).matches()) {
            throw new BadRequestException("Handle must match ^[a-zA-Z0-9-]{3,39}$");
        }
    }
}
