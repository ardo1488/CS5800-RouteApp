package org.example;

/**
 * Concrete State: Registering
 *
 * This is a transitional state that handles the registration process.
 * It validates the new user data and transitions to either LoggedInState
 * (auto-login after registration) or back to LoggedOutState on failure.
 */
public class RegisteringState implements AuthState {

    private final String username;
    private final String password;
    private final String confirmPassword;

    // Password requirements
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MIN_USERNAME_LENGTH = 3;

    public RegisteringState(String username, String password, String confirmPassword) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    /**
     * Process the registration attempt
     * Called by AuthContext after state transition
     */
    public void processRegistration(AuthContext context) {
        System.out.println("Processing registration for user: " + username);

        // Validate username
        if (username == null || username.trim().length() < MIN_USERNAME_LENGTH) {
            registrationFailed(context,
                    "Username must be at least " + MIN_USERNAME_LENGTH + " characters");
            return;
        }

        String trimmedUsername = username.trim();

        // Check if username already exists
        Database db = context.getDatabase();
        if (db.userExists(trimmedUsername)) {
            registrationFailed(context, "Username already taken");
            return;
        }

        // Validate password
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            registrationFailed(context,
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            return;
        }

        // Check password confirmation
        if (!password.equals(confirmPassword)) {
            registrationFailed(context, "Passwords do not match");
            return;
        }

        // Create new user
        UserProfile newUser = db.createUser(trimmedUsername, password);

        if (newUser != null) {
            registrationSucceeded(context, newUser);
        } else {
            registrationFailed(context, "Failed to create account. Please try again.");
        }
    }

    private void registrationSucceeded(AuthContext context, UserProfile user) {
        // Auto-login after successful registration
        context.setCurrentUser(user);
        context.setState(new LoggedInState());
        context.notifyRegistrationSuccess(user);
        System.out.println("Registration successful for: " + user.getUserName());
    }

    private void registrationFailed(AuthContext context, String reason) {
        context.setState(new LoggedOutState());
        context.notifyRegistrationFailure(reason);
        System.out.println("Registration failed: " + reason);
    }

    @Override
    public void login(AuthContext context, String username, String password) {
        // Can't login while registering
        System.out.println("Cannot login while registration is in progress");
    }

    @Override
    public void logout(AuthContext context) {
        // Cancel registration and return to logged out
        context.setState(new LoggedOutState());
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {
        // Already registering - ignore
        System.out.println("Registration already in progress...");
    }

    @Override
    public String getStateName() {
        return "Registering";
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getStateMessage() {
        return "Creating your account...";
    }
}