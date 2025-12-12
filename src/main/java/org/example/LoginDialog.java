package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Dialog for user login and registration.
 *
 * Provides a tabbed interface for:
 * - Login with existing account
 * - Register new account
 */

public class LoginDialog extends JDialog implements AuthContext.AuthStateListener {

    private final AuthContext authContext;

    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JButton loginButton;
    private JLabel loginStatusLabel;

    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JPasswordField registerConfirmField;
    private JButton registerButton;
    private JLabel registerStatusLabel;


    private JTabbedPane tabbedPane;

    private boolean loginSuccessful = false;

    public LoginDialog(Frame parent) {
        super(parent, "Login / Register", true);
        this.authContext = AuthContext.getInstance();

        initializeUI();

        authContext.addListener(this);

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();

        JPanel loginPanel = createLoginPanel();
        tabbedPane.addTab("Login", loginPanel);

        JPanel registerPanel = createRegisterPanel();
        tabbedPane.addTab("Register", registerPanel);

        add(tabbedPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            loginSuccessful = false;
            dispose();
        });
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Username label
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);

        // Username field
        loginUsernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(loginUsernameField, gbc);

        // Password label
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);

        // Password field
        loginPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(loginPasswordField, gbc);

        // Login button
        loginButton = new JButton("Login");
        loginButton.addActionListener(this::handleLogin);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        panel.add(loginButton, gbc);

        // Status label
        loginStatusLabel = new JLabel(" ");
        loginStatusLabel.setForeground(Color.RED);
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(loginStatusLabel, gbc);

        // Enter key to login
        loginPasswordField.addActionListener(this::handleLogin);
        loginUsernameField.addActionListener(e -> loginPasswordField.requestFocus());

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        // Username label
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);

        // Username field
        registerUsernameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(registerUsernameField, gbc);

        // Password label
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);

        // Password field
        registerPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(registerPasswordField, gbc);

        // Confirm password label
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Confirm Password:"), gbc);

        // Confirm password field
        registerConfirmField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(registerConfirmField, gbc);

        // Register button
        registerButton = new JButton("Create Account");
        registerButton.addActionListener(this::handleRegister);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;
        panel.add(registerButton, gbc);

        // Status label
        registerStatusLabel = new JLabel(" ");
        registerStatusLabel.setForeground(Color.RED);
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(registerStatusLabel, gbc);

        // Enter key to register
        registerConfirmField.addActionListener(this::handleRegister);

        return panel;
    }

    private void handleLogin(ActionEvent e) {
        String username = loginUsernameField.getText();
        String password = new String(loginPasswordField.getPassword());

        loginButton.setEnabled(false);
        loginStatusLabel.setText("Logging in...");
        loginStatusLabel.setForeground(Color.BLUE);

        // Process login through state pattern
        authContext.login(username, password);

        // The state will handle the transition and notify us via listener
        if (authContext.getState() instanceof LoggingInState) {
            ((LoggingInState) authContext.getState()).processLoginAttempt(authContext);
        }
    }

    private void handleRegister(ActionEvent e) {
        String username = registerUsernameField.getText();
        String password = new String(registerPasswordField.getPassword());
        String confirm = new String(registerConfirmField.getPassword());

        registerButton.setEnabled(false);
        registerStatusLabel.setText("Creating account...");
        registerStatusLabel.setForeground(Color.BLUE);

        // Process registration through state pattern
        authContext.register(username, password, confirm);

        // The state will handle the transition and notify us via listener
        if (authContext.getState() instanceof RegisteringState) {
            ((RegisteringState) authContext.getState()).processRegistration(authContext);
        }
    }



    @Override
    public void onStateChanged(AuthState oldState, AuthState newState) {
        // Update UI based on new state
        SwingUtilities.invokeLater(() -> {
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);
        });
    }

    @Override
    public void onLoginSuccess(UserProfile user) {
        SwingUtilities.invokeLater(() -> {
            loginStatusLabel.setText("Welcome, " + user.getUserName() + "!");
            loginStatusLabel.setForeground(new Color(0, 128, 0));
            loginSuccessful = true;

            // Close dialog after brief delay
            Timer timer = new Timer(500, e -> dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    @Override
    public void onLoginFailure(String reason) {
        SwingUtilities.invokeLater(() -> {
            loginStatusLabel.setText(reason);
            loginStatusLabel.setForeground(Color.RED);
            loginButton.setEnabled(true);
            loginPasswordField.setText("");
            loginPasswordField.requestFocus();
        });
    }

    @Override
    public void onLogout() {

    }

    @Override
    public void onRegistrationSuccess(UserProfile user) {
        SwingUtilities.invokeLater(() -> {
            registerStatusLabel.setText("Account created! Welcome, " + user.getUserName() + "!");
            registerStatusLabel.setForeground(new Color(0, 128, 0));
            loginSuccessful = true;

            // Close dialog after brief delay
            Timer timer = new Timer(500, e -> dispose());
            timer.setRepeats(false);
            timer.start();
        });
    }

    @Override
    public void onRegistrationFailure(String reason) {
        SwingUtilities.invokeLater(() -> {
            registerStatusLabel.setText(reason);
            registerStatusLabel.setForeground(Color.RED);
            registerButton.setEnabled(true);
        });
    }


    public boolean showDialogAndLoginStatus() {
        setVisible(true);
        // Remove listener when done
        authContext.removeListener(this);
        return loginSuccessful;
    }
}