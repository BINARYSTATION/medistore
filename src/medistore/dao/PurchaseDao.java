package medistore.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.Purchase;
import medistore.model.PurchaseItem;

/**
 * Saves stock-in invoices.
 *
 * The header, its lines and the stock increase are written inside one
 * transaction, so the books and the shelves can never disagree.
 */
public class PurchaseDao {

    private final MedicineDao medicineDao = new MedicineDao();

    /** Suggests the next invoice number, for example PUR-0007. */
    public String nextInvoiceNo() {
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT IFNULL(MAX(purchase_id), 0) + 1 FROM purchases")) {
            return String.format("PUR-%04d", rs.next() ? rs.getInt(1) : 1);
        } catch (SQLException e) {
            throw new DataAccessException("Could not generate an invoice number", e);
        }
    }

    /** Writes the invoice and adds every line's quantity to the stock on hand. */
    public void save(Purchase purchase) {
        if (purchase.getItems().isEmpty()) {
            throw new DataAccessException("Add at least one medicine before saving the invoice.");
        }
        Connection conn = Database.get();
        try {
            conn.setAutoCommit(false);

            String header = "INSERT INTO purchases (invoice_no, supplier_id, purchase_date, "
                    + "total_amount) VALUES (?,?,?,?)";
            int purchaseId;
            try (PreparedStatement ps = conn.prepareStatement(header, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, purchase.getInvoiceNo().trim());
                ps.setInt(2, purchase.getSupplierId());
                ps.setString(3, purchase.getPurchaseDate());
                ps.setDouble(4, purchase.getTotalAmount());
                ps.executeUpdate();
                purchaseId = UserDao.generatedId(ps);
            }
            purchase.setPurchaseId(purchaseId);

            String line = "INSERT INTO purchase_items (purchase_id, medicine_id, quantity, rate, "
                    + "amount) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(line)) {
                for (PurchaseItem item : purchase.getItems()) {
                    ps.setInt(1, purchaseId);
                    ps.setInt(2, item.getMedicineId());
                    ps.setInt(3, item.getQuantity());
                    ps.setDouble(4, item.getRate());
                    ps.setDouble(5, item.getAmount());
                    ps.addBatch();
                    medicineDao.adjustStock(conn, item.getMedicineId(), item.getQuantity());
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new DataAccessException("Could not save the purchase invoice", e);
        } finally {
            restoreAutoCommit(conn);
        }
    }

    public List<Purchase> findAll() {
        String sql = "SELECT p.*, s.supplier_name FROM purchases p "
                + "LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id "
                + "ORDER BY p.purchase_date DESC, p.purchase_id DESC";
        List<Purchase> list = new ArrayList<>();
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Purchase p = new Purchase();
                p.setPurchaseId(rs.getInt("purchase_id"));
                p.setInvoiceNo(rs.getString("invoice_no"));
                p.setSupplierId(rs.getInt("supplier_id"));
                String name = rs.getString("supplier_name");
                p.setSupplierName(name == null ? "" : name);
                p.setPurchaseDate(rs.getString("purchase_date"));
                // The lines are loaded only when an invoice is opened, so the list
                // screen reads the total that was saved with the header.
                p.setStoredTotal(rs.getDouble("total_amount"));
                list.add(p);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the purchase list", e);
        }
        return list;
    }

    /** The medicine lines that make up one invoice. */
    public List<PurchaseItem> findItems(int purchaseId) {
        String sql = "SELECT pi.*, m.medicine_name FROM purchase_items pi "
                + "JOIN medicines m ON m.medicine_id = pi.medicine_id WHERE pi.purchase_id = ?";
        List<PurchaseItem> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseItem item = new PurchaseItem();
                    item.setPurchaseItemId(rs.getInt("purchase_item_id"));
                    item.setPurchaseId(purchaseId);
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setRate(rs.getDouble("rate"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the invoice lines", e);
        }
        return list;
    }

    static void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // The original failure is the one worth reporting.
        }
    }

    static void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Nothing useful can be done here.
        }
    }
}
