package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Context class for the Authentication State design pattern.
 *
 * Manages the current authentication state and delegates all authentication
 * operations to the current state object. Also maintains the currently
 * logged-in user profile.
 */
public class AuthContext {

    // Singleton instance
    private static AuthContext instance;

    // Current state
    private AuthState currentState;

    // Currently logged in user (null if not logged in)
    private UserProfile currentUser;

    // Database for authentication (uses the unified Database singleton)
    private final Database database;

    // Listeners for state changes
    private final List<AuthStateListener> listeners;

    /**
     * Listener interface for authentication state changes
     */
    public interface AuthStateListener {
        void onStateChanged(AuthState oldState, AuthState newState);
        void onLoginSuccess(UserProfile user);
        void onLoginFailure(String reason);
        void onLogout();
        void onRegistrationSuccess(UserProfile user);
        void onRegistrationFailure(String reason);
    }

    /**
     * Private constructor for singleton
     */
    private AuthContext() {
        this.database = Database.getInstance();
        this.listeners = new ArrayList<>();
        this.currentState = new LoggedOutState();
        this.currentUser = null;
    }

    /**
     * Get singleton instance
     */
    public static AuthContext getInstance() {
        if (instance == null) {
            instance = new AuthContext();
        }
        return instance;
    }

    // ==================== State Management ====================

    /**
     * Change to a new state
     * @param newState The new state to transition to
     */
    public void setState(AuthState newState) {
        AuthState oldState = this.currentState;
        this.currentState = newState;
        notifyStateChanged(oldState, newState);
        System.out.println("Auth state changed: " + oldState.getStateName() + " -> " + newState.getStateName());
    }

    /**
     * Get the current state
     */
    public AuthState getState() {
        return currentState;
    }

    // ==================== Delegated Operations ====================
    // These methods delegate to the current state

    /**
     * Attempt to log in
     */
    public void login(String username, String password) {
        currentState.login(this, username, password);
    }

    /**
     * Attempt to log out
     */
    public void logout() {
        currentState.logout(this);
    }

    /**
     * Attempt to register a new user
     */
    public void register(String username, String password, String confirmPassword) {
        currentState.register(this, username, password, confirmPassword);
    }

    /**
     * Check if currently authenticated
     */
    public boolean isAuthenticated() {
        return currentState.isAuthenticated();
    }

    /**
     * Get the current state name
     */
    public String getStateName() {
        return currentState.getStateName();
    }

    /**
     * Get the current state message
     */
    public String getStateMessage() {
        return currentState.getStateMessage();
    }

    // ==================== User Management ====================

    /**
     * Set the current logged-in user
     */
    public void setCurrentUser(UserProfile user) {
        this.currentUser = user;
    }

    /**
     * Get the current logged-in user
     */
    public UserProfile getCurrentUser() {
        return currentUser;
    }

    /**
     * Get the database
     */
    public Database getDatabase() {
        return database;
    }

    // ==================== Listener Management ====================

    public void addListener(AuthStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(AuthStateListener listener) {
        listeners.remove(listener);
    }

    // Notification methods called by states

    void notifyStateChanged(AuthState oldState, AuthState newState) {
        for (AuthStateListener l : listeners) {
            l.onStateChanged(oldState, newState);
        }
    }

    void notifyLoginSuccess(UserProfile user) {
        for (AuthStateListener l : listeners) {
            l.onLoginSuccess(user);
        }
    }

    void notifyLoginFailure(String reason) {
        for (AuthStateListener l : listeners) {
            l.onLoginFailure(reason);
        }
    }

    void notifyLogout() {
        for (AuthStateListener l : listeners) {
            l.onLogout();
        }
    }

    void notifyRegistrationSuccess(UserProfile user) {
        for (AuthStateListener l : listeners) {
            l.onRegistrationSuccess(user);
        }
    }

    void notifyRegistrationFailure(String reason) {
        for (AuthStateListener l : listeners) {
            l.onRegistrationFailure(reason);
        }
    }
}