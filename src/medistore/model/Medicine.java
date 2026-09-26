package medistore.model;

import java.time.LocalDate;

/** One medicine batch held in stock. */
public class Medicine {
    private int medicineId;
    private String medicineName = "";
    private String company = "";
    private String category = "";
    private String batchNo = "";
    private String expiryDate = "";
    private int quantity;
    private double purchasePrice;
    private double mrp;
    private int reorderLevel = 10;
    private int supplierId;
    private String supplierName = "";

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    /** Stock value at cost price, used by the stock report. */
    public double getStockValue() { return quantity * purchasePrice; }

    public boolean isLowStock() { return quantity <= reorderLevel; }

    /** True once the batch is past its printed expiry date. */
    public boolean isExpired() {
        LocalDate expiry = parseExpiry();
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    /** Days left before the batch expires; negative once it already has. */
    public long getDaysToExpiry() {
        LocalDate expiry = parseExpiry();
        if (expiry == null) {
            return Long.MAX_VALUE;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiry);
    }

    private LocalDate parseExpiry() {
        if (expiryDate == null || expiryDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(expiryDate);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override public String toString() { return medicineName + " (" + batchNo + ")"; }
}
