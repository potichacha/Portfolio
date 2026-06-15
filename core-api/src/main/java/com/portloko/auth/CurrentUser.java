package com.portloko.auth;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

/**
 * BACK-7 — request-scoped view of the authenticated caller, resolved from the
 * validated JWT. Inject this instead of re-parsing {@code SecurityContext} in
 * every resource.
 *
 * <p>The JWT subject ({@code sub}) carries the user id; {@code handle} is an
 * optional custom claim set when the token is issued (BACK-6).
 */
@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    /** @return true when a valid JWT is present on the request. */
    public boolean isAuthenticated() {
        return identity != null && !identity.isAnonymous();
    }

    /** @return the authenticated user id, or empty when anonymous/invalid. */
    public Optional<UUID> id() {
        if (!isAuthenticated() || identity.getPrincipal() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(identity.getPrincipal().getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** @return the authenticated user id, or throws if the caller is anonymous. */
    public UUID requireId() {
        return id().orElseThrow(() -> new IllegalStateException("No authenticated user on request"));
    }

    /** @return the optional {@code handle} claim, when present in the token. */
    public Optional<String> handle() {
        if (!isAuthenticated()) {
            return Optional.empty();
        }
        Object claim = identity.getAttribute("handle");
        return Optional.ofNullable(claim).map(Object::toString);
    }
}
