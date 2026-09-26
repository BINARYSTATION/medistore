package medistore.ui;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import medistore.dao.DataAccessException;
import medistore.dao.UserDao;
import medistore.model.User;
import medistore.util.Session;
import medistore.util.UI;

/**
 * The sign-in window - the first screen the user meets.
 *
 * Shown as a modal dialog before the main window is built, so nothing in the
 * application can be reached without a valid account.
 */
public class LoginForm extends JDialog {

    private final JTextField usernameField = UI.textField();
    private final JPasswordField passwordField = UI.passwordField();
    private final JLabel messageLabel = UI.label(" ", UI.small(), UI.DANGER);
    private final JButton loginButton = UI.primaryButton("Sign In");
    private final UserDao userDao = new UserDao();

    private boolean succeeded;

    public LoginForm() {
        super((Frame) null, "Medical Store Management System", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(loginButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(buildBanner(), BorderLayout.WEST);
        root.add(buildForm(), BorderLayout.CENTER);
        return root;
    }

    /** The teal panel on the left carrying the product name. */
    private JPanel buildBanner() {
        JPanel banner = new JPanel();
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBackground(UI.SIDEBAR);
        banner.setBorder(UI.padding(48, 36, 48, 36));
        banner.setPreferredSize(new Dimension(300, 380));

        JLabel mark = UI.label("✚", UI.font(52, Font.BOLD), Color.WHITE);
        mark.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = UI.label("MediStore", UI.font(28, Font.BOLD), Color.WHITE);
        name.setAlignmentX(LEFT_ALIGNMENT);
        name.setBorder(UI.padding(16, 0, 0, 0));

        JLabel tagline = UI.label("<html>Medical Store<br>Management System</html>",
                UI.font(14, Font.PLAIN), new Color(0x99, 0xF6, 0xE4));
        tagline.setAlignmentX(LEFT_ALIGNMENT);
        tagline.setBorder(UI.padding(8, 0, 0, 0));

        JLabel footer = UI.label("Stock · Billing · Reports",
                UI.font(12, Font.PLAIN), new Color(0x5E, 0xEA, 0xD4));
        footer.setAlignmentX(LEFT_ALIGNMENT);

        banner.add(mark);
        banner.add(name);
        banner.add(tagline);
        banner.add(Box.createVerticalGlue());
        banner.add(footer);
        return banner;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(UI.padding(48, 40, 40, 40));
        form.setPreferredSize(new Dimension(340, 380));

        JLabel welcome = UI.label("Welcome back", UI.h1(), UI.TEXT);
        JLabel hint = UI.muted("Sign in to continue to the store");

        usernameField.setPreferredSize(new Dimension(260, 38));
        passwordField.setPreferredSize(new Dimension(260, 38));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setAlignmentX(LEFT_ALIGNMENT);

        loginButton.addActionListener(e -> attemptLogin());

        for (JComponent c : new JComponent[]{welcome, hint, usernameField, passwordField,
                messageLabel, loginButton}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
        }

        form.add(welcome);
        hint.setBorder(UI.padding(4, 0, 26, 0));
        form.add(hint);
        form.add(labelled("USER NAME", usernameField));
        form.add(Box.createVerticalStrut(16));
        form.add(labelled("PASSWORD", passwordField));
        form.add(Box.createVerticalStrut(10));
        form.add(messageLabel);
        form.add(Box.createVerticalStrut(14));
        form.add(loginButton);
        form.add(Box.createVerticalGlue());
        form.add(UI.muted("Default account:  admin  /  admin123"));
        return form;
    }

    private JPanel labelled(String caption, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        p.add(UI.fieldLabel(caption), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter both the user name and the password.");
            return;
        }
        try {
            User user = userDao.authenticate(username, password);
            if (user == null) {
                showMessage("That user name and password do not match.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }
            Session.setUser(user);
            succeeded = true;
            dispose();
        } catch (DataAccessException e) {
            showMessage(e.getMessage());
        }
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
        Toolkit.getDefaultToolkit().beep();
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}
