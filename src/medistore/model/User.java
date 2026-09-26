package medistore.model;

/** One login account of the medical store software. */
public class User {
    private int userId;
    private String username = "";
    private String password = "";
    private String fullName = "";
    private String role = "Pharmacist";
    private String createdOn = "";

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedOn() { return createdOn; }
    public void setCreatedOn(String createdOn) { this.createdOn = createdOn; }

    @Override public String toString() { return fullName; }
}
