package com.portloko.user;

import com.portloko.shared.NotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    /**
     * Returns the active user for the given UUID.
     * Throws NotFoundException (→ 404) if not found or soft-deleted.
     */
    public UserResponse getById(UUID id) {
        return userRepository.findActiveById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Returns the active user for a public handle.
     * Throws NotFoundException (→ 404) if not found or soft-deleted.
     */
    public UserResponse getByHandle(String handle) {
        return userRepository.findActiveByHandle(handle)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
