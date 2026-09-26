package medistore.ui.report;

import java.awt.*;
import java.awt.print.PrinterException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.SaleDao;
import medistore.model.Sale;
import medistore.model.SaleItem;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;

/**
 * Answers "what exactly did we sell on that bill?".
 *
 * Every bill in the chosen dates, narrowed by bill number or customer name.
 * Selecting a bill lists its medicines underneath, and View Receipt shows the
 * counter receipt ready to print again.
 */
public class BillHistoryForm extends BaseForm {

    private static final int RECEIPT_WIDTH = 48;

    private final SaleDao saleDao = new SaleDao();

    private final JTextField fromField = UI.textField();
    private final JTextField toField = UI.textField();
    private final JTextField searchField = UI.textField();
    private final JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 16));
    private final DefaultTableModel billModel = ReportSupport.readOnlyModel(
            "Bill No", "Date", "Customer", "Items", "Gross", "Discount", "Net");
    private final DefaultTableModel itemModel = ReportSupport.readOnlyModel(
            "Medicine", "Batch", "Qty", "Rate", "Amount");
    private final JTable billTable = UI.table(billModel);
    private final JTable itemTable = UI.table(itemModel);

    /** Bills in the chosen dates; the search box narrows these into {@link #shown}. */
    private List<Sale> loaded = new ArrayList<>();
    private List<Sale> shown = new ArrayList<>();
    private final Map<Integer, Integer> itemCounts = new HashMap<>();
    /** The range last shown, so the printout matches the table even if the boxes were edited since. */
    private String period = "";

    public BillHistoryForm() {
        super("Bill History", "Every bill raised, with its items");

        LocalDate today = LocalDate.now();
        fromField.setText(today.minusDays(30).toString());
        toField.setText(today.toString());
        fromField.addActionListener(e -> reload());
        toField.addActionListener(e -> reload());
        ReportSupport.onTextChange(searchField, this::showMatches);

        JButton printBtn = UI.primaryButton("Print");
        JButton exportBtn = UI.secondaryButton("Export to CSV");
        printBtn.addActionListener(e -> ReportSupport.print(
                this, billTable, "Bill History - " + period));
        exportBtn.addActionListener(e -> ReportSupport.exportCsv(
                this, billTable, "bill-history-" + LocalDate.now() + ".csv"));
        setHeaderActions(UI.rowRight(10, printBtn, exportBtn));

        billTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showItems();
            }
        });
        setColumnWidths(billTable, 110, 110, 220, 70, 130, 120, 130);
        setColumnWidths(itemTable, 260, 120, 70, 130, 140);

        tiles.setOpaque(false);

        JButton applyBtn = UI.secondaryButton("Apply");
        applyBtn.addActionListener(e -> reload());
        JPanel fields = new FormGrid(4)
                .add("From date (yyyy-MM-dd)", fromField)
                .add("To date (yyyy-MM-dd)", toField)
                .add("Search bill number or customer", searchField)
                .add(" ", applyBtn)
                .panel();
        JPanel filters = new JPanel(new BorderLayout());
        filters.setOpaque(false);
        filters.add(fields, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(UI.card("Filters", filters), BorderLayout.NORTH);
        top.add(tiles, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(0, 18));
        lower.setOpaque(false);
        lower.add(UI.scroll(billTable), BorderLayout.CENTER);
        lower.add(buildItemsCard(), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        setBody(body);
    }

    /** The Bill Items card, with the receipt button beside its caption. */
    private JPanel buildItemsCard() {
        JButton receiptBtn = UI.secondaryButton("View Receipt");
        receiptBtn.addActionListener(e -> showReceipt());

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.setBorder(UI.padding(0, 0, 12, 0));
        heading.add(UI.label("Bill Items", UI.h3(), UI.TEXT), BorderLayout.WEST);
        heading.add(receiptBtn, BorderLayout.EAST);

        JScrollPane itemScroll = UI.scroll(itemTable);
        itemScroll.setPreferredSize(new Dimension(100, 120));

        JPanel card = UI.card();
        card.add(heading, BorderLayout.NORTH);
        card.add(itemScroll, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        if (!ReportSupport.validRange(this, fromField, toField)) {
            return;
        }
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        try {
            loaded = saleDao.findBetween(from, to);
            itemCounts.clear();
            for (Sale sale : loaded) {
                itemCounts.put(sale.getSaleId(), saleDao.findItems(sale.getSaleId()).size());
            }
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }
        period = UI.displayDate(from) + " to " + UI.displayDate(to);
        showMatches();
    }

    /** Applies the search box to the loaded bills; needs no database call, so it can run per keystroke. */
    private void showMatches() {
        String term = searchField.getText().trim().toLowerCase();
        double gross = 0;
        double discount = 0;
        double net = 0;

        shown = new ArrayList<>();
        billModel.setRowCount(0);
        for (Sale sale : loaded) {
            if (!term.isEmpty() && !sale.getBillNo().toLowerCase().contains(term)
                    && !sale.getCustomerName().toLowerCase().contains(term)) {
                continue;
            }
            shown.add(sale);
            gross += sale.getTotalAmount();
            discount += sale.getDiscount();
            net += sale.getNetAmount();
            billModel.addRow(new Object[]{
                    sale.getBillNo(), UI.displayDate(sale.getSaleDate()), sale.getCustomerName(),
                    itemCounts.getOrDefault(sale.getSaleId(), 0),
                    UI.money(sale.getTotalAmount()), UI.money(sale.getDiscount()),
                    UI.money(sale.getNetAmount())});
        }
        showTiles(shown.size(), gross, discount, net);
    }

    private void showTiles(int bills, double gross, double discount, double net) {
        tiles.removeAll();
        tiles.add(ReportSupport.tile("BILLS SHOWN", String.valueOf(bills),
                "Matching the filters", UI.MUTED));
        tiles.add(ReportSupport.tile("GROSS", UI.money(gross),
                "Before discount", UI.MUTED));
        tiles.add(ReportSupport.tile("DISCOUNT", UI.money(discount),
                "Given to customers", UI.WARNING));
        tiles.add(ReportSupport.tile("NET", UI.money(net),
                "Actually collected", UI.SUCCESS));
        tiles.revalidate();
        tiles.repaint();
    }

    /** The bill highlighted in the list, or null when nothing is selected. */
    private Sale selectedSale() {
        int row = billTable.getSelectedRow();
        return row < 0 || row >= shown.size() ? null : shown.get(row);
    }

    private void showItems() {
        itemModel.setRowCount(0);
        Sale sale = selectedSale();
        if (sale == null) {
            return;
        }
        try {
            for (SaleItem item : saleDao.findItems(sale.getSaleId())) {
                itemModel.addRow(new Object[]{
                        item.getMedicineName(), item.getBatchNo(), item.getQuantity(),
                        UI.money(item.getRate()), UI.money(item.getAmount())});
            }
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void showReceipt() {
        Sale sale = selectedSale();
        if (sale == null) {
            UI.error(this, "Select a bill in the list first.");
            return;
        }
        List<SaleItem> items;
        try {
            items = saleDao.findItems(sale.getSaleId());
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
            return;
        }

        JTextArea receipt = new JTextArea(receiptText(sale, items));
        receipt.setFont(UI.mono(12));
        receipt.setEditable(false);
        receipt.setBorder(UI.padding(14, 16, 14, 16));

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Receipt " + sale.getBillNo(), Dialog.ModalityType.APPLICATION_MODAL);
        JButton printBtn = UI.primaryButton("Print");
        JButton closeBtn = UI.secondaryButton("Close");
        printBtn.addActionListener(e -> printReceipt(dialog, receipt));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel buttons = UI.rowRight(10, printBtn, closeBtn);
        buttons.setBorder(UI.padding(10, 0, 10, 6));

        dialog.getContentPane().setBackground(UI.BACKGROUND);
        dialog.add(UI.scroll(receipt), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(500, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void printReceipt(Component parent, JTextArea receipt) {
        try {
            receipt.print();
        } catch (PrinterException e) {
            UI.error(parent, "Could not print the receipt. " + e.getMessage());
        }
    }

    /** Lays the bill out like the slip handed over the counter, in fixed-width columns. */
    private static String receiptText(Sale sale, List<SaleItem> items) {
        String rule = "-".repeat(RECEIPT_WIDTH);
        StringBuilder text = new StringBuilder();
        text.append(centred("MediStore Pharmacy")).append('\n');
        text.append(centred("Main Road, Junnar, Pune - 410502")).append('\n');
        text.append(rule).append('\n');
        text.append("Bill No  : ").append(sale.getBillNo()).append('\n');
        text.append("Date     : ").append(UI.displayDate(sale.getSaleDate())).append('\n');
        text.append("Customer : ").append(sale.getCustomerName()).append('\n');
        text.append(rule).append('\n');
        text.append(String.format("%-22s %4s %9s %10s\n", "Medicine", "Qty", "Rate", "Amount"));
        text.append(rule).append('\n');
        for (SaleItem item : items) {
            String name = item.getMedicineName();
            if (name.length() > 22) {
                name = name.substring(0, 21) + ".";
            }
            text.append(String.format("%-22s %4d %9s %10s\n", name, item.getQuantity(),
                    UI.number(item.getRate()), UI.number(item.getAmount())));
        }
        text.append(rule).append('\n');
        text.append(spread("Total", UI.money(sale.getTotalAmount()))).append('\n');
        text.append(spread("Discount", UI.money(sale.getDiscount()))).append('\n');
        text.append(rule).append('\n');
        text.append(spread("Net Payable", UI.money(sale.getNetAmount()))).append('\n');
        text.append(rule).append('\n');
        return text.toString();
    }

    private static String centred(String line) {
        return " ".repeat(Math.max(0, (RECEIPT_WIDTH - line.length()) / 2)) + line;
    }

    /** Puts {@code left} and {@code right} at opposite edges of one receipt line. */
    private static String spread(String left, String right) {
        return left + " ".repeat(Math.max(1, RECEIPT_WIDTH - left.length() - right.length())) + right;
    }

    private static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}
