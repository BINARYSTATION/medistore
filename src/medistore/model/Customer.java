package medistore.model;

/** A walk-in or regular buyer at the counter. */
public class Customer {
    private int customerId;
    private String customerName = "";
    private String phone = "";
    private String email = "";
    private String address = "";
    private String registeredOn = "";

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(String registeredOn) { this.registeredOn = registeredOn; }

    @Override public String toString() { return customerName; }
}
