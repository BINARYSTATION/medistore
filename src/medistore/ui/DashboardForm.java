package medistore.ui;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import medistore.dao.MedicineDao;
import medistore.dao.ReportDao;
import medistore.model.Medicine;
import medistore.util.Session;
import medistore.util.UI;

/**
 * The opening screen: the eight figures that describe the shop right now,
 * plus the two lists the owner actually acts on.
 */
public class DashboardForm extends BaseForm {

    private final ReportDao reportDao = new ReportDao();
    private final MedicineDao medicineDao = new MedicineDao();

    private final JPanel tiles = new JPanel(new GridLayout(2, 4, 16, 16));
    private final DefaultTableModel lowStockModel = readOnlyModel(
            new String[]{"Medicine", "Company", "In Stock", "Reorder At"});
    private final DefaultTableModel topSellerModel = readOnlyModel(
            new String[]{"#", "Medicine", "Company", "Units Sold", "Revenue"});

    public DashboardForm() {
        super("Dashboard", "Welcome back, " + Session.userName()
                + "  ·  " + UI.displayDate(LocalDate.now().toString()));

        tiles.setOpaque(false);

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(UI.card("Low Stock Alerts", buildTable(lowStockModel, new int[]{220, 150, 90, 90})));
        lists.add(UI.card("Top Selling Medicines", buildTable(topSellerModel, new int[]{36, 210, 90, 130})));

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(tiles, BorderLayout.NORTH);
        body.add(lists, BorderLayout.CENTER);
        setBody(body);
    }

    @Override
    public void onShow() {
        loadTiles();
        loadLowStock();
        loadTopSellers();
    }

    private void loadTiles() {
        tiles.removeAll();
        tiles.add(tile("TOTAL MEDICINES", String.valueOf(reportDao.countMedicines()),
                reportDao.countOutOfStock() + " out of stock", UI.MUTED));
        tiles.add(tile("LOW STOCK ITEMS", String.valueOf(reportDao.countLowStock()),
                "At or below reorder level", UI.WARNING));
        tiles.add(tile("EXPIRING IN 90 DAYS", String.valueOf(reportDao.countExpiringWithin(90)),
                "Check the expiry report", UI.DANGER));
        tiles.add(tile("TODAY'S SALES", UI.money(reportDao.salesOn(UI.today())),
                "Billed today", UI.SUCCESS));
        tiles.add(tile("STOCK VALUE (COST)", UI.money(reportDao.stockValue()),
                "At purchase price", UI.MUTED));
        tiles.add(tile("SALES THIS MONTH", UI.money(reportDao.salesThisMonth()),
                LocalDate.now().getMonth().toString().charAt(0)
                        + LocalDate.now().getMonth().toString().substring(1).toLowerCase(),
                UI.SUCCESS));
        tiles.add(tile("CUSTOMERS", String.valueOf(reportDao.countCustomers()),
                "Registered at the counter", UI.MUTED));
        tiles.add(tile("BILLS RAISED", String.valueOf(reportDao.countBills()),
                "Since the shop opened", UI.MUTED));
        tiles.revalidate();
        tiles.repaint();
    }

    /** One headline figure, boxed. */
    private JPanel tile(String caption, String value, String note, Color noteColour) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UI.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UI.BORDER),
                UI.padding(14, 16, 14, 16)));

        JLabel captionLabel = UI.label(caption, UI.font(10, Font.BOLD), UI.MUTED);
        JLabel valueLabel = UI.label(value, UI.font(21, Font.BOLD), UI.TEXT);
        JLabel noteLabel = UI.label(note, UI.font(11, Font.PLAIN), noteColour);

        captionLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(LEFT_ALIGNMENT);
        valueLabel.setBorder(UI.padding(6, 0, 4, 0));

        card.add(captionLabel);
        card.add(valueLabel);
        card.add(noteLabel);
        return card;
    }

    private void loadLowStock() {
        lowStockModel.setRowCount(0);
        List<Medicine> list = medicineDao.findLowStock();
        for (Medicine m : list) {
            lowStockModel.addRow(new Object[]{
                    m.getMedicineName(), m.getCompany(), m.getQuantity(), m.getReorderLevel()});
        }
        if (list.isEmpty()) {
            lowStockModel.addRow(new Object[]{"Every medicine is above its reorder level.", "", "", ""});
        }
    }

    private void loadTopSellers() {
        topSellerModel.setRowCount(0);
        List<ReportDao.Row> rows = reportDao.topSellingMedicines(8);
        for (ReportDao.Row row : rows) {
            topSellerModel.addRow(new Object[]{row.cells[0], row.cells[1], row.cells[2],
                    row.cells[3], UI.money((Double) row.cells[4])});
        }
        if (rows.isEmpty()) {
            topSellerModel.addRow(new Object[]{"", "No bills have been raised yet.", "", "", ""});
        }
    }

    private JScrollPane buildTable(DefaultTableModel model, int[] widths) {
        JTable table = UI.table(model);
        if (model == topSellerModel) {
            // The company is in the model but the tile is too narrow to show it.
            table.getColumnModel().removeColumn(table.getColumnModel().getColumn(2));
        }
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = UI.scroll(table);
        scroll.setPreferredSize(new Dimension(100, 240));
        return scroll;
    }

    /** Dashboard tables are for looking at, never for typing into. */
    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }
}
