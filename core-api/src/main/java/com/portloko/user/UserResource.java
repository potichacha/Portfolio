package com.portloko.user;

import com.portloko.auth.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User profile endpoints")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    CurrentUser currentUser;

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get current user profile")
    @APIResponse(responseCode = "200", description = "Authenticated user profile")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    public Response getMe() {
        UserResponse userResponse = userService.getById(currentUser.requireId());
        return Response.ok(userResponse).build();
    }

    @PATCH
    @Path("/me/profile")
    @Authenticated
    @Operation(summary = "Update current user profile")
    @APIResponse(responseCode = "200", description = "Updated authenticated user profile")
    @APIResponse(responseCode = "400", description = "Invalid handle or bio")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    @APIResponse(responseCode = "409", description = "Handle already taken")
    public Response updateProfile(UpdateProfileRequest request) {
        UserResponse userResponse = userService.updateProfile(currentUser.requireId(), request);
        return Response.ok(userResponse).build();
    }

    @GET
    @Path("/users/{handle}")
    @Operation(summary = "Get public user profile")
    @APIResponse(responseCode = "200", description = "Public user profile")
    @APIResponse(responseCode = "404", description = "User not found")
    public Response getPublicProfile(@PathParam("handle") String handle) {
        PublicUserResponse userResponse = userService.getPublicProfile(handle);
        return Response.ok(userResponse).build();
    }
}
