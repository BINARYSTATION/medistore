package medistore.model;

/** One medicine line on a customer bill. */
public class SaleItem {
    private int saleItemId;
    private int saleId;
    private int medicineId;
    private String medicineName = "";
    private String batchNo = "";
    private int quantity;
    private double rate;

    public int getSaleItemId() { return saleItemId; }
    public void setSaleItemId(int saleItemId) { this.saleItemId = saleItemId; }
    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public double getAmount() { return quantity * rate; }
}
