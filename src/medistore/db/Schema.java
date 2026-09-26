package medistore.db;

/**
 * The complete database design in one place.
 *
 * Eight tables: four masters (users, suppliers, customers, medicines) and two
 * transactions that each split into a header and a detail table (purchases and
 * sales). This is the structure documented by the ER diagram and the data
 * dictionary in the project report.
 */
final class Schema {

    private Schema() {
    }

    static final String[] STATEMENTS = {

            """
            CREATE TABLE IF NOT EXISTS users (
                user_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                username   TEXT    NOT NULL UNIQUE,
                password   TEXT    NOT NULL,
                full_name  TEXT    NOT NULL,
                role       TEXT    NOT NULL DEFAULT 'Pharmacist',
                created_on TEXT    NOT NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS suppliers (
                supplier_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                supplier_name  TEXT    NOT NULL,
                contact_person TEXT,
                phone          TEXT,
                email          TEXT,
                address        TEXT,
                gst_number     TEXT
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS customers (
                customer_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_name TEXT    NOT NULL,
                phone         TEXT,
                email         TEXT,
                address       TEXT,
                registered_on TEXT
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS medicines (
                medicine_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                medicine_name  TEXT    NOT NULL,
                company        TEXT,
                category       TEXT,
                batch_no       TEXT,
                expiry_date    TEXT,
                quantity       INTEGER NOT NULL DEFAULT 0,
                purchase_price REAL    NOT NULL DEFAULT 0,
                mrp            REAL    NOT NULL DEFAULT 0,
                reorder_level  INTEGER NOT NULL DEFAULT 10,
                supplier_id    INTEGER REFERENCES suppliers(supplier_id)
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS purchases (
                purchase_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                invoice_no    TEXT    NOT NULL,
                supplier_id   INTEGER NOT NULL REFERENCES suppliers(supplier_id),
                purchase_date TEXT    NOT NULL,
                total_amount  REAL    NOT NULL DEFAULT 0
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS purchase_items (
                purchase_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                purchase_id      INTEGER NOT NULL REFERENCES purchases(purchase_id),
                medicine_id      INTEGER NOT NULL REFERENCES medicines(medicine_id),
                quantity         INTEGER NOT NULL,
                rate             REAL    NOT NULL,
                amount           REAL    NOT NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS sales (
                sale_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                bill_no      TEXT    NOT NULL UNIQUE,
                customer_id  INTEGER REFERENCES customers(customer_id),
                sale_date    TEXT    NOT NULL,
                total_amount REAL    NOT NULL DEFAULT 0,
                discount     REAL    NOT NULL DEFAULT 0,
                net_amount   REAL    NOT NULL DEFAULT 0,
                user_id      INTEGER REFERENCES users(user_id)
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS sale_items (
                sale_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
                sale_id      INTEGER NOT NULL REFERENCES sales(sale_id),
                medicine_id  INTEGER NOT NULL REFERENCES medicines(medicine_id),
                quantity     INTEGER NOT NULL,
                rate         REAL    NOT NULL,
                amount       REAL    NOT NULL
            )
            """
    };
}
