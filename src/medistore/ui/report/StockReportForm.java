package medistore.ui.report;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.MedicineDao;
import medistore.model.Medicine;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;

/**
 * Answers "what is on the shelves, and what is it worth?".
 *
 * Every medicine batch with its quantity, reorder level and value at cost, so
 * the owner can see at a glance what needs ordering. The summary tiles follow
 * the search box; the status filter only narrows the table.
 */
public class StockReportForm extends BaseForm {

    private static final String[] STATUS_FILTERS = {"All Items", "In Stock", "Low Stock", "Out of Stock"};
    private static final int STATUS_COLUMN = 9;

    private final MedicineDao medicineDao = new MedicineDao();

    private final JTextField searchField = UI.textField();
    private final JComboBox<String> statusCombo = UI.comboBox();
    private final JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 16));
    private final DefaultTableModel model = ReportSupport.readOnlyModel(
            "Medicine", "Company", "Category", "Batch", "Qty", "Reorder Level",
            "Purchase Price", "MRP", "Stock Value", "Status");
    private final JTable table = UI.table(model);

    public StockReportForm() {
        super("Stock Report", "What is on the shelves right now");

        for (String filter : STATUS_FILTERS) {
            statusCombo.addItem(filter);
        }
        ReportSupport.onTextChange(searchField, this::reload);
        statusCombo.addActionListener(e -> reload());

        JButton printBtn = UI.primaryButton("Print");
        JButton exportBtn = UI.secondaryButton("Export to CSV");
        printBtn.addActionListener(e -> ReportSupport.print(
                this, table, "Stock Report - " + UI.displayDate(UI.today())));
        exportBtn.addActionListener(e -> ReportSupport.exportCsv(
                this, table, "stock-report-" + LocalDate.now() + ".csv"));
        setHeaderActions(UI.rowRight(10, printBtn, exportBtn));

        table.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusRenderer());
        int[] widths = {200, 140, 120, 90, 60, 100, 110, 90, 110, 100};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        tiles.setOpaque(false);

        JPanel filters = new FormGrid(4)
                .add("Search medicine, company or batch", searchField, 2)
                .add("Stock status", statusCombo)
                .panel();

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(UI.card("Filters", filters), BorderLayout.NORTH);
        top.add(tiles, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(UI.scroll(table), BorderLayout.CENTER);
        setBody(body);
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        try {
            List<Medicine> matches = medicineDao.search(searchField.getText());
            String filter = (String) statusCombo.getSelectedItem();

            int lowStock = 0;
            int outOfStock = 0;
            double stockValue = 0;
            model.setRowCount(0);
            for (Medicine m : matches) {
                if (m.isLowStock()) {
                    lowStock++;
                }
                if (m.getQuantity() == 0) {
                    outOfStock++;
                }
                stockValue += m.getStockValue();

                if (passes(m, filter)) {
                    model.addRow(new Object[]{
                            m.getMedicineName(), m.getCompany(), m.getCategory(), m.getBatchNo(),
                            m.getQuantity(), m.getReorderLevel(),
                            UI.money(m.getPurchasePrice()), UI.money(m.getMrp()),
                            UI.money(m.getStockValue()), statusOf(m)});
                }
            }
            showTiles(matches.size(), lowStock, outOfStock, stockValue);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void showTiles(int items, int lowStock, int outOfStock, double stockValue) {
        tiles.removeAll();
        tiles.add(ReportSupport.tile("TOTAL ITEMS", String.valueOf(items),
                "Medicine batches listed", UI.MUTED));
        tiles.add(ReportSupport.tile("LOW STOCK ITEMS", String.valueOf(lowStock),
                "At or below reorder level", UI.WARNING));
        tiles.add(ReportSupport.tile("OUT OF STOCK", String.valueOf(outOfStock),
                "Nothing left to sell", UI.DANGER));
        tiles.add(ReportSupport.tile("TOTAL STOCK VALUE", UI.money(stockValue),
                "At purchase price", UI.MUTED));
        tiles.revalidate();
        tiles.repaint();
    }

    private static boolean passes(Medicine m, String filter) {
        if ("In Stock".equals(filter)) {
            return m.getQuantity() > 0;
        }
        if ("Low Stock".equals(filter)) {
            return m.isLowStock();
        }
        if ("Out of Stock".equals(filter)) {
            return m.getQuantity() == 0;
        }
        return true;
    }

    private static String statusOf(Medicine m) {
        if (m.getQuantity() == 0) {
            return "Out of Stock";
        }
        return m.isLowStock() ? "Low Stock" : "OK";
    }

    /** Colours the Status cell so problem rows stand out when scanning the list. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : UI.ROW_ALT);
            }
            if ("Out of Stock".equals(value)) {
                c.setForeground(UI.DANGER);
            } else if ("Low Stock".equals(value)) {
                c.setForeground(UI.WARNING);
            } else {
                c.setForeground(UI.SUCCESS);
            }
            c.setFont(UI.font(13, Font.BOLD));
            setBorder(UI.padding(0, 10, 0, 10));
            return c;
        }
    }
}
