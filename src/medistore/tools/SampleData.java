package medistore.tools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import medistore.dao.*;
import medistore.db.Database;
import medistore.model.*;

/**
 * Fills an empty database with a realistic month of trading.
 *
 * Run it once before a demonstration so every screen has something to show.
 * Purchases and sales are written through the normal DAOs rather than straight
 * into the tables, so the stock figures it produces are the same ones the
 * application itself would produce.
 */
public final class SampleData {

    /** Fixed seed: the demo data is the same every time, so screenshots match the report. */
    private static final Random RANDOM = new Random(20260921L);

    private static final String[][] MEDICINES = {
            // name, company, category, purchase price, mrp
            {"Crocin Advance 500mg",     "GSK",            "Tablet",   "18.40",  "24.50"},
            {"Dolo 650mg",               "Micro Labs",     "Tablet",   "22.10",  "30.00"},
            {"Azithral 500mg",           "Alembic",        "Tablet",   "85.00",  "112.00"},
            {"Augmentin 625 Duo",        "GSK",            "Tablet",   "152.00", "201.50"},
            {"Pan 40mg",                 "Alkem",          "Tablet",   "98.00",  "131.00"},
            {"Shelcal 500mg",            "Torrent",        "Tablet",   "82.00",  "108.00"},
            {"Allegra 120mg",            "Sanofi",         "Tablet",   "165.00", "218.00"},
            {"Montair LC",               "Cipla",          "Tablet",   "142.00", "189.00"},
            {"Zincovit",                 "Apex Labs",      "Tablet",   "88.00",  "115.00"},
            {"Thyronorm 50mcg",          "Abbott",         "Tablet",   "118.00", "155.00"},
            {"Metformin 500mg",          "USV",            "Tablet",   "26.00",  "35.00"},
            {"Amlodipine 5mg",           "Cipla",          "Tablet",   "31.00",  "42.00"},
            {"Benadryl Cough Syrup",     "J&J",            "Syrup",    "108.00", "142.00"},
            {"Ascoril LS Syrup",         "Glenmark",       "Syrup",    "94.00",  "124.00"},
            {"Digene Gel Mint",          "Abbott",         "Syrup",    "118.00", "152.00"},
            {"Cetirizine 10mg",          "Dr Reddys",      "Tablet",   "14.00",  "20.00"},
            {"Volini Pain Relief Gel",   "Sun Pharma",     "Ointment", "128.00", "168.00"},
            {"Moov Cream",               "Reckitt",        "Ointment", "92.00",  "121.00"},
            {"Betadine Ointment 20g",    "Win Medicare",   "Ointment", "78.00",  "102.00"},
            {"Soframycin Cream",         "Sanofi",         "Ointment", "44.00",  "58.00"},
            {"Insulin Actrapid 100IU",   "Novo Nordisk",   "Injection","262.00", "345.00"},
            {"Voveran Injection",        "Novartis",       "Injection","32.00",  "44.00"},
            {"Dettol Antiseptic 250ml",  "Reckitt",        "Other",    "132.00", "172.00"},
            {"Accu-Chek Test Strips",    "Roche",          "Other",    "820.00", "1045.00"},
            {"Surgical Face Mask (50)",  "Romsons",        "Other",    "145.00", "199.00"},
    };

    private static final String[][] SUPPLIERS = {
            {"Sharma Medical Distributors", "Rakesh Sharma", "9822013455", "orders@sharmamed.in",
             "14 Laxmi Road, Pune, MH 411030",          "27AABCS1429B1Z5"},
            {"Apex Pharma Agency",          "Nitin Verma",   "9811204477", "sales@apexpharma.co.in",
             "Plot 8, Bhosari MIDC, Pimpri-Chinchwad, MH", "27AACCA9921K1Z2"},
            {"Sahyadri Drug House",         "Sunita Rathore","9765443210", "contact@sahyadridrug.in",
             "Main Road, Junnar, Pune, MH 410502",      "27AADCS4410M1Z9"},
            {"Om Sai Medicos Supply",       "Prakash Joshi", "9890112233", "omsai.supply@gmail.com",
             "Budhwar Peth, Pune, MH 411002",           "27AAECO7788J1Z4"},
            {"Healthline Wholesale",        "Farhan Qureshi","9755667788", "info@healthlinewhl.in",
             "Market Yard, Pune, MH 411037",            "27AAFCH2255L1Z7"},
            {"Ganesh Pharmaceuticals",      "Meena Agarwal", "9926554411", "ganeshpharma@yahoo.in",
             "Station Road, Narayangaon, Pune, MH 410504","27AAGCG6633P1Z1"},
    };

    private static final String[][] CUSTOMERS = {
            {"Anil Kumar Jain",   "9425011223", "aniljain@gmail.com",     "22 Shivaji Nagar, Junnar"},
            {"Sunita Patil",      "9826033445", "sunita.patil@gmail.com", "5 Kothrud, Pune"},
            {"Mohammed Irfan",    "9754022110", "irfan.m@outlook.com",    "B-9 Kondhwa, Pune"},
            {"Rekha Sharma",      "9893044556", "",                       "78 Karve Nagar, Pune"},
            {"Deepak Chavan",     "9691055667", "deepak.c@gmail.com",     "Narayangaon, Tal. Junnar"},
            {"Priya Nair",        "9977066778", "priya.nair@gmail.com",   "14 Baner, Pune"},
            {"Ramesh Yadav",      "9425077889", "",                       "Old Sangvi, Pune"},
            {"Fatima Sheikh",     "9826088990", "fatima.s@gmail.com",     "Camp, Pune"},
            {"Vikram Singh Pawar","9755099001", "vikram.pawar@gmail.com", "Otur, Tal. Junnar"},
            {"Kavita Deshmukh",   "9893100112", "",                       "Alephata, Tal. Junnar"},
            {"Sanjay Gupta",      "9691111223", "sanjaygupta@gmail.com",  "Pimple Saudagar, Pune"},
            {"Neha Kulkarni",     "9977122334", "neha.k@gmail.com",       "Sadashiv Peth, Pune"},
    };

    private SampleData() {
    }

    public static void main(String[] args) {
        System.out.println("Rebuilding the demonstration database...");
        Database.reset();

        List<Integer> supplierIds = insertSuppliers();
        insertUsers();
        insertCustomers();
        List<Medicine> medicines = insertMedicines(supplierIds);
        insertPurchases(medicines, supplierIds);
        insertSales(medicines);

        report();
        Database.close();
        System.out.println("Done.");
    }

    private static void insertUsers() {
        UserDao dao = new UserDao();

        User admin = new User();
        admin.setUsername("admin");
        admin.setFullName("Store Owner");
        admin.setRole("Administrator");
        admin.setCreatedOn("2026-08-01");
        dao.save(admin, "admin123");

        User pharmacist = new User();
        pharmacist.setUsername("pooja");
        pharmacist.setFullName("Pooja Verma");
        pharmacist.setRole("Pharmacist");
        pharmacist.setCreatedOn("2026-08-14");
        dao.save(pharmacist, "pooja123");

        User counter = new User();
        counter.setUsername("ravi");
        counter.setFullName("Ravi Solanki");
        counter.setRole("Counter Staff");
        counter.setCreatedOn("2026-09-02");
        dao.save(counter, "ravi123");
    }

    private static List<Integer> insertSuppliers() {
        SupplierDao dao = new SupplierDao();
        List<Integer> ids = new ArrayList<>();
        for (String[] row : SUPPLIERS) {
            Supplier s = new Supplier();
            s.setSupplierName(row[0]);
            s.setContactPerson(row[1]);
            s.setPhone(row[2]);
            s.setEmail(row[3]);
            s.setAddress(row[4]);
            s.setGstNumber(row[5]);
            dao.save(s);
            ids.add(s.getSupplierId());
        }
        return ids;
    }

    private static void insertCustomers() {
        CustomerDao dao = new CustomerDao();
        LocalDate from = LocalDate.now().minusMonths(6);
        for (String[] row : CUSTOMERS) {
            Customer c = new Customer();
            c.setCustomerName(row[0]);
            c.setPhone(row[1]);
            c.setEmail(row[2]);
            c.setAddress(row[3]);
            c.setRegisteredOn(from.plusDays(RANDOM.nextInt(170)).toString());
            dao.save(c);
        }
    }

    /**
     * Creates the medicine master with a deliberate spread: a few batches already
     * expired, a few expiring within three months, and a few down at reorder level,
     * so the alert screens have something real to report.
     */
    private static List<Medicine> insertMedicines(List<Integer> supplierIds) {
        MedicineDao dao = new MedicineDao();
        List<Medicine> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < MEDICINES.length; i++) {
            String[] row = MEDICINES[i];
            Medicine m = new Medicine();
            m.setMedicineName(row[0]);
            m.setCompany(row[1]);
            m.setCategory(row[2]);
            m.setBatchNo(String.format("B%s%03d", row[1].substring(0, 2).toUpperCase(), 101 + i));
            m.setPurchasePrice(Double.parseDouble(row[3]));
            m.setMrp(Double.parseDouble(row[4]));
            m.setReorderLevel(row[2].equals("Other") ? 5 : 20);

            // Two batches already past date, three more due within ninety days.
            if (i == 4 || i == 17) {
                m.setExpiryDate(today.minusDays(20L + RANDOM.nextInt(60)).toString());
            } else if (i == 2 || i == 9 || i == 21) {
                m.setExpiryDate(today.plusDays(25L + RANDOM.nextInt(60)).toString());
            } else {
                m.setExpiryDate(today.plusMonths(8L + RANDOM.nextInt(22)).toString());
            }

            // Opening stock; three items start low so the reorder list is not empty.
            m.setQuantity(i == 6 || i == 13 || i == 23 ? 3 + RANDOM.nextInt(6)
                                                       : 40 + RANDOM.nextInt(90));
            m.setSupplierId(supplierIds.get(i % supplierIds.size()));
            dao.save(m);
            list.add(m);
        }
        return list;
    }

    /** Four stock-in invoices spread over the past six weeks. */
    private static void insertPurchases(List<Medicine> medicines, List<Integer> supplierIds) {
        PurchaseDao dao = new PurchaseDao();
        LocalDate today = LocalDate.now();

        for (int invoice = 0; invoice < 4; invoice++) {
            Purchase p = new Purchase();
            p.setInvoiceNo(dao.nextInvoiceNo());
            p.setSupplierId(supplierIds.get(invoice % supplierIds.size()));
            p.setPurchaseDate(today.minusDays(42L - invoice * 11L).toString());

            for (int line = 0; line < 4 + RANDOM.nextInt(3); line++) {
                Medicine m = medicines.get(RANDOM.nextInt(medicines.size()));
                PurchaseItem item = new PurchaseItem();
                item.setMedicineId(m.getMedicineId());
                item.setMedicineName(m.getMedicineName());
                item.setQuantity(20 + RANDOM.nextInt(60));
                item.setRate(m.getPurchasePrice());
                p.getItems().add(item);
            }
            dao.save(p);
        }
    }

    /** Roughly forty bills across the past thirty days. */
    private static void insertSales(List<Medicine> medicines) {
        SaleDao saleDao = new SaleDao();
        MedicineDao medicineDao = new MedicineDao();
        LocalDate today = LocalDate.now();

        for (int day = 29; day >= 0; day--) {
            int billsToday = 1 + RANDOM.nextInt(3);
            for (int bill = 0; bill < billsToday; bill++) {
                Sale sale = new Sale();
                sale.setBillNo(saleDao.nextBillNo());
                sale.setCustomerId(RANDOM.nextInt(5) == 0 ? 0 : 1 + RANDOM.nextInt(CUSTOMERS.length));
                sale.setSaleDate(today.minusDays(day).toString());
                sale.setUserId(1 + RANDOM.nextInt(3));

                // Read stock back each time so the demo never tries to oversell.
                for (int line = 0; line < 1 + RANDOM.nextInt(3); line++) {
                    Medicine fresh = medicineDao.findById(
                            medicines.get(RANDOM.nextInt(medicines.size())).getMedicineId());
                    if (fresh == null || fresh.getQuantity() < 4 || fresh.isExpired()) {
                        continue;
                    }
                    SaleItem item = new SaleItem();
                    item.setMedicineId(fresh.getMedicineId());
                    item.setMedicineName(fresh.getMedicineName());
                    item.setBatchNo(fresh.getBatchNo());
                    item.setQuantity(1 + RANDOM.nextInt(3));
                    item.setRate(fresh.getMrp());
                    sale.getItems().add(item);
                }
                if (sale.getItems().isEmpty()) {
                    continue;
                }
                // A rounded-off discount on roughly one bill in three.
                sale.setDiscount(RANDOM.nextInt(3) == 0
                        ? Math.round(sale.getTotalAmount() * 0.05) : 0);
                saleDao.save(sale);
            }
        }
    }

    private static void report() {
        ReportDao dao = new ReportDao();
        System.out.printf("  medicines     : %d%n", dao.countMedicines());
        System.out.printf("  suppliers     : %d%n", dao.countSuppliers());
        System.out.printf("  customers     : %d%n", dao.countCustomers());
        System.out.printf("  bills         : %d%n", dao.countBills());
        System.out.printf("  low stock     : %d%n", dao.countLowStock());
        System.out.printf("  expiring 90d  : %d%n", dao.countExpiringWithin(90));
        System.out.printf("  stock value   : %.2f%n", dao.stockValue());
        System.out.printf("  sales 30 days : %.2f%n",
                dao.salesBetween(LocalDate.now().minusDays(30).toString(), LocalDate.now().toString()));
    }
}
