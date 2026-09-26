package medistore.ui.report;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.MedicineDao;
import medistore.dao.ReportDao;
import medistore.model.Medicine;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;

/**
 * Answers "which batches will I lose money on if I do not act now?".
 *
 * Lists every batch that has expired or will expire inside the chosen window,
 * soonest first, together with the stock value tied up in them. The count tiles
 * always describe the whole shop; only Value at Risk follows the selected window.
 */
public class ExpiryReportForm extends BaseForm {

    private static final String[] WINDOWS = {
            "Already Expired", "Within 30 Days", "Within 90 Days", "Within 180 Days"};
    // -1 stops at yesterday, so "Already Expired" never lists a batch that is still saleable today.
    private static final int[] WINDOW_DAYS = {-1, 30, 90, 180};
    private static final int DAYS_LEFT_COLUMN = 4;
    private static final int STATUS_COLUMN = 7;
    private static final long SOON = 30;

    private final MedicineDao medicineDao = new MedicineDao();
    private final ReportDao reportDao = new ReportDao();

    private final JComboBox<String> windowCombo = UI.comboBox();
    private final JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 16));
    private final DefaultTableModel model = ReportSupport.readOnlyModel(
            "Medicine", "Company", "Batch", "Expiry Date", "Days Left", "Qty",
            "Stock Value", "Status");
    private final JTable table = UI.table(model);

    public ExpiryReportForm() {
        super("Expiry Report", "Batches that are close to expiring");

        for (String window : WINDOWS) {
            windowCombo.addItem(window);
        }
        windowCombo.setSelectedIndex(2);
        windowCombo.addActionListener(e -> reload());

        JButton printBtn = UI.primaryButton("Print");
        JButton exportBtn = UI.secondaryButton("Export to CSV");
        printBtn.addActionListener(e -> ReportSupport.print(
                this, table, "Expiry Report - " + UI.displayDate(UI.today())));
        exportBtn.addActionListener(e -> ReportSupport.exportCsv(
                this, table, "expiry-report-" + LocalDate.now() + ".csv"));
        setHeaderActions(UI.rowRight(10, printBtn, exportBtn));

        DefaultTableCellRenderer urgency = new UrgencyRenderer();
        table.getColumnModel().getColumn(DAYS_LEFT_COLUMN).setCellRenderer(urgency);
        table.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(urgency);
        int[] widths = {220, 150, 100, 110, 90, 70, 120, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        tiles.setOpaque(false);

        JPanel filters = new FormGrid(4)
                .add("Show batches expiring", windowCombo)
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
            int days = WINDOW_DAYS[Math.max(0, windowCombo.getSelectedIndex())];
            List<Medicine> batches = medicineDao.findExpiringWithin(days);

            double valueAtRisk = 0;
            model.setRowCount(0);
            for (Medicine m : batches) {
                long daysLeft = m.getDaysToExpiry();
                valueAtRisk += m.getStockValue();
                model.addRow(new Object[]{
                        m.getMedicineName(), m.getCompany(), m.getBatchNo(),
                        UI.displayDate(m.getExpiryDate()),
                        daysLeft < 0 ? "Expired" : (Object) daysLeft,
                        m.getQuantity(), UI.money(m.getStockValue()), statusOf(daysLeft)});
            }
            showTiles(valueAtRisk);
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void showTiles(double valueAtRisk) {
        tiles.removeAll();
        tiles.add(ReportSupport.tile("EXPIRED BATCHES",
                String.valueOf(reportDao.countExpiringWithin(-1)),
                "Past their expiry date", UI.DANGER));
        tiles.add(ReportSupport.tile("EXPIRING IN 30 DAYS",
                String.valueOf(reportDao.countExpiringWithin(30)),
                "Including those already expired", UI.WARNING));
        tiles.add(ReportSupport.tile("EXPIRING IN 90 DAYS",
                String.valueOf(reportDao.countExpiringWithin(90)),
                "Including those already expired", UI.MUTED));
        tiles.add(ReportSupport.tile("VALUE AT RISK", UI.money(valueAtRisk),
                "Stock value of the batches listed", UI.MUTED));
        tiles.revalidate();
        tiles.repaint();
    }

    private static String statusOf(long daysLeft) {
        if (daysLeft < 0) {
            return "Expired";
        }
        return daysLeft < SOON ? "Expiring Soon" : "OK";
    }

    /** Red once expired, amber inside the last 30 days, green otherwise. */
    private static class UrgencyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : UI.ROW_ALT);
            }
            c.setForeground(colourFor(value));
            c.setFont(UI.font(13, Font.BOLD));
            setBorder(UI.padding(0, 10, 0, 10));
            return c;
        }

        private static Color colourFor(Object value) {
            if (value instanceof Long days) {
                return days < SOON ? UI.WARNING : UI.SUCCESS;
            }
            if ("Expired".equals(value)) {
                return UI.DANGER;
            }
            return "Expiring Soon".equals(value) ? UI.WARNING : UI.SUCCESS;
        }
    }
}
