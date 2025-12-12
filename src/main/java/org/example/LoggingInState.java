package org.example;

/**
 * Concrete State: Logging In
 *
 * This is a transitional state that handles the login process.
 * It validates credentials and transitions to either LoggedInState
 * or back to LoggedOutState based on the result.
 */

public class LoggingInState implements AuthState {

    private final String username;
    private final String password;

    public LoggingInState(String username, String password) {
        this.username = username;
        this.password = password;

    }


    public void processLoginAttempt(AuthContext context) {
        System.out.println("Processing login for user: " + username);


        if (username == null || username.trim().isEmpty()) {
            loginFailed(context, "Username cannot be empty");
            return;
        }

        if (password == null || password.isEmpty()) {
            loginFailed(context, "Password cannot be empty");
            return;
        }


        Database db = context.getDatabase();
        UserProfile user = db.authenticateAUser(username.trim(), password);

        if (user != null) {

            loginSucceeded(context, user);
        } else {

            loginFailed(context, "Invalid username or password");
        }
    }

    private void loginSucceeded(AuthContext context, UserProfile user) {
        context.setCurrentUser(user);
        context.setState(new LoggedInState());
        context.notifyLoginSuccess(user);
        System.out.println("Login successful for: " + user.getUserName());
    }

    private void loginFailed(AuthContext context, String reason) {
        context.setState(new LoggedOutState());
        context.notifyLoginFailure(reason);
        System.out.println("Login failed: " + reason);
    }

    @Override
    public void login(AuthContext context, String username, String password) {
        System.out.println("Login already in progress...");
    }

    @Override
    public void logout(AuthContext context) {
        context.setState(new LoggedOutState());
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {

        System.out.println("Cannot register while login is in progress");
    }

    @Override
    public String getStateName() {
        return "Logging In";
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getStateMessage() {
        return "Authenticating...";
    }
}