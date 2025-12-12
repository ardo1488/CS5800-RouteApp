package org.example;

/**
 * Concrete State: Logged Out
 *
 * This state represents when no user is currently logged in.
 * Valid actions: login, register
 * Invalid actions: logout
 */
public class LoggedOutState implements AuthState {

    @Override
    public void login(AuthContext context, String username, String password) {
        // Transition to logging in state
        context.setState(new LoggingInState(username, password));
    }

    @Override
    public void logout(AuthContext context) {
        // Already logged out - no action needed
        System.out.println("Already logged out.");
        context.notifyLoginFailure("Already logged out");
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {
        // Transition to registering state
        context.setState(new RegisteringState(username, password, confirmPassword));
    }

    @Override
    public String getStateName() {
        return "Logged Out";
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getStateMessage() {
        return "Please log in or create an account to access your profile.";
    }
}