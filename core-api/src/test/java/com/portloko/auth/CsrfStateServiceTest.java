package com.portloko.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Tests pour le service de gestion du state CSRF
 */
@QuarkusTest
public class CsrfStateServiceTest {

    @Inject
    CsrfStateService csrfStateService;

    @BeforeEach
    public void setUp() {
        // Nettoyer avant chaque test (si nécessaire)
    }

    /**
     * Test: La génération et stockage du state fonctionne
     */
    @Test
    public void testCreateAndStoreState() {
        String state = csrfStateService.createAndStoreState();

        assertNotNull(state, "State should not be null");
        assertFalse(state.isEmpty(), "State should not be empty");
        assertTrue(state.length() >= 32, "State should have sufficient length");
    }

    /**
     * Test: Chaque state généré doit être unique
     */
    @Test
    public void testStateIsUnique() {
        String state1 = csrfStateService.createAndStoreState();
        String state2 = csrfStateService.createAndStoreState();

        assertNotEquals(state1, state2, "Each generated state should be unique");
    }

    /**
     * Test: La validation d'un state valide doit retourner true
     */
    @Test
    public void testValidateAndRemoveStateWithValidState() {
        String state = csrfStateService.createAndStoreState();
        boolean isValid = csrfStateService.validateAndRemoveState(state);

        assertTrue(isValid, "Valid state should return true");
    }

    /**
     * Test: La validation d'un state invalide doit retourner false
     */
    @Test
    public void testValidateAndRemoveStateWithInvalidState() {
        boolean isValid = csrfStateService.validateAndRemoveState("invalid_state_xyz");

        assertFalse(isValid, "Invalid state should return false");
    }

    /**
     * Test: Un state ne peut être utilisé qu'une fois (one-time use)
     */
    @Test
    public void testStateCanBeUsedOnlyOnce() {
        String state = csrfStateService.createAndStoreState();

        // Première validation - doit être valide
        boolean firstValidation = csrfStateService.validateAndRemoveState(state);
        assertTrue(firstValidation, "First validation should succeed");

        // Deuxième validation - doit échouer (state a été supprimé)
        boolean secondValidation = csrfStateService.validateAndRemoveState(state);
        assertFalse(secondValidation, "Second validation should fail (one-time use)");
    }

    /**
     * Test: Un state null doit être invalide
     */
    @Test
    public void testValidateAndRemoveStateWithNullState() {
        boolean isValid = csrfStateService.validateAndRemoveState(null);

        assertFalse(isValid, "Null state should return false");
    }
}
