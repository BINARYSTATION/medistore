package medistore.ui.master;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.UserDao;
import medistore.model.User;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.Session;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * Lets the administrator create login accounts, change roles and reset passwords.
 *
 * Passwords are never loaded back into the form or shown in the table; leaving
 * both password boxes empty while editing keeps the existing password.
 */
public class UserForm extends BaseForm {

    private static final String[] ROLES = {"Administrator", "Pharmacist", "Counter Staff"};

    private final UserDao userDao = new UserDao();

    private final JTextField fullNameField = UI.textField();
    private final JTextField usernameField = UI.textField();
    private final JPasswordField passwordField = UI.passwordField();
    private final JPasswordField confirmField = UI.passwordField();
    private final JComboBox<String> roleCombo = UI.comboBox();

    private final JButton saveButton = UI.primaryButton("Save");
    private final JButton updateButton = UI.secondaryButton("Update");
    private final JButton deleteButton = UI.dangerButton("Delete");
    private final JButton clearButton = UI.secondaryButton("Clear");

    private final JTextField searchField = UI.textField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Full Name", "User Name", "Role", "Created On"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UI.table(tableModel);

    private List<User> currentRows = List.of();
    private User selected;

    public UserForm() {
        super("User Accounts", "Who may sign in to the software");
        for (String role : ROLES) {
            roleCombo.addItem(role);
        }
        passwordField.setToolTipText("When editing, leave both password boxes empty to keep the current password");

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        JPanel entryCard = buildEntryCard();
        entryCard.setPreferredSize(new Dimension(420, 10));
        body.add(entryCard, BorderLayout.WEST);
        body.add(buildListCard(), BorderLayout.CENTER);
        setBody(body);

        // Dates are never ellipsised, so that column gets a floor width.
        int[] widths = {150, 120, 110, 95};
        int[] minimum = {40,  40,  40, 92};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimum[i]);
        }
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, user name or role");

        wireEvents();
        clearForm();
    }

    private JPanel buildEntryCard() {
        FormGrid grid = new FormGrid(2)
                .add("Full Name", fullNameField)
                .add("User Name", usernameField)
                .add("Password", passwordField)
                .add("Confirm Password", confirmField)
                .add("Role", roleCombo);

        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.add(grid.panel(), BorderLayout.NORTH);
        formPanel.add(buttonRow(), BorderLayout.CENTER);
        return UI.card("User Details", formPanel);
    }

    /** Left-aligned so the first button lines up with the edge of the fields above it. */
    private JPanel buttonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(saveButton);
        for (JButton button : new JButton[]{updateButton, deleteButton, clearButton}) {
            row.add(Box.createHorizontalStrut(10));
            row.add(button);
        }
        return row;
    }

    private JPanel buildListCard() {
        JPanel listBody = new JPanel(new BorderLayout(0, 12));
        listBody.setOpaque(false);
        listBody.add(searchField, BorderLayout.NORTH);
        listBody.add(UI.scroll(table), BorderLayout.CENTER);
        return UI.card("Users", listBody);
    }

    private void wireEvents() {
        saveButton.addActionListener(e -> save());
        updateButton.addActionListener(e -> update());
        deleteButton.addActionListener(e -> delete());
        clearButton.addActionListener(e -> clearForm());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void removeUpdate(DocumentEvent e) { reloadTable(); }
            @Override public void changedUpdate(DocumentEvent e) { reloadTable(); }
        });

        // Cells are ellipsised when the window is narrow, so show the full text on hover.
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                table.setToolTipText(row < 0 || column < 0
                        ? null : String.valueOf(table.getValueAt(row, column)));
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0 && row < currentRows.size()) {
                loadRow(currentRows.get(row));
            }
        });
    }

    @Override
    public void onShow() {
        reloadTable();
    }

    /** UserDao has no search query, so the short user list is filtered here instead. */
    private void reloadTable() {
        List<User> all;
        try {
            all = userDao.findAll();
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        String term = searchField.getText().trim().toLowerCase(Locale.ROOT);
        currentRows = new ArrayList<>();
        for (User u : all) {
            if (term.isEmpty() || matches(u, term)) {
                currentRows.add(u);
            }
        }
        tableModel.setRowCount(0);
        for (User u : currentRows) {
            tableModel.addRow(new Object[]{
                    u.getFullName(), u.getUsername(), u.getRole(), UI.displayDate(u.getCreatedOn())
            });
        }
    }

    private boolean matches(User u, String term) {
        return u.getFullName().toLowerCase(Locale.ROOT).contains(term)
                || u.getUsername().toLowerCase(Locale.ROOT).contains(term)
                || u.getRole().toLowerCase(Locale.ROOT).contains(term);
    }

    private void loadRow(User u) {
        selected = u;
        fullNameField.setText(u.getFullName());
        usernameField.setText(u.getUsername());
        passwordField.setText("");
        confirmField.setText("");
        roleCombo.setSelectedItem(u.getRole());
        setEditing(true);
    }

    private void clearForm() {
        selected = null;
        fullNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        roleCombo.setSelectedIndex(1);
        table.clearSelection();
        setEditing(false);
    }

    private void setEditing(boolean editing) {
        updateButton.setEnabled(editing);
        deleteButton.setEnabled(editing);
    }

    private void save() {
        if (!inputIsValid(0)) {
            return;
        }
        User u = new User();
        fill(u);
        try {
            userDao.save(u, new String(passwordField.getPassword()));
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "User account created.");
        clearForm();
    }

    private void update() {
        if (selected == null || !inputIsValid(selected.getUserId())) {
            return;
        }
        User u = new User();
        u.setUserId(selected.getUserId());
        u.setCreatedOn(selected.getCreatedOn());
        fill(u);
        String typed = new String(passwordField.getPassword());
        try {
            userDao.save(u, typed.isEmpty() ? null : typed);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "User account updated.");
        clearForm();
    }

    private void delete() {
        if (selected == null) {
            return;
        }
        if (selected.getUserId() == Session.userId()) {
            UI.error(this, "You cannot delete the account you are signed in with.");
            return;
        }
        if (!UI.confirm(this, "Delete the account \"" + selected.getUsername() + "\"?")) {
            return;
        }
        try {
            userDao.delete(selected.getUserId());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "User account deleted.");
        clearForm();
    }

    private void fill(User u) {
        u.setFullName(fullNameField.getText().trim());
        u.setUsername(usernameField.getText().trim());
        u.setRole((String) roleCombo.getSelectedItem());
    }

    /** {@code userId} is 0 for a new account, otherwise the account being edited. */
    private boolean inputIsValid(int userId) {
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        Validator v = new Validator();
        v.required(fullNameField, "Full name");
        v.required(usernameField, "User name");
        v.selected(roleCombo, roleCombo.getSelectedItem(), "role");
        if (userId == 0) {
            v.check(!password.isEmpty(), "Password is required.");
            v.check(!confirm.isEmpty(), "Please confirm the password.");
        }
        v.check(password.equals(confirm), "The two passwords do not match.");
        try {
            v.check(!userDao.usernameExists(usernameField.getText(), userId),
                    "That user name is already taken.");
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return false;
        }
        return v.showIfInvalid(this);
    }
}
