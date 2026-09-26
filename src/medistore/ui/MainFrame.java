package medistore.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.*;
import medistore.ui.master.CustomerForm;
import medistore.ui.master.MedicineForm;
import medistore.ui.master.SupplierForm;
import medistore.ui.master.UserForm;
import medistore.ui.report.BillHistoryForm;
import medistore.ui.report.ExpiryReportForm;
import medistore.ui.report.SalesReportForm;
import medistore.ui.report.StockReportForm;
import medistore.ui.transaction.BillingForm;
import medistore.ui.transaction.PurchaseForm;
import medistore.util.Icons;
import medistore.util.Session;
import medistore.util.UI;

/**
 * The main window: a fixed sidebar of menu options on the left and the selected
 * screen on the right.
 *
 * Screens are built the first time they are opened and then kept, so moving
 * between them is instant and each one keeps whatever the user had typed.
 */
public class MainFrame extends JFrame {

    /** One entry in the sidebar menu. */
    private record NavItem(String key, String label, Icons.Glyph glyph,
                           Supplier<BaseForm> factory) {
    }

    /** The window's starting size on any screen big enough for it. */
    private static final int DESIGN_WIDTH = 1320;
    private static final int DESIGN_HEIGHT = 800;
    /** The smallest size every screen is designed for; below it the content scrolls. */
    private static final int MIN_WIDTH = 1180;
    private static final int MIN_HEIGHT = 700;
    private static final int SIDEBAR_WIDTH = 236;

    /**
     * The content area's size at the minimum window size. Decides when the content
     * area starts scrolling; a screen's own preferred size is unreliable for this
     * because screens holding a JTable report an oversized one.
     */
    private Dimension scrollThreshold;

    private final ScrollableContainer content = new ScrollableContainer(new CardLayout(), 16) {
        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    return c.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }

        @Override
        protected Dimension trackingReferenceSize() {
            return scrollThreshold != null ? scrollThreshold : getPreferredSize();
        }
    };
    private final JScrollPane contentScroll = new JScrollPane(content);
    private final Map<String, BaseForm> openForms = new LinkedHashMap<>();
    private final Map<String, JPanel> navButtons = new LinkedHashMap<>();
    private final Map<String, NavItem> navItems = new LinkedHashMap<>();
    private String activeKey;

    public MainFrame() {
        super("Medical Store Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // On a small laptop screen (e.g. 1366x768 at 125% scaling) the usable area is
        // smaller than the sizes below, which would push the window off-screen and put
        // Save/Print buttons out of reach. Clamp both to whatever actually fits; on a
        // normal-size screen this leaves the numbers untouched. There is no usable area
        // to measure in a headless environment, so keep the original sizes there.
        Rectangle usable = GraphicsEnvironment.isHeadless() ? new Rectangle(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT)
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(clampToScreen(new Dimension(MIN_WIDTH, MIN_HEIGHT), usable));
        setPreferredSize(clampToScreen(new Dimension(DESIGN_WIDTH, DESIGN_HEIGHT), usable));

        content.setBackground(UI.BACKGROUND);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getViewport().setBackground(UI.BACKGROUND);

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(contentScroll, BorderLayout.CENTER);

        pack();
        // Real window insets (title bar height and the like) are only known once the
        // window has been made real by pack(); they do not depend on window size, so
        // this gives the true content size at the minimum design size even when the
        // window itself has had to be clamped smaller than that for this screen.
        Insets insets = getInsets();
        scrollThreshold = new Dimension(
                Math.max(0, MIN_WIDTH - insets.left - insets.right - SIDEBAR_WIDTH),
                Math.max(0, MIN_HEIGHT - insets.top - insets.bottom));

        setLocationRelativeTo(null);
        show("dashboard");
    }

    /** Shrinks a wanted window size so it never exceeds the screen's usable area. */
    private static Dimension clampToScreen(Dimension wanted, Rectangle usable) {
        return new Dimension(Math.min(wanted.width, usable.width), Math.min(wanted.height, usable.height));
    }

    /**
     * What each role may open. Administrators see everything, pharmacists everything
     * except user accounts, and counter staff only billing, history and the stock reports.
     */
    private static boolean allowed(String key) {
        if (Session.isAdmin()) {
            return true;
        }
        String role = Session.getUser() == null ? "" : Session.getUser().getRole();
        if ("Pharmacist".equalsIgnoreCase(role)) {
            return !key.equals("user");
        }
        return Set.of("dashboard", "billing", "bills", "stock", "expiry").contains(key);
    }

    /** The menu for the signed-in user, without empty sections. */
    private List<Object> menu() {
        List<Object> visible = new ArrayList<>();
        String pendingSection = null;
        for (Object entry : allEntries()) {
            if (entry instanceof String section) {
                pendingSection = section;
            } else if (entry instanceof NavItem item && allowed(item.key())) {
                if (pendingSection != null) {
                    visible.add(pendingSection);
                    pendingSection = null;
                }
                visible.add(item);
            }
        }
        return visible;
    }

    /** Every menu entry, in the order it appears down the sidebar. */
    private List<Object> allEntries() {
        List<Object> entries = new ArrayList<>();
        entries.add("MAIN");
        entries.add(new NavItem("dashboard", "Dashboard", Icons.Glyph.GRID, DashboardForm::new));
        entries.add("MASTER ENTRY");
        entries.add(new NavItem("medicine", "Medicine Master", Icons.Glyph.PILL, MedicineForm::new));
        entries.add(new NavItem("supplier", "Supplier Master", Icons.Glyph.TRUCK, SupplierForm::new));
        entries.add(new NavItem("customer", "Customer Master", Icons.Glyph.PERSON, CustomerForm::new));
        entries.add(new NavItem("user", "User Accounts", Icons.Glyph.KEY, UserForm::new));
        entries.add("TRANSACTIONS");
        entries.add(new NavItem("purchase", "Purchase Entry", Icons.Glyph.BOX, PurchaseForm::new));
        entries.add(new NavItem("billing", "Sales Billing", Icons.Glyph.CART, BillingForm::new));
        entries.add("REPORTS");
        entries.add(new NavItem("stock", "Stock Report", Icons.Glyph.CHART, StockReportForm::new));
        entries.add(new NavItem("expiry", "Expiry Report", Icons.Glyph.CLOCK, ExpiryReportForm::new));
        entries.add(new NavItem("sales", "Sales Report", Icons.Glyph.CHART, SalesReportForm::new));
        entries.add(new NavItem("bills", "Bill History", Icons.Glyph.PAGE, BillHistoryForm::new));
        return entries;
    }

    /**
     * The sidebar, scrollable so a short screen (e.g. a laptop at 125% DPI scaling)
     * still lets every menu item be reached instead of clipping the bottom ones.
     * The width stays pinned to 236 regardless of the real preferred width of its
     * buttons; the height is left to its natural value so scrolling only kicks in
     * when it does not fit.
     */
    private JScrollPane buildSidebar() {
        ScrollableContainer sidebar = new ScrollableContainer(16) {
            @Override
            public Dimension getPreferredSize() {
                Dimension natural = super.getPreferredSize();
                return new Dimension(SIDEBAR_WIDTH, natural.height);
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UI.SIDEBAR);

        sidebar.add(buildBrand());

        for (Object entry : menu()) {
            if (entry instanceof String section) {
                sidebar.add(buildSectionLabel(section));
            } else if (entry instanceof NavItem item) {
                navItems.put(item.key(), item);
                JPanel button = buildNavButton(item);
                navButtons.put(item.key(), button);
                sidebar.add(button);
            }
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(buildFooter());

        JScrollPane scroll = new JScrollPane(sidebar);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UI.SIDEBAR);
        return scroll;
    }

    private JPanel buildBrand() {
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.setBorder(UI.padding(22, 20, 22, 20));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel mark = UI.label("✚", UI.font(26, Font.BOLD), new Color(0x5E, 0xEA, 0xD4));
        JLabel name = UI.label("MediStore", UI.font(19, Font.BOLD), Color.WHITE);
        brand.add(mark);
        brand.add(name);
        return brand;
    }

    private JLabel buildSectionLabel(String text) {
        JLabel label = UI.label(text, UI.font(10, Font.BOLD), new Color(0x5E, 0xEA, 0xD4));
        label.setBorder(UI.padding(14, 22, 6, 20));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return label;
    }

    /**
     * A panel is used rather than a JButton because a plain panel gives complete
     * control of the hover and selected colours on every platform.
     */
    private JPanel buildNavButton(NavItem item) {
        JPanel button = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        button.setOpaque(true);
        button.setBackground(UI.SIDEBAR);
        button.setBorder(UI.padding(11, 20, 11, 20));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(Icons.of(item.glyph(), 17, new Color(0xCC, 0xFB, 0xF1)));
        JLabel text = UI.label(item.label(), UI.font(13, Font.PLAIN), new Color(0xE2, 0xF5, 0xF2));
        button.add(icon);
        button.add(text);

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { show(item.key()); }
            @Override public void mouseEntered(MouseEvent e) {
                if (!item.key().equals(activeKey)) {
                    button.setBackground(UI.SIDEBAR_HOVER);
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!item.key().equals(activeKey)) {
                    button.setBackground(UI.SIDEBAR);
                }
            }
        });
        return button;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UI.SIDEBAR_HOVER),
                UI.padding(14, 20, 16, 16)));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        footer.setAlignmentX(LEFT_ALIGNMENT);

        JPanel who = new JPanel();
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.setOpaque(false);
        JLabel name = UI.label(Session.userName(), UI.font(13, Font.BOLD), Color.WHITE);
        JLabel role = UI.label(Session.getUser() == null ? "" : Session.getUser().getRole(),
                UI.font(11, Font.PLAIN), new Color(0x99, 0xF6, 0xE4));
        name.setAlignmentX(LEFT_ALIGNMENT);
        role.setAlignmentX(LEFT_ALIGNMENT);
        who.add(name);
        who.add(role);

        JLabel logout = new JLabel(Icons.of(Icons.Glyph.EXIT, 18, new Color(0x99, 0xF6, 0xE4)));
        logout.setToolTipText("Sign out");
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { signOut(); }
        });

        footer.add(who, BorderLayout.CENTER);
        footer.add(logout, BorderLayout.EAST);
        return footer;
    }

    /** Brings a screen to the front, building it the first time it is asked for. */
    public void show(String key) {
        NavItem item = navItems.get(key);
        if (item == null) {
            return;
        }
        BaseForm form = openForms.get(key);
        if (form == null) {
            form = item.factory().get();
            openForms.put(key, form);
            content.add(form, key);
        }
        ((CardLayout) content.getLayout()).show(content, key);
        // Otherwise a form opened while scrolled down from a previous one would start
        // part-way down instead of at its own top.
        contentScroll.getViewport().setViewPosition(new Point(0, 0));
        form.onShow();
        highlight(key);
    }

    private void highlight(String key) {
        activeKey = key;
        navButtons.forEach((navKey, button) ->
                button.setBackground(navKey.equals(key) ? UI.PRIMARY : UI.SIDEBAR));
    }

    private void signOut() {
        if (!UI.confirm(this, "Sign out of MediStore?")) {
            return;
        }
        Session.clear();
        dispose();
        SwingUtilities.invokeLater(medistore.Main::startLogin);
    }
}
