package medistore.dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.Medicine;

/**
 * Reads and writes the medicine master, and answers the stock questions the
 * reports are built on.
 */
public class MedicineDao {

    /** Every query joins the supplier so screens can show the name, not the id. */
    private static final String BASE =
            "SELECT m.*, s.supplier_name FROM medicines m "
            + "LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id ";

    public List<Medicine> findAll() {
        return query(BASE + "ORDER BY m.medicine_name", null);
    }

    /** Matches the term against medicine name, company or batch number. */
    public List<Medicine> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String sql = BASE + "WHERE m.medicine_name LIKE ? OR m.company LIKE ? OR m.batch_no LIKE ? "
                + "ORDER BY m.medicine_name";
        return query(sql, "%" + term.trim() + "%");
    }

    /**
     * The batches the billing screen may sell: units left on the shelf and not yet past
     * the expiry date. An expired medicine must never reach a customer.
     */
    public List<Medicine> findInStock() {
        String sql = BASE + "WHERE m.quantity > 0 AND (m.expiry_date IS NULL OR m.expiry_date = '' "
                + "OR m.expiry_date >= ?) ORDER BY m.medicine_name";
        List<Medicine> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the medicines in stock", e);
        }
        return list;
    }

    public Medicine findById(int medicineId) {
        try (PreparedStatement ps = Database.get()
                .prepareStatement(BASE + "WHERE m.medicine_id = ?")) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the medicine", e);
        }
    }

    /** Batches at or below their reorder level - the purchase reminder list. */
    public List<Medicine> findLowStock() {
        return query(BASE + "WHERE m.quantity <= m.reorder_level ORDER BY m.quantity", null);
    }

    /** Batches expiring within the given number of days, already expired ones included. */
    public List<Medicine> findExpiringWithin(int days) {
        String limit = LocalDate.now().plusDays(days).toString();
        String sql = BASE + "WHERE m.expiry_date IS NOT NULL AND m.expiry_date <> '' "
                + "AND m.expiry_date <= ? ORDER BY m.expiry_date";
        List<Medicine> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the expiry list", e);
        }
        return list;
    }

    public void save(Medicine m) {
        try {
            if (m.getMedicineId() == 0) {
                String sql = "INSERT INTO medicines (medicine_name, company, category, batch_no, "
                        + "expiry_date, quantity, purchase_price, mrp, reorder_level, supplier_id) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = Database.get()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, m);
                    ps.executeUpdate();
                    m.setMedicineId(UserDao.generatedId(ps));
                }
            } else {
                String sql = "UPDATE medicines SET medicine_name=?, company=?, category=?, "
                        + "batch_no=?, expiry_date=?, quantity=?, purchase_price=?, mrp=?, "
                        + "reorder_level=?, supplier_id=? WHERE medicine_id=?";
                try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                    bind(ps, m);
                    ps.setInt(11, m.getMedicineId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the medicine", e);
        }
    }

    /** Refuses to delete a batch that already appears on a bill or purchase invoice. */
    public void delete(int medicineId) {
        if (countReferences(medicineId) > 0) {
            throw new DataAccessException(
                    "This medicine cannot be deleted because it already appears on a bill "
                    + "or a purchase invoice.");
        }
        try (PreparedStatement ps = Database.get()
                .prepareStatement("DELETE FROM medicines WHERE medicine_id = ?")) {
            ps.setInt(1, medicineId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the medicine", e);
        }
    }

    /**
     * Moves stock by {@code change} units - positive on purchase, negative on sale.
     * Runs inside the caller's transaction so stock can never drift from the invoice.
     */
    void adjustStock(Connection conn, int medicineId, int change) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE medicines SET quantity = quantity + ? WHERE medicine_id = ?")) {
            ps.setInt(1, change);
            ps.setInt(2, medicineId);
            ps.executeUpdate();
        }
    }

    /** True when the batch is past its printed expiry date, read fresh inside the caller's transaction. */
    boolean isExpired(Connection conn, int medicineId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT expiry_date FROM medicines WHERE medicine_id = ?")) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString(1) == null || rs.getString(1).isBlank()) {
                    return false;
                }
                try {
                    return LocalDate.parse(rs.getString(1)).isBefore(LocalDate.now());
                } catch (RuntimeException notADate) {
                    return false;
                }
            }
        }
    }

    /** Units currently on hand, read fresh so two tills cannot oversell the same batch. */
    int currentQuantity(Connection conn, int medicineId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT quantity FROM medicines WHERE medicine_id = ?")) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int countReferences(int medicineId) {
        String sql = "SELECT (SELECT COUNT(*) FROM sale_items WHERE medicine_id = ?) + "
                + "(SELECT COUNT(*) FROM purchase_items WHERE medicine_id = ?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.setInt(2, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the medicine", e);
        }
    }

    private void bind(PreparedStatement ps, Medicine m) throws SQLException {
        ps.setString(1, m.getMedicineName().trim());
        ps.setString(2, m.getCompany());
        ps.setString(3, m.getCategory());
        ps.setString(4, m.getBatchNo());
        ps.setString(5, m.getExpiryDate());
        ps.setInt(6, m.getQuantity());
        ps.setDouble(7, m.getPurchasePrice());
        ps.setDouble(8, m.getMrp());
        ps.setInt(9, m.getReorderLevel());
        if (m.getSupplierId() > 0) {
            ps.setInt(10, m.getSupplierId());
        } else {
            ps.setNull(10, Types.INTEGER);
        }
    }

    private List<Medicine> query(String sql, String likeTerm) {
        List<Medicine> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (likeTerm != null) {
                for (int i = 1; i <= 3; i++) {
                    ps.setString(i, likeTerm);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the medicine list", e);
        }
        return list;
    }

    private Medicine map(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setMedicineId(rs.getInt("medicine_id"));
        m.setMedicineName(rs.getString("medicine_name"));
        m.setCompany(rs.getString("company"));
        m.setCategory(rs.getString("category"));
        m.setBatchNo(rs.getString("batch_no"));
        m.setExpiryDate(rs.getString("expiry_date"));
        m.setQuantity(rs.getInt("quantity"));
        m.setPurchasePrice(rs.getDouble("purchase_price"));
        m.setMrp(rs.getDouble("mrp"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setSupplierId(rs.getInt("supplier_id"));
        String supplier = rs.getString("supplier_name");
        m.setSupplierName(supplier == null ? "" : supplier);
        return m;
    }
}
