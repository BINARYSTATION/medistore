package medistore.model;

import java.util.ArrayList;
import java.util.List;

/** A counter bill: header details plus the medicines sold on it. */
public class Sale {
    private int saleId;
    private String billNo = "";
    private int customerId;
    private String customerName = "";
    private String saleDate = "";
    private double discount;
    private int userId;
    private double storedTotal;
    private final List<SaleItem> items = new ArrayList<>();

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getSaleDate() { return saleDate; }
    public void setSaleDate(String saleDate) { this.saleDate = saleDate; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public List<SaleItem> getItems() { return items; }

    /** Used when a list screen reads the saved total without loading every line. */
    public void setStoredTotal(double storedTotal) { this.storedTotal = storedTotal; }

    /**
     * Sum of every line before discount: added up from the lines while a bill is
     * being made, and taken from the saved figure on list screens.
     */
    public double getTotalAmount() {
        if (items.isEmpty()) {
            return storedTotal;
        }
        double total = 0;
        for (SaleItem item : items) {
            total += item.getAmount();
        }
        return total;
    }

    /** What the customer actually pays. */
    public double getNetAmount() { return getTotalAmount() - discount; }
}
