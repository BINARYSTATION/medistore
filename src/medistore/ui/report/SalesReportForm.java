package medistore.ui.report;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import medistore.dao.DataAccessException;
import medistore.dao.ReportDao;
import medistore.ui.BaseForm;
import medistore.util.FormGrid;
import medistore.util.UI;

/**
 * Answers "how much did the shop sell, day by day, in this period?".
 *
 * The date range drives the totals and the daily table; the best sellers card
 * below is an all-time ranking and does not change with the range.
 */
public class SalesReportForm extends BaseForm {

    private final ReportDao reportDao = new ReportDao();

    private final JTextField fromField = UI.textField();
    private final JTextField toField = UI.textField();
    private final JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 16));
    /** The range last shown, so the printout matches the table even if the boxes were edited since. */
    private String period = "";
    private final DefaultTableModel dailyModel = ReportSupport.readOnlyModel(
            "Date", "Bills", "Gross Amount", "Discount", "Net Amount");
    private final DefaultTableModel bestSellerModel = ReportSupport.readOnlyModel(
            "#", "Medicine", "Company", "Units Sold", "Revenue");
    private final JTable dailyTable = UI.table(dailyModel);
    private final JTable bestSellerTable = UI.table(bestSellerModel);

    public SalesReportForm() {
        super("Sales Report", "Day by day sales for a chosen period");

        LocalDate today = LocalDate.now();
        fromField.setText(today.withDayOfMonth(1).toString());
        toField.setText(today.toString());
        fromField.addActionListener(e -> reload());
        toField.addActionListener(e -> reload());

        JButton printBtn = UI.primaryButton("Print");
        JButton exportBtn = UI.secondaryButton("Export to CSV");
        printBtn.addActionListener(e -> ReportSupport.print(
                this, dailyTable, "Sales Report - " + period));
        exportBtn.addActionListener(e -> ReportSupport.exportCsv(
                this, dailyTable, "sales-report-" + LocalDate.now() + ".csv"));
        setHeaderActions(UI.rowRight(10, printBtn, exportBtn));

        tiles.setOpaque(false);

        JButton todayBtn = UI.secondaryButton("Today");
        JButton weekBtn = UI.secondaryButton("This Week");
        JButton monthBtn = UI.secondaryButton("This Month");
        JButton applyBtn = UI.secondaryButton("Apply");
        todayBtn.addActionListener(e -> pick(LocalDate.now(), LocalDate.now()));
        weekBtn.addActionListener(e -> pick(
                LocalDate.now().with(DayOfWeek.MONDAY), LocalDate.now()));
        monthBtn.addActionListener(e -> pick(
                LocalDate.now().withDayOfMonth(1), LocalDate.now()));
        applyBtn.addActionListener(e -> reload());

        JPanel dates = new FormGrid(4)
                .add("From date (yyyy-MM-dd)", fromField)
                .add("To date (yyyy-MM-dd)", toField)
                .add("Quick pick", UI.row(8, todayBtn, weekBtn, monthBtn, applyBtn), 2)
                .panel();
        JPanel filters = new JPanel(new BorderLayout());
        filters.setOpaque(false);
        filters.add(dates, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(UI.card("Filters", filters), BorderLayout.NORTH);
        top.add(tiles, BorderLayout.CENTER);

        JScrollPane bestSellerScroll = UI.scroll(bestSellerTable);
        bestSellerScroll.setPreferredSize(new Dimension(100, 140));
        JPanel lower = new JPanel(new BorderLayout(0, 18));
        lower.setOpaque(false);
        lower.add(UI.scroll(dailyTable), BorderLayout.CENTER);
        lower.add(UI.card("Best Selling Medicines", bestSellerScroll), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        setBody(body);

        setColumnWidths(dailyTable, 140, 80, 140, 140, 140);
        setColumnWidths(bestSellerTable, 40, 240, 180, 100, 130);
    }

    @Override
    public void onShow() {
        reload();
    }

    /** Fills both date boxes from a quick-pick button and shows the result straight away. */
    private void pick(LocalDate from, LocalDate to) {
        fromField.setText(from.toString());
        toField.setText(to.toString());
        reload();
    }

    private void reload() {
        if (!ReportSupport.validRange(this, fromField, toField)) {
            return;
        }
        String from = fromField.getText().trim();
        String to = toField.getText().trim();

        try {
            loadDailySales(from, to);
            loadBestSellers();
        } catch (DataAccessException e) {
            UI.error(this, e.getMessage());
        }
    }

    private void loadDailySales(String from, String to) {
        int bills = 0;
        double gross = 0;
        double discount = 0;
        double net = 0;

        period = UI.displayDate(from) + " to " + UI.displayDate(to);
        dailyModel.setRowCount(0);
        for (ReportDao.Row row : reportDao.salesByDay(from, to)) {
            bills += (Integer) row.cells[1];
            gross += (Double) row.cells[2];
            discount += (Double) row.cells[3];
            net += (Double) row.cells[4];
            dailyModel.addRow(new Object[]{
                    UI.displayDate((String) row.cells[0]), row.cells[1],
                    UI.money((Double) row.cells[2]), UI.money((Double) row.cells[3]),
                    UI.money((Double) row.cells[4])});
        }
        showTiles(bills, gross, discount, net);
    }

    private void loadBestSellers() {
        bestSellerModel.setRowCount(0);
        List<ReportDao.Row> rows = reportDao.topSellingMedicines(10);
        for (ReportDao.Row row : rows) {
            bestSellerModel.addRow(new Object[]{row.cells[0], row.cells[1], row.cells[2],
                    row.cells[3], UI.money((Double) row.cells[4])});
        }
    }

    private void showTiles(int bills, double gross, double discount, double net) {
        tiles.removeAll();
        tiles.add(ReportSupport.tile("TOTAL BILLS", String.valueOf(bills),
                "Raised in this period", UI.MUTED));
        tiles.add(ReportSupport.tile("GROSS SALES", UI.money(gross),
                "Before discount", UI.MUTED));
        tiles.add(ReportSupport.tile("TOTAL DISCOUNT", UI.money(discount),
                "Given to customers", UI.WARNING));
        tiles.add(ReportSupport.tile("NET SALES", UI.money(net),
                "Actually collected", UI.SUCCESS));
        tiles.revalidate();
        tiles.repaint();
    }

    private static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}
