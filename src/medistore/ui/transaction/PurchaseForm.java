package medistore.ui.transaction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import medistore.dao.DataAccessException;
import medistore.dao.MedicineDao;
import medistore.dao.PurchaseDao;
import medistore.dao.SupplierDao;
import medistore.model.Medicine;
import medistore.model.Purchase;
import medistore.model.PurchaseItem;
import medistore.model.Supplier;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * Records stock received from a supplier.
 *
 * The user fills in the invoice header, adds one line per medicine received and
 * saves; the DAO then writes the invoice and raises the stock of every medicine
 * on it in a single transaction.
 */
public class PurchaseForm extends BaseForm {

    private final PurchaseDao purchaseDao = new PurchaseDao();
    private final SupplierDao supplierDao = new SupplierDao();
    private final MedicineDao medicineDao = new MedicineDao();

    private final JTextField invoiceField = UI.textField();
    private final JComboBox<Supplier> supplierCombo = UI.comboBox();
    private final JTextField dateField = UI.textField();

    private final JComboBox<Medicine> medicineCombo = UI.comboBox();
    private final JTextField quantityField = UI.textField();
    private final JTextField rateField = UI.textField();

    private final List<PurchaseItem> items = new ArrayList<>();
    private final DefaultTableModel itemsModel =
            readOnlyModel(new String[]{"#", "Medicine", "Quantity", "Rate", "Amount"});
    private final JTable itemsTable = UI.table(itemsModel);

    private final JLabel totalLabel = UI.label(UI.money(0), UI.font(24, Font.BOLD), UI.PRIMARY_DARK);

    /** Set while the medicine list is being reloaded so the rate the user typed is not overwritten. */
    private boolean reloadingMedicines;

    public PurchaseForm() {
        super("Purchase Entry", "Record stock received from a supplier");

        invoiceField.setEditable(false);
        dateField.setText(UI.today());
        quantityField.setColumns(6);
        rateField.setColumns(8);
        medicineCombo.setPreferredSize(new Dimension(260, quantityField.getPreferredSize().height));

        medicineCombo.addActionListener(e -> {
            if (!reloadingMedicines) {
                fillRate();
            }
        });
        quantityField.addActionListener(e -> addItem());
        rateField.addActionListener(e -> addItem());

        JPanel header = new FormGrid(4)
                .add("Invoice No", invoiceField)
                .add("Supplier", supplierCombo)
                .add("Purchase Date", dateField)
                .panel();

        JButton addButton = UI.primaryButton("Add Item");
        addButton.addActionListener(e -> addItem());
        JButton removeButton = UI.secondaryButton("Remove Selected");
        removeButton.addActionListener(e -> removeSelected());

        JPanel entryRow = entryRow(
                fieldColumn("Medicine", medicineCombo),
                fieldColumn("Quantity", quantityField),
                fieldColumn("Rate", rateField),
                actionColumn(addButton),
                actionColumn(removeButton));

        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleColumns(new int[]{40, 300, 90, 120, 130}, 2);

        JPanel itemsBody = new JPanel(new BorderLayout(0, 14));
        itemsBody.setOpaque(false);
        itemsBody.add(entryRow, BorderLayout.NORTH);
        itemsBody.add(UI.scroll(itemsTable), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UI.card("Invoice Details", header), BorderLayout.NORTH);
        body.add(UI.card("Items", itemsBody), BorderLayout.CENTER);
        body.add(buildBottom(), BorderLayout.SOUTH);
        setBody(body);
    }

    private JPanel buildBottom() {
        JButton saveButton = UI.primaryButton("Save Invoice");
        saveButton.addActionListener(e -> save());
        JButton clearButton = UI.secondaryButton("Clear");
        clearButton.addActionListener(e -> clear());

        JPanel total = UI.rowRight(14, UI.fieldLabel("TOTAL AMOUNT"), totalLabel);
        JPanel buttons = UI.rowRight(12, clearButton, saveButton);

        JPanel inner = new JPanel(new BorderLayout(0, 14));
        inner.setOpaque(false);
        inner.add(total, BorderLayout.NORTH);
        inner.add(buttons, BorderLayout.SOUTH);

        JPanel card = UI.card();
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void onShow() {
        try {
            loadSuppliers();
            loadMedicines();
            invoiceField.setText(purchaseDao.nextInvoiceNo());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void loadSuppliers() {
        int previousId = supplierCombo.getSelectedItem() instanceof Supplier s ? s.getSupplierId() : -1;
        supplierCombo.removeAllItems();
        for (Supplier supplier : supplierDao.findAll()) {
            supplierCombo.addItem(supplier);
        }
        for (int i = 0; i < supplierCombo.getItemCount(); i++) {
            if (supplierCombo.getItemAt(i).getSupplierId() == previousId) {
                supplierCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /** Every medicine is offered, including ones at zero stock - that is exactly what gets restocked. */
    private void loadMedicines() {
        int previousId = medicineCombo.getSelectedItem() instanceof Medicine m ? m.getMedicineId() : -1;
        boolean sameMedicine = false;
        reloadingMedicines = true;
        try {
            medicineCombo.removeAllItems();
            for (Medicine medicine : medicineDao.findAll()) {
                medicineCombo.addItem(medicine);
            }
            for (int i = 0; i < medicineCombo.getItemCount(); i++) {
                if (medicineCombo.getItemAt(i).getMedicineId() == previousId) {
                    medicineCombo.setSelectedIndex(i);
                    sameMedicine = true;
                    break;
                }
            }
        } finally {
            reloadingMedicines = false;
        }
        if (!sameMedicine || rateField.getText().isBlank()) {
            fillRate();
        }
    }

    private void fillRate() {
        if (medicineCombo.getSelectedItem() instanceof Medicine medicine) {
            rateField.setText(String.format(Locale.US, "%.2f", medicine.getPurchasePrice()));
        } else {
            rateField.setText("");
        }
    }

    private void addItem() {
        Medicine medicine = (Medicine) medicineCombo.getSelectedItem();

        Validator v = new Validator()
                .selected(medicineCombo, medicine, "medicine")
                .wholeNumber(quantityField, "Quantity", 1)
                .decimal(rateField, "Rate", 0);
        if (!v.showIfInvalid(this)) {
            return;
        }

        PurchaseItem item = new PurchaseItem();
        item.setMedicineId(medicine.getMedicineId());
        item.setMedicineName(medicine.getMedicineName());
        item.setQuantity(Integer.parseInt(quantityField.getText().trim()));
        item.setRate(Double.parseDouble(rateField.getText().trim()));
        items.add(item);

        quantityField.setText("");
        refreshItems();
        quantityField.requestFocusInWindow();
    }

    private void removeSelected() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) {
            UI.error(this, "Select a line in the table first, then press Remove Selected.");
            return;
        }
        items.remove(row);
        refreshItems();
    }

    /** Rebuilds the table and the total from the list, which is the single source of truth. */
    private void refreshItems() {
        itemsModel.setRowCount(0);
        double total = 0;
        int number = 1;
        for (PurchaseItem item : items) {
            itemsModel.addRow(new Object[]{number++, item.getMedicineName(), item.getQuantity(),
                    UI.money(item.getRate()), UI.money(item.getAmount())});
            total += item.getAmount();
        }
        totalLabel.setText(UI.money(total));
    }

    private void save() {
        Supplier supplier = (Supplier) supplierCombo.getSelectedItem();

        Validator v = new Validator()
                .selected(supplierCombo, supplier, "supplier")
                .date(dateField, "Purchase date")
                .check(!items.isEmpty(), "Add at least one medicine to the invoice.");
        if (!v.showIfInvalid(this)) {
            return;
        }

        Purchase purchase = new Purchase();
        purchase.setInvoiceNo(invoiceField.getText().trim());
        purchase.setSupplierId(supplier.getSupplierId());
        purchase.setSupplierName(supplier.getSupplierName());
        purchase.setPurchaseDate(dateField.getText().trim());
        purchase.getItems().addAll(items);

        try {
            purchaseDao.save(purchase);
            UI.info(this, "Purchase invoice " + purchase.getInvoiceNo() + " has been saved.\n"
                    + "Stock has been updated for " + purchase.getItems().size() + " medicine line(s).");
            resetForNextEntry();
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void clear() {
        if (!items.isEmpty() && !UI.confirm(this, "Discard the lines entered so far and start again?")) {
            return;
        }
        resetForNextEntry();
    }

    private void resetForNextEntry() {
        items.clear();
        refreshItems();
        quantityField.setText("");
        dateField.setText(UI.today());
        if (supplierCombo.getItemCount() > 0) {
            supplierCombo.setSelectedIndex(0);
        }
        try {
            invoiceField.setText(purchaseDao.nextInvoiceNo());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    /** A label sitting above its input, the same look FormGrid gives the header fields. */
    private static JPanel fieldColumn(String label, JComponent field) {
        JLabel caption = UI.fieldLabel(label.toUpperCase());
        caption.setBorder(UI.padding(0, 0, 5, 0));
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(caption, BorderLayout.NORTH);
        column.add(field, BorderLayout.CENTER);
        return column;
    }

    /** A button given an empty caption so it lines up with the labelled inputs beside it. */
    private static JPanel actionColumn(JButton button) {
        JLabel spacer = UI.fieldLabel(" ");
        spacer.setBorder(UI.padding(0, 0, 5, 0));
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(spacer, BorderLayout.NORTH);
        column.add(button, BorderLayout.CENTER);
        return column;
    }

    /** Lays the entry controls in a row where the first one (the medicine) takes any spare width. */
    private static JPanel entryRow(JComponent... columns) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        for (int i = 0; i < columns.length; i++) {
            GridBagConstraints at = new GridBagConstraints();
            at.gridx = i;
            at.anchor = GridBagConstraints.NORTHWEST;
            at.fill = i == 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            at.weightx = i == 0 ? 1 : 0;
            at.insets = new Insets(0, 0, 0, i == columns.length - 1 ? 0 : 12);
            row.add(columns[i], at);
        }
        return row;
    }

    /** Sets column widths and right-aligns the figure columns, from {@code firstFigure} onwards. */
    private void styleColumns(int[] widths, int firstFigure) {
        TableCellRenderer cells = itemsTable.getDefaultRenderer(Object.class);
        TableCellRenderer heads = itemsTable.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < itemsTable.getColumnCount(); i++) {
            int alignment = i >= firstFigure ? SwingConstants.RIGHT : SwingConstants.LEFT;
            var column = itemsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
            column.setCellRenderer(aligned(cells, alignment));
            column.setHeaderRenderer(aligned(heads, alignment));
        }
    }

    /**
     * The shared renderers keep their state between calls, so alignment is set on every render.
     * Header and body get the same side padding so a heading sits directly above its column.
     */
    private static TableCellRenderer aligned(TableCellRenderer base, int alignment) {
        return (table, value, selected, focused, row, column) -> {
            Component c = base.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(alignment);
                label.setBorder(UI.padding(0, 10, 0, 10));
            }
            return c;
        };
    }

    /** The lines are for reading; changes are made by adding and removing whole lines. */
    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }
}
