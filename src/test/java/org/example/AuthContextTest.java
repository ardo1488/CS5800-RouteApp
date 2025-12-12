package org.example;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuthContextTest {

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
            ((List<?>) f.get(ctx)).clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestListener implements AuthContext.AuthStateListener {
        boolean stateChangedCalled;
        AuthState oldStateSeen;
        AuthState newStateSeen;

        boolean loginSuccessCalled;
        UserProfile loginSuccessUser;

        boolean loginFailureCalled;
        String loginFailureReason;

        boolean logoutCalled;

        boolean registrationSuccessCalled;
        UserProfile registrationSuccessUser;

        boolean registrationFailureCalled;
        String registrationFailureReason;

        @Override
        public void onStateChanged(AuthState oldState, AuthState newState) {
            stateChangedCalled = true;
            oldStateSeen = oldState;
            newStateSeen = newState;
        }

        @Override
        public void onLoginSuccess(UserProfile user) {
            loginSuccessCalled = true;
            loginSuccessUser = user;
        }

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
        public void onRegistrationSuccess(UserProfile user) {
            registrationSuccessCalled = true;
            registrationSuccessUser = user;
        }

        @Override
        public void onRegistrationFailure(String reason) {
            registrationFailureCalled = true;
            registrationFailureReason = reason;
        }
    }

    // -------------------------------------------------------------
    // getInstance() tests
    // -------------------------------------------------------------

    @Test
    public void getInstanceNeverReturnsNullTest() {
        AuthContext ctx = AuthContext.getInstance();
        assertNotNull(ctx);
    }

    @Test
    public void getInstanceReturnsSameSingletonInstanceTest() {
        AuthContext ctx1 = AuthContext.getInstance();
        AuthContext ctx2 = AuthContext.getInstance();
        assertSame(ctx1, ctx2);
    }

    // -------------------------------------------------------------
    // setState(...) tests
    // -------------------------------------------------------------

    @Test
    public void setStateChangesCurrentStateAndNotifiesListenersTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener = new TestListener();
        ctx.addListener(listener);

        AuthState oldState = ctx.getState();
        AuthState newState = new LoggedInState();

        ctx.setState(newState);

        assertSame(newState, ctx.getState());
        assertTrue(listener.stateChangedCalled);
        assertSame(oldState, listener.oldStateSeen);
        assertSame(newState, listener.newStateSeen);

        resetContext(ctx);
    }

    @Test
    public void setStateUpdatesStateNameAndAuthenticationFlagBasedOnNewStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.setState(new LoggedInState());

        assertEquals("Logged In", ctx.getStateName());
        assertTrue(ctx.isAuthenticated());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getState() tests
    // -------------------------------------------------------------

    @Test
    public void getStateInitiallyLoggedOutStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        assertTrue(ctx.getState() instanceof LoggedOutState);
    }

    @Test
    public void getStateReflectsLastSetStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        AuthState custom = new LoggedInState();
        ctx.setState(custom);

        assertSame(custom, ctx.getState());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // login(...) tests
    // -------------------------------------------------------------

    @Test
    public void loginFromLoggedOutTransitionsToLoggingInStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.login("userLoginTest", "pw");

        assertTrue(ctx.getState() instanceof LoggingInState);
        resetContext(ctx);
    }

    @Test
    public void loginWhileLoggedInDoesNotChangeCurrentUserForSameUsernameTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        // prepare a current user
        UserProfile user = new UserProfile(1);
        user.setUserName("sameUser");
        ctx.setCurrentUser(user);
        ctx.setState(new LoggedInState());

        ctx.login("sameUser", "any");

        assertSame(user, ctx.getCurrentUser());
        assertTrue(ctx.getState() instanceof LoggedInState);

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // logout() tests
    // -------------------------------------------------------------

    @Test
    public void logoutFromLoggedInClearsCurrentUserAndTransitionsToLoggedOutTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        UserProfile user = new UserProfile(2);
        user.setUserName("logoutUser");
        ctx.setCurrentUser(user);
        ctx.setState(new LoggedInState());

        ctx.logout();

        assertNull(ctx.getCurrentUser());
        assertTrue(ctx.getState() instanceof LoggedOutState);
        assertFalse(ctx.isAuthenticated());

        resetContext(ctx);
    }

    @Test
    public void logoutWhileLoggedOutKeepsStateLoggedOutAndNotifiesLoginFailureTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener = new TestListener();
        ctx.addListener(listener);

        ctx.logout();  // LoggedOutState.logout triggers notifyLoginFailure("Already logged out")

        assertTrue(ctx.getState() instanceof LoggedOutState);
        assertTrue(listener.loginFailureCalled);
        assertNotNull(listener.loginFailureReason);
        assertTrue(listener.loginFailureReason.toLowerCase().contains("already"));

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // register(...) tests
    // -------------------------------------------------------------

    @Test
    public void registerFromLoggedOutTransitionsToRegisteringStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.register("newUser", "pw", "pw");

        assertTrue(ctx.getState() instanceof RegisteringState);
        resetContext(ctx);
    }

    @Test
    public void registerFromLoggedInDoesNotChangeStateAndNotifiesRegistrationFailureTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.setState(new LoggedInState());
        TestListener listener = new TestListener();
        ctx.addListener(listener);

        ctx.register("anotherUser", "pw", "pw");

        assertTrue(ctx.getState() instanceof LoggedInState);
        assertTrue(listener.registrationFailureCalled);
        assertNotNull(listener.registrationFailureReason);

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // isAuthenticated() tests
    // -------------------------------------------------------------

    @Test
    public void isAuthenticatedReflectsLoggedOutStateAsFalseTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        assertFalse(ctx.isAuthenticated());
    }

    @Test
    public void isAuthenticatedReflectsLoggedInStateAsTrueTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.setState(new LoggedInState());

        assertTrue(ctx.isAuthenticated());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateName() tests
    // -------------------------------------------------------------

    @Test
    public void getStateNameReturnsLoggedOutForDefaultStateTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        assertEquals("Logged Out", ctx.getStateName());
    }

    @Test
    public void getStateNameMatchesCurrentStateNameAfterTransitionTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.setState(new LoggedInState());

        assertEquals("Logged In", ctx.getStateName());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getStateMessage() tests
    // -------------------------------------------------------------

    @Test
    public void getStateMessageForLoggedOutMentionsLoginOrAccountTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        String msg = ctx.getStateMessage().toLowerCase();
        assertTrue(msg.contains("log in") || msg.contains("login") || msg.contains("account"));
    }

    @Test
    public void getStateMessageForLoggedInMentionsLoggedInOrSavingTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        ctx.setState(new LoggedInState());

        String msg = ctx.getStateMessage().toLowerCase();
        assertTrue(msg.contains("logged in") || msg.contains("save"));

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // setCurrentUser(...) tests
    // -------------------------------------------------------------

    @Test
    public void setCurrentUserStoresGivenUserAndGetCurrentUserReturnsSameTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        UserProfile user = new UserProfile(5);
        user.setUserName("TestUser");

        ctx.setCurrentUser(user);

        assertSame(user, ctx.getCurrentUser());

        resetContext(ctx);
    }

    @Test
    public void setCurrentUserCanOverwritePreviousUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        UserProfile user1 = new UserProfile(1);
        UserProfile user2 = new UserProfile(2);

        ctx.setCurrentUser(user1);
        ctx.setCurrentUser(user2);

        assertSame(user2, ctx.getCurrentUser());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getCurrentUser() tests
    // -------------------------------------------------------------

    @Test
    public void getCurrentUserInitiallyNullTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        assertNull(ctx.getCurrentUser());
    }

    @Test
    public void getCurrentUserReflectsLastSetUserTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        UserProfile user = new UserProfile(9);
        ctx.setCurrentUser(user);

        assertSame(user, ctx.getCurrentUser());

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // getDatabase() tests
    // -------------------------------------------------------------

    @Test
    public void getDatabaseNeverReturnsNullTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        Database db = ctx.getDatabase();
        assertNotNull(db);

        resetContext(ctx);
    }

    @Test
    public void getDatabaseReturnsSameInstanceAsDatabaseSingletonTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        Database fromContext = ctx.getDatabase();
        Database global = Database.getInstance();

        assertSame(global, fromContext);

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // addListener(...) tests
    // -------------------------------------------------------------

    @Test
    public void addListenerRegistersListenerAndPreventsDuplicatesTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener = new TestListener();
        ctx.addListener(listener);
        ctx.addListener(listener); // duplicate

        Field f = AuthContext.class.getDeclaredField("listeners");
        f.setAccessible(true);
        List<?> list = (List<?>) f.get(ctx);

        assertEquals(1, list.size());

        resetContext(ctx);
    }

    @Test
    public void addListenerReceivesCallbacksOnStateChangeTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener = new TestListener();
        ctx.addListener(listener);

        ctx.setState(new LoggedInState());

        assertTrue(listener.stateChangedCalled);
        assertTrue(listener.newStateSeen instanceof LoggedInState);

        resetContext(ctx);
    }

    // -------------------------------------------------------------
    // removeListener(...) tests
    // -------------------------------------------------------------

    @Test
    public void removeListenerStopsReceivingCallbacksTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener = new TestListener();
        ctx.addListener(listener);
        ctx.removeListener(listener);

        ctx.setState(new LoggedInState());

        assertFalse(listener.stateChangedCalled);

        resetContext(ctx);
    }

    @Test
    public void removeListenerOnListenerNotInListDoesNotThrowAndKeepsOthersTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);

        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();

        ctx.addListener(listener1);

        assertDoesNotThrow(() -> ctx.removeListener(listener2));

        Field f = AuthContext.class.getDeclaredField("listeners");
        f.setAccessible(true);
        List<?> list = (List<?>) f.get(ctx);

        assertEquals(1, list.size());
        assertSame(listener1, list.get(0));

        resetContext(ctx);
    }
}
