package com.portloko.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * Ressource REST pour l'authentification OAuth GitHub
 * Endpoint: GET /v1/auth/github/login
 */
@ApplicationScoped
@Path("/v1/auth")
public class GitHubAuthResource {

    private static final Logger LOG = Logger.getLogger(GitHubAuthResource.class);

    @Inject
    GitHubOAuthConfig oauthConfig;

    @Inject
    CsrfStateService csrfStateService;

    /**
     * Initie le flux de connexion GitHub OAuth
     * 
     * Génère un state CSRF, le stocke en session, et redirige vers GitHub
     * avec les paramètres OAuth appropriés.
     * 
     * @return Redirection vers github.com/login/oauth/authorize
     */
    @GET
    @Path("/github/login")
    public Response initiateGitHubLogin() {
        LOG.info("Initiating GitHub OAuth login flow");

        try {
            // 1. Générer et stocker le state CSRF
            String state = csrfStateService.createAndStoreState();
            LOG.debug("Generated CSRF state token: " + state);

            // 2. Construire l'URL de redirection vers GitHub
            String authorizeUrl = buildGitHubAuthorizeUrl(state);
            LOG.debug("Redirecting to GitHub authorize URL");

            // 3. Rediriger l'utilisateur vers GitHub
            return Response.seeOther(URI.create(authorizeUrl)).build();

        } catch (Exception e) {
            LOG.error("Error initiating GitHub OAuth login", e);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Unable to initiate GitHub login")
                    .build();
        }
    }

    /**
     * Construit l'URL d'autorisation GitHub avec les paramètres OAuth
     * 
     * @param state token CSRF pour validation au callback
     * @return URL complète pour redirection
     */
    private String buildGitHubAuthorizeUrl(String state) {
        String scope = "read:user,repo";
        
        String url = oauthConfig.getAuthorizeUrl()
                + "?client_id=" + URLEncoder.encode(oauthConfig.getClientId(), StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(oauthConfig.getRedirectUri(), StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        
        LOG.debug("Built GitHub authorize URL (without secrets)");
        return url;
    }
}
