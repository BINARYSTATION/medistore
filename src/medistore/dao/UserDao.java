package medistore.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import medistore.db.Database;
import medistore.model.User;
import medistore.util.Passwords;
import medistore.util.UI;

/** Reads and writes login accounts. */
public class UserDao {

    /** Returns the matching user, or null when the name or password is wrong. */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Passwords.matches(password, rs.getString("password"))) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not verify the login", e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY full_name";
        try (Statement st = Database.get().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the user list", e);
        }
        return users;
    }

    public boolean usernameExists(String username, int exceptUserId) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND user_id <> ?";
        try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setInt(2, exceptUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the user name", e);
        }
    }

    /** Inserts a new user, or updates the existing one when the id is set. */
    public void save(User user, String plainPasswordOrNull) {
        try {
            if (user.getUserId() == 0) {
                String sql = "INSERT INTO users (username, password, full_name, role, created_on) "
                        + "VALUES (?,?,?,?,?)";
                try (PreparedStatement ps = Database.get()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, user.getUsername().trim());
                    ps.setString(2, Passwords.hash(plainPasswordOrNull));
                    ps.setString(3, user.getFullName().trim());
                    ps.setString(4, user.getRole());
                    ps.setString(5, user.getCreatedOn().isBlank() ? UI.today() : user.getCreatedOn());
                    ps.executeUpdate();
                    user.setUserId(generatedId(ps));
                }
            } else if (plainPasswordOrNull == null || plainPasswordOrNull.isEmpty()) {
                // Editing the person's details without touching their password.
                String sql = "UPDATE users SET username=?, full_name=?, role=? WHERE user_id=?";
                try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                    ps.setString(1, user.getUsername().trim());
                    ps.setString(2, user.getFullName().trim());
                    ps.setString(3, user.getRole());
                    ps.setInt(4, user.getUserId());
                    ps.executeUpdate();
                }
            } else {
                String sql = "UPDATE users SET username=?, password=?, full_name=?, role=? WHERE user_id=?";
                try (PreparedStatement ps = Database.get().prepareStatement(sql)) {
                    ps.setString(1, user.getUsername().trim());
                    ps.setString(2, Passwords.hash(plainPasswordOrNull));
                    ps.setString(3, user.getFullName().trim());
                    ps.setString(4, user.getRole());
                    ps.setInt(5, user.getUserId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the user", e);
        }
    }

    public void delete(int userId) {
        try (PreparedStatement ps = Database.get()
                .prepareStatement("DELETE FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the user", e);
        }
    }

    static int generatedId(Statement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            return keys.next() ? keys.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setCreatedOn(rs.getString("created_on"));
        return u;
    }
}
