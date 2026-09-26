package medistore.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.Sale;
import medistore.model.SaleItem;

/**
 * Saves counter bills.
 *
 * Before anything is written the stock is re-checked inside the transaction, so
 * a bill can never sell units the shop does not have.
 */
public class SaleDao {

    private final MedicineDao medicineDao = new MedicineDao();

    /** Suggests the next bill number, for example BILL-0023. */
    public String nextBillNo() {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT IFNULL(MAX(sale_id), 0) + 1 FROM sales")) {
            return String.format("BILL-%04d", rs.next() ? rs.getInt(1) : 1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate a bill number", e);
        }
    }

    /** Writes the bill and takes every line's quantity out of stock. */
    public void save(Sale sale) {
        if (sale.getItems().isEmpty()) {
            throw new DataAccessException("Add at least one medicine before saving the bill.");
        }
        if (sale.getDiscount() > sale.getTotalAmount()) {
            throw new DataAccessException("The discount cannot be more than the bill total.");
        }
        Connection conn = Database.get();
        try {
            conn.setAutoCommit(false);

            // Re-read stock now rather than trusting what the screen showed earlier.
            for (SaleItem item : sale.getItems()) {
                if (medicineDao.isExpired(conn, item.getMedicineId())) {
                    throw new DataAccessException(item.getMedicineName()
                            + " has expired and cannot be sold.");
                }
                int available = medicineDao.currentQuantity(conn, item.getMedicineId());
                if (item.getQuantity() > available) {
                    throw new DataAccessException("Only " + available + " unit(s) of "
                            + item.getMedicineName() + " are left in stock.");
                }
            }

            String header = "INSERT INTO sales (bill_no, customer_id, sale_date, total_amount, "
                    + "discount, net_amount, user_id) VALUES (?,?,?,?,?,?,?)";
            int saleId;
            try (PreparedStatement ps = conn.prepareStatement(header, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, sale.getBillNo().trim());
                if (sale.getCustomerId() > 0) {
                    ps.setInt(2, sale.getCustomerId());
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setString(3, sale.getSaleDate());
                ps.setDouble(4, sale.getTotalAmount());
                ps.setDouble(5, sale.getDiscount());
                ps.setDouble(6, sale.getNetAmount());
                ps.setInt(7, sale.getUserId());
                ps.executeUpdate();
                saleId = UserDao.generatedId(ps);
            }
            sale.setSaleId(saleId);

            String line = "INSERT INTO sale_items (sale_id, medicine_id, quantity, rate, amount) "
                    + "VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(line)) {
                for (SaleItem item : sale.getItems()) {
                    ps.setInt(1, saleId);
                    ps.setInt(2, item.getMedicineId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getRate());
                    ps.setDouble(5, item.getAmount());
                    ps.addBatch();
                    medicineDao.adjustStock(conn, item.getMedicineId(), -item.getQuantity());
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (DataAccessException e) {
            PurchaseDao.rollback(conn);
            throw e;
        } catch (SQLException e) {
            PurchaseDao.rollback(conn);
            throw new DataAccessException("Could not save the bill", e);
        } finally {
            PurchaseDao.restoreAutoCommit(conn);
        }
    }

    public List<Sale> findAll() {
        return findBetween(null, null);
    }

    /** Bills raised in the given date range; pass nulls for everything. */
    public List<Sale> findBetween(String fromDate, String toDate) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, c.customer_name FROM sales s "
                + "LEFT JOIN customers c ON c.customer_id = s.customer_id ");
        boolean ranged = fromDate != null && toDate != null;
        if (ranged) {
            sql.append("WHERE s.sale_date BETWEEN ? AND ? ");
        }
        sql.append("ORDER BY s.sale_date DESC, s.sale_id DESC");

        List<Sale> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql.toString())) {
            if (ranged) {
                ps.setString(1, fromDate);
                ps.setString(2, toDate);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setSaleId(rs.getInt("sale_id"));
                    s.setBillNo(rs.getString("bill_no"));
                    s.setCustomerId(rs.getInt("customer_id"));
                    String name = rs.getString("customer_name");
                    s.setCustomerName(name == null ? "Walk-in Customer" : name);
                    s.setSaleDate(rs.getString("sale_date"));
                    s.setDiscount(rs.getDouble("discount"));
                    s.setUserId(rs.getInt("user_id"));
                    s.setStoredTotal(rs.getDouble("total_amount"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the bill list", e);
        }
        return list;
    }

    /** The medicine lines that make up one bill. */
    public List<SaleItem> findItems(int saleId) {
        String sql = "SELECT si.*, m.medicine_name, m.batch_no FROM sale_items si "
                + "JOIN medicines m ON m.medicine_id = si.medicine_id WHERE si.sale_id = ?";
        List<SaleItem> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem item = new SaleItem();
                    item.setSaleItemId(rs.getInt("sale_item_id"));
                    item.setSaleId(saleId);
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setBatchNo(rs.getString("batch_no"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setRate(rs.getDouble("rate"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the bill lines", e);
        }
        return list;
    }
}
