package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class LoggedOutStateTest {

    // ---------- helpers ----------

    private AuthContext getContext() {
        return AuthContext.getInstance();
    }

    private void resetContext(AuthContext ctx) {
        ctx.setState(new LoggedOutState());
        ctx.setCurrentUser(null);
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

        boolean loginFailureCalled;
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
        public void onLoginFailure(String reason) {
            loginFailureCalled = true;
            failureReason = reason;
        }

        @Override
        public void onLogout() { }

        @Override
        public void onRegistrationSuccess(UserProfile user) { }

        @Override
        public void onRegistrationFailure(String reason) { }
    }

    // -------------------------------------------------------------
    // login(...) tests
    // -------------------------------------------------------------

    @Test
    public void loginFromLoggedOutTransitionsToLoggingInStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        state.login(ctx, "user123", "pw");

        assertEquals("Logging In", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void loginFromLoggedOutDoesNotAuthenticateImmediatelyTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        state.login(ctx, "user123", "pw");

        assertNull(ctx.getCurrentUser());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // logout(...) tests
    // -------------------------------------------------------------

    @Test
    public void logoutWhileLoggedOutKeepsStateLoggedOutAndNotAuthenticatedTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        state.logout(ctx);

        assertEquals("Logged Out", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void logoutWhileLoggedOutEmitsLoginFailureNotificationTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            state.logout(ctx);

            assertTrue(listener.loginFailureCalled);
            assertNotNull(listener.failureReason);
            assertTrue(listener.failureReason.toLowerCase().contains("already"));
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // register(...) tests
    // -------------------------------------------------------------

    @Test
    public void registerFromLoggedOutTransitionsToRegisteringStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        state.register(ctx, "newUser", "pw", "pw");

        assertEquals("Registering", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void registerFromLoggedOutDoesNotSetCurrentUserImmediatelyTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        state.register(ctx, "newUser", "pw", "pw");

        assertNull(ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateName() tests
    // -------------------------------------------------------------

    @Test
    public void getStateNameReturnsLoggedOutTextTest() {
        LoggedOutState state = new LoggedOutState();

        assertEquals("Logged Out", state.getStateName());
    }

    @Test
    public void getStateNameIsConsistentAcrossInstancesTest() {
        LoggedOutState s1 = new LoggedOutState();
        LoggedOutState s2 = new LoggedOutState();

        assertEquals(s1.getStateName(), s2.getStateName());
    }

    // -------------------------------------------------------------
    // isAuthenticated() tests
    // -------------------------------------------------------------

    @Test
    public void isAuthenticatedAlwaysFalseInLoggedOutStateTest() {
        LoggedOutState state = new LoggedOutState();

        assertFalse(state.isAuthenticated());
    }

    @Test
    public void isAuthenticatedDoesNotChangeWithContextTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedOutState state = new LoggedOutState();
        ctx.setState(state);

        assertFalse(state.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateMessage() tests
    // -------------------------------------------------------------

    @Test
    public void getStateMessageMentionsLoginOrAccountTest() {
        LoggedOutState state = new LoggedOutState();

        String msg = state.getStateMessage().toLowerCase();
        assertTrue(msg.contains("log in") || msg.contains("login") || msg.contains("account"));
    }

    @Test
    public void getStateMessageIsNonEmptyStringTest() {
        LoggedOutState state = new LoggedOutState();

        String msg = state.getStateMessage();
        assertNotNull(msg);
        assertFalse(msg.trim().isEmpty());
    }
}
