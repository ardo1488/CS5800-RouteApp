package org.example;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class LoginDialogTest {

    // ---------- reflection helpers that walk superclasses ----------

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return (T) f.get(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private static void setBooleanField(Object target, String name, boolean value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.setBoolean(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private static boolean getBooleanField(Object target, String name) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.getBoolean(target);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException(new NoSuchFieldException(name));
    }

    private AuthContext getContext() {
        return AuthContext.getInstance();
    }

    private void resetContext(AuthContext ctx) {
        // your LoggedOutState should have a no-arg ctor
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

    /**
     * Test subclass that overrides setVisible so tests don't block or show a window.
     */
    private static class TestLoginDialog extends LoginDialog {
        boolean setVisibleCalled;

        public TestLoginDialog(Frame parent) {
            super(parent);
        }

        @Override
        public void setVisible(boolean b) {
            setVisibleCalled = true;
            // do NOT call super.setVisible(b) to avoid blocking UI
        }
    }

    // -------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------

    @Test
    public void constructorInitializesUiComponentsNonNullTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            JTextField loginUser = getField(dialog, "loginUsernameField", JTextField.class);
            JPasswordField loginPass = getField(dialog, "loginPasswordField", JPasswordField.class);
            JButton loginButton = getField(dialog, "loginButton", JButton.class);
            JLabel loginStatus = getField(dialog, "loginStatusLabel", JLabel.class);

            JTextField regUser = getField(dialog, "registerUsernameField", JTextField.class);
            JPasswordField regPass = getField(dialog, "registerPasswordField", JPasswordField.class);
            JPasswordField regConfirm = getField(dialog, "registerConfirmField", JPasswordField.class);
            JButton regButton = getField(dialog, "registerButton", JButton.class);
            JLabel regStatus = getField(dialog, "registerStatusLabel", JLabel.class);

            JTabbedPane tabs = getField(dialog, "tabbedPane", JTabbedPane.class);

            assertNotNull(loginUser);
            assertNotNull(loginPass);
            assertNotNull(loginButton);
            assertNotNull(loginStatus);
            assertNotNull(regUser);
            assertNotNull(regPass);
            assertNotNull(regConfirm);
            assertNotNull(regButton);
            assertNotNull(regStatus);
            assertNotNull(tabs);
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void constructorRegistersDialogAsAuthStateListenerTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            // Just make sure notifying state change with nulls doesn't blow up
            assertDoesNotThrow(() -> ctx.notifyStateChanged(null, null));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // showDialogAndLoginStatus() tests
    // -------------------------------------------------------------

    @Test
    public void showDialogAndLoginStatusReturnsFalseWhenLoginNeverSucceededTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            boolean result = dialog.showDialogAndLoginStatus();

            assertFalse(result);
            assertTrue(dialog.setVisibleCalled);
        } finally {
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void showDialogAndLoginStatusReturnsTrueWhenLoginSuccessfulFlagSetTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            setBooleanField(dialog, "loginSuccessful", true);

            boolean result = dialog.showDialogAndLoginStatus();

            assertTrue(result);
        } finally {
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onStateChanged(...) tests
    // -------------------------------------------------------------

    @Test
    public void onStateChangedReEnablesLoginAndRegisterButtonsTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            JButton loginButton = getField(dialog, "loginButton", JButton.class);
            JButton registerButton = getField(dialog, "registerButton", JButton.class);

            loginButton.setEnabled(false);
            registerButton.setEnabled(false);

            dialog.onStateChanged(null, null);

            SwingUtilities.invokeAndWait(() -> {});

            assertTrue(loginButton.isEnabled());
            assertTrue(registerButton.isEnabled());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onStateChangedHandlesNullStatesWithoutExceptionTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            assertDoesNotThrow(() -> dialog.onStateChanged(null, null));
            SwingUtilities.invokeAndWait(() -> {});
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onLoginSuccess(...) tests
    // -------------------------------------------------------------

    @Test
    public void onLoginSuccessUpdatesStatusLabelAndSetsLoginSuccessfulTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            UserProfile user = new UserProfile(1);
            user.setUserName("Tester");

            dialog.onLoginSuccess(user);

            SwingUtilities.invokeAndWait(() -> {});

            JLabel statusLabel = getField(dialog, "loginStatusLabel", JLabel.class);
            assertTrue(statusLabel.getText().contains("Tester"));
            assertEquals(new Color(0, 128, 0), statusLabel.getForeground());
            assertTrue(getBooleanField(dialog, "loginSuccessful"));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onLoginSuccessDoesNotDisableRegisterButtonTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            UserProfile user = new UserProfile(2);
            dialog.onLoginSuccess(user);

            SwingUtilities.invokeAndWait(() -> {});

            JButton registerButton = getField(dialog, "registerButton", JButton.class);
            assertTrue(registerButton.isEnabled());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onLoginFailure(...) tests
    // -------------------------------------------------------------

    @Test
    public void onLoginFailureDisplaysErrorMessageAndResetsPasswordFieldTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            JPasswordField passwordField = getField(dialog, "loginPasswordField", JPasswordField.class);
            JButton loginButton = getField(dialog, "loginButton", JButton.class);

            passwordField.setText("secret");
            loginButton.setEnabled(false);

            dialog.onLoginFailure("Wrong password");

            SwingUtilities.invokeAndWait(() -> {});

            JLabel statusLabel = getField(dialog, "loginStatusLabel", JLabel.class);
            assertEquals("Wrong password", statusLabel.getText());
            assertEquals(Color.RED, statusLabel.getForeground());
            assertEquals("", new String(passwordField.getPassword()));
            assertTrue(loginButton.isEnabled());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onLoginFailureDoesNotSetLoginSuccessfulFlagTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            dialog.onLoginFailure("Error");
            SwingUtilities.invokeAndWait(() -> {});

            assertFalse(getBooleanField(dialog, "loginSuccessful"));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onLogout() tests
    // -------------------------------------------------------------

    @Test
    public void onLogoutDoesNotThrowOrChangeLoginSuccessfulFlagTest() {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            setBooleanField(dialog, "loginSuccessful", true);

            assertDoesNotThrow(dialog::onLogout);
            assertTrue(getBooleanField(dialog, "loginSuccessful"));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onLogoutLeavesStatusLabelsUnchangedTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            JLabel loginStatus = getField(dialog, "loginStatusLabel", JLabel.class);
            JLabel registerStatus = getField(dialog, "registerStatusLabel", JLabel.class);

            loginStatus.setText("before");
            registerStatus.setText("before");

            dialog.onLogout();
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals("before", loginStatus.getText());
            assertEquals("before", registerStatus.getText());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onRegistrationSuccess(...) tests
    // -------------------------------------------------------------

    @Test
    public void onRegistrationSuccessUpdatesRegisterStatusAndSetsLoginSuccessfulTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            UserProfile user = new UserProfile(10);
            user.setUserName("NewUser");

            dialog.onRegistrationSuccess(user);
            SwingUtilities.invokeAndWait(() -> {});

            JLabel regStatus = getField(dialog, "registerStatusLabel", JLabel.class);
            assertTrue(regStatus.getText().contains("NewUser"));
            assertEquals(new Color(0, 128, 0), regStatus.getForeground());
            assertTrue(getBooleanField(dialog, "loginSuccessful"));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onRegistrationSuccessDoesNotDisableLoginButtonTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            UserProfile user = new UserProfile(11);
            dialog.onRegistrationSuccess(user);
            SwingUtilities.invokeAndWait(() -> {});

            JButton loginButton = getField(dialog, "loginButton", JButton.class);
            assertTrue(loginButton.isEnabled());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    // -------------------------------------------------------------
    // onRegistrationFailure(...) tests
    // -------------------------------------------------------------

    @Test
    public void onRegistrationFailureDisplaysErrorAndReEnablesRegisterButtonTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            JButton registerButton = getField(dialog, "registerButton", JButton.class);
            registerButton.setEnabled(false);

            dialog.onRegistrationFailure("Username taken");
            SwingUtilities.invokeAndWait(() -> {});

            JLabel regStatus = getField(dialog, "registerStatusLabel", JLabel.class);
            assertEquals("Username taken", regStatus.getText());
            assertEquals(Color.RED, regStatus.getForeground());
            assertTrue(registerButton.isEnabled());
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }

    @Test
    public void onRegistrationFailureDoesNotSetLoginSuccessfulFlagTest() throws Exception {
        AuthContext ctx = getContext();
        resetContext(ctx);
        TestLoginDialog dialog = new TestLoginDialog(null);
        try {
            dialog.onRegistrationFailure("Error");
            SwingUtilities.invokeAndWait(() -> {});

            assertFalse(getBooleanField(dialog, "loginSuccessful"));
        } finally {
            ctx.removeListener(dialog);
            dialog.dispose();
            resetContext(ctx);
        }
    }
}
