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

    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MIN_USERNAME_LENGTH = 3;

    public RegisteringState(String username, String password, String confirmPassword) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public void processRegistration(AuthContext context) {
        System.out.println("Processing registration for user: " + username);

        if (!isUsernameValid()) {
            registrationFailed(context, "Username must be at least " + MIN_USERNAME_LENGTH + " characters");
            return;
        }

        String trimmedUsername = username.trim();

        if (isUsernameTaken(context, trimmedUsername)) {
            registrationFailed(context, "Username already taken");
            return;
        }

        if (!isPasswordValid()) {
            registrationFailed(context, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            return;
        }

        if (!doPasswordsMatch()) {
            registrationFailed(context, "Passwords do not match");
            return;
        }

        UserProfile newUser = createUserInDatabase(context, trimmedUsername);

        if (newUser != null) {
            registrationSucceeded(context, newUser);
        } else {
            registrationFailed(context, "Failed to create account. Please try again.");
        }
    }


    private boolean isUsernameValid() {
        return username != null && username.trim().length() >= MIN_USERNAME_LENGTH;
    }

    private boolean isUsernameTaken(AuthContext context, String trimmedUsername) {
        Database db = context.getDatabase();
        return db.userExists(trimmedUsername);
    }

    private boolean isPasswordValid() {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    private boolean doPasswordsMatch() {
        return password.equals(confirmPassword);
    }

    private UserProfile createUserInDatabase(AuthContext context, String trimmedUsername) {
        Database db = context.getDatabase();
        return db.createNewUser(trimmedUsername, password);
    }


    private void registrationSucceeded(AuthContext context, UserProfile user) {
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
        System.out.println("Cannot login while registration is in progress");
    }

    @Override
    public void logout(AuthContext context) {
        context.setState(new LoggedOutState());
    }

    @Override
    public void register(AuthContext context, String username, String password, String confirmPassword) {
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