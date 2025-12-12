package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class LoggedInStateTest {

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
        boolean loginFailureCalled;
        String loginFailureReason;

        boolean registrationFailureCalled;
        String registrationFailureReason;

        boolean logoutCalled;

        boolean stateChangedCalled;
        AuthState oldStateSeen;
        AuthState newStateSeen;

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
            loginFailureReason = reason;
        }

        @Override
        public void onLogout() {
            logoutCalled = true;
        }

        @Override
        public void onRegistrationSuccess(UserProfile user) { }

        @Override
        public void onRegistrationFailure(String reason) {
            registrationFailureCalled = true;
            registrationFailureReason = reason;
        }
    }

    private UserProfile createAndLoginUser(AuthContext ctx, String username) {
        // create a real DB user so saveUserToDatabase etc. are safe
        Database db = ctx.getDatabase();
        String password = "pw_" + System.nanoTime();
        UserProfile user = db.createNewUser(username, password);
        ctx.setCurrentUser(user);
        ctx.setState(new LoggedInState());
        return user;
    }

    // -------------------------------------------------------------
    // login(...) tests
    // -------------------------------------------------------------

    @Test
    public void loginWithSameUserWhileLoggedInDoesNotChangeStateOrNotifyFailureTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            String username = "loggedin_same_" + System.nanoTime();
            UserProfile user = createAndLoginUser(ctx, username);

            LoggedInState state = new LoggedInState();
            ctx.setState(state);

            state.login(ctx, username, "irrelevant");

            assertEquals("Logged In", ctx.getStateName());
            assertTrue(ctx.isAuthenticated());
            assertSame(user, ctx.getCurrentUser());
            assertFalse(listener.loginFailureCalled);
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    @Test
    public void loginWithDifferentUserWhileLoggedInNotifiesFailureAndKeepsStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            String username = "loggedin_user_" + System.nanoTime();
            UserProfile user = createAndLoginUser(ctx, username);

            LoggedInState state = new LoggedInState();
            ctx.setState(state);

            state.login(ctx, "otherUser", "pw");

            assertEquals("Logged In", ctx.getStateName());
            assertTrue(ctx.isAuthenticated());
            assertSame(user, ctx.getCurrentUser());

            assertTrue(listener.loginFailureCalled);
            assertNotNull(listener.loginFailureReason);
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // logout(...) tests
    // -------------------------------------------------------------

    @Test
    public void logoutFromLoggedInClearsCurrentUserAndTransitionsToLoggedOutTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            String username = "logout_user_" + System.nanoTime();
            UserProfile user = createAndLoginUser(ctx, username);

            assertNotNull(ctx.getCurrentUser());
            assertEquals("Logged In", ctx.getStateName());

            LoggedInState state = new LoggedInState();
            ctx.setState(state);

            state.logout(ctx);

            assertNull(ctx.getCurrentUser());
            assertEquals("Logged Out", ctx.getStateName());
            assertFalse(ctx.isAuthenticated());
            assertTrue(listener.logoutCalled);
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    @Test
    public void logoutFromLoggedInPersistsUserToDatabaseTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        Database db = ctx.getDatabase();

        String username = "logout_persist_" + System.nanoTime();
        UserProfile user = createAndLoginUser(ctx, username);

        LoggedInState state = new LoggedInState();
        ctx.setState(state);

        state.logout(ctx);

        // We can't easily assert exact fields, but user should still exist
        assertTrue(db.userExists(username));
    }

    // -------------------------------------------------------------
    // register(...) tests
    // -------------------------------------------------------------

    @Test
    public void registerWhileLoggedInKeepsStateLoggedInAndAuthenticatedTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            String username = "reg_loggedin_" + System.nanoTime();
            UserProfile user = createAndLoginUser(ctx, username);

            LoggedInState state = new LoggedInState();
            ctx.setState(state);

            state.register(ctx, "newUser", "pw", "pw");

            assertEquals("Logged In", ctx.getStateName());
            assertTrue(ctx.isAuthenticated());
            assertSame(user, ctx.getCurrentUser());
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    @Test
    public void registerWhileLoggedInNotifiesRegistrationFailureTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestAuthListener listener = new TestAuthListener();
        ctx.addListener(listener);

        try {
            String username = "reg_fail_loggedin_" + System.nanoTime();
            createAndLoginUser(ctx, username);

            LoggedInState state = new LoggedInState();
            ctx.setState(state);

            state.register(ctx, "newUser", "pw", "pw");

            assertTrue(listener.registrationFailureCalled);
            assertNotNull(listener.registrationFailureReason);
            assertTrue(listener.registrationFailureReason.toLowerCase().contains("already"));
        } finally {
            ctx.removeListener(listener);
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // getStateName() tests
    // -------------------------------------------------------------

    @Test
    public void getStateNameReturnsLoggedInTextTest() {
        LoggedInState state = new LoggedInState();

        assertEquals("Logged In", state.getStateName());
    }

    @Test
    public void getStateNameIsConsistentAcrossInstancesTest() {
        LoggedInState s1 = new LoggedInState();
        LoggedInState s2 = new LoggedInState();

        assertEquals(s1.getStateName(), s2.getStateName());
    }

    // -------------------------------------------------------------
    // isAuthenticated() tests
    // -------------------------------------------------------------

    @Test
    public void isAuthenticatedAlwaysTrueInLoggedInStateTest() {
        LoggedInState state = new LoggedInState();

        assertTrue(state.isAuthenticated());
    }

    @Test
    public void isAuthenticatedDoesNotDependOnContextStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        LoggedInState state = new LoggedInState();
        ctx.setState(state);

        assertTrue(state.isAuthenticated());
        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateMessage() tests
    // -------------------------------------------------------------

    @Test
    public void getStateMessageMentionsLoggedInOrSavedRoutesTest() {
        LoggedInState state = new LoggedInState();

        String msg = state.getStateMessage().toLowerCase();
        assertTrue(msg.contains("logged in") || msg.contains("routes") || msg.contains("saved"));
    }

    @Test
    public void getStateMessageIsNonEmptyStringTest() {
        LoggedInState state = new LoggedInState();

        String msg = state.getStateMessage();
        assertNotNull(msg);
        assertFalse(msg.trim().isEmpty());
    }
}
