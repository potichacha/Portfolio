package com.portloko.auth;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * BACK-7 — session lifecycle endpoints.
 *
 * <p>The JWT is stateless and lives in an HttpOnly cookie named {@code jwt}.
 * Logout clears that cookie with an immediately-expiring, empty replacement so
 * the browser drops it; the now-absent token is rejected by {@code @Authenticated}
 * on the next request.
 */
@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Auth", description = "Authentication & session endpoints")
public class AuthResource {

    /** Cookie name carrying the JWT (HttpOnly, set during the OAuth flow, BACK-6). */
    static final String JWT_COOKIE = "jwt";

    @POST
    @Path("/logout")
    @Authenticated
    @Operation(summary = "Log out the current user by clearing the JWT cookie")
    @APIResponse(responseCode = "204", description = "Session cleared")
    @APIResponse(responseCode = "401", description = "Missing or invalid JWT")
    public Response logout() {
        NewCookie cleared = new NewCookie.Builder(JWT_COOKIE)
                .value("")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .maxAge(0)        // expire immediately
                .build();
        return Response.noContent().cookie(cleared).build();
    }
}
