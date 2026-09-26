package medistore.tools;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.JTextField;
import medistore.dao.*;
import medistore.model.*;
import medistore.util.Validator;

/**
 * Runs the twelve test cases listed in the project report against a throwaway
 * database, so every "Pass" in the report has actually been observed.
 *
 * Start it with -Dmedistore.db=<some temp file> so the real data is never touched.
 * Screens are covered through the classes they call: the same Validator, DAOs and
 * business rules sit behind every form, so the outcome is the one a user would see.
 */
public final class SmokeTest {

    private static int passed;
    private static int failed;

    private SmokeTest() {
    }

    public static void main(String[] args) {
        String db = System.getProperty("medistore.db", "");
        if (db.isBlank() || db.contains("medistore.db") && !db.contains("tmp")) {
            System.err.println("Refusing to run against the real database. "
                    + "Pass -Dmedistore.db=/tmp/smoke.db");
            System.exit(2);
        }
        SampleData.main(new String[0]);
        System.out.println();
        System.out.println("Running the report's test cases");
        System.out.println("-------------------------------");

        MedicineDao medicines = new MedicineDao();
        SupplierDao suppliers = new SupplierDao();
        UserDao users = new UserDao();
        PurchaseDao purchases = new PurchaseDao();
        SaleDao sales = new SaleDao();

        check("T1  valid login opens the application",
                () -> users.authenticate("admin", "admin123") != null);

        check("T2  wrong password is rejected",
                () -> users.authenticate("admin", "not-the-password") == null);

        check("T3  empty medicine name is refused with the right message", () -> {
            Validator v = new Validator().required(new JTextField(""), "Medicine name");
            return !v.isValid() && v.message().contains("Medicine name is required.");
        });

        check("T4  negative quantity is refused", () -> {
            Validator v = new Validator().wholeNumber(new JTextField("-5"), "Quantity", 0);
            return !v.isValid() && v.message().contains("cannot be less than 0");
        });

        check("T5  nine digit phone number is refused", () -> {
            Validator v = new Validator().phone(new JTextField("123456789"), "Phone");
            return !v.isValid() && v.message().contains("must be exactly 10 digits");
        });

        Medicine target = medicines.findInStock().get(0);

        check("T6  purchase of 50 units raises stock by 50", () -> {
            int before = medicines.findById(target.getMedicineId()).getQuantity();
            Purchase p = new Purchase();
            p.setInvoiceNo(purchases.nextInvoiceNo());
            p.setSupplierId(suppliers.findAll().get(0).getSupplierId());
            p.setPurchaseDate(LocalDate.now().toString());
            PurchaseItem line = new PurchaseItem();
            line.setMedicineId(target.getMedicineId());
            line.setMedicineName(target.getMedicineName());
            line.setQuantity(50);
            line.setRate(target.getPurchasePrice());
            p.getItems().add(line);
            purchases.save(p);
            return medicines.findById(target.getMedicineId()).getQuantity() == before + 50;
        });

        check("T7  bill of 3 units lowers stock by 3", () -> {
            int before = medicines.findById(target.getMedicineId()).getQuantity();
            sales.save(billFor(sales, target, 3, 0));
            return medicines.findById(target.getMedicineId()).getQuantity() == before - 3;
        });

        check("T8  bill for more than the stock is refused and changes nothing", () -> {
            int stockBefore = medicines.findById(target.getMedicineId()).getQuantity();
            int billsBefore = sales.findAll().size();
            try {
                sales.save(billFor(sales, target, stockBefore + 1, 0));
                return false;
            } catch (DataAccessException expected) {
                return expected.getMessage().contains("left in stock")
                        && medicines.findById(target.getMedicineId()).getQuantity() == stockBefore
                        && sales.findAll().size() == billsBefore;
            }
        });

        check("T9  supplier with linked medicines cannot be deleted", () -> {
            int linked = target.getSupplierId();
            try {
                suppliers.delete(linked);
                return false;
            } catch (DataAccessException expected) {
                return expected.getMessage().contains("cannot be deleted")
                        && suppliers.findById(linked) != null;
            }
        });

        check("T10 duplicate user name is refused", () -> {
            boolean formCheck = users.usernameExists("admin", 0);
            User duplicate = new User();
            duplicate.setUsername("admin");
            duplicate.setFullName("Someone Else");
            try {
                users.save(duplicate, "whatever1");
                return false;
            } catch (DataAccessException expected) {
                return formCheck;
            }
        });

        check("T11 expiry report lists batches already past their date", () -> {
            List<Medicine> expired = medicines.findExpiringWithin(-1);
            return !expired.isEmpty() && expired.stream().allMatch(Medicine::isExpired);
        });

        check("T12 discount larger than the bill total is refused", () -> {
            try {
                sales.save(billFor(sales, target, 1, 1_000_000));
                return false;
            } catch (DataAccessException expected) {
                return expected.getMessage().contains("discount");
            }
        });

        check("T13 expired batch cannot be billed and is not offered for sale", () -> {
            Medicine expired = medicines.findExpiringWithin(-1).stream()
                    .filter(m -> m.getQuantity() > 0).findFirst().orElseThrow();
            boolean offered = medicines.findInStock().stream()
                    .anyMatch(m -> m.getMedicineId() == expired.getMedicineId());
            try {
                sales.save(billFor(sales, expired, 1, 0));
                return false;
            } catch (DataAccessException refused) {
                return !offered && refused.getMessage().contains("expired");
            }
        });

        System.out.println("-------------------------------");
        System.out.printf("%d passed, %d failed%n", passed, failed);
        medistore.db.Database.close();
        System.exit(failed == 0 ? 0 : 1);
    }

    private static Sale billFor(SaleDao dao, Medicine medicine, int quantity, double discount) {
        Sale sale = new Sale();
        sale.setBillNo(dao.nextBillNo());
        sale.setSaleDate(LocalDate.now().toString());
        sale.setUserId(1);
        sale.setDiscount(discount);
        SaleItem line = new SaleItem();
        line.setMedicineId(medicine.getMedicineId());
        line.setMedicineName(medicine.getMedicineName());
        line.setBatchNo(medicine.getBatchNo());
        line.setQuantity(quantity);
        line.setRate(medicine.getMrp());
        sale.getItems().add(line);
        return sale;
    }

    private static void check(String name, BooleanSupplier test) {
        boolean ok;
        try {
            ok = test.getAsBoolean();
        } catch (RuntimeException e) {
            System.out.println("  FAIL  " + name + "   (" + e + ")");
            failed++;
            return;
        }
        System.out.println((ok ? "  PASS  " : "  FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
