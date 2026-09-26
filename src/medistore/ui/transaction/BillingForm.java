package medistore.ui.transaction;

import java.awt.*;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import medistore.dao.CustomerDao;
import medistore.dao.DataAccessException;
import medistore.dao.MedicineDao;
import medistore.dao.SaleDao;
import medistore.model.Customer;
import medistore.model.Medicine;
import medistore.model.Sale;
import medistore.model.SaleItem;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.Session;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * The counter billing screen.
 *
 * The cashier picks medicines that are actually in stock, applies a discount and
 * saves; the DAO writes the bill and takes the units off the shelf in one
 * transaction, and a printable receipt is shown straight afterwards.
 */
public class BillingForm extends BaseForm {

    private static final String SHOP_NAME = "MediStore Pharmacy";
    private static final String SHOP_ADDRESS = "Main Road, Junnar, Pune - 410502";
    private static final int RECEIPT_WIDTH = 48;

    private final SaleDao saleDao = new SaleDao();
    private final CustomerDao customerDao = new CustomerDao();
    private final MedicineDao medicineDao = new MedicineDao();

    private final JTextField billNoField = UI.textField();
    private final JComboBox<Customer> customerCombo = UI.comboBox();
    private final JTextField dateField = UI.textField();

    private final JComboBox<Medicine> medicineCombo = UI.comboBox();
    private final JTextField quantityField = UI.textField();
    private final JTextField rateField = UI.textField();

    private final List<SaleItem> items = new ArrayList<>();
    private final DefaultTableModel itemsModel =
            readOnlyModel(new String[]{"#", "Medicine", "Batch", "Quantity", "Rate", "Amount"});
    private final JTable itemsTable = UI.table(itemsModel);

    private final JTextField discountField = UI.textField();
    private final JLabel totalLabel = UI.label(UI.money(0), UI.font(15, Font.BOLD), UI.TEXT);
    private final JLabel netLabel = UI.label(UI.money(0), UI.font(26, Font.BOLD), UI.SUCCESS);

    /** Set while the medicine list is being reloaded so the rate the user typed is not overwritten. */
    private boolean reloadingMedicines;

    public BillingForm() {
        super("Sales Billing", "Make a bill and take it out of stock");

        billNoField.setEditable(false);
        dateField.setText(UI.today());
        discountField.setText("0");
        discountField.setColumns(8);
        discountField.setHorizontalAlignment(JTextField.RIGHT);
        quantityField.setColumns(6);
        rateField.setColumns(8);
        medicineCombo.setPreferredSize(new Dimension(300, quantityField.getPreferredSize().height));
        medicineCombo.setRenderer(new StockRenderer());

        medicineCombo.addActionListener(e -> {
            if (!reloadingMedicines) {
                fillRate();
            }
        });
        quantityField.addActionListener(e -> addItem());
        rateField.addActionListener(e -> addItem());
        discountField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateTotals(); }
            @Override public void removeUpdate(DocumentEvent e) { updateTotals(); }
            @Override public void changedUpdate(DocumentEvent e) { updateTotals(); }
        });

        JPanel header = new FormGrid(4)
                .add("Bill No", billNoField)
                .add("Customer", customerCombo)
                .add("Bill Date", dateField)
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
        styleColumns(new int[]{40, 240, 110, 90, 110, 120}, 3);

        JPanel itemsBody = new JPanel(new BorderLayout(0, 14));
        itemsBody.setOpaque(false);
        itemsBody.add(entryRow, BorderLayout.NORTH);
        itemsBody.add(UI.scroll(itemsTable), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(UI.card("Bill Details", header), BorderLayout.NORTH);
        body.add(UI.card("Items", itemsBody), BorderLayout.CENTER);
        body.add(buildBottom(), BorderLayout.SOUTH);
        setBody(body);
    }

    private JPanel buildBottom() {
        JButton saveButton = UI.primaryButton("Save & Print Bill");
        saveButton.addActionListener(e -> save());
        JButton clearButton = UI.secondaryButton("Clear");
        clearButton.addActionListener(e -> clear());

        // Two columns so the captions and the figures each line up on the right edge.
        JPanel figures = new JPanel(new GridBagLayout());
        figures.setOpaque(false);
        addFigureRow(figures, 0, "TOTAL AMOUNT", totalLabel);
        addFigureRow(figures, 1, "DISCOUNT (RS.)", discountField);
        addFigureRow(figures, 2, "NET PAYABLE", netLabel);

        JPanel buttons = UI.rowRight(12, clearButton, saveButton);

        JPanel inner = new JPanel(new BorderLayout(0, 14));
        inner.setOpaque(false);
        inner.add(UI.rowRight(0, figures), BorderLayout.NORTH);
        inner.add(buttons, BorderLayout.SOUTH);

        JPanel card = UI.card();
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private static void addFigureRow(JPanel grid, int row, String caption, JComponent value) {
        GridBagConstraints captionAt = new GridBagConstraints();
        captionAt.gridx = 0;
        captionAt.gridy = row;
        captionAt.anchor = GridBagConstraints.EAST;
        captionAt.insets = new Insets(4, 0, 4, 16);
        grid.add(UI.fieldLabel(caption), captionAt);

        GridBagConstraints valueAt = new GridBagConstraints();
        valueAt.gridx = 1;
        valueAt.gridy = row;
        valueAt.anchor = GridBagConstraints.EAST;
        valueAt.insets = new Insets(4, 0, 4, 0);
        grid.add(value, valueAt);
    }

    @Override
    public void onShow() {
        try {
            loadCustomers();
            loadMedicines();
            billNoField.setText(saleDao.nextBillNo());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    /** Index 0 is always the walk-in entry; customer id 0 is stored as "no customer". */
    private void loadCustomers() {
        int previousId = customerCombo.getSelectedItem() instanceof Customer c ? c.getCustomerId() : 0;
        customerCombo.removeAllItems();

        Customer walkIn = new Customer();
        walkIn.setCustomerId(0);
        walkIn.setCustomerName("Walk-in Customer");
        customerCombo.addItem(walkIn);
        for (Customer customer : customerDao.findAll()) {
            customerCombo.addItem(customer);
        }
        for (int i = 0; i < customerCombo.getItemCount(); i++) {
            if (customerCombo.getItemAt(i).getCustomerId() == previousId) {
                customerCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    /** Only medicines with units on the shelf are offered, so nothing can be sold from empty. */
    private void loadMedicines() {
        int previousId = medicineCombo.getSelectedItem() instanceof Medicine m ? m.getMedicineId() : -1;
        boolean sameMedicine = false;
        reloadingMedicines = true;
        try {
            medicineCombo.removeAllItems();
            for (Medicine medicine : medicineDao.findInStock()) {
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
            rateField.setText(String.format(Locale.US, "%.2f", medicine.getMrp()));
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

        int quantity = Integer.parseInt(quantityField.getText().trim());
        // The same batch may be added on more than one line, so count what is already on the bill.
        int alreadyOnBill = items.stream()
                .filter(line -> line.getMedicineId() == medicine.getMedicineId())
                .mapToInt(SaleItem::getQuantity)
                .sum();
        if (quantity + alreadyOnBill > medicine.getQuantity()) {
            UI.error(this, "Only " + medicine.getQuantity() + " unit(s) of "
                    + medicine.getMedicineName() + " (batch " + medicine.getBatchNo()
                    + ") are in stock"
                    + (alreadyOnBill > 0 ? ", and " + alreadyOnBill + " are already on this bill." : "."));
            return;
        }

        SaleItem item = new SaleItem();
        item.setMedicineId(medicine.getMedicineId());
        item.setMedicineName(medicine.getMedicineName());
        item.setBatchNo(medicine.getBatchNo());
        item.setQuantity(quantity);
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

    /** Rebuilds the table from the list, which is the single source of truth, then the totals. */
    private void refreshItems() {
        itemsModel.setRowCount(0);
        int number = 1;
        for (SaleItem item : items) {
            itemsModel.addRow(new Object[]{number++, item.getMedicineName(), item.getBatchNo(),
                    item.getQuantity(), UI.money(item.getRate()), UI.money(item.getAmount())});
        }
        updateTotals();
    }

    private double itemsTotal() {
        double total = 0;
        for (SaleItem item : items) {
            total += item.getAmount();
        }
        return total;
    }

    /** A half-typed or invalid discount counts as zero here; save() reports it properly. */
    private double typedDiscount() {
        try {
            return Double.parseDouble(discountField.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateTotals() {
        double total = itemsTotal();
        totalLabel.setText(UI.money(total));
        netLabel.setText(UI.money(total - typedDiscount()));
    }

    private void save() {
        Customer customer = (Customer) customerCombo.getSelectedItem();

        Validator v = new Validator()
                .selected(customerCombo, customer, "customer")
                .date(dateField, "Bill date")
                .check(!items.isEmpty(), "Add at least one medicine to the bill.")
                .decimal(discountField, "Discount", 0)
                .check(typedDiscount() <= itemsTotal(), "The discount cannot be more than the bill total.");
        if (!v.showIfInvalid(this)) {
            return;
        }

        Sale sale = new Sale();
        sale.setBillNo(billNoField.getText().trim());
        sale.setCustomerId(customer.getCustomerId());
        sale.setCustomerName(customer.getCustomerName());
        sale.setSaleDate(dateField.getText().trim());
        sale.setDiscount(typedDiscount());
        sale.setUserId(Session.userId());
        sale.getItems().addAll(items);

        try {
            saleDao.save(sale);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }

        UI.info(this, "Bill " + sale.getBillNo() + " has been saved.\n"
                + "Net payable: " + UI.money(sale.getNetAmount()));
        showReceipt(sale);
        resetForNextEntry();
    }

    private void clear() {
        if (!items.isEmpty() && !UI.confirm(this, "Discard the bill entered so far and start again?")) {
            return;
        }
        resetForNextEntry();
    }

    /** Also reloads stock, since the bill that was just saved has changed what is on the shelf. */
    private void resetForNextEntry() {
        items.clear();
        quantityField.setText("");
        discountField.setText("0");
        dateField.setText(UI.today());
        if (customerCombo.getItemCount() > 0) {
            customerCombo.setSelectedIndex(0);
        }
        refreshItems();
        try {
            loadMedicines();
            billNoField.setText(saleDao.nextBillNo());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    // ---------- Receipt ----------

    /** Shows the saved bill in a modal window laid out like a counter receipt. */
    private void showReceipt(Sale sale) {
        JTextArea receipt = new JTextArea(receiptText(sale));
        receipt.setFont(UI.mono(12));
        receipt.setForeground(UI.TEXT);
        receipt.setBackground(UI.CARD);
        receipt.setEditable(false);
        receipt.setFocusable(false);
        receipt.setBorder(UI.padding(18, 22, 18, 22));

        JScrollPane scroll = UI.scroll(receipt);
        Dimension size = receipt.getPreferredSize();
        scroll.setPreferredSize(new Dimension(size.width + 24, Math.min(size.height + 4, 560)));

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Bill " + sale.getBillNo(), Dialog.ModalityType.APPLICATION_MODAL);

        JButton printButton = UI.primaryButton("Print");
        printButton.addActionListener(e -> {
            try {
                receipt.print();
            } catch (PrinterException ex) {
                UI.error(dialog, "The bill could not be printed. " + ex.getMessage());
            }
        });
        JButton closeButton = UI.secondaryButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UI.BACKGROUND);
        root.setBorder(UI.padding(16, 16, 16, 16));
        root.add(scroll, BorderLayout.CENTER);
        root.add(UI.rowRight(10, closeButton, printButton), BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(printButton);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static String receiptText(Sale sale) {
        String rule = "-".repeat(RECEIPT_WIDTH);
        String doubleRule = "=".repeat(RECEIPT_WIDTH);
        StringBuilder text = new StringBuilder();

        text.append(centred(SHOP_NAME)).append('\n');
        text.append(centred(SHOP_ADDRESS)).append('\n');
        text.append(doubleRule).append('\n');
        text.append(detail("Bill No", sale.getBillNo())).append('\n');
        text.append(detail("Date", UI.displayDate(sale.getSaleDate()))).append('\n');
        text.append(detail("Customer", sale.getCustomerName())).append('\n');
        text.append(detail("Cashier", Session.userName())).append('\n');
        text.append(rule).append('\n');
        text.append(String.format("%-22s%4s%10s%12s%n", "Medicine", "Qty", "Rate", "Amount"));
        text.append(rule).append('\n');

        for (SaleItem item : sale.getItems()) {
            text.append(String.format("%-22s%4d%10s%12s%n", shorten(item.getMedicineName(), 21),
                    item.getQuantity(), UI.number(item.getRate()), UI.number(item.getAmount())));
            if (item.getBatchNo() != null && !item.getBatchNo().isBlank()) {
                text.append("  Batch ").append(item.getBatchNo()).append('\n');
            }
        }

        text.append(rule).append('\n');
        text.append(figure("Total Amount", UI.money(sale.getTotalAmount()))).append('\n');
        text.append(figure("Discount", "- " + UI.money(sale.getDiscount()))).append('\n');
        text.append(doubleRule).append('\n');
        text.append(figure("NET PAYABLE", UI.money(sale.getNetAmount()))).append('\n');
        text.append(doubleRule).append('\n');
        text.append('\n');
        text.append(centred("Thank you for shopping with us!")).append('\n');
        text.append(centred("Get well soon.")).append('\n');
        return text.toString();
    }

    private static String centred(String line) {
        int pad = Math.max(0, (RECEIPT_WIDTH - line.length()) / 2);
        return " ".repeat(pad) + line;
    }

    private static String detail(String caption, String value) {
        return String.format("%-10s: %s", caption, value);
    }

    /** A caption on the left and its amount pushed to the right edge. */
    private static String figure(String caption, String amount) {
        return String.format("%-" + (RECEIPT_WIDTH - 16) + "s%16s", caption, amount);
    }

    private static String shorten(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 1) + ".";
    }

    // ---------- Layout helpers ----------

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

    /** Shows each batch with the units left, so the cashier never has to guess what is on the shelf. */
    private static class StockRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof Medicine m) {
                label.setText(m.getMedicineName() + " (" + m.getBatchNo() + ")  -  "
                        + m.getQuantity() + " in stock");
            }
            return label;
        }
    }
}
