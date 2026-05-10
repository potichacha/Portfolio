package com.portloko.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service pour gérer les états CSRF lors du flux OAuth GitHub
 * Stocke temporairement les états de session pour validation au callback
 */
@ApplicationScoped
public class CsrfStateService {

    @Inject
    GitHubOAuthConfig oauthConfig;

    // Stockage en mémoire des états CSRF (en production, utiliser Redis ou une DB)
    private final Map<String, CsrfState> stateStore = new HashMap<>();

    /**
     * Crée et stocke un nouvel état CSRF
     * @return le token CSRF généré
     */
    public String createAndStoreState() {
        String state = CsrfTokenGenerator.generateToken();
        CsrfState csrfState = new CsrfState(state, Instant.now().getEpochSecond());
        stateStore.put(state, csrfState);
        return state;
    }

    /**
     * Valide et supprime un état CSRF
     * @param state le token CSRF à valider
     * @return true si le state est valide et pas expiré, false sinon
     */
    public boolean validateAndRemoveState(String state) {
        if (state == null || !stateStore.containsKey(state)) {
            return false;
        }

        CsrfState csrfState = stateStore.get(state);
        long currentTime = Instant.now().getEpochSecond();
        long age = currentTime - csrfState.createdAt;

        // Vérifier que le state n'a pas expiré
        if (age > oauthConfig.getSessionTimeout()) {
            stateStore.remove(state);
            return false;
        }

        // Supprimer le state après validation (one-time use)
        stateStore.remove(state);
        return true;
    }

    /**
     * Classe interne pour stocker l'état CSRF avec un timestamp
     */
    private static class CsrfState {
        String token;
        long createdAt;

        CsrfState(String token, long createdAt) {
            this.token = token;
            this.createdAt = createdAt;
        }
    }
}
