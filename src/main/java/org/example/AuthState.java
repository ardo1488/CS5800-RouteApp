package org.example;

/**
 * State interface for the Authentication State design pattern.
 *
 * Defines all possible actions that can be performed in any authentication state.
 * Each concrete state will implement these methods differently based on
 * what actions are valid in that state.
 */
public interface AuthState {

    /**
     * Attempt to log in with credentials
     * @param context The authentication context
     * @param username The username
     * @param password The password
     */
    void login(AuthContext context, String username, String password);

    /**
     * Attempt to log out the current user
     * @param context The authentication context
     */
    void logout(AuthContext context);

    /**
     * Attempt to register a new user
     * @param context The authentication context
     * @param username The desired username
     * @param password The desired password
     * @param confirmPassword Password confirmation
     */
    void register(AuthContext context, String username, String password, String confirmPassword);

    /**
     * Get the name of the current state (for display purposes)
     * @return State name
     */
    String getStateName();

    /**
     * Check if the user can access protected features in this state
     * @return true if user is authenticated
     */
    boolean isAuthenticated();

    /**
     * Get a message describing what the user can do in this state
     * @return Help message
     */
    String getStateMessage();
}