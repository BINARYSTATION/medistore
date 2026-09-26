package medistore.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.Customer;
import medistore.util.UI;

/** Reads and writes the customer master. */
public class CustomerDao {

    public List<Customer> findAll() {
        return query("SELECT * FROM customers ORDER BY customer_name", null);
    }

    /** Matches the term against name or phone number. */
    public List<Customer> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        String sql = "SELECT * FROM customers WHERE customer_name LIKE ? OR phone LIKE ? "
                + "ORDER BY customer_name";
        return query(sql, "%" + term.trim() + "%");
    }

    public void save(Customer c) {
        try {
            if (c.getCustomerId() == 0) {
                String sql = "INSERT INTO customers (customer_name, phone, email, address, "
                        + "registered_on) VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = Database.get()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bind(ps, c);
                    ps.setString(5, c.getRegisteredOn() == null || c.getRegisteredOn().isBlank()
                            ? UI.today() : c.getRegisteredOn());
                    ps.executeUpdate();
                    c.setCustomerId(UserDao.generatedId(ps));
                }
            } else {
                String sql = "UPDATE customers SET customer_name=?, phone=?, email=?, address=? "
                        + "WHERE customer_id=?";
                try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                    bind(ps, c);
                    ps.setInt(5, c.getCustomerId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the customer", e);
        }
    }

    /** Refuses to delete a customer who already has bills on record. */
    public void delete(int customerId) {
        if (billCount(customerId) > 0) {
            throw new DataAccessException(
                    "This customer cannot be deleted because bills are still linked to them.");
        }
        try (PreparedStatement ps = Database.get()
                .prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the customer", e);
        }
    }

    private int billCount(int customerId) {
        try (PreparedStatement ps = Database.get()
                .prepareStatement("SELECT COUNT(*) FROM sales WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the customer", e);
        }
    }

    private void bind(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getCustomerName().trim());
        ps.setString(2, c.getPhone());
        ps.setString(3, c.getEmail());
        ps.setString(4, c.getAddress());
    }

    private List<Customer> query(String sql, String likeTerm) {
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            if (likeTerm != null) {
                ps.setString(1, likeTerm);
                ps.setString(2, likeTerm);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the customer list", e);
        }
        return list;
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("customer_id"));
        c.setCustomerName(rs.getString("customer_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setRegisteredOn(rs.getString("registered_on"));
        return c;
    }
}
