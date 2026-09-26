# 2. Acknowledgement

I take this opportunity to express my sincere gratitude to everyone who supported me in completing this project, the Medical Store Management System.

First, I thank my project guide, {{GUIDE}}, for patient guidance, regular feedback and constant encouragement.

I am grateful to the Head of the Department for providing the laboratory facilities and the freedom to choose a problem taken from everyday life. I also thank the Principal of {{COLLEGE}} for maintaining an environment in which students are encouraged to learn by building working systems.

I extend my thanks to all the faculty members of the department. Their teaching of databases, programming and computer applications supplied the foundation on which this project stands.

Finally, I thank my family and friends. They supported me through the long working days, and this work would not have been completed without them.

{{STUDENT1_NAME}}

# 3. Abstract

Small medical stores still keep most of their records in paper registers: one for purchases, one for stock, a bill book for sales and a diary for supplier addresses. Finding the balance of a medicine, noticing that a batch is close to its expiry date, or adding up a day's sales all depend on turning pages and doing arithmetic by hand. Mistakes are easy to make and hard to trace.

This project presents the Medical Store Management System, working name MediStore, a desktop application that replaces those registers for a single store. It is written in Java 21 with a Swing graphical interface in the FlatLaf look-and-feel, and it keeps its data in a SQLite database file stored beside the program, so no server and no installation are needed.

The software provides a login screen with three account roles, master screens for medicines, suppliers, customers and user accounts, a Purchase Entry screen that adds received stock, and a Sales Billing screen that deducts sold stock, applies a discount and shows a printable receipt. Stock is changed inside one database transaction together with the invoice or bill, and a bill cannot sell more units than are on hand. A dashboard shows low-stock medicines, batches expiring within 90 days and the sales of the day. Stock, expiry and sales reports and a bill history can be printed or exported to CSV.

The design is documented with data flow diagrams, a data dictionary of eight tables, a relational data model and an entity relationship diagram. Thirteen test cases covering login, validation, stock movement, expiry and delete protection were run and passed. The result is a working prototype for a small pharmacy, and this report states its limitations and possible extensions.

# 5. Introduction

## a) Motivation of the Project

A medical store differs from most small shops in one important way: a large part of its stock has a printed expiry date, and selling a medicine after that date is a safety problem, not only a financial loss. Keeping track of what is on the shelf, how many units remain and which batch will expire next is therefore a daily task, and it has to be done accurately. In many neighborhood stores this work is still carried out with registers and a calculator.

The motivation for this project came from looking at that gap. Software for pharmacies exists, but the packages built for large chains often assume trained staff, a network, a server and a recurring fee. A store with one counter and one or two employees needs something much smaller: a program that starts from a folder, works without an internet connection and covers the handful of tasks that really matter, which are buying stock, selling stock and knowing what is left.

The project also gave the developer a chance to apply the subjects studied during the course to one realistic problem. Database design, normalization, transactions, input validation and graphical user interface programming all appear in a medical store system, and each has a visible effect on the result. If the stock update and the bill were not written together, the shelf count would drift from the books. If input were not validated, a mistyped price or date would reach the reports. The problem is small enough to finish and detailed enough to test what was learned.

## b) Scope and Limitation of the Project

The system is intended for one medical store working on one computer. Within that setting it covers the following areas.

- Master records for medicines (with batch number, expiry date, price and reorder level), suppliers, customers and user accounts.
- Purchase entry, in which an invoice from a supplier is recorded and the received quantities are added to stock.
- Sales billing, in which a counter bill is prepared from medicines that are in stock, a discount is applied, the sold quantities are deducted from stock and a receipt is shown for printing.
- A dashboard, a stock report, an expiry report, a sales report with day-wise totals and best sellers, and a bill history with item detail.
- Login with three roles: Administrator, Pharmacist and Counter Staff.
- Printing of reports and receipts through the standard print dialog, and export of reports to CSV files.

The limitations below were checked against the program and are stated so that the reader does not expect more than the prototype provides.

- **Single computer.** The database is one SQLite file used by one copy of the application. There is no server, no network operation and no synchronization between several counters.
- **No online features.** The system does not place orders with suppliers, accept online orders or send messages to customers.
- **No barcode input.** Medicines are chosen from a drop-down list; a barcode or QR scanner is not supported.
- **No tax computation.** A supplier's GST number can be stored as text, but the software does not calculate taxes, print tax lines or prepare any return.
- **One row per batch.** Batch number and expiry date are columns of the medicine record. A purchase adds units to the selected row; a new batch with a different expiry date is entered as a separate medicine record in Medicine Master. Batches are not allocated automatically in first-expiry-first-out order.
- **Saved documents are final.** A saved purchase invoice or bill cannot be edited or cancelled, and there is no screen for sales returns. There is also no screen that lists past purchase invoices.
- **Role control is limited to the menu.** The menu shows only the screens that the role of the signed-in user may open, but individual actions inside a screen, such as deleting a record, are not restricted further and no audit log of changes is kept.
- **Basic password protection.** Passwords are stored as SHA-256 hashes. No salt, password policy or lock-out after repeated failures is applied.
- **Expiry is judged per medicine record.** The billing screen hides a batch once its expiry date has passed, and the program refuses to bill it. Because each batch is one medicine record, a medicine held in two batches with different dates has to be entered as two records, and the software does not choose the earlier batch automatically.
- **Fixed receipt heading.** The shop name and address printed on the receipt are fixed text in the program and are not yet a setting.
- **No built-in backup.** The data can be backed up by copying the database file, but the software has no backup or restore command.

## c) Problem Statement

A small medical store must record every purchase and every sale, keep the stock balance of each medicine correct, watch the expiry dates and know at the end of the day how much was sold. When these tasks are done in paper registers, the same fact has to be written more than once, the balance of a medicine can be found only by searching, and no register can answer questions such as which medicines are below their reorder level or which batches expire in the next three months.

The problem addressed by this project is to design and build a computerized system that keeps the medicine, supplier and customer records in one database, updates the stock automatically whenever stock is received or sold, prevents a sale that the stock cannot support, and produces the stock, expiry and sales information that the owner needs, without requiring a server, an internet connection or technical skill from the user.

## d) Need of the Project

**Accuracy of stock.** In a manual system the stock balance is kept correct only if every sale is also entered in the stock register, and this is often postponed until the shop is quiet. A computerized system removes the second entry: the bill itself reduces the stock, inside the same database transaction, so the record of what was sold and the count of what remains cannot disagree.

**Safety and loss from expiry.** An expired medicine that is left on the shelf is either a loss or a risk to the customer. Checking every shelf by hand is slow, and a batch that is missed is discovered only when a customer or an inspector notices it. A system that stores the expiry date of each batch can list, at any time, the batches that have expired or will expire within 30, 90 or 180 days, together with the stock value tied up in them.

**Speed at the counter.** Writing a bill by hand, multiplying quantities by prices and subtracting a discount takes time and produces arithmetic mistakes. When the line amount, total and net payable are calculated by the program, the counter staff only choose the medicine and the quantity, and the customer receives a printed receipt.

**Information for decisions.** A store owner has to decide what to reorder and which items sell well. The reorder list, the day-wise sales report and the best-selling list give this information directly from the recorded data instead of from the owner's memory.

**Low cost and easy use.** A desktop program that needs no server and no installation, and that keeps all its data in one file, can be used by a store that cannot afford or maintain a larger system.

## e) Project Purpose and Objectives

The purpose of the project is to provide a small pharmacy with a reliable, easy to operate desktop application that replaces its purchase, stock and sales registers, and to demonstrate, through a complete working system, the application of database design and software engineering methods learned during the course.

The objectives of the project are the following.

1. To store medicine, supplier, customer and user records in a normalized relational database with primary keys, foreign keys and constraints.
2. To provide data entry screens for the four master records, with searching, updating and deletion that is refused when other records still depend on the entry.
3. To record purchase invoices and increase the stock of each medicine received, in the same database transaction as the invoice.
4. To prepare sales bills from in-stock medicines only, to calculate totals and discounts automatically, to reduce stock in the same transaction and to refuse any bill that would sell more than the stock on hand.
5. To warn about low stock and about batches that have expired or will expire soon.
6. To produce stock, expiry and sales reports and a bill history, with printing and CSV export.
7. To restrict access to the software through a login screen and to store passwords only as hashes.
8. To validate every input, to report all mistakes on a form in a single message, and to keep the screens consistent so that a new user can learn them quickly.
9. To package the application as a portable folder that runs on an ordinary Windows computer without installation.


# 6. Proposed System

## a) Existing System

In the existing system, a medical store is run with paper records. The details differ from store to store, but the same set of documents appears in almost all of them.

- A **purchase register**, or a file of the suppliers' invoices, in which each delivery is noted with its date, supplier, medicines, quantities and rates.
- A **stock register** or ledger, with a page or a line for each medicine, in which the quantity received is added and the quantity sold is subtracted.
- A **bill book**, in which the counter staff write each bill by hand, usually with a carbon copy for the customer, and calculate the amounts and the discount on paper or on a calculator.
- A **supplier diary** with names, telephone numbers and addresses, and sometimes a notebook of regular customers.

The daily flow of work is as follows. When a delivery arrives, it is checked against the supplier's invoice, written in the purchase register and then added to the stock register. When a medicine is sold, a bill is written and the customer takes the copy. The stock register is updated from the bill book, either immediately or at a quieter time of the day. At closing time the bills of the day are added up to find the sales. Expiry dates are checked by looking at the packs on the shelves, and reorder decisions are made from the owner's memory and from a look at the shelf.

This method works, and it needs no electricity or training, but it has several weaknesses.

- The same sale is written twice, once on the bill and once in the stock register, so the two can disagree, and the stock balance of a medicine is correct only as long as nobody forgets an entry.
- Finding the balance of one medicine means searching through pages, and no register can list all the medicines that are below their reorder level.
- Expiry is discovered by inspection. A batch pushed to the back of a shelf can pass its expiry date without anyone noticing.
- Hand calculation of line amounts, totals and discounts produces errors, and an error in a bill is difficult to find later.
- Daily, weekly and monthly sales figures need manual addition, and the best-selling medicines can be known only approximately.
- Registers can be lost, damaged or become illegible, and there is no second copy.
- Customer and supplier details are scattered over diaries and notebooks and are hard to search.

## b) User Requirement

This section records what the users of the existing manual system need from their work, regardless of how a computer might supply it. Three kinds of users were identified: the owner or pharmacist who runs the store, the counter staff who serve customers, and the person who places orders with suppliers, who in a small store is usually the owner.

The owner or pharmacist needs to:

- know, at any moment, how many units of a medicine are on hand and what the whole stock is worth at cost;
- be warned about medicines that are running low, so that they can be reordered before the shelf is empty;
- be warned about medicines that have expired or are about to expire, so that they can be returned to the supplier or removed;
- see how much was sold on a day, in a week or in a month, and which medicines sell the most;
- keep the details of suppliers and regular customers where they can be found quickly;
- control who may use the records.

The counter staff need to:

- prepare a bill quickly, without doing arithmetic by hand;
- see only the medicines that can actually be sold, together with the number of units left;
- give a discount when the owner allows it and hand the customer a clear receipt;
- look up an earlier bill when a customer asks about it.

The person placing orders needs to:

- record what was received from a supplier and have the stock corrected without a second entry;
- see which supplier normally provides which medicine.

## c) Scope and Limitation of the Existing System

**Scope.** The manual system covers every function of the store in a basic way. It records purchases and sales, keeps a stock count for each medicine, produces a written bill for each customer and keeps the addresses of suppliers. It needs no equipment other than books and a calculator, it works during a power cut, and anyone in the store can learn it in a few days.

**Limitations.** The system is limited in the following ways.

- It depends completely on the care of the person who writes the entries. There is no check that a quantity, a price or a date is sensible, and there is no rule that stops a bill for more units than are in stock.
- It cannot answer questions across records. A list of low-stock medicines, a list of expiring batches or a total of one month's sales must be prepared by going through the books.
- The stock register and the bill book are updated separately, so they can drift apart, and the difference is found only during a physical stock count.
- It keeps one copy of each record on paper. A lost or damaged book means a lost record.
- It does not identify who made an entry, so that mistakes cannot be traced to a shift or a person.
- It is slow at busy times, because every bill has to be written and calculated by hand.
- As the number of medicines and bills grows, the registers become larger and searching them becomes slower.

## d) Project Features

The proposed system, MediStore, is organized into the modules below. Each module corresponds to an entry in the menu at the left of the main window.

- **Login and accounts.** The application opens on a login window and cannot be used without a valid user name and password. Three roles are available: Administrator, Pharmacist and Counter Staff. Passwords are stored as SHA-256 hashes. The signed-in user's name and role are shown at the foot of the menu, and the same user is recorded on every bill that is raised. The sidebar shows only the entries that the role of the user may open.
- **Dashboard.** The opening screen shows eight figures: the number of medicines and how many are out of stock, low-stock items, batches expiring within 90 days, today's sales, stock value at cost, sales for the current month, the number of customers and the number of bills raised. Two lists sit below the figures: Low Stock Alerts and Top Selling Medicines.
- **Medicine Master.** Add, update, delete and search medicines by name, company or batch number. Each record holds the company, category, batch number, expiry date, quantity, purchase price, MRP, reorder level and regular supplier. A medicine that appears on a bill or a purchase invoice cannot be deleted.
- **Supplier Master.** Maintain the distributors from whom stock is bought, with contact person, phone, email, address and GST number. A supplier who has medicines or purchase invoices linked cannot be deleted.
- **Customer Master.** Maintain regular customers with phone, email and address. The registration date is recorded automatically. A customer who has bills cannot be deleted. Bills can also be raised for a walk-in customer without any customer record.
- **User Accounts.** Create accounts, change roles and reset passwords. Passwords are never displayed, a password left blank while editing keeps the old one, and the account in use cannot be deleted.
- **Purchase Entry.** Enter a supplier's invoice with one line for each medicine received. The invoice number is generated by the program, the total is calculated, and saving the invoice adds the received quantities to the stock.
- **Sales Billing.** Prepare a bill from medicines that have units in stock. The screen shows the batch and the units left for each medicine, refuses quantities beyond the stock, applies a discount, shows the net payable amount and, when the bill is saved, deducts the stock and displays a printable receipt. A batch that has passed its expiry date is not offered, and the program refuses to bill it even if it were selected.
- **Stock Report.** All medicine batches with quantity, reorder level, prices, stock value and a status of OK, Low Stock or Out of Stock, filtered by a search box and a status drop-down.
- **Expiry Report.** Batches that are already expired or will expire within 30, 90 or 180 days, with days left, stock value and a colored status.
- **Sales Report.** Day-wise bills, gross amount, discount and net amount for a chosen period, with quick buttons for Today, This Week and This Month, and a list of best-selling medicines.
- **Bill History.** Every bill in a chosen period, searchable by bill number or customer, with the items of the selected bill and a View Receipt button that shows the receipt again.
- **Common features.** Every data-entry screen validates its input and lists all the mistakes in a single message. Every report can be printed and exported to CSV. The application runs from a portable folder and keeps its data in a single database file.

## e) User Requirement (Proposed System)

This section states, in detail, what the proposed system must do. The functional requirements describe the behavior of the software; the non-functional requirements describe the qualities it must have.

### Functional Requirements

| ID | Requirement |
|---|---|
| FR1 | The system shall show a login window before any other screen and shall open the main window only after a valid user name and password are entered. |
| FR2 | The system shall reject a login with an empty field or a wrong password and shall show a message without closing the login window. |
| FR3 | The system shall store each password only as a SHA-256 hash and shall never display it. |
| FR4 | The system shall let a user add, update, delete and search medicine records by name, company or batch number. |
| FR5 | The system shall require a medicine name, a valid expiry date, a non-negative quantity, non-negative prices, a non-negative reorder level and a supplier, and shall refuse an MRP that is lower than the purchase price. |
| FR6 | The system shall let a user add, update, delete and search supplier records, and shall require a name and a ten-digit phone number. |
| FR7 | The system shall let a user add, update, delete and search customer records, and shall require a name and a ten-digit phone number. |
| FR8 | The system shall let a user create accounts, change the name, user name and role of an account, and reset a password, and shall refuse a user name that already exists. |
| FR9 | The system shall refuse to delete a supplier that has medicines or purchase invoices linked to it, a customer that has bills, and a medicine that appears on any bill or purchase invoice. |
| FR10 | The system shall refuse to delete the account that is currently signed in. |
| FR11 | The system shall generate the purchase invoice number and the bill number and shall not let the user type them. |
| FR12 | The system shall let a user build a purchase invoice from one or more lines, each with a medicine, a quantity of at least one and a rate, and shall calculate the line amounts and the invoice total. |
| FR13 | The system shall, when a purchase invoice is saved, add the quantity of every line to the stock of its medicine in the same database transaction that stores the invoice. |
| FR14 | The system shall offer only medicines with units in stock on the billing screen and shall show the batch and the units available. |
| FR15 | The system shall refuse a bill line whose quantity, together with the same medicine already on the bill, exceeds the stock on hand, and shall check the stock again when the bill is saved. |
| FR16 | The system shall calculate the bill total and the net payable amount, shall accept a discount of zero or more and shall refuse a discount greater than the bill total. |
| FR17 | The system shall allow a bill to be raised for a walk-in customer or for a registered customer, and shall record the signed-in user on the bill. |
| FR18 | The system shall, when a bill is saved, deduct the quantity of every line from stock in the same database transaction that stores the bill, and shall save nothing if any step fails. |
| FR19 | The system shall show a printable receipt after a bill is saved and shall let the user print it. |
| FR20 | The dashboard shall show the medicine count, low-stock count, the count of batches expiring within 90 days, today's sales, stock value at cost, this month's sales, the customer count and the bill count. |
| FR21 | The dashboard shall list the medicines at or below their reorder level and the best-selling medicines. |
| FR22 | The Stock Report shall list every medicine batch with its stock value and status and shall filter by search text and by stock status. |
| FR23 | The Expiry Report shall list batches that have expired or will expire within a chosen window of 30, 90 or 180 days, soonest first, and shall show the days left and the value at risk. |
| FR24 | The Sales Report shall show, for a chosen date range, the bills, gross amount, discount and net amount of each day, and shall list the best-selling medicines. |
| FR25 | Bill History shall list the bills in a date range, shall filter them by bill number or customer name, and shall show the items of a selected bill and its receipt. |
| FR26 | Each report shall be printable through the standard print dialog and exportable to a CSV file chosen by the user. |
| FR27 | Every data-entry screen shall check all its fields together and shall list every mistake in one message. |
| FR28 | The system shall not offer a batch that has passed its expiry date for sale, and shall refuse to bill it. |
| FR29 | The system shall show each user only the menu entries that the role of the user may open. |

### Non-Functional Requirements

| Category | Requirement |
|---|---|
| Usability | All screens shall follow one layout: a title, an entry card with a label above every field, and a table with search. A user who has seen one master screen shall be able to use the others without instruction. |
| Data integrity | Stock changes shall be stored together with the invoice or bill that caused them. Foreign key enforcement shall be switched on in the database so that a record cannot point to a missing parent. |
| Reliability | If a database operation fails, the screen shall show a message and the stored data shall remain unchanged. |
| Security | Access shall require a login, and passwords shall be stored only as hashes. |
| Performance | Screens shall respond without noticeable delay for the data volume of one small store, and search boxes shall filter as the user types. |
| Portability | The application shall run on Windows 10 and 11 and shall also run on macOS and Linux, from a folder, without an installer. |
| Maintainability | The code shall be divided into database, model, data-access, interface and utility packages; database access shall be kept out of the screens; colors and fonts shall be defined in one place. |
| Language and format | All screen text shall be in English. Dates shall be entered in yyyy-MM-dd form, and amounts shall be shown in rupees with two decimals. |
| Independence | The application shall not need a server, an internet connection or a separately installed database. |

## f) Fact Finding Techniques

Before the requirements were written, the developer collected information about how a medical store works. Four standard fact-finding techniques were used.

**Observation.** The developer observed the working of a neighborhood medical store: how a delivery is received and checked, how a customer's request is turned into a bill, how the shelves are arranged and how the staff decide that a medicine is running low. Observation showed the sequence of tasks as they really happen, including small steps such as looking at the expiry date printed on a pack before handing it over, which are easily left out of a spoken description of the job.

**Interview.** The pharmacist or owner was interviewed to understand what the store needs from a record-keeping system and what causes trouble in the present method. The interview followed a short list of open questions: what is recorded at purchase and at sale, how expiry and low stock are noticed, what the owner wants to know at the end of the day, and which reports are used for decisions. The answers helped to decide which screens were essential and which could be left out of the first version.

**Examination of records.** The paper registers and bills used by the store were examined: the purchase register, the stock register, the bill book and the supplier and customer notes. This showed which items each document contains, in what order and with what totals. The fields of the medicine, supplier, customer, purchase and sales tables correspond to the entries found in these documents, and the receipt follows the general form of a counter bill.

**Study of existing software.** Existing pharmacy and shop management software was studied to see which functions are common to all of them and which appear only in large systems. Common functions, such as masters, purchase and sales entry, stock and expiry alerts and sales reports, were included. Functions that need a network, a server or statutory filing were left out, as explained in the scope of the project.

The information from these four sources was compared, the tasks were grouped into the modules listed under Project Features, and the requirements in the previous section were written from them.

# 7. System Analysis and Design

The analysis of the system followed the structured approach. The flow of data through the store was first drawn as data flow diagrams, then the data that has to be remembered was described in a data dictionary and a relational data model, and finally the relationships between the things the store deals with were drawn as an entity relationship diagram. The diagrams in this chapter were drawn with the same table and column names that the program uses.

## a) Data Flow Diagram

A data flow diagram shows how data enters the system, how it is processed, where it is stored and what leaves the system. It does not show the order of screens or the program logic.

### Context Level (Level 0)

At the context level the whole software is one process, numbered 0. Four external parties exchange data with it.

![Context level data flow diagram (Level 0)](docs/diagrams/dfd-level-0.png)

The **Pharmacist or Counter Staff** member signs in and enters data, and receives screens and alerts in return. The **Supplier** provides the details of the stock that is delivered, and receives the list of medicines that must be reordered. The **Customer** asks for medicines and receives a printed bill. The **Store Manager** asks for reports and receives the stock and sales reports. The supplier and the customer do not operate the software themselves; they are shown as external entities because they are the source and the destination of the documents that the staff enter into the system and print from it.

### Level 1

At level 1 the single process is divided into five processes. They read and write five data stores.

![Level 1 data flow diagram](docs/diagrams/dfd-level-1.png)

The data stores are listed below.

| Store | Database tables | Contents |
|---|---|---|
| D1 | users | Login accounts, password hashes and roles |
| D2 | suppliers, customers | Supplier and customer details |
| D3 | medicines | Medicine details, batch, expiry, prices and stock on hand |
| D4 | purchases, purchase_items | Purchase invoices and their lines |
| D5 | sales, sale_items | Customer bills and their lines |

**Process 1.0, User Login.** The staff member enters a user name and a password. The process looks up the account in D1, hashes the typed password with SHA-256 and compares the result with the stored hash. If both match, the account is kept in memory as the signed-in user, so that the menu can show its name and role and each bill can record who raised it; otherwise an error message is returned and no other screen can be reached.

**Process 2.0, Master Management.** The staff enter and change master data: medicines, suppliers, customers and user accounts. The process validates each entry, saves it to D2 or D3, and, before a delete, checks whether other records still refer to the entry. User accounts are held in D1; that flow is not drawn separately, to keep the diagram readable.

**Process 3.0, Purchase Entry.** The details of a supplier's invoice are entered, using the supplier list from D2 and the medicine list from D3; these reads are not drawn separately. The process calculates the line amounts and the total, writes the invoice and its lines to D4, and increases the stock of each medicine in D3. Both writes are made in one transaction, so either both happen or neither does.

**Process 4.0, Sales Billing.** The customer's request is turned into a bill. The process offers only the medicines in D3 that have units on hand, allows a registered customer from D2 (a read that is not drawn separately) or a walk-in customer, calculates the total, discount and net payable amount, checks the stock again, writes the bill and its lines to D5 and reduces the stock in D3, again in one transaction. The bill is returned to the customer as a receipt.

**Process 5.0, Report Generation.** On the request of the store manager, the process reads D3 to produce the stock and expiry reports and the low-stock list, and reads D5 to produce the sales report, the best sellers and the bill history. The results are shown on the screen and can be printed or exported. This process only reads data and never changes it.

## b) Data Dictionary

The data dictionary describes every table of the database, column by column. The database is SQLite, whose column types are INTEGER (whole numbers), TEXT (character strings) and REAL (numbers with a fractional part). SQLite has no separate date type, so every date is kept as text in the form yyyy-MM-dd, for example 2027-03-31. In this form the dates sort in calendar order as plain text, which makes range searches such as "between two dates" simple and reliable. The tables were created from the statements in the program's schema, and the entries below repeat them without change.

### Table: users

Login accounts of the software.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| user_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the account, generated by the database. |
| username | TEXT | NOT NULL UNIQUE | Name typed at login; no two accounts can have the same user name. |
| password | TEXT | NOT NULL | SHA-256 hash of the password as 64 hexadecimal characters; the readable password is never stored. |
| full_name | TEXT | NOT NULL | Full name of the person, shown in the menu and on the receipt. |
| role | TEXT | NOT NULL DEFAULT 'Pharmacist' | Administrator, Pharmacist or Counter Staff. |
| created_on | TEXT | NOT NULL | Date on which the account was created, stored as yyyy-MM-dd. |

### Table: suppliers

Distributors from whom the store buys its stock.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| supplier_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the supplier. |
| supplier_name | TEXT | NOT NULL | Name of the distributor or agency. |
| contact_person | TEXT | None (optional) | Name of the person to contact at the supplier. |
| phone | TEXT | None (optional) | Ten-digit telephone number. |
| email | TEXT | None (optional) | Email address. |
| address | TEXT | None (optional) | Postal address. |
| gst_number | TEXT | None (optional) | GST registration number of the supplier, kept for reference. |

### Table: customers

Regular buyers at the counter.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| customer_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the customer. |
| customer_name | TEXT | NOT NULL | Name of the customer. |
| phone | TEXT | None (optional) | Ten-digit telephone number. |
| email | TEXT | None (optional) | Email address. |
| address | TEXT | None (optional) | Postal address. |
| registered_on | TEXT | None (optional) | Date of registration as yyyy-MM-dd, filled in automatically when the customer is added. |

### Table: medicines

One row for each medicine batch held in stock.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| medicine_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the medicine batch. |
| medicine_name | TEXT | NOT NULL | Name of the medicine. |
| company | TEXT | None (optional) | Manufacturer. |
| category | TEXT | None (optional) | Type of medicine: Tablet, Syrup, Injection, Ointment, Capsule, Drops or Other. |
| batch_no | TEXT | None (optional) | Batch number printed on the pack. |
| expiry_date | TEXT | None (optional) | Expiry date of the batch as yyyy-MM-dd. |
| quantity | INTEGER | NOT NULL DEFAULT 0 | Units on hand. Raised by purchases and lowered by sales. |
| purchase_price | REAL | NOT NULL DEFAULT 0 | Cost of one unit to the store. |
| mrp | REAL | NOT NULL DEFAULT 0 | Maximum retail price of one unit; the default selling rate. |
| reorder_level | INTEGER | NOT NULL DEFAULT 10 | Quantity at or below which the medicine is reported as low stock. |
| supplier_id | INTEGER | REFERENCES suppliers(supplier_id) | Regular supplier of the medicine. |

### Table: purchases

Header of a stock-in invoice.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| purchase_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the purchase. |
| invoice_no | TEXT | NOT NULL | Invoice number generated by the program, for example PUR-0007. |
| supplier_id | INTEGER | NOT NULL REFERENCES suppliers(supplier_id) | Supplier who delivered the stock. |
| purchase_date | TEXT | NOT NULL | Date of the invoice as yyyy-MM-dd. |
| total_amount | REAL | NOT NULL DEFAULT 0 | Sum of the line amounts, calculated by the program. |

### Table: purchase_items

One line for each medicine on a purchase invoice.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| purchase_item_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the line. |
| purchase_id | INTEGER | NOT NULL REFERENCES purchases(purchase_id) | Invoice to which the line belongs. |
| medicine_id | INTEGER | NOT NULL REFERENCES medicines(medicine_id) | Medicine received. |
| quantity | INTEGER | NOT NULL | Units received. |
| rate | REAL | NOT NULL | Rate paid for one unit. |
| amount | REAL | NOT NULL | Quantity multiplied by rate. |

### Table: sales

Header of a customer bill.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| sale_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the bill. |
| bill_no | TEXT | NOT NULL UNIQUE | Bill number generated by the program, for example BILL-0023. |
| customer_id | INTEGER | REFERENCES customers(customer_id) | Customer billed; empty for a walk-in customer. |
| sale_date | TEXT | NOT NULL | Date of the bill as yyyy-MM-dd. |
| total_amount | REAL | NOT NULL DEFAULT 0 | Sum of the line amounts before discount. |
| discount | REAL | NOT NULL DEFAULT 0 | Discount in rupees, never more than the total. |
| net_amount | REAL | NOT NULL DEFAULT 0 | Amount payable: total_amount minus discount. |
| user_id | INTEGER | REFERENCES users(user_id) | User who raised the bill. |

### Table: sale_items

One line for each medicine on a bill.

| Field | Data Type | Constraint | Description |
|---|---|---|---|
| sale_item_id | INTEGER | PRIMARY KEY AUTOINCREMENT | Number that identifies the line. |
| sale_id | INTEGER | NOT NULL REFERENCES sales(sale_id) | Bill to which the line belongs. |
| medicine_id | INTEGER | NOT NULL REFERENCES medicines(medicine_id) | Medicine sold. |
| quantity | INTEGER | NOT NULL | Units sold. |
| rate | REAL | NOT NULL | Rate charged for one unit. |
| amount | REAL | NOT NULL | Quantity multiplied by rate. |

Two points about the dictionary should be noted. First, the purchase invoice number is generated by the program from the next purchase number, and the database does not require it to be unique; the bill number, by contrast, carries a UNIQUE constraint. Second, money is stored as REAL. For the size of a single store this is adequate and all amounts are shown rounded to two decimals, but a commercial system would store amounts as whole paise to avoid floating-point rounding.

## c) Data Model

The data model is relational. Each table has a primary key, and relationships between tables are expressed by foreign keys. The figure shows the eight tables with their columns; PK marks a primary key and FK marks a foreign key.

![Relational data model with eight tables](docs/diagrams/data-model.png)

There are four master tables (users, suppliers, customers and medicines) and two business transactions, purchase and sale. Each transaction is split into a header table (purchases, sales) that holds what is common to the whole document, and an item table (purchase_items, sale_items) that holds one row for each medicine on it. This is the standard way to store a document with a variable number of lines without repeating columns.

The eight foreign keys are the following.

| Table | Foreign key column | References |
|---|---|---|
| medicines | supplier_id | suppliers(supplier_id) |
| purchases | supplier_id | suppliers(supplier_id) |
| purchase_items | purchase_id | purchases(purchase_id) |
| purchase_items | medicine_id | medicines(medicine_id) |
| sales | customer_id | customers(customer_id) |
| sales | user_id | users(user_id) |
| sale_items | sale_id | sales(sale_id) |
| sale_items | medicine_id | medicines(medicine_id) |

The following design decisions shaped the model.

- **Normalization.** Every table describes one kind of thing, has no repeating groups, and every non-key column depends on the key of its own table. Supplier details are stored once in suppliers and referred to by number, not copied into each medicine or invoice. The design therefore satisfies the third normal form, with the one deliberate exception described next.
- **Stored totals.** The columns amount, total_amount and net_amount can be derived from other columns. They are stored so that reports and the bill history read them directly and so that a printed document always shows the figures it was created with. They are always calculated by the program and are never typed by the user.
- **Price at the time of the transaction.** The rate is copied into each purchase and sale line. A later change of the MRP in the medicine record therefore does not alter any bill that was already issued.
- **Optional relationships.** The columns sales.customer_id and sales.user_id may be empty. An empty customer_id represents a walk-in customer who has no customer record.
- **Enforcement.** SQLite ignores foreign keys unless they are switched on for the connection, so the program switches them on every time it opens the database. A row that refers to a missing parent is then refused by the database itself, in addition to the checks made by the screens.

## d) Entity Relationship Diagram

The entity relationship diagram uses Chen notation: rectangles for entities, diamonds for relationships, ovals for attributes with the primary key underlined, and 1, M and N for the cardinality at each end. Foreign keys are not drawn as attributes, because in an entity relationship diagram they are represented by the relationships themselves. The two many-to-many relationships carry attributes of their own (quantity, rate and amount), which is why each of them becomes a separate table in the data model.

![Entity relationship diagram](docs/diagrams/erd.png)

The six entities are SUPPLIER, MEDICINE, PURCHASE, SALE, CUSTOMER and USER. The two item tables of the data model do not appear as entities; they are the way in which the two many-to-many relationships are stored. The relationships are summarized below.

| Relationship | Entities | Cardinality | Stored in |
|---|---|---|---|
| supplies | SUPPLIER and MEDICINE | One to many | medicines.supplier_id |
| issues | SUPPLIER and PURCHASE | One to many | purchases.supplier_id |
| purchased in | PURCHASE and MEDICINE | Many to many | purchase_items |
| billed to | CUSTOMER and SALE | One to many | sales.customer_id |
| sold in | MEDICINE and SALE | Many to many | sale_items |
| raises | USER and SALE | One to many | sales.user_id |

**supplies (1:M).** One supplier is the regular source of many medicines, while a medicine record names one regular supplier. **issues (1:M).** One supplier issues many purchase invoices over time, and each invoice comes from exactly one supplier.

**purchased in (M:N).** One purchase invoice contains many medicines, and one medicine appears on many purchase invoices over time. A many-to-many relationship cannot be stored directly in a relational database, so it is resolved by the table purchase_items, which holds one row for each pair of an invoice and a medicine, together with the quantity and the rate.

**billed to (1:M).** One customer can have many bills, and each bill belongs to at most one customer; a bill for a walk-in customer belongs to none. **sold in (M:N).** One bill contains many medicines, and one medicine is sold on many bills; the table sale_items resolves the relationship in the same way as purchase_items.

**raises (1:M).** One user raises many bills, and each bill records the user who raised it. This relationship makes it possible to see which account made a sale.

# 8. Implementation Details

## a) Software and Hardware Specification

### Software Specification

The application was developed on a macOS computer with a text editor and the plain Java compiler, and it is intended to run on an ordinary Windows computer. The software used is listed below.

| Item | Specification |
|---|---|
| Operating system (run) | Windows 10 or Windows 11; the application also runs on macOS and Linux |
| Operating system (development) | macOS |
| Programming language | Java 21 (JDK 21 LTS) |
| Graphical interface | Java Swing, part of the standard Java platform |
| Look-and-feel | FlatLaf 3.5 |
| Database | SQLite, accessed through the sqlite-jdbc driver 3.46 |
| Logging | SLF4J API with a no-operation binding, which the SQLite driver needs and which keeps the console quiet |
| Build | Plain javac driven by a shell script; no Maven or Gradle |
| Editor | A text editor or Java IDE |

### Hardware Specification

The application is light. It holds the data of one store in a single file and draws a desktop window, so an ordinary office computer is sufficient.

| Component | Minimum | Recommended |
|---|---|---|
| Processor | 1.6 GHz dual core | 2.0 GHz or faster, four cores |
| Memory | 4 GB RAM | 8 GB RAM |
| Free disk space | 500 MB, including the bundled Java runtime and the database | 1 GB or more |
| Display | 1366 x 768 pixels (the main window has a minimum size of 1180 x 700 pixels) | 1920 x 1080 pixels |
| Input devices | Keyboard and mouse | Keyboard and mouse |
| Printer | Optional; needed only for printed bills and reports | Any printer that is installed on the computer |

### Structure of the Application

The source code is arranged in packages, each with one responsibility.

- **db.** The class Database opens the SQLite file, switches foreign keys on and creates the tables on first use from the statements held in the class Schema. There is one shared connection.
- **model.** Plain classes for Medicine, Supplier, Customer, User, Purchase, PurchaseItem, Sale and SaleItem. The calculated values, such as a line amount, a bill total, the stock value or the days left before expiry, are methods of these classes, so that they are worked out in one place only.
- **dao.** Data-access classes, one for each kind of record, plus a class for the report queries. All SQL is kept here. The screens never build SQL themselves, and every database error is wrapped in a single exception type, so that a screen can show a readable message.
- **ui.** The main window with its menu, the login window, the dashboard and a base class that gives every screen the same heading and spacing. The sub-packages master, transaction and report hold the four master screens, the two transaction screens and the four report screens.
- **util.** Shared helpers: one class that defines every color, font and widget style, a builder for the label-above-field layout, the Validator, the password hashing class and a holder for the signed-in user.
- **tools.** Programs that are used during development and are not part of daily operation: the sample-data generator, the diagram generator and the test program for the test cases.

The application starts in the class Main, which sets the look-and-feel, opens the database, shows the login window and creates the main window only after a login succeeds.

### Implementation of the Main Rules

**Stock movement in one transaction.** When a purchase invoice is saved, the data-access class turns off automatic commit, inserts the invoice header, inserts every line, adds each line's quantity to the medicine's stock and then commits. When a bill is saved, the same is done with the quantities subtracted. If any step fails, the transaction is rolled back, so the database never holds an invoice without its stock change or a stock change without its invoice.

**Protection against overselling.** The billing screen offers only medicines with stock and refuses a line that would exceed the units on hand, counting the units of the same medicine already on the bill. The stock is read again from the database inside the saving transaction, so a bill cannot be saved with an out-of-date figure. The discount is checked in the same way.

**Protected deletion.** Before deleting a supplier, a customer or a medicine, the data-access class counts the records that refer to it. If there are any, it refuses and returns an explanatory message. The database's foreign keys are a second line of defense.

**Validation.** The Validator class collects the problems found on a form in a list and shows them together in one dialog. Each screen builds a Validator, adds the checks that apply to it and saves only if the list is empty.

**Passwords.** The password typed by the user is converted to a SHA-256 hash, and only the hash is stored. At login the typed password is hashed again and compared with the stored value. When an account is edited, a blank password box means that the existing hash is kept.

**Generated numbers.** The next invoice or bill number is formed from one more than the highest purchase or sale record number in the database, with a prefix and four digits, for example PUR-0007 and BILL-0023.

### Packaging and Deployment

The application is delivered as a portable folder that can be copied to any Windows computer. The folder contains the compiled program, the libraries (the SQLite driver, FlatLaf and SLF4J), its own Java runtime and a launcher file. Because the runtime is inside the folder, Java need not be installed on the computer, and starting the application is a double-click on the launcher. The database is a single file, data/medistore.db, stored in the data folder next to the program. Copying the folder therefore copies the data with it, and the database can be backed up by copying that one file. The delivered folder contains a prepared database that already holds the default administrator account, and further accounts are created from the User Accounts screen. The folder should be placed in a path that contains no spaces, because Java and Windows batch files handle such paths poorly. If the data folder or the database file is missing, the program creates a new empty database on its first start together with the default administrator account (admin), so that it can always be signed in to.

# 9. Outputs and Report Testing

This chapter shows the screens of the finished application, explains how the input screens are designed, describes the format of the reports and lists the test cases. The screenshots were taken from the running application filled with demonstration data prepared by the sample-data tool, so the names and figures in them are sample data and do not describe a real store.

## a) Menu Screens

The main window has a menu, called the sidebar, on the left and the selected screen on the right. The sidebar is divided into four groups. A screen is created the first time its menu entry is clicked and is then kept, so moving between screens is quick and a half-typed entry is still there when the user returns. At the foot of the sidebar are the name and role of the signed-in user and a sign-out icon, which asks for confirmation before it closes the main window and shows the login window again. The entries shown depend on the role of the signed-in user: an Administrator sees every entry, a Pharmacist sees every entry except User Accounts, and Counter Staff see only Dashboard, Sales Billing, Stock Report, Expiry Report and Bill History.

| Group | Screen | Purpose |
|---|---|---|
| Main | Dashboard | Figures and alerts that describe the store today |
| Master Entry | Medicine Master | Add, edit, search and delete medicine records |
| Master Entry | Supplier Master | Maintain the suppliers of the store |
| Master Entry | Customer Master | Maintain registered customers |
| Master Entry | User Accounts | Create accounts, set roles and reset passwords |
| Transactions | Purchase Entry | Record stock received from a supplier |
| Transactions | Sales Billing | Make a bill and take the sold units out of stock |
| Reports | Stock Report | Quantity and value of every medicine on the shelves |
| Reports | Expiry Report | Batches that have expired or will expire soon |
| Reports | Sales Report | Day-wise sales and best-selling medicines |
| Reports | Bill History | Every bill with its items and receipt |

### Login

The login window is the first screen and appears before the main window exists. A colored panel on the left carries the product name, and the form on the right has two labeled fields, USER NAME and PASSWORD, and a Sign In button. Pressing the Enter key has the same effect as clicking the button. If either field is empty the window says so, and if the pair is wrong it shows "That user name and password do not match.", clears the password box and stays open. Closing the window ends the program. For demonstration the window also prints the default account below the form; a real installation would remove that line.

![Login window](screenshots/01-login.png)

### Dashboard

The dashboard is the opening screen after a successful login. It greets the user by name and shows the date. Eight tiles give the current position of the store: total medicines with the number that are out of stock, low-stock items, batches expiring within 90 days (expired batches are included in the count), today's sales, stock value at cost, sales for the month, registered customers and bills raised. Below the tiles, the Low Stock Alerts list shows each medicine at or below its reorder level, and the Top Selling Medicines list ranks the best sellers by units sold and shows the revenue each has earned. The figures are recalculated each time the screen is opened.

![Dashboard](screenshots/02-dashboard.png)

### Medicine Master

Medicine Master is used to keep the list of medicines that the store stocks. The screen is divided into an entry card on the left, titled Medicine Details, and a searchable table on the right. The search box above the table filters as the user types, by medicine name, company or batch number. Clicking a row in the table copies it into the entry card and enables the Update and Delete buttons; Clear empties the card for a new entry. Delete asks for confirmation and is refused with an explanation if the medicine already appears on a bill or a purchase invoice. The list shows the name, company, batch, expiry date, quantity and MRP of each medicine; the category and the supplier are not shown in the list to keep the columns readable, but they are loaded into the entry card as soon as a row is clicked.

![Medicine Master with the list of medicines](screenshots/03-medicine-master.png)

The next figure shows the entry card filled in for a new medicine before Save is pressed. The table that follows lists each field and what is entered in it.

![Medicine entry form filled in before saving](screenshots/04-medicine-entry.png)

| Field | What is entered |
|---|---|
| Medicine Name | Name of the medicine, typed. Required. |
| Company | Manufacturer, typed. |
| Category | Chosen from a drop-down: Tablet, Syrup, Injection, Ointment, Capsule, Drops or Other. |
| Batch No | Batch number printed on the pack, typed. |
| Expiry Date | Expiry date typed as yyyy-MM-dd, for example 2027-03-31. Required and must be a real date. |
| Quantity | Units on hand, a whole number of 0 or more. The field starts at 0; stock is normally raised later through Purchase Entry. |
| Purchase Price | Cost of one unit to the store, a number of 0 or more. |
| MRP | Maximum retail price of one unit, a number that is not lower than the purchase price. It becomes the default rate on the billing screen. |
| Reorder Level | A whole number of 0 or more, starting at 10. At or below this quantity the medicine is reported as low stock. |
| Supplier | Chosen from a drop-down of the suppliers entered in Supplier Master. Required. |

### Supplier Master

Supplier Master keeps the distributors from whom stock is bought. It has the same layout as Medicine Master: an entry card, Save, Update, Delete and Clear buttons, and a table that can be searched by name, contact person or phone number. The list shows the name, contact person, phone and e-mail; the GST number is loaded into the entry card when a row is clicked. A supplier that still has medicines or purchase invoices linked to it cannot be deleted, and the screen explains why.

![Supplier Master](screenshots/05-supplier-master.png)

| Field | What is entered |
|---|---|
| Supplier Name | Name of the distributor or agency, typed. Required. |
| Contact Person | Name of the person to speak to, typed. |
| Phone | Ten-digit number without spaces or symbols. Required. |
| Email | Email address. Optional, but if it is entered it must look like an email address. |
| GST Number | GST registration number of the supplier, typed for reference. Optional. |
| Address | Postal address, typed in a multi-line box. |

### Customer Master

Customer Master keeps the regular customers whom the counter staff can pick on the billing screen. Its table is searched by name or phone number. The list shows the name, phone, address and registration date; the e-mail address is loaded into the entry card when a row is clicked. The date of registration is not typed: it is filled in with the current date when the customer is saved. A customer who already has bills cannot be deleted.

![Customer Master](screenshots/06-customer-master.png)

| Field | What is entered |
|---|---|
| Customer Name | Name of the customer, typed. Required. |
| Phone | Ten-digit number without spaces or symbols. Required. |
| Email | Email address. Optional, but if it is entered it must look like an email address. |
| Address | Postal address, typed in a multi-line box. |

### User Accounts

User Accounts lists the people who may sign in and is used to create an account, change a role or reset a password. The table shows the full name, user name, role and creation date; it never shows a password. The search box matches the name, the user name or the role. The password boxes show dots while typing. When an existing account is edited, leaving both password boxes empty keeps the current password. The account that is signed in cannot be deleted from this screen.

![User Accounts](screenshots/07-user-accounts.png)

| Field | What is entered |
|---|---|
| Full Name | Name of the person, typed. Required. |
| User Name | Name used to sign in. Required and must not already exist. |
| Password | Password for a new account, required. For an existing account it is left empty unless it is to be changed. |
| Confirm Password | The same password typed again; the two must match. |
| Role | Chosen from a drop-down: Administrator, Pharmacist or Counter Staff. New accounts start as Pharmacist. |

### Purchase Entry

Purchase Entry records a delivery from a supplier. The upper card, Invoice Details, holds the invoice header. Below it, the Items card has a row of fields for one medicine line, the buttons Add Item and Remove Selected, and a table of the lines entered so far, with each line's amount. The total is calculated at the bottom of the screen. Save Invoice stores the invoice and, in the same transaction, adds every quantity to the stock of its medicine, and then a message confirms how many lines were updated. Clear discards the lines after a confirmation. The screenshot shows an invoice with several lines added.

![Purchase Entry with an invoice and its lines](screenshots/08-purchase-entry.png)

| Field | What is entered |
|---|---|
| Invoice No | Shown by the program, for example PUR-0007. It is read-only. |
| Supplier | Chosen from a drop-down of suppliers. Required. |
| Purchase Date | Date of the invoice as yyyy-MM-dd. It starts as today's date. |
| Medicine | Chosen from a drop-down of all medicines, including those that are out of stock. |
| Quantity | Units received, a whole number of 1 or more. |
| Rate | Cost of one unit. It is filled in from the medicine's purchase price and can be changed. |
| Total Amount | Calculated as the sum of the line amounts. It cannot be typed. |

### Sales Billing

Sales Billing is the screen used at the counter. Its layout follows Purchase Entry: a Bill Details card, an Items card with the medicine, quantity and rate fields, and a summary at the bottom. The medicine drop-down lists only medicines that have units in stock and shows the batch and the units left beside each name. A line that would take more than the stock is refused with a message that states the units available. Below the item table, the total amount, the discount field and the net payable amount are shown, and the net payable amount changes as soon as a discount is typed. The button Save & Print Bill stores the bill, deducts the stock and opens the receipt. The screenshot shows a bill with several lines and a discount.

![Sales Billing with a bill, its lines and a discount](screenshots/09-billing.png)

| Field | What is entered |
|---|---|
| Bill No | Shown by the program, for example BILL-0023. It is read-only. |
| Customer | Chosen from a drop-down. The first choice, Walk-in Customer, is used when the buyer is not registered. |
| Bill Date | Date of the bill as yyyy-MM-dd. It starts as today's date. |
| Medicine | Chosen from a drop-down of the medicines that have stock. |
| Quantity | Units sold, a whole number of 1 or more and not more than the units in stock. |
| Rate | Selling price of one unit. It is filled in from the MRP and can be changed at the counter. |
| Discount (Rs.) | Discount in rupees, starting at 0. It cannot be negative or larger than the total amount. |
| Total Amount and Net Payable | Calculated by the program. They cannot be typed. |

## b) Design of Input

The input screens follow a small set of design principles, applied in the same way on every screen.

- **One label above each field.** Every field has a single caption above it, in capital letters, set in a grid that is the same on all forms. The user never has to guess which box a label belongs to.
- **Validation collected into one message.** When Save is pressed, all the fields of the form are checked and every mistake is listed in one dialog, each on its own line, so the user can correct the whole form in one pass. Nothing is saved until the list is empty.
- **Drop-downs instead of free text for fixed values.** Category, role, supplier, customer, medicine, stock status and expiry window are chosen from lists. This removes spelling variations and makes it impossible to enter a value that does not exist.
- **One date format.** Dates are typed as yyyy-MM-dd, and the message shows an example, 2027-03-31. The tables and reports display dates as dd-MM-yyyy, which is the form people read, and the database stores yyyy-MM-dd.
- **Computed values are never typed.** Line amounts, invoice totals, bill totals, net payable amounts, stock values and days left before expiry are calculated by the program.
- **Read-only generated numbers.** The invoice number and the bill number are shown in fields that cannot be edited.
- **Sensible defaults.** Dates start as today, the reorder level starts at 10, the quantity of a new medicine starts at 0, the discount starts at 0 and the rate of a line is filled in from the medicine record.
- **Safe editing.** Update and Delete are disabled until a record is selected, and deleting, clearing entered lines and signing out all ask for confirmation.
- **Fast keyboard entry.** In the item rows, pressing Enter in the quantity or rate box adds the line and returns the cursor to the quantity box. Pressing Enter on the login window signs in.
- **Passwords protected.** Password boxes show dots, and a saved password is never loaded back into a form.

The checks used by the Validator are summarized below.

| Check | Where it is used | Message shown |
|---|---|---|
| Required text | Names, phone, user name | "Medicine name is required." |
| Whole number with a minimum | Quantity, reorder level | "Quantity cannot be less than 0." and "Quantity must be a whole number." |
| Number with a minimum | Prices, rate, discount | "Purchase price cannot be less than 0.0." and "MRP must be a number." |
| Real date | Expiry date, purchase and bill dates, report ranges | "Expiry date must be a date in yyyy-MM-dd form, for example 2027-03-31." |
| Ten-digit phone | Supplier and customer phone | "Phone must be exactly 10 digits." |
| Email format | Supplier and customer email | "Email does not look like a valid email address." |
| Selection made | Supplier, customer, medicine | "Please select a supplier." |
| Rules of the form | MRP against purchase price, password confirmation, unique user name, discount against total | "MRP cannot be less than the purchase price.", "The two passwords do not match.", "That user name is already taken.", "The discount cannot be more than the bill total." |

The next figure shows the dialog that appears when a form is saved with several mistakes. Each problem is listed on its own line under the heading "Please correct the following:".

![Validation dialog listing every mistake on a form](screenshots/15-validation-error.png)

## c) Report Format

The four report screens share one layout. A card of filters is at the top, a row of summary tiles follows, and a table with the detail is below. The buttons Print and Export to CSV sit at the upper right of the screen. The tiles and the table read the database each time the screen is opened, so a report is always current.

Printing uses the standard print dialog of the operating system. The table is printed to fit the page width, with the report title at the top, followed by the date or the period covered, and the page number at the foot of every page. Export to CSV opens a file chooser, proposes a file name that contains the report name and the date, and writes the column headings followed by the rows that are on the screen. Values are written as they appear on the screen, and any value that contains a comma, such as an amount with a thousands separator, is placed in quotation marks, so that the file opens correctly in a spreadsheet.

### Bill Receipt

When a bill is saved, a receipt window opens. The receipt is laid out in a fixed-width font, like a counter slip. At the top are the shop name and address, which are fixed text in the program, followed by the bill number, date, customer and cashier. The item table lists the medicine, quantity, rate and amount of each line, with the batch number under it. The total amount, the discount and the net payable amount close the slip, followed by a thank-you line. The button Print sends the receipt to the printer through the standard print dialog, and Close returns to the billing screen, which is then ready for the next bill.

![Printable bill receipt](screenshots/10-bill-receipt.png)

### Stock Report

The Stock Report answers the question of what is on the shelves and what it is worth. The filter card has a search box for medicine, company or batch, and a Stock status drop-down with the choices All Items, In Stock, Low Stock and Out of Stock. Four tiles show total items, low-stock items, out-of-stock items and total stock value at purchase price; they follow the search box, but not the status drop-down. The table has ten columns: Medicine, Company, Category, Batch, Qty, Reorder Level, Purchase Price, MRP, Stock Value and Status. The status is OK, Low Stock (quantity at or below the reorder level) or Out of Stock, and it is colored green, amber or red.

![Stock Report](screenshots/11-stock-report.png)

### Expiry Report

The Expiry Report lists the batches that will cause a loss if nothing is done. The drop-down Show batches expiring offers Already Expired, Within 30 Days, Within 90 Days and Within 180 Days, and the list starts with 90 days. The windows of 30, 90 and 180 days also include the batches that have already expired, while Already Expired lists only those. The list is ordered by expiry date, soonest first. The tiles show the number of expired batches, the number expiring within 30 days, the number expiring within 90 days and the value at risk of the batches listed. The columns are Medicine, Company, Batch, Expiry Date, Days Left, Qty, Stock Value and Status. Days Left shows the word Expired for a batch that is past its date, and the status is Expired, Expiring Soon (less than 30 days left) or OK, in red, amber and green.

![Expiry Report](screenshots/12-expiry-report.png)

### Sales Report

The Sales Report shows sales day by day. The user enters a From date and a To date as yyyy-MM-dd, or uses the buttons Today, This Week and This Month, and presses Apply. The tiles show the total bills, the gross sales before discount, the total discount and the net sales for the period. The main table has one row for each day with the columns Date, Bills, Gross Amount, Discount and Net Amount. A card below it, Best Selling Medicines, ranks the ten medicines with the most units sold; this ranking covers all bills and does not change with the date range. The Print and Export buttons of this screen work on the day-wise table.

![Sales Report](screenshots/13-sales-report.png)

### Bill History

Bill History lists every bill in a date range, which starts as the last 30 days, and can be narrowed by typing part of a bill number or a customer name. The tiles show the number of bills shown and their gross, discount and net totals. The bill table has the columns Bill No, Date, Customer, Items, Gross, Discount and Net. When a bill is selected, the card Bill Items below shows its medicines with batch, quantity, rate and amount, and the button View Receipt opens the bill again in receipt form, where it can be printed. That copy has the same items and totals, but it does not repeat the shop address or the cashier line. Print and Export work on the bill table.

![Bill History](screenshots/14-bill-history.png)

## d) Test Cases

Thirteen test cases were prepared for the rules that matter most: login, validation of input, movement of stock, protection of records and the reports. They were executed by an automated check, a small test program in the tools package of the project, that runs against a temporary database filled with the demonstration data and reports Pass or Fail for each case. The program calls the same validation class and the same data-access classes that the screens use, so the outcome is the one the user sees: the screens show the message that these classes return. All thirteen cases passed.

| ID | Screen | Input / Action | Expected Result | Result |
|---|---|---|---|---|
| T1 | Login | Enter the user name admin and the password admin123 and press Sign In. | The login is accepted, the login window closes and the main window opens. | Pass |
| T2 | Login | Enter the user name admin with a wrong password and press Sign In. | The message "That user name and password do not match." is shown and the login window stays open. | Pass |
| T3 | Medicine Master | Leave Medicine Name empty, fill the other fields correctly and press Save. | The save is refused and the message "Medicine name is required." is listed. | Pass |
| T4 | Medicine Master | Enter -5 as the Quantity and press Save. | The save is refused with the message "Quantity cannot be less than 0." | Pass |
| T5 | Supplier Master | Enter a 9-digit phone number, for example 123456789, and press Save. | The save is refused with the message "Phone must be exactly 10 digits." | Pass |
| T6 | Purchase Entry | Add a line of 50 units of one medicine to an invoice and save it. | The invoice is saved and the stock of that medicine increases by exactly 50. | Pass |
| T7 | Sales Billing | Add a line of 3 units of one medicine to a bill and save it. | The bill is saved and the stock of that medicine decreases by exactly 3. | Pass |
| T8 | Sales Billing | Add a line for more units than are in stock. | The line is refused with a message that states the units left, and no bill is saved and no stock changes. | Pass |
| T9 | Supplier Master | Select a supplier that has medicines linked to it and press Delete. | The delete is refused with the message that the supplier cannot be deleted because medicines or purchase invoices are still linked to it, and the supplier remains. | Pass |
| T10 | User Accounts | Create a user with a user name that already exists, such as admin. | The account is not created and the message "That user name is already taken." is shown. | Pass |
| T11 | Expiry Report | Choose Already Expired in the Show batches expiring drop-down. | The list shows the batches whose expiry date is earlier than today, with the status Expired. | Pass |
| T12 | Sales Billing | Enter a discount that is larger than the bill total and press Save & Print Bill. | The bill is refused with the message "The discount cannot be more than the bill total." and nothing is saved. | Pass |
| T13 | Sales Billing | Look for a batch whose expiry date has passed in the medicine list of the billing screen, and try to bill it directly through the data-access class. | The expired batch is not offered on the screen, and a bill for it is refused with the message "... has expired and cannot be sold." | Pass |

Cases T6, T7 and T8 check the central promise of the system, that the stock always agrees with the invoices and bills. In T8 the stock is read again after the refusal, and it is found to be unchanged, and the number of bills in the database is also unchanged. In T9 and T10 the second line of defense, the foreign key and the UNIQUE constraint of the database, exists in addition to the checks of the screens. The cases do not cover printing, since it depends on the printer that is connected to the computer.

# 10. Conclusion and Recommendations

The project set out to replace the purchase, stock and sales registers of a small medical store with a desktop application that is simple to run and safe in its handling of stock. The finished system meets that aim. It stores the medicine, supplier, customer and user records in a normalized SQLite database, records purchase invoices and bills, keeps the stock consistent with them by writing both in one transaction, refuses a bill that the stock cannot support, and reports low stock, expiring batches and sales. Each objective listed in the introduction is met by a working screen, and the thirteen test cases passed.

Several parts of the design proved useful during the work. Keeping all SQL in the data-access classes made the screens short and easy to change. Putting every color and font in one class kept the eleven screens uniform. Collecting the validation messages into one dialog made the forms more pleasant to use than a separate message for each mistake. The choice of SQLite removed the need for a database server and made the application portable, since the whole store lives in one file. Working on the project also gave the developer practical experience of drawing data flow and entity relationship diagrams, turning them into tables, and testing the business rules that the tables cannot express alone.

The system is a working prototype and has the limits stated in the scope: it serves one computer, it restricts the menu by role but not the individual actions, it stores unsalted password hashes, it does not compute taxes and it cannot cancel a saved bill. These limits do not affect the correctness of what the system does, but they matter for use in a real store.

The following recommendations apply before the software is put to use in a store.

- Run the software in parallel with the paper registers for some weeks, and compare the stock and the daily totals, before the registers are set aside.
- Enter the opening stock carefully, with the correct batch numbers and expiry dates, since every report depends on them.
- Change the default administrator password and remove the default account hint from the login window.
- Copy the database file to a second location at the end of each day.
- Have the receipt heading, that is the shop name and address, changed in the program to the store's own details.
- Review the rules with the pharmacist, particularly the handling of batches and expired stock, and adjust them to the practice of the store.

# 11. Future Scope

The prototype can be extended in several directions. The items below are listed roughly in the order in which they would give the most benefit to a store.

- **Finer access control.** The menu already depends on the role of the user. The next step is to control individual actions, for example allowing only the Administrator to delete records, and to keep an audit log of who changed what.
- **True batch handling.** Store each purchase as its own batch with its own expiry date and quantity, and allocate sales to the batch that expires first, so that the same medicine can be held in several batches.
- **Returns and corrections.** Add sales returns, purchase returns and the cancellation of a bill, each written as a new entry that reverses the stock change, so that saved documents remain intact.
- **Purchase history.** Add a screen that lists the saved purchase invoices with their lines, in the same way as Bill History does for sales.
- **GST billing.** Add tax rates to medicines and produce tax invoices, so that the bill and the reports carry the tax details that the store needs.
- **Barcode support.** Allow a barcode scanner to select the medicine at the counter and speed up billing.
- **Credit and payments.** Record amounts due from customers and payments owed to suppliers, with a ledger for each.
- **Stronger security.** Store passwords with a salted, slow hashing method, add a password policy and lock an account after repeated failures.
- **Backup and restore.** Add commands to back up the database to a chosen folder and to restore it, with an optional daily automatic backup.
- **Store settings.** Move the shop name, address and receipt footer into a settings screen.
- **First-expiry-first-out selling.** When the same medicine is held in several batches, offer the batch with the earliest expiry date first.
- **Ordering aids.** Prepare a suggested purchase order for each supplier from the low-stock list.
- **Several counters.** Replace the single-file database with a database server, so that more than one computer can use the same data at the same time.
- **Better charts and exports.** Add charts of sales over time to the dashboard and export reports to PDF and spreadsheet formats.

# 12. Bibliography and References

1. Oracle. *The Java Tutorials*, including the trail "Creating a GUI With Swing". https://docs.oracle.com/javase/tutorial/
2. SQLite. *SQLite Documentation*. https://www.sqlite.org/docs.html
3. FlatLaf. *FlatLaf - Flat Look and Feel*, project of JFormDesigner. https://github.com/JFormDesigner/flatlaf
4. Silberschatz, A., Korth, H. F. and Sudarshan, S. *Database System Concepts*. McGraw Hill.
5. Sommerville, I. *Software Engineering*. Pearson.
6. Schildt, H. *Java: The Complete Reference*. McGraw Hill.
