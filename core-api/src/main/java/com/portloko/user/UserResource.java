package com.portloko.user;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User profile endpoints")
public class UserResource {

    @Inject
    UserService userService;

    /**
     * BACK-005 — GET /v1/me
     * Returns the authenticated user's profile.
     * 401 if no valid JWT is present (enforced by @Authenticated).
     */
    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get current user profile")
    @APIResponse(responseCode = "200", description = "Authenticated user profile")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    public Response getMe(@Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        UserResponse userResponse = userService.getById(userId);
        return Response.ok(userResponse).build();
    }
}
