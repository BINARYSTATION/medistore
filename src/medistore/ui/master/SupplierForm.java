package medistore.ui.master;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.SupplierDao;
import medistore.model.Supplier;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * Lets the shop keep a master list of the distributors it buys stock from.
 */
public class SupplierForm extends BaseForm {

    private final SupplierDao supplierDao = new SupplierDao();

    private final JTextField nameField = UI.textField();
    private final JTextField contactPersonField = UI.textField();
    private final JTextField phoneField = UI.textField();
    private final JTextField emailField = UI.textField();
    private final JTextField gstField = UI.textField();
    private final JTextArea addressArea = UI.textArea(3);

    private final JButton saveButton = UI.primaryButton("Save");
    private final JButton updateButton = UI.secondaryButton("Update");
    private final JButton deleteButton = UI.dangerButton("Delete");
    private final JButton clearButton = UI.secondaryButton("Clear");

    private final JTextField searchField = UI.textField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Name", "Contact Person", "Phone", "Email", "GST No"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UI.table(tableModel);

    private List<Supplier> currentRows = List.of();
    private Supplier selected;

    public SupplierForm() {
        super("Supplier Master", "Distributors you buy stock from");

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        JPanel entryCard = buildEntryCard();
        entryCard.setPreferredSize(new Dimension(420, 10));
        body.add(entryCard, BorderLayout.WEST);
        body.add(buildListCard(), BorderLayout.CENTER);
        setBody(body);

        // Phone numbers are never ellipsised, so that column gets a floor width.
        // The GST number stays in the data and loads into the form when a row is picked;
        // it is left out of the list so names and e-mail addresses are not cut off.
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(4));
        int[] widths = {190, 130, 100, 210};
        int[] minimum = {110, 90, 96, 120};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(minimum[i]);
        }
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, contact person or phone");

        wireEvents();
        clearForm();
    }

    private JPanel buildEntryCard() {
        FormGrid grid = new FormGrid(2)
                .add("Supplier Name", nameField)
                .add("Contact Person", contactPersonField)
                .add("Phone", phoneField)
                .add("Email", emailField)
                .add("GST Number", gstField)
                .skip()
                .add("Address", UI.scroll(addressArea), 2);

        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.add(grid.panel(), BorderLayout.NORTH);
        formPanel.add(buttonRow(), BorderLayout.CENTER);
        return UI.card("Supplier Details", formPanel);
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
        return UI.card("Suppliers", listBody);
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
            currentRows = supplierDao.search(searchField.getText());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Supplier s : currentRows) {
            tableModel.addRow(new Object[]{
                    s.getSupplierName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getGstNumber()
            });
        }
    }

    private void loadRow(Supplier s) {
        selected = s;
        nameField.setText(s.getSupplierName());
        contactPersonField.setText(s.getContactPerson());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
        gstField.setText(s.getGstNumber());
        addressArea.setText(s.getAddress());
        setEditing(true);
    }

    private void clearForm() {
        selected = null;
        nameField.setText("");
        contactPersonField.setText("");
        phoneField.setText("");
        emailField.setText("");
        gstField.setText("");
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
        Supplier s = new Supplier();
        fill(s);
        try {
            supplierDao.save(s);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Supplier added.");
        clearForm();
    }

    private void update() {
        if (selected == null || !inputIsValid()) {
            return;
        }
        Supplier s = new Supplier();
        s.setSupplierId(selected.getSupplierId());
        fill(s);
        try {
            supplierDao.save(s);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Supplier updated.");
        clearForm();
    }

    private void delete() {
        if (selected == null) {
            return;
        }
        if (!UI.confirm(this, "Delete supplier \"" + selected.getSupplierName() + "\"?")) {
            return;
        }
        try {
            supplierDao.delete(selected.getSupplierId());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Supplier deleted.");
        clearForm();
    }

    private void fill(Supplier s) {
        s.setSupplierName(nameField.getText().trim());
        s.setContactPerson(contactPersonField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setGstNumber(gstField.getText().trim());
        s.setAddress(addressArea.getText().trim());
    }

    private boolean inputIsValid() {
        Validator v = new Validator();
        v.required(nameField, "Supplier name");
        v.required(phoneField, "Phone");
        v.phone(phoneField, "Phone");
        v.email(emailField, "Email");
        return v.showIfInvalid(this);
    }
}
