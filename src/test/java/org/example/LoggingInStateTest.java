package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class LoggingInStateTest {

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

        boolean loginSuccessCalled;
        UserProfile successUser;

        boolean loginFailureCalled;
        String failureReason;

        @Override
        public void onStateChanged(AuthState oldState, AuthState newState) {
            stateChangedCalled = true;
            oldStateSeen = oldState;
            newStateSeen = newState;
        }

        @Override
        public void onLoginSuccess(UserProfile user) {
            loginSuccessCalled = true;
            successUser = user;
        }

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
    // processLoginAttempt(...) tests
    // -------------------------------------------------------------

    @Test
    public void processLoginAttemptWithValidCredentialsLogsUserInTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        String username = "login_user_" + System.nanoTime();
        String password = "secret123";

        // Create user directly in DB
        Database db = ctx.getDatabase();
        UserProfile created = db.createNewUser(username, password);
        assertNotNull(created);
        assertTrue(db.userExists(username));

        LoggingInState state = new LoggingInState(username, password);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            ctx.setState(state);
            state.processLoginAttempt(ctx);

            assertTrue(listener.loginSuccessCalled);
            assertFalse(listener.loginFailureCalled);
            assertNotNull(listener.successUser);
            assertEquals(username, listener.successUser.getUserName());

            assertTrue(ctx.isAuthenticated());
            assertEquals("Logged In", ctx.getStateName());
            assertNotNull(ctx.getCurrentUser());
            assertEquals(username, ctx.getCurrentUser().getUserName());
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    @Test
    public void processLoginAttemptWithWrongPasswordTriggersFailureAndLoggedOutTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        String username = "login_fail_user_" + System.nanoTime();
        String password = "secret123";
        String wrongPassword = "wrongpass";

        Database db = ctx.getDatabase();
        UserProfile created = db.createNewUser(username, password);
        assertNotNull(created);

        LoggingInState state = new LoggingInState(username, wrongPassword);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            ctx.setState(state);
            state.processLoginAttempt(ctx);

            assertFalse(listener.loginSuccessCalled);
            assertTrue(listener.loginFailureCalled);
            assertNotNull(listener.failureReason);

            assertFalse(ctx.isAuthenticated());
            assertNull(ctx.getCurrentUser());
            assertEquals("Logged Out", ctx.getStateName());
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // login(...) tests
    // -------------------------------------------------------------

    @Test
    public void loginWhileLoggingInDoesNotChangeAuthStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        String beforeStateName = ctx.getStateName();
        state.login(ctx, "other", "otherpass");

        assertEquals(beforeStateName, ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void loginWhileLoggingInDoesNotSetCurrentUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        state.login(ctx, "user", "pass");

        assertNull(ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // logout(...) tests
    // -------------------------------------------------------------

    @Test
    public void logoutWhileLoggingInTransitionsToLoggedOutStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        state.logout(ctx);

        assertEquals("Logged Out", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void logoutWhileLoggingInDoesNotClearCurrentUserFieldTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        UserProfile u = new UserProfile(42);
        ctx.setCurrentUser(u);

        state.logout(ctx);

        // implementation only changes state, not user reference
        assertSame(u, ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // register(...) tests
    // -------------------------------------------------------------

    @Test
    public void registerWhileLoggingInDoesNotChangeAuthStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        state.register(ctx, "newUser", "pw", "pw");

        assertEquals("Logging In", ctx.getStateName());
        assertFalse(ctx.isAuthenticated());
        resetContext(ctx);
    }

    @Test
    public void registerWhileLoggingInDoesNotSetCurrentUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("user", "pass");
        ctx.setState(state);

        state.register(ctx, "newUser", "pw", "pw");

        assertNull(ctx.getCurrentUser());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateName() tests
    // -------------------------------------------------------------

    @Test
    public void getStateNameReturnsLoggingInTextTest() {
        LoggingInState state = new LoggingInState("u", "p");

        assertEquals("Logging In", state.getStateName());
    }

    @Test
    public void getStateNameIsConsistentAcrossInstancesTest() {
        LoggingInState s1 = new LoggingInState("u1", "p1");
        LoggingInState s2 = new LoggingInState("u2", "p2");

        assertEquals(s1.getStateName(), s2.getStateName());
    }

    // -------------------------------------------------------------
    // isAuthenticated() tests
    // -------------------------------------------------------------

    @Test
    public void isAuthenticatedAlwaysFalseInLoggingInStateTest() {
        LoggingInState state = new LoggingInState("u", "p");

        assertFalse(state.isAuthenticated());
    }

    @Test
    public void isAuthenticatedDoesNotDependOnContextTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggingInState state = new LoggingInState("u", "p");
        ctx.setState(state);

        assertFalse(state.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateMessage() tests
    // -------------------------------------------------------------

    @Test
    public void getStateMessageReturnsAuthenticatingMessageTest() {
        LoggingInState state = new LoggingInState("u", "p");

        String msg = state.getStateMessage();
        assertTrue(msg.toLowerCase().contains("authenticat"));
    }

    @Test
    public void getStateMessageIsNonEmptyStringTest() {
        LoggingInState state = new LoggingInState("u", "p");

        String msg = state.getStateMessage();
        assertNotNull(msg);
        assertFalse(msg.trim().isEmpty());
    }
}
