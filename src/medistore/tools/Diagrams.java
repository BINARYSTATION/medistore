package medistore.tools;

/**
 * Draws the four design diagrams the project report needs: the context level and
 * level one data flow diagrams, the entity relationship diagram and the
 * relational data model.
 *
 * Generating them from code keeps them honest - the table boxes list the same
 * columns the database actually has, so the report cannot drift from the software.
 */
public final class Diagrams {

    private static final String OUT = "docs/diagrams/";

    private Diagrams() {
    }

    public static void main(String[] args) {
        System.out.println("Drawing the design diagrams...");
        contextDiagram();
        levelOneDiagram();
        entityRelationshipDiagram();
        dataModelDiagram();
        System.out.println("Done.");
    }

    /** Level 0: the whole system as one process, with the four outside parties. */
    private static void contextDiagram() {
        DiagramCanvas c = new DiagramCanvas(900, 520);
        c.heading("Context Level Data Flow Diagram (Level 0)");
        c.subheading("Medical Store Management System");

        c.box(60, 110, 150, 56, "Pharmacist / Counter Staff",
                DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(690, 110, 150, 56, "Supplier",
                DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(60, 380, 150, 56, "Customer",
                DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(690, 380, 150, 56, "Store Manager",
                DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);

        c.process(350, 225, 200, 110, "0", "Medical Store Management System");

        c.arrow(210, 118, 352, 236, "login, data entry");
        c.arrow(352, 285, 210, 160, "screens, alerts");
        c.arrow(690, 118, 548, 236, "supply details");
        c.arrow(548, 285, 690, 160, "reorder list");
        c.arrow(210, 392, 352, 325, "medicine request");
        c.arrow(352, 280, 210, 378, "printed bill");
        c.arrow(690, 392, 548, 325, "report request");
        c.arrow(548, 280, 690, 378, "stock & sales reports");

        c.legend(60, 480, new String[][]{
                {"External entity", "#E0E7FF"},
                {"Process", "#CCFBF1"}});

        c.save(OUT + "dfd-level-0.png");
    }

    /** Level 1: the five processes the system breaks into, and the files they use. */
    private static void levelOneDiagram() {
        DiagramCanvas c = new DiagramCanvas(1000, 720);
        c.heading("Level 1 Data Flow Diagram");
        c.subheading("Medical Store Management System");

        // Outside parties down the left, each level with the process it talks to.
        c.box(30, 110, 120, 52, "Pharmacist", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(30, 330, 120, 52, "Supplier", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(30, 452, 120, 52, "Customer", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(30, 590, 120, 52, "Store Manager", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);

        // The five processes down the middle.
        c.process(290, 100, 170, 74, "1.0", "User Login");
        c.process(290, 215, 170, 74, "2.0", "Master Management");
        c.process(290, 330, 170, 74, "3.0", "Purchase Entry");
        c.process(290, 445, 170, 74, "4.0", "Sales Billing");
        c.process(290, 575, 170, 74, "5.0", "Report Generation");

        // The files down the right. Medicines is tall because four processes touch it.
        c.store(640, 95, 300, 38, "D1", "users");
        c.store(640, 160, 300, 38, "D2", "suppliers / customers");
        c.store(640, 225, 300, 110, "D3", "medicines");
        c.store(640, 360, 300, 38, "D4", "purchases / purchase_items");
        c.store(640, 450, 300, 38, "D5", "sales / sale_items");

        // Outside parties and the processes they exchange data with.
        c.arrow(150, 130, 288, 130, "credentials");
        c.arrow(150, 152, 288, 240, "master data");
        c.arrow(150, 356, 288, 366, "invoice");
        c.arrow(150, 470, 288, 470, "request");
        c.arrow(288, 494, 150, 494, "bill");
        c.arrow(150, 606, 288, 606, "request");
        c.arrow(288, 632, 150, 632, "reports");

        // Processes writing to the files.
        c.arrow(460, 128, 638, 114, "verify");
        c.arrow(460, 240, 638, 179, "save");
        c.arrow(460, 262, 638, 245, "save");
        c.arrow(460, 352, 638, 270, "stock +");
        c.arrow(460, 378, 638, 379, "invoice", 0.3);
        c.arrow(460, 470, 638, 298, "stock -", 0.72);
        c.arrow(460, 496, 638, 469, "bill");

        // Report Generation reading them back; each starts on the edge of a store.
        c.arrow(638, 322, 460, 592, "read");
        c.arrow(638, 478, 460, 624, "read");

        c.legend(30, 680, new String[][]{
                {"External entity", "#E0E7FF"},
                {"Process", "#CCFBF1"},
                {"Data store", "#FEF3C7"}});

        c.save(OUT + "dfd-level-1.png");
    }

    /**
     * The entity relationship diagram in Chen notation, with every attribute. Foreign
     * keys are not drawn as attributes: in an ERD they are the relationships.
     */
    private static void entityRelationshipDiagram() {
        DiagramCanvas c = new DiagramCanvas(1120, 1240);
        c.heading("Entity Relationship Diagram");
        c.subheading("Medical Store Management System  ·  Chen notation  ·  "
                + "an underlined attribute is the primary key");
        c.shift(0, -60);

        // Centre of every entity and relationship.
        double[] supplier = {230, 360}, customer = {890, 360}, medicine = {560, 720};
        double[] sale = {890, 720}, purchase = {230, 1080}, user = {890, 1080};
        double[] purchasedIn = {430, 1080}, soldIn = {727, 720};

        record Attribute(double[] owner, String name, double x, double y) {
        }
        Attribute[] attributes = {
                new Attribute(supplier, "supplier_id", 72, 360),
                new Attribute(supplier, "supplier_name", 84, 292),
                new Attribute(supplier, "contact_person", 118, 228),
                new Attribute(supplier, "phone", 185, 180),
                new Attribute(supplier, "email", 300, 180),
                new Attribute(supplier, "address", 380, 222),
                new Attribute(supplier, "gst_number", 400, 285),

                new Attribute(customer, "customer_id", 1048, 360),
                new Attribute(customer, "customer_name", 1036, 292),
                new Attribute(customer, "phone", 1000, 228),
                new Attribute(customer, "email", 930, 180),
                new Attribute(customer, "address", 815, 180),
                new Attribute(customer, "registered_on", 745, 232),

                // Medicine's attributes fan out on its only free side, the left.
                new Attribute(medicine, "medicine_id", 490, 438),
                new Attribute(medicine, "medicine_name", 441, 476),
                new Attribute(medicine, "company", 400, 532),
                new Attribute(medicine, "category", 372, 601),
                new Attribute(medicine, "batch_no", 357, 679),
                new Attribute(medicine, "expiry_date", 357, 761),
                new Attribute(medicine, "quantity", 372, 839),
                new Attribute(medicine, "purchase_price", 400, 908),
                new Attribute(medicine, "mrp", 441, 964),
                new Attribute(medicine, "reorder_level", 490, 1002),

                new Attribute(sale, "sale_id", 985, 564),
                new Attribute(sale, "bill_no", 1028, 617),
                new Attribute(sale, "sale_date", 1052, 684),
                new Attribute(sale, "total_amount", 1052, 756),
                new Attribute(sale, "discount", 1028, 823),
                new Attribute(sale, "net_amount", 985, 876),

                new Attribute(purchase, "purchase_id", 72, 1080),
                new Attribute(purchase, "invoice_no", 84, 1148),
                new Attribute(purchase, "purchase_date", 160, 1200),
                new Attribute(purchase, "total_amount", 270, 1215),

                new Attribute(user, "user_id", 1048, 1080),
                new Attribute(user, "username", 1036, 1148),
                new Attribute(user, "password", 975, 1200),
                new Attribute(user, "full_name", 865, 1215),
                new Attribute(user, "role", 765, 1180),
                new Attribute(user, "created_on", 735, 1115),

                // The two many-to-many relationships carry the line details.
                new Attribute(purchasedIn, "quantity", 390, 1170),
                new Attribute(purchasedIn, "rate", 500, 1175),
                new Attribute(purchasedIn, "amount", 600, 1135),
                new Attribute(soldIn, "quantity", 670, 630),
                new Attribute(soldIn, "rate", 790, 630),
                new Attribute(soldIn, "amount", 727, 810)};

        // Lines first, so every shape is painted over the ends of its lines.
        for (Attribute a : attributes) {
            c.line(a.owner()[0], a.owner()[1], a.x(), a.y());
        }
        c.line(300, 360, 367, 360);      // SUPPLIER supplies MEDICINE
        c.line(493, 360, 560, 360);
        c.line(560, 360, 560, 692);
        c.line(230, 386, 230, 691);      // SUPPLIER issues PURCHASE
        c.line(230, 749, 230, 1054);
        c.line(300, 1080, 360, 1080);    // PURCHASE purchased in MEDICINE
        c.line(500, 1080, 560, 1080);
        c.line(560, 1080, 560, 748);
        c.line(635, 720, 664, 720);      // MEDICINE sold in SALE
        c.line(790, 720, 820, 720);
        c.line(890, 386, 890, 511);      // CUSTOMER billed to SALE
        c.line(890, 569, 890, 694);
        c.line(890, 746, 890, 871);      // USER raises SALE
        c.line(890, 929, 890, 1054);

        for (Attribute a : attributes) {
            c.attribute(a.x(), a.y(), 102, 28, a.name(), a.name().endsWith("_id"));
        }

        c.diamond(430, 360, 126, 58, "supplies");
        c.diamond(230, 720, 126, 58, "issues");
        c.diamond(purchasedIn[0], purchasedIn[1], 140, 58, "purchased in");
        c.diamond(soldIn[0], soldIn[1], 126, 58, "sold in");
        c.diamond(890, 540, 126, 58, "billed to");
        c.diamond(890, 900, 126, 58, "raises");

        c.box(160, 334, 140, 52, "SUPPLIER", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(820, 334, 140, 52, "CUSTOMER", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(485, 692, 150, 56, "MEDICINE", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(820, 694, 140, 52, "SALE", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(160, 1054, 140, 52, "PURCHASE", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);
        c.box(820, 1054, 140, 52, "USER", DiagramCanvas.ENTITY, DiagramCanvas.ENTITY_ED);

        c.cardinality("1", 316, 352);
        c.cardinality("M", 574, 684);
        c.cardinality("1", 244, 404);
        c.cardinality("M", 244, 1046);
        c.cardinality("M", 316, 1072);
        c.cardinality("N", 574, 766);
        c.cardinality("N", 650, 712);
        c.cardinality("M", 806, 712);
        c.cardinality("1", 904, 404);
        c.cardinality("M", 904, 686);
        c.cardinality("M", 904, 764);
        c.cardinality("1", 904, 1046);

        c.legend(60, 1255, new String[][]{
                {"Entity", "#E0E7FF"},
                {"Relationship", "#FCE7F3"},
                {"Attribute (underlined: primary key)", "#F0FDF4"}});

        c.save(OUT + "erd.png");
    }

    /** The relational schema: every table, every column, and the foreign keys. */
    private static void dataModelDiagram() {
        DiagramCanvas c = new DiagramCanvas(1160, 800);
        c.heading("Data Model — Relational Schema");
        c.subheading("Eight tables  ·  PK marks a primary key, FK marks a foreign key");

        c.tableBox(50, 90, 200, "suppliers", new String[]{
                "PK supplier_id", "supplier_name", "contact_person", "phone", "email",
                "address", "gst_number"});
        c.tableBox(50, 300, 200, "customers", new String[]{
                "PK customer_id", "customer_name", "phone", "email", "address",
                "registered_on"});
        c.tableBox(50, 500, 200, "users", new String[]{
                "PK user_id", "username", "password", "full_name", "role", "created_on"});

        c.tableBox(470, 60, 200, "purchases", new String[]{
                "PK purchase_id", "invoice_no", "FK supplier_id", "purchase_date",
                "total_amount"});
        c.tableBox(470, 240, 200, "medicines", new String[]{
                "PK medicine_id", "medicine_name", "company", "category", "batch_no",
                "expiry_date", "quantity", "purchase_price", "mrp", "reorder_level",
                "FK supplier_id"});
        c.tableBox(470, 540, 200, "sales", new String[]{
                "PK sale_id", "bill_no", "FK customer_id", "sale_date", "total_amount",
                "discount", "net_amount", "FK user_id"});

        c.tableBox(870, 60, 210, "purchase_items", new String[]{
                "PK purchase_item_id", "FK purchase_id", "FK medicine_id", "quantity",
                "rate", "amount"});
        c.tableBox(870, 540, 210, "sale_items", new String[]{
                "PK sale_item_id", "FK sale_id", "FK medicine_id", "quantity",
                "rate", "amount"});

        c.relation(250, 120, 470, 110);
        c.relation(250, 200, 470, 300);
        c.relation(670, 115, 870, 110);
        c.relation(670, 275, 870, 165);
        c.relation(250, 370, 470, 590);
        c.relation(250, 560, 470, 640);
        c.relation(670, 665, 870, 650);
        c.relation(670, 430, 870, 600);

        c.legend(50, 740, new String[][]{
                {"A single bar marks the \"one\" side, a crow's foot marks the \"many\" side", "#FFFFFF"}});

        c.save(OUT + "data-model.png");
    }
}
