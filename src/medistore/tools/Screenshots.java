package medistore.tools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import medistore.Main;
import medistore.dao.UserDao;
import medistore.ui.LoginForm;
import medistore.ui.MainFrame;
import medistore.ui.master.*;
import medistore.ui.report.*;
import medistore.ui.transaction.*;
import medistore.util.Session;

/**
 * Takes the screenshots that go into the project report, by driving the real
 * screens the way a user would: typing into fields, picking from lists and
 * pressing the same buttons.
 *
 * Windows are painted straight into images instead of being photographed off the
 * display, so it needs no screen-recording permission and the pictures come out
 * identical every run. Run it against a throwaway database so the demo data is
 * never disturbed:
 *
 *   java -Dmedistore.db=/tmp/shots.db medistore.tools.Screenshots screenshots
 */
public final class Screenshots {

    private static final int WIDTH = 1320;
    private static final int HEIGHT = 800;
    private static final int TITLE_BAR = 34;

    private static File outDir;
    private static MainFrame frame;
    private static int taken;

    private Screenshots() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        String db = System.getProperty("medistore.db", "");
        if (db.isBlank() || db.endsWith("data/medistore.db")) {
            System.err.println("Refusing to run on the real database. Pass -Dmedistore.db=/tmp/shots.db");
            System.exit(2);
        }
        System.setProperty("apple.awt.UIElement", "true");
        outDir = new File(args.length > 0 ? args[0] : "screenshots");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create " + outDir);
        }

        SampleData.main(new String[0]);
        medistore.db.Database.get();
        System.out.println();
        System.out.println("Taking screenshots into " + outDir);

        ui(Main::applyLookAndFeel);
        loginScreen();
        Session.setUser(new UserDao().authenticate("admin", "admin123"));
        ui(() -> frame = new MainFrame());

        dashboard();
        medicineScreens();
        supplierScreen();
        customerScreen();
        userScreen();
        purchaseScreen();
        billingScreens();
        reportScreens();

        System.out.println("Done - " + taken + " screenshots.");
        System.exit(0);
    }

    // ------------------------------------------------------------------ screens

    private static void loginScreen() throws Exception {
        LoginForm login = onEdt(LoginForm::new);
        ui(() -> {
            setText(login.getContentPane(), "USER NAME", "admin");
            setText(login.getContentPane(), "PASSWORD", "admin123");
        });
        shot("01-login", login.getTitle(), login.getContentPane(), null);
        ui(login::dispose);
    }

    private static void dashboard() throws Exception {
        show("dashboard");
        shotFrame("02-dashboard");
    }

    private static void medicineScreens() throws Exception {
        show("medicine");
        shotFrame("03-medicine-master");

        MedicineForm form = form(MedicineForm.class);
        ui(() -> {
            setText(form, "MEDICINE NAME", "Pantocid DSR");
            setText(form, "COMPANY", "Sun Pharma");
            choose(form, "CATEGORY", "Capsule");
            setText(form, "BATCH NO", "BSU140");
            setText(form, "EXPIRY DATE", LocalDate.now().plusMonths(18).toString());
            setText(form, "QUANTITY", "60");
            setText(form, "PURCHASE PRICE", "92.00");
            setText(form, "MRP", "121.00");
            setText(form, "REORDER LEVEL", "20");
            chooseIndex(form, "SUPPLIER", 0);
        });
        shotFrame("04-medicine-entry");

        // The same form with mistakes in it: the Save button reports every one at once.
        ui(() -> {
            setText(form, "MEDICINE NAME", "");
            setText(form, "EXPIRY DATE", "31-03-2027");
            setText(form, "QUANTITY", "-5");
            setText(form, "PURCHASE PRICE", "abc");
        });
        JDialog error = awaitAfter(() -> click(form, "Save"));
        shotDialog("15-validation-error", error);
        ui(() -> click(form, "Clear"));
    }

    private static void supplierScreen() throws Exception {
        show("supplier");
        SupplierForm form = form(SupplierForm.class);
        ui(() -> selectRow(form, 0, 1));
        shotFrame("05-supplier-master");
    }

    private static void customerScreen() throws Exception {
        show("customer");
        CustomerForm form = form(CustomerForm.class);
        ui(() -> selectRow(form, 0, 2));
        shotFrame("06-customer-master");
    }

    private static void userScreen() throws Exception {
        show("user");
        UserForm form = form(UserForm.class);
        ui(() -> selectRow(form, 0, 1));
        shotFrame("07-user-accounts");
    }

    private static void purchaseScreen() throws Exception {
        show("purchase");
        PurchaseForm form = form(PurchaseForm.class);
        ui(() -> {
            chooseIndex(form, "SUPPLIER", 1);
            addLine(form, 2, "40");
            addLine(form, 4, "25");
            addLine(form, 8, "60");
        });
        shotFrame("08-purchase-entry");
        // Saving opens a confirmation box that blocks the screen, so start it without waiting.
        JDialog saved = awaitAfter(() -> click(form, "Save Invoice"));
        ui(saved::dispose);
        dismissAll();
    }

    private static void billingScreens() throws Exception {
        show("billing");
        BillingForm form = form(BillingForm.class);
        ui(() -> {
            chooseIndex(form, "CUSTOMER", 3);
            addLine(form, 0, "2");
            addLine(form, 3, "1");
            addLine(form, 7, "3");
            setText(form, "DISCOUNT", "20");
        });
        shotFrame("09-billing");

        // Saving shows a confirmation and then the printable receipt.
        JDialog message = awaitAfter(() -> click(form, "Save & Print Bill"));
        ui(message::dispose);
        JDialog receipt = awaitDialog(5000);
        shotDialog("10-bill-receipt", receipt);
        ui(receipt::dispose);
    }

    private static void reportScreens() throws Exception {
        show("stock");
        shotFrame("11-stock-report");
        show("expiry");
        shotFrame("12-expiry-report");
        show("sales");
        shotFrame("13-sales-report");
        show("bills");
        BillHistoryForm history = form(BillHistoryForm.class);
        ui(() -> selectRow(history, 0, 0));
        shotFrame("14-bill-history");
    }

    // --------------------------------------------------------- driving the forms

    /** Picks a medicine, types a quantity and presses Add Item, as a cashier would. */
    private static void addLine(Container form, int medicineIndex, String quantity) {
        chooseIndex(form, "MEDICINE", medicineIndex);
        setText(form, "QUANTITY", quantity);
        click(form, "Add Item");
    }

    private static void show(String key) throws Exception {
        ui(() -> frame.show(key));
    }

    private static <T extends Container> T form(Class<T> type) {
        List<Component> all = new ArrayList<>();
        flatten(frame, all);
        for (Component c : all) {
            if (type.isInstance(c)) {
                return type.cast(c);
            }
        }
        throw new IllegalStateException("Screen not found: " + type.getSimpleName());
    }

    private static void setText(Container root, String label, String text) {
        JTextComponent field = after(root, label, JTextComponent.class);
        field.setText(text);
    }

    private static void choose(Container root, String label, String item) {
        JComboBox<?> combo = after(root, label, JComboBox.class);
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (String.valueOf(combo.getItemAt(i)).equals(item)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        throw new IllegalStateException("No '" + item + "' in the " + label + " list");
    }

    private static void chooseIndex(Container root, String label, int index) {
        JComboBox<?> combo = after(root, label, JComboBox.class);
        combo.setSelectedIndex(Math.min(index, combo.getItemCount() - 1));
    }

    private static void click(Container root, String buttonText) {
        List<Component> all = new ArrayList<>();
        flatten(root, all);
        for (Component c : all) {
            if (c instanceof JButton b && buttonText.equals(b.getText())) {
                b.doClick();
                return;
            }
        }
        throw new IllegalStateException("Button not found: " + buttonText);
    }

    /** Clicks a row of the n-th table on the screen, which loads that record into the form. */
    private static void selectRow(Container root, int tableIndex, int row) {
        List<Component> all = new ArrayList<>();
        flatten(root, all);
        int seen = 0;
        for (Component c : all) {
            if (c instanceof JTable t && seen++ == tableIndex) {
                t.setRowSelectionInterval(row, row);
                return;
            }
        }
        throw new IllegalStateException("Table " + tableIndex + " not found");
    }

    /** The first component of the wanted type that follows the caption in the layout order. */
    private static <T extends Component> T after(Container root, String label, Class<T> type) {
        List<Component> all = new ArrayList<>();
        flatten(root, all);
        boolean found = false;
        for (Component c : all) {
            if (!found) {
                found = c instanceof JLabel l && l.getText() != null
                        && l.getText().trim().toUpperCase().startsWith(label.toUpperCase());
            } else if (type.isInstance(c) && !(c.getParent() instanceof JComboBox)) {
                return type.cast(c);
            }
        }
        throw new IllegalStateException("No " + type.getSimpleName() + " after the '" + label + "' caption");
    }

    private static void flatten(Component c, List<Component> into) {
        into.add(c);
        if (c instanceof Container k) {
            for (Component child : k.getComponents()) {
                flatten(child, into);
            }
        }
    }

    // ----------------------------------------------------------------- modal dialogs

    /** Starts an action that opens a modal dialog, and returns that dialog once it is up. */
    private static JDialog awaitAfter(Runnable action) throws Exception {
        SwingUtilities.invokeLater(action);
        return awaitDialog(5000);
    }

    private static JDialog awaitDialog(int timeoutMs) throws Exception {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog d && d.isShowing() && d.isModal()) {
                    Thread.sleep(250);
                    return d;
                }
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("The expected dialog never appeared");
    }

    /** Closes whatever confirmation boxes a Save left open. */
    private static void dismissAll() throws Exception {
        for (int i = 0; i < 3; i++) {
            try {
                JDialog d = awaitDialog(1200);
                ui(d::dispose);
            } catch (IllegalStateException noMore) {
                return;
            }
        }
    }

    // -------------------------------------------------------------- taking pictures

    private static void shotFrame(String name) throws Exception {
        ui(() -> {
            frame.setSize(WIDTH, HEIGHT + 60);
            frame.validate();
        });
        Thread.sleep(150);
        ui(() -> {
            Container content = frame.getContentPane();
            content.setSize(WIDTH, HEIGHT);
            layout(content);
        });
        shot(name, frame.getTitle(), frame.getContentPane(), new Dimension(WIDTH, HEIGHT));
    }

    private static void shotDialog(String name, JDialog dialog) throws Exception {
        shot(name, dialog.getTitle(), dialog.getContentPane(), null);
    }

    private static void shot(String name, String title, Container content, Dimension size) throws Exception {
        BufferedImage[] image = new BufferedImage[1];
        ui(() -> {
            Dimension d = size != null ? size : content.getSize().width > 0
                    ? content.getSize() : content.getPreferredSize();
            content.setSize(d);
            layout(content);
            image[0] = withTitleBar(paint(content, d), title);
        });
        File file = new File(outDir, name + ".png");
        ImageIO.write(image[0], "png", file);
        taken++;
        System.out.printf("  %-26s %dx%d%n", file.getName(), image[0].getWidth(), image[0].getHeight());
    }

    private static void layout(Component c) {
        c.doLayout();
        if (c instanceof Container k) {
            for (Component child : k.getComponents()) {
                layout(child);
            }
        }
    }

    private static BufferedImage paint(Container c, Dimension size) {
        BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size.width, size.height);
        c.printAll(g);
        g.dispose();
        return img;
    }

    /** Adds a plain window title bar so each picture reads as a program window. */
    private static BufferedImage withTitleBar(BufferedImage body, String title) {
        BufferedImage out = new BufferedImage(body.getWidth(), body.getHeight() + TITLE_BAR,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xE9, 0xEC, 0xEF));
        g.fillRect(0, 0, out.getWidth(), TITLE_BAR);
        g.setColor(new Color(0xC7, 0xCC, 0xD1));
        g.fillRect(0, TITLE_BAR - 1, out.getWidth(), 1);
        Color[] dots = {new Color(0xFF, 0x5F, 0x57), new Color(0xFE, 0xBC, 0x2E), new Color(0x28, 0xC8, 0x40)};
        for (int i = 0; i < dots.length; i++) {
            g.setColor(dots[i]);
            g.fillOval(14 + i * 22, 11, 12, 12);
        }
        g.setColor(new Color(0x37, 0x41, 0x51));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String text = title == null ? "" : title;
        g.drawString(text, (out.getWidth() - g.getFontMetrics().stringWidth(text)) / 2, 22);
        g.drawImage(body, 0, TITLE_BAR, null);
        g.setColor(new Color(0xC7, 0xCC, 0xD1));
        g.drawRect(0, 0, out.getWidth() - 1, out.getHeight() - 1);
        g.dispose();
        return out;
    }

    // ------------------------------------------------------------------ threading

    private static void ui(Runnable r) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
    }

    private static <T> T onEdt(java.util.function.Supplier<T> s) throws Exception {
        Object[] box = new Object[1];
        ui(() -> box[0] = s.get());
        @SuppressWarnings("unchecked") T value = (T) box[0];
        return value;
    }
}
