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
import medistore.dao.MedicineDao;
import medistore.dao.SupplierDao;
import medistore.model.Medicine;
import medistore.model.Supplier;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * Lets the pharmacist add, edit, search and retire medicine batches.
 *
 * The left card is a single entry form used for both adding a new batch and
 * editing whichever one is selected in the table on the right.
 */
public class MedicineForm extends BaseForm {

    private static final String[] CATEGORIES = {
            "Tablet", "Syrup", "Injection", "Ointment", "Capsule", "Drops", "Other"
    };

    private final MedicineDao medicineDao = new MedicineDao();
    private final SupplierDao supplierDao = new SupplierDao();

    private final JTextField nameField = UI.textField();
    private final JTextField companyField = UI.textField();
    private final JComboBox<String> categoryCombo = UI.comboBox();
    private final JTextField batchField = UI.textField();
    private final JTextField expiryField = UI.textField();
    private final JTextField quantityField = UI.textField();
    private final JTextField purchasePriceField = UI.textField();
    private final JTextField mrpField = UI.textField();
    private final JTextField reorderLevelField = UI.textField();
    private final JComboBox<Supplier> supplierCombo = UI.comboBox();

    private final JButton saveButton = UI.primaryButton("Save");
    private final JButton updateButton = UI.secondaryButton("Update");
    private final JButton deleteButton = UI.dangerButton("Delete");
    private final JButton clearButton = UI.secondaryButton("Clear");

    private final JTextField searchField = UI.textField();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Name", "Company", "Category", "Batch", "Expiry", "Qty", "MRP", "Supplier"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UI.table(tableModel);

    private List<Medicine> currentRows = List.of();
    private Medicine selected;

    public MedicineForm() {
        super("Medicine Master", "Add, edit and search the medicines you stock");
        for (String category : CATEGORIES) {
            categoryCombo.addItem(category);
        }
        expiryField.setToolTipText("yyyy-MM-dd, for example 2027-03-31");

        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setOpaque(false);
        JPanel entryCard = buildEntryCard();
        entryCard.setPreferredSize(new Dimension(400, 10));
        body.add(entryCard, BorderLayout.WEST);
        body.add(buildListCard(), BorderLayout.CENTER);
        setBody(body);

        // Dates, quantities and prices are never ellipsised, so those columns get a floor width.
        // Category and supplier are still in the data and load into the form when a row is
        // picked; they are left out of the list so the name and price never get cut off.
        var columns = table.getColumnModel();
        columns.removeColumn(columns.getColumn(7));
        columns.removeColumn(columns.getColumn(2));
        int[] widths = {180, 108, 84, 94, 48, 108};
        int[] minimum = {110, 70, 70, 92, 46, 104};
        for (int i = 0; i < widths.length; i++) {
            columns.getColumn(i).setPreferredWidth(widths[i]);
            columns.getColumn(i).setMinWidth(minimum[i]);
        }
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, company or batch no");

        wireEvents();
        clearForm();
    }

    private JPanel buildEntryCard() {
        FormGrid grid = new FormGrid(2)
                .add("Medicine Name", nameField)
                .add("Company", companyField)
                .add("Category", categoryCombo)
                .add("Batch No", batchField)
                .add("Expiry Date", expiryField)
                .add("Quantity", quantityField)
                .add("Purchase Price", purchasePriceField)
                .add("MRP", mrpField)
                .add("Reorder Level", reorderLevelField)
                .add("Supplier", supplierCombo);

        JPanel formPanel = new JPanel(new BorderLayout());
        formPanel.setOpaque(false);
        formPanel.add(grid.panel(), BorderLayout.NORTH);
        formPanel.add(buttonRow(), BorderLayout.CENTER);
        return UI.card("Medicine Details", formPanel);
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
        return UI.card("Medicines", listBody);
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
        loadSuppliers();
        reloadTable();
    }

    private void loadSuppliers() {
        Supplier keep = (Supplier) supplierCombo.getSelectedItem();
        supplierCombo.removeAllItems();
        try {
            for (Supplier s : supplierDao.findAll()) {
                supplierCombo.addItem(s);
            }
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        if (keep != null) {
            selectSupplier(keep.getSupplierId());
        }
    }

    private void reloadTable() {
        try {
            currentRows = medicineDao.search(searchField.getText());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        tableModel.setRowCount(0);
        for (Medicine m : currentRows) {
            tableModel.addRow(new Object[]{
                    m.getMedicineName(), m.getCompany(), m.getCategory(), m.getBatchNo(),
                    UI.displayDate(m.getExpiryDate()), m.getQuantity(), UI.money(m.getMrp()),
                    m.getSupplierName()
            });
        }
    }

    private void loadRow(Medicine m) {
        selected = m;
        nameField.setText(m.getMedicineName());
        companyField.setText(m.getCompany());
        categoryCombo.setSelectedItem(m.getCategory());
        batchField.setText(m.getBatchNo());
        expiryField.setText(m.getExpiryDate());
        quantityField.setText(String.valueOf(m.getQuantity()));
        purchasePriceField.setText(UI.number(m.getPurchasePrice()));
        mrpField.setText(UI.number(m.getMrp()));
        reorderLevelField.setText(String.valueOf(m.getReorderLevel()));
        selectSupplier(m.getSupplierId());
        setEditing(true);
    }

    private void selectSupplier(int supplierId) {
        for (int i = 0; i < supplierCombo.getItemCount(); i++) {
            if (supplierCombo.getItemAt(i).getSupplierId() == supplierId) {
                supplierCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void clearForm() {
        selected = null;
        nameField.setText("");
        companyField.setText("");
        categoryCombo.setSelectedIndex(0);
        batchField.setText("");
        expiryField.setText("");
        quantityField.setText("0");
        purchasePriceField.setText("");
        mrpField.setText("");
        reorderLevelField.setText("10");
        if (supplierCombo.getItemCount() > 0) {
            supplierCombo.setSelectedIndex(0);
        }
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
        Medicine m = new Medicine();
        fill(m);
        try {
            medicineDao.save(m);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Medicine added.");
        clearForm();
    }

    private void update() {
        if (selected == null || !inputIsValid()) {
            return;
        }
        Medicine m = new Medicine();
        m.setMedicineId(selected.getMedicineId());
        fill(m);
        try {
            medicineDao.save(m);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Medicine updated.");
        clearForm();
    }

    private void delete() {
        if (selected == null) {
            return;
        }
        if (!UI.confirm(this, "Delete \"" + selected.getMedicineName() + "\" (batch "
                + selected.getBatchNo() + ")?")) {
            return;
        }
        try {
            medicineDao.delete(selected.getMedicineId());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        reloadTable();
        UI.info(this, "Medicine deleted.");
        clearForm();
    }

    private void fill(Medicine m) {
        m.setMedicineName(nameField.getText().trim());
        m.setCompany(companyField.getText().trim());
        m.setCategory((String) categoryCombo.getSelectedItem());
        m.setBatchNo(batchField.getText().trim());
        m.setExpiryDate(expiryField.getText().trim());
        m.setQuantity(Integer.parseInt(quantityField.getText().trim()));
        m.setPurchasePrice(Double.parseDouble(purchasePriceField.getText().trim()));
        m.setMrp(Double.parseDouble(mrpField.getText().trim()));
        m.setReorderLevel(Integer.parseInt(reorderLevelField.getText().trim()));
        Supplier supplier = (Supplier) supplierCombo.getSelectedItem();
        m.setSupplierId(supplier == null ? 0 : supplier.getSupplierId());
    }

    private boolean inputIsValid() {
        Validator v = new Validator();
        v.required(nameField, "Medicine name");
        v.date(expiryField, "Expiry date");
        v.wholeNumber(quantityField, "Quantity", 0);
        v.decimal(purchasePriceField, "Purchase price", 0);
        v.decimal(mrpField, "MRP", 0);
        v.wholeNumber(reorderLevelField, "Reorder level", 0);
        v.selected(supplierCombo, supplierCombo.getSelectedItem(), "supplier");
        try {
            double purchase = Double.parseDouble(purchasePriceField.getText().trim());
            double mrp = Double.parseDouble(mrpField.getText().trim());
            v.check(mrp >= purchase, "MRP cannot be less than the purchase price.");
        } catch (NumberFormatException ignored) {
            // already reported above
        }
        return v.showIfInvalid(this);
    }
}
