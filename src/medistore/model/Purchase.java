package medistore.model;

import java.util.ArrayList;
import java.util.List;

/** A stock-in invoice received from one supplier. */
public class Purchase {
    private int purchaseId;
    private String invoiceNo = "";
    private int supplierId;
    private String supplierName = "";
    private String purchaseDate = "";
    private double storedTotal;
    private final List<PurchaseItem> items = new ArrayList<>();

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
    public List<PurchaseItem> getItems() { return items; }

    /** Used when a list screen reads the saved total without loading every line. */
    public void setStoredTotal(double storedTotal) { this.storedTotal = storedTotal; }

    /**
     * Total of the invoice: added up from the lines while one is being entered,
     * and taken from the saved figure on list screens that skip loading them.
     */
    public double getTotalAmount() {
        if (items.isEmpty()) {
            return storedTotal;
        }
        double total = 0;
        for (PurchaseItem item : items) {
            total += item.getAmount();
        }
        return total;
    }
}
