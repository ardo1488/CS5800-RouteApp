package org.example;

/**
 * Concrete State: Logged In
 *
 * This state represents when a user is successfully authenticated.
 * Valid actions: logout
 * Invalid actions: login, register (already have an account)
 */

public class LoggedInState implements AuthState {

    @Override
    public void login(AuthContext context, String username, String password) {

        UserProfile currentUser = context.getCurrentUser();
        if (currentUser != null && currentUser.getUserName().equals(username)) {
            System.out.println("Already logged in as " + username);
        } else {

            System.out.println("Please log out first before logging in as a different user");
            context.notifyLoginFailure("Please log out first");
        }
    }

    @Override
    public void logout(AuthContext context) {
        UserProfile user = context.getCurrentUser();
        String username = user != null ? user.getUserName() : "Unknown";

        if (user != null) {
            context.getDatabase().saveUserToDatabase(user);
        }

        context.setCurrentUser(null);
        context.setState(new LoggedOutState());
        context.notifyLogout();

        System.out.println("User logged out: " + username);
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {

        System.out.println("Already logged in. Please log out to create a new account.");
        context.notifyRegistrationFailure("Already logged in");
    }

    @Override
    public String getStateName() {
        return "Logged In";
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getStateMessage() {
        return "You are logged in. Your routes and preferences will be saved.";
    }
}