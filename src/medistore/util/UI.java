package medistore.util;

import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

/**
 * Every colour, font and widget style the software uses.
 *
 * Keeping them here is what makes all fifteen screens look like one product
 * instead of fifteen separate exercises - change a value once and the whole
 * application follows.
 */
public final class UI {

    // ---------- Colours ----------
    public static final Color PRIMARY      = new Color(0x0D, 0x94, 0x88);
    public static final Color PRIMARY_DARK = new Color(0x0F, 0x76, 0x6E);
    public static final Color SIDEBAR      = new Color(0x13, 0x4E, 0x4A);
    public static final Color SIDEBAR_HOVER= new Color(0x11, 0x5E, 0x59);
    public static final Color BACKGROUND   = new Color(0xF1, 0xF5, 0xF9);
    public static final Color CARD         = Color.WHITE;
    public static final Color TEXT         = new Color(0x0F, 0x17, 0x2A);
    public static final Color MUTED        = new Color(0x64, 0x74, 0x8B);
    public static final Color BORDER       = new Color(0xCB, 0xD5, 0xE1);
    public static final Color SUCCESS      = new Color(0x16, 0xA3, 0x4A);
    public static final Color DANGER       = new Color(0xDC, 0x26, 0x26);
    public static final Color WARNING      = new Color(0xD9, 0x77, 0x06);
    public static final Color ROW_ALT      = new Color(0xF8, 0xFA, 0xFC);

    private static final String FAMILY = pickFamily();

    private UI() {
    }

    /** Picks whichever system font is available, so it looks native on Mac and Windows alike. */
    private static String pickFamily() {
        String[] wanted = {"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Dialog"};
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : wanted) {
            for (String have : installed) {
                if (have.equalsIgnoreCase(candidate)) {
                    return have;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    // ---------- Fonts ----------
    public static Font font(int size, int style) { return new Font(FAMILY, style, size); }
    public static Font h1()    { return font(24, Font.BOLD); }
    public static Font h2()    { return font(17, Font.BOLD); }
    public static Font h3()    { return font(14, Font.BOLD); }
    public static Font body()  { return font(13, Font.PLAIN); }
    public static Font small() { return font(12, Font.PLAIN); }
    public static Font mono(int size) { return new Font(Font.MONOSPACED, Font.PLAIN, size); }

    // ---------- Labels ----------
    public static JLabel label(String text, Font font, Color colour) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(colour);
        return l;
    }

    public static JLabel title(String text)     { return label(text, h1(), TEXT); }
    public static JLabel heading(String text)   { return label(text, h2(), TEXT); }
    public static JLabel fieldLabel(String text){ return label(text, font(12, Font.BOLD), MUTED); }
    public static JLabel muted(String text)     { return label(text, small(), MUTED); }

    // ---------- Inputs ----------
    public static JTextField textField() {
        JTextField f = new JTextField();
        styleInput(f);
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        styleInput(f);
        return f;
    }

    public static JTextArea textArea(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setFont(body());
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    public static <T> JComboBox<T> comboBox() {
        JComboBox<T> c = new JComboBox<>();
        c.setFont(body());
        c.setBackground(Color.WHITE);
        return c;
    }

    private static void styleInput(JTextField field) {
        field.setFont(body());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    // ---------- Buttons ----------
    public static JButton button(String text, Color background, Color foreground) {
        JButton b = new JButton(text);
        b.setFont(font(13, Font.BOLD));
        b.setBackground(background);
        b.setForeground(foreground);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton primaryButton(String text)   { return button(text, PRIMARY, Color.WHITE); }
    public static JButton successButton(String text)   { return button(text, SUCCESS, Color.WHITE); }
    public static JButton dangerButton(String text)    { return button(text, DANGER, Color.WHITE); }

    public static JButton secondaryButton(String text) {
        JButton b = button(text, Color.WHITE, TEXT);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 17, 8, 17)));
        return b;
    }

    // ---------- Containers ----------
    public static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    /** A white panel with a hairline border - the basic building block of every screen. */
    public static JPanel card() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                padding(16, 16, 16, 16)));
        return p;
    }

    /** A card carrying a bold caption above its contents. */
    public static JPanel card(String caption, Component body) {
        JPanel p = card();
        JLabel cap = label(caption, h3(), TEXT);
        cap.setBorder(padding(0, 0, 12, 0));
        p.add(cap, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    public static JPanel row(int gap, Component... children) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (Component c : children) {
            p.add(c);
        }
        return p;
    }

    public static JPanel rowRight(int gap, Component... children) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, gap, 0));
        p.setOpaque(false);
        for (Component c : children) {
            p.add(c);
        }
        return p;
    }

    // ---------- Tables ----------
    /** Applies the shared look: tall rows, tinted header, alternating stripes. */
    public static JTable table(TableModel model) {
        JTable t = new JTable(model);
        t.setFont(body());
        t.setRowHeight(30);
        t.setGridColor(new Color(0xE2, 0xE8, 0xF0));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(0xCC, 0xFB, 0xF1));
        t.setSelectionForeground(TEXT);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JTableHeader header = t.getTableHeader();
        header.setFont(font(12, Font.BOLD));
        header.setBackground(new Color(0xE2, 0xE8, 0xF0));
        header.setForeground(TEXT);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                if (!selected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                }
                setBorder(padding(0, 10, 0, 10));
                return c;
            }
        });
        return t;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // ---------- Dialogs ----------
    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Medical Store", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Please check", JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    // ---------- Formatting ----------
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static String money(double amount) { return "Rs. " + MONEY.format(amount); }
    public static String number(double value)  { return MONEY.format(value); }

    public static String today() { return LocalDate.now().toString(); }

    /** Turns a stored yyyy-MM-dd date into the dd-MM-yyyy form people expect on screen. */
    public static String displayDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(isoDate).format(DISPLAY);
        } catch (RuntimeException e) {
            return isoDate;
        }
    }
}
