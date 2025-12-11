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

        // Automatically process login when entering this state
        // In a real app, this might be done asynchronously
    }

    /**
     * Process the login attempt
     * Called by AuthContext after state transition
     */
    public void processLogin(AuthContext context) {
        System.out.println("Processing login for user: " + username);

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            loginFailed(context, "Username cannot be empty");
            return;
        }

        if (password == null || password.isEmpty()) {
            loginFailed(context, "Password cannot be empty");
            return;
        }

        // Attempt authentication
        Database db = context.getDatabase();
        UserProfile user = db.authenticate(username.trim(), password);

        if (user != null) {
            // Login successful
            loginSucceeded(context, user);
        } else {
            // Login failed
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
        // Already logging in - ignore
        System.out.println("Login already in progress...");
    }

    @Override
    public void logout(AuthContext context) {
        // Cancel login and return to logged out
        context.setState(new LoggedOutState());
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {
        // Can't register while logging in
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