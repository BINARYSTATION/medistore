package medistore.model;

/** One medicine line inside a purchase invoice. */
public class PurchaseItem {
    private int purchaseItemId;
    private int purchaseId;
    private int medicineId;
    private String medicineName = "";
    private int quantity;
    private double rate;

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }
    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    /** Line total is always derived, never typed in, so it can never disagree. */
    public double getAmount() { return quantity * rate; }
}
