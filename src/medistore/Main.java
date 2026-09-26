package medistore;

import javax.swing.*;
import medistore.dao.UserDao;
import medistore.db.Database;
import medistore.model.User;
import medistore.ui.LoginForm;
import medistore.ui.MainFrame;

/**
 * Starting point of the Medical Store Management System.
 *
 * Sets the look and feel, makes sure the database exists, then shows the login
 * window; the main window only appears once a login succeeds.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        applyLookAndFeel();
        // Touching the database here means any problem with the data file is
        // reported before a window is drawn, not halfway through a sale.
        Database.get();
        ensureAdministrator();
        SwingUtilities.invokeLater(Main::startLogin);
    }

    /**
     * A brand new database has no users, and with nobody able to sign in nobody could
     * ever add one. So the first start creates the default administrator account.
     */
    private static void ensureAdministrator() {
        UserDao users = new UserDao();
        if (users.findAll().isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setRole("Administrator");
            users.save(admin, "admin123");
        }
    }

    /** Shows the login window, and the main window behind it once it succeeds. */
    public static void startLogin() {
        LoginForm login = new LoginForm();
        login.setVisible(true);
        if (login.isSucceeded()) {
            new MainFrame().setVisible(true);
        } else {
            System.exit(0);
        }
    }

    public static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("Table.showVerticalLines", false);
        } catch (Exception e) {
            // The application works with the default look too; appearance is not
            // worth failing a startup over.
            System.err.println("Falling back to the default look and feel: " + e.getMessage());
        }
    }
}
