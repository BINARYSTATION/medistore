package medistore.model;

/** A distributor the store buys its stock from. */
public class Supplier {
    private int supplierId;
    private String supplierName = "";
    private String contactPerson = "";
    private String phone = "";
    private String email = "";
    private String address = "";
    private String gstNumber = "";

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    /** Combo boxes show the supplier name, so keep this readable. */
    @Override public String toString() { return supplierName; }
}
