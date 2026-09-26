package medistore.ui.master;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import medistore.dao.CustomerDao;
import medistore.dao.DataAccessException;
import medistore.model.Customer;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * Lets the counter staff register regular customers and keep their details up to date.
 */
public class CustomerForm extends BaseForm {

    private final CustomerDao customerDao = new CustomerDao();

    private final JTextField nameField = UI.textField();
    private final JTextField phoneField = UI.textField();
    private final JTextField emailField = UI.textField();
    private final JTextArea addressArea = UI.textArea(3);

    private final JButton saveButton = UI.primaryButton("Save");
    private final JButton updateButton = UI.secondaryButton("Update");
    private final JButton deleteButton = UI.dangerButton("Delete");
    private final JButton clearButton = UI.secondaryButton("Clear");

    private final JTextField searchField = UI.textField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Name", "Phone", "Email", "Address", "Registered On"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UI.table(tableModel);

    private List<Customer> currentRows = List.of();
    private Customer selected;

    public CustomerForm() {
        super("Customer Master", "People who buy at the counter");

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        JPanel entryCard = buildEntryCard();
        entryCard.setPreferredSize(new Dimension(420, 10));
        body.add(entryCard, BorderLayout.WEST);
        body.add(buildListCard(), BorderLayout.CENTER);
        setBody(body);

        // Phone numbers and dates are never ellipsised, so those columns get a floor width.
        // The e-mail address stays in the data and loads into the form when a row is picked;
        // it is left out of the list so names and addresses are not cut off.
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(2));
        int[] widths = {170, 100, 200, 100};
        int[] minimum = {110, 96, 110, 92};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimum[i]);
        }
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or phone");

        wireEvents();
        clearForm();
    }

    private JPanel buildEntryCard() {
        FormGrid grid = new FormGrid(2)
                .add("Customer Name", nameField)
                .add("Phone", phoneField)
                .add("Email", emailField)
                .skip()
                .add("Address", UI.scroll(addressArea), 2);

        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.add(grid.panel(), BorderLayout.NORTH);
        formPanel.add(buttonRow(), BorderLayout.CENTER);
        return UI.card("Customer Details", formPanel);
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
        return UI.card("Customers", listBody);
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

    private void reloadTable() {
        try {
            currentRows = customerDao.search(searchField.getText());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Customer c : currentRows) {
            tableModel.addRow(new Object[]{
                    c.getCustomerName(), c.getPhone(), c.getEmail(), c.getAddress(),
                    UI.displayDate(c.getRegisteredOn())
            });
        }
    }

    private void loadRow(Customer c) {
        selected = c;
        nameField.setText(c.getCustomerName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        addressArea.setText(c.getAddress());
        setEditing(true);
    }

    private void clearForm() {
        selected = null;
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressArea.setText("");
        table.clearSelection();
        setEditing(false);
    }

    private void setEditing(boolean editing) {
        updateButton.setEnabled(editing);
        deleteButton.setEnabled(editing);
    }

    private void save() {
        if (!inputIsValid()) {
            return;
        }
        Customer c = new Customer();
        fill(c);
        try {
            customerDao.save(c);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Customer added.");
        clearForm();
    }

    private void update() {
        if (selected == null || !inputIsValid()) {
            return;
        }
        Customer c = new Customer();
        c.setCustomerId(selected.getCustomerId());
        fill(c);
        try {
            customerDao.save(c);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Customer updated.");
        clearForm();
    }

    private void delete() {
        if (selected == null) {
            return;
        }
        if (!UI.confirm(this, "Delete customer \"" + selected.getCustomerName() + "\"?")) {
            return;
        }
        try {
            customerDao.delete(selected.getCustomerId());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Customer deleted.");
        clearForm();
    }

    private void fill(Customer c) {
        c.setCustomerName(nameField.getText().trim());
        c.setPhone(phoneField.getText().trim());
        c.setEmail(emailField.getText().trim());
        c.setAddress(addressArea.getText().trim());
    }

    private boolean inputIsValid() {
        Validator v = new Validator();
        v.required(nameField, "Customer name");
        v.required(phoneField, "Phone");
        v.phone(phoneField, "Phone");
        v.email(emailField, "Email");
        return v.showIfInvalid(this);
    }
}
