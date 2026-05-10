package com.portloko.auth;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Configuration pour l'authentification OAuth GitHub
 */
@ApplicationScoped
public class GitHubOAuthConfig {

    @ConfigProperty(name = "github.oauth.client-id", defaultValue = "test-client-id")
    private String clientId;

    @ConfigProperty(name = "github.oauth.client-secret", defaultValue = "test-client-secret")
    private String clientSecret;

    @ConfigProperty(name = "github.oauth.redirect-uri", defaultValue = "http://localhost:3000/auth/callback")
    private String redirectUri;

    @ConfigProperty(name = "github.oauth.authorize-url", defaultValue = "https://github.com/login/oauth/authorize")
    private String authorizeUrl;

    @ConfigProperty(name = "app.oauth.session-timeout", defaultValue = "600")
    private long sessionTimeout; // en secondes

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }
}
