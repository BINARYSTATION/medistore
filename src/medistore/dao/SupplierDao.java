package medistore.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.Supplier;

/** Reads and writes the supplier master. */
public class SupplierDao {

    public List<Supplier> findAll() {
        return query("SELECT * FROM suppliers ORDER BY supplier_name", null);
    }

    /** Matches the term against name, contact person or phone. */
    public List<Supplier> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String sql = "SELECT * FROM suppliers WHERE supplier_name LIKE ? OR contact_person LIKE ? "
                + "OR phone LIKE ? ORDER BY supplier_name";
        return query(sql, "%" + term.trim() + "%");
    }

    public Supplier findById(int supplierId) {
        String sql = "SELECT * FROM suppliers WHERE supplier_id = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the supplier", e);
        }
    }

    public void save(Supplier s) {
        try {
            if (s.getSupplierId() == 0) {
                String sql = "INSERT INTO suppliers (supplier_name, contact_person, phone, email, "
                        + "address, gst_number) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = Database.get()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, s);
                    ps.executeUpdate();
                    s.setSupplierId(UserDao.generatedId(ps));
                }
            } else {
                String sql = "UPDATE suppliers SET supplier_name=?, contact_person=?, phone=?, "
                        + "email=?, address=?, gst_number=? WHERE supplier_id=?";
                try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                    bind(ps, s);
                    ps.setInt(7, s.getSupplierId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the supplier", e);
        }
    }

    /** Refuses to delete a supplier whose medicines or invoices are still on record. */
    public void delete(int supplierId) {
        if (countReferences(supplierId) > 0) {
            throw new DataAccessException(
                    "This supplier cannot be deleted because medicines or purchase invoices "
                    + "are still linked to it.");
        }
        try (PreparedStatement ps = Database.get()
                .prepareStatement("DELETE FROM suppliers WHERE supplier_id = ?")) {
            ps.setInt(1, supplierId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the supplier", e);
        }
    }

    private int countReferences(int supplierId) {
        String sql = "SELECT (SELECT COUNT(*) FROM medicines WHERE supplier_id = ?) + "
                + "(SELECT COUNT(*) FROM purchases WHERE supplier_id = ?)";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            ps.setInt(2, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the supplier", e);
        }
    }

    private void bind(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getSupplierName().trim());
        ps.setString(2, s.getContactPerson());
        ps.setString(3, s.getPhone());
        ps.setString(4, s.getEmail());
        ps.setString(5, s.getAddress());
        ps.setString(6, s.getGstNumber());
    }

    private List<Supplier> query(String sql, String likeTerm) {
        List<Supplier> list = new ArrayList<>();
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
            throw new DataAccessException("Could not load the supplier list", e);
        }
        return list;
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setSupplierId(rs.getInt("supplier_id"));
        s.setSupplierName(rs.getString("supplier_name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setGstNumber(rs.getString("gst_number"));
        return s;
    }
}
