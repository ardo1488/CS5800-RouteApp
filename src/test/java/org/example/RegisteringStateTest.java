package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RegisteringStateTest {

    // ---------- helpers ----------

    private AuthContext getContext() {
        return AuthContext.getInstance();
    }

    private void resetContext(AuthContext ctx) {
        // reset state & current user
        ctx.setState(new LoggedOutState());
        ctx.setCurrentUser(null);

        // clear listeners (so tests don't leak)
        try {
            Field f = AuthContext.class.getDeclaredField("listeners");
            f.setAccessible(true);
            ((java.util.List<?>) f.get(ctx)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestAuthListener implements AuthContext.AuthStateListener {
        boolean stateChangedCalled;
        AuthState oldStateSeen;
        AuthState newStateSeen;

        boolean registrationSuccessCalled;
        UserProfile successUser;

        boolean registrationFailureCalled;
        String failureReason;

        @Override
        public void onStateChanged(AuthState oldState, AuthState newState) {
            stateChangedCalled = true;
            oldStateSeen = oldState;
            newStateSeen = newState;
        }

        @Override
        public void onLoginSuccess(UserProfile user) { }

        @Override
        public void onLoginFailure(String reason) { }

        @Override
        public void onLogout() { }

        @Override
        public void onRegistrationSuccess(UserProfile user) {
            registrationSuccessCalled = true;
            successUser = user;
        }

        @Override
        public void onRegistrationFailure(String reason) {
            registrationFailureCalled = true;
            failureReason = reason;
        }
    }

    // -------------------------------------------------------------
    // processRegistration(...) tests
    // -------------------------------------------------------------

    @Test
    public void processRegistrationWithValidNewUserCreatesAccountAndLogsInTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        // unique username to avoid collisions in persistent DB
        String username = "reg_user_" + System.nanoTime();
        String password = "secret123";

        // sanity: should not exist yet
        assertFalse(ctx.getDatabase().userExists(username));

        RegisteringState state = new RegisteringState(username, password, password);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            state.processRegistration(ctx);

            assertTrue(listener.registrationSuccessCalled);
            assertFalse(listener.registrationFailureCalled);

            UserProfile current = ctx.getCurrentUser();
            assertNotNull(current);
            assertEquals(username, current.getUserName());

            assertEquals("Logged In", ctx.getStateName());
            assertTrue(ctx.isAuthenticated());
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    @Test
    public void processRegistrationWithInvalidShortUsernameTriggersFailureTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        String badUsername = "ab"; // too short
        String password = "secret123";

        RegisteringState state = new RegisteringState(badUsername, password, password);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            state.processRegistration(ctx);

            assertFalse(listener.registrationSuccessCalled);
            assertTrue(listener.registrationFailureCalled);
            assertNotNull(listener.failureReason);
            assertTrue(listener.failureReason.toLowerCase().contains("username"));

            assertNull(ctx.getCurrentUser());
            assertEquals("Logged Out", ctx.getStateName());
            assertFalse(ctx.isAuthenticated());
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // login(...) tests
    // -------------------------------------------------------------

    @Test
    public void loginWhileRegisteringDoesNotChangeAuthStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state); // set current state to registering

        String beforeStateName = ctx.getStateName();
        state.login(ctx, "someone", "else");

        assertEquals(beforeStateName, ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void loginWhileRegisteringDoesNotSetCurrentUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state);

        state.login(ctx, "user", "pass");

        assertNull(ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // logout(...) tests
    // -------------------------------------------------------------

    @Test
    public void logoutWhileRegisteringTransitionsToLoggedOutStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state);

        state.logout(ctx);

        assertEquals("Logged Out", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void logoutWhileRegisteringDoesNotClearCurrentUserFieldTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state);

        UserProfile user = new UserProfile(1);
        ctx.setCurrentUser(user);

        state.logout(ctx);

        // implementation only changes state; it does not null out the user
        assertSame(user, ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // register(...) tests
    // -------------------------------------------------------------

    @Test
    public void registerWhileRegisteringDoesNotChangeAuthStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state);

        state.register(ctx, "other", "p", "p");

        assertEquals("Registering", ctx.getStateName());
        resetContext(ctx);
    }

    @Test
    public void registerWhileRegisteringDoesNotAuthenticateUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("user", "pass", "pass");
        ctx.setState(state);

        state.register(ctx, "other", "p", "p");

        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateName() tests
    // -------------------------------------------------------------

    @Test
    public void getStateNameReturnsRegisteringTest() {
        RegisteringState state = new RegisteringState("u", "p", "p");

        assertEquals("Registering", state.getStateName());
    }

    @Test
    public void getStateNameIsConsistentAcrossInstancesTest() {
        RegisteringState s1 = new RegisteringState("u1", "p", "p");
        RegisteringState s2 = new RegisteringState("u2", "p", "p");

        assertEquals(s1.getStateName(), s2.getStateName());
    }

    // -------------------------------------------------------------
    // isAuthenticated() tests
    // -------------------------------------------------------------

    @Test
    public void isAuthenticatedAlwaysFalseInRegisteringStateTest() {
        RegisteringState state = new RegisteringState("u", "p", "p");

        assertFalse(state.isAuthenticated());
    }

    @Test
    public void isAuthenticatedDoesNotDependOnContextStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        RegisteringState state = new RegisteringState("u", "p", "p");
        ctx.setState(state);

        assertFalse(state.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateMessage() tests
    // -------------------------------------------------------------

    @Test
    public void getStateMessageReturnsRegistrationMessageTest() {
        RegisteringState state = new RegisteringState("u", "p", "p");

        String msg = state.getStateMessage();
        assertTrue(msg.toLowerCase().contains("account"));
    }

    @Test
    public void getStateMessageNonEmptyTest() {
        RegisteringState state = new RegisteringState("u", "p", "p");

        String msg = state.getStateMessage();
        assertNotNull(msg);
        assertFalse(msg.trim().isEmpty());
    }
}
