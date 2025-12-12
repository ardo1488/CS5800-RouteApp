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


    private static AuthContext instance;
    private AuthState currentState;
    private UserProfile currentUser;
    private final Database database;
    private final List<AuthStateListener> listeners;


    public interface AuthStateListener {
        void onStateChanged(AuthState oldState, AuthState newState);
        void onLoginSuccess(UserProfile user);
        void onLoginFailure(String reason);
        void onLogout();
        void onRegistrationSuccess(UserProfile user);
        void onRegistrationFailure(String reason);
    }


    private AuthContext() {
        this.database = Database.getInstance();
        this.listeners = new ArrayList<>();
        this.currentState = new LoggedOutState();
        this.currentUser = null;
    }


    public static AuthContext getInstance() {
        if (instance == null) {
            instance = new AuthContext();
        }
        return instance;
    }


    public void setState(AuthState newState) {
        AuthState oldState = this.currentState;
        this.currentState = newState;
        notifyStateChanged(oldState, newState);
        System.out.println("Auth state changed: " + oldState.getStateName() + " -> " + newState.getStateName());
    }


    public AuthState getState() {
        return currentState;
    }

    public void login(String username, String password) {
        currentState.login(this, username, password);
    }


    public void logout() {
        currentState.logout(this);
    }


    public void register(String username, String password, String confirmPassword) {
        currentState.register(this, username, password, confirmPassword);
    }


    public boolean isAuthenticated() {
        return currentState.isAuthenticated();
    }


    public String getStateName() {
        return currentState.getStateName();
    }


    public String getStateMessage() {
        return currentState.getStateMessage();
    }

    public void setCurrentUser(UserProfile user) {
        this.currentUser = user;
    }


    public UserProfile getCurrentUser() {
        return currentUser;
    }


    public Database getDatabase() {
        return database;
    }


    public void addListener(AuthStateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(AuthStateListener listener) {
        listeners.remove(listener);
    }


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