package org.example;

/**
 * Concrete State: Registering
 *
 * This is a transitional state that handles the registration process.
 * It validates the new user data and transitions to either LoggedInState
 * (auto-login after registration) or back to LoggedOutState on failure.
 */
public class RegisteringState implements AuthState {


}