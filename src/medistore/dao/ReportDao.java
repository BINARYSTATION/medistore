package medistore.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;

/**
 * The summary figures behind the dashboard and the printed reports.
 *
 * These are read-only aggregates, kept apart from the master DAOs so that the
 * reporting queries can be tuned without disturbing data entry.
 */
public class ReportDao {

    /** One headline number shown on the dashboard. */
    public static class Stat {
        public final String caption;
        public final String value;
        public final String note;

        public Stat(String caption, String value, String note) {
            this.caption = caption;
            this.value = value;
            this.note = note;
        }
    }

    /** One line of a best-sellers or sales summary table. */
    public static class Row {
        public final Object[] cells;

        public Row(Object... cells) {
            this.cells = cells;
        }
    }

    public int countMedicines()  { return single("SELECT COUNT(*) FROM medicines"); }
    public int countSuppliers()  { return single("SELECT COUNT(*) FROM suppliers"); }
    public int countCustomers()  { return single("SELECT COUNT(*) FROM customers"); }
    public int countBills()      { return single("SELECT COUNT(*) FROM sales"); }

    public int countLowStock() {
        return single("SELECT COUNT(*) FROM medicines WHERE quantity <= reorder_level");
    }

    public int countOutOfStock() {
        return single("SELECT COUNT(*) FROM medicines WHERE quantity = 0");
    }

    /** Batches expiring within the given number of days, already expired ones included. */
    public int countExpiringWithin(int days) {
        String limit = LocalDate.now().plusDays(days).toString();
        return singleWithDate("SELECT COUNT(*) FROM medicines WHERE expiry_date <> '' "
                + "AND expiry_date <= ?", limit);
    }

    /** What the stock on the shelves cost the shop. */
    public double stockValue() {
        return singleDouble("SELECT IFNULL(SUM(quantity * purchase_price), 0) FROM medicines");
    }

    /** What that same stock would fetch at printed price. */
    public double stockValueAtMrp() {
        return singleDouble("SELECT IFNULL(SUM(quantity * mrp), 0) FROM medicines");
    }

    public double salesOn(String isoDate) {
        return singleDoubleWithDate(
                "SELECT IFNULL(SUM(net_amount), 0) FROM sales WHERE sale_date = ?", isoDate);
    }

    public double salesBetween(String fromDate, String toDate) {
        try (PreparedStatement ps = Database.get().prepareStatement(
                "SELECT IFNULL(SUM(net_amount), 0) FROM sales WHERE sale_date BETWEEN ? AND ?")) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not total the sales", e);
        }
    }

    /** Sales for the current calendar month. */
    public double salesThisMonth() {
        LocalDate today = LocalDate.now();
        return salesBetween(today.withDayOfMonth(1).toString(), today.toString());
    }

    /** Best selling medicines by units sold. */
    public List<Row> topSellingMedicines(int limit) {
        String sql = "SELECT m.medicine_name, m.company, SUM(si.quantity) AS units, "
                + "SUM(si.amount) AS revenue FROM sale_items si "
                + "JOIN medicines m ON m.medicine_id = si.medicine_id "
                + "GROUP BY si.medicine_id ORDER BY units DESC LIMIT ?";
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    rows.add(new Row(rank++, rs.getString("medicine_name"),
                            rs.getString("company"), rs.getInt("units"), rs.getDouble("revenue")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the best sellers", e);
        }
        return rows;
    }

    /** Day by day totals for the sales report. */
    public List<Row> salesByDay(String fromDate, String toDate) {
        String sql = "SELECT sale_date, COUNT(*) AS bills, IFNULL(SUM(total_amount), 0) AS gross, "
                + "IFNULL(SUM(discount), 0) AS discount, IFNULL(SUM(net_amount), 0) AS net "
                + "FROM sales WHERE sale_date BETWEEN ? AND ? GROUP BY sale_date ORDER BY sale_date";
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Row(rs.getString("sale_date"), rs.getInt("bills"),
                            rs.getDouble("gross"), rs.getDouble("discount"), rs.getDouble("net")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not build the sales report", e);
        }
        return rows;
    }

    // ---------- small query helpers ----------

    private int single(String sql) {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read a summary figure", e);
        }
    }

    private double singleDouble(String sql) {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read a summary figure", e);
        }
    }

    private int singleWithDate(String sql, String date) {
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read a summary figure", e);
        }
    }

    private double singleDoubleWithDate(String sql, String date) {
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read a summary figure", e);
        }
    }
}
