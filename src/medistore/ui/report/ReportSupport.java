package medistore.ui.report;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import medistore.util.UI;
import medistore.util.Validator;

/**
 * The pieces every report screen needs: the summary tile, a read-only table
 * model, and the CSV export and print actions behind the header buttons.
 *
 * They live here so the four screens stay short and behave identically.
 */
final class ReportSupport {

    private ReportSupport() {
    }

    /** One headline figure, boxed the same way as on the dashboard. */
    static JPanel tile(String caption, String value, String note, Color noteColour) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UI.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UI.BORDER),
                UI.padding(14, 16, 14, 16)));

        JLabel captionLabel = UI.label(caption, UI.font(10, Font.BOLD), UI.MUTED);
        JLabel valueLabel = UI.label(value, UI.font(21, Font.BOLD), UI.TEXT);
        JLabel noteLabel = UI.label(note, UI.font(11, Font.PLAIN), noteColour);

        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(UI.padding(6, 0, 4, 0));

        card.add(captionLabel);
        card.add(valueLabel);
        card.add(noteLabel);
        return card;
    }

    /** A report table is for reading, never for typing into. */
    static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    /**
     * Checks that both date boxes hold real dates and that the range runs forwards.
     * Shows what is wrong and returns false when it does not.
     */
    static boolean validRange(Component parent, JTextField from, JTextField to) {
        Validator check = new Validator().date(from, "From date").date(to, "To date");
        if (!check.showIfInvalid(parent)) {
            return false;
        }
        if (LocalDate.parse(from.getText().trim()).isAfter(LocalDate.parse(to.getText().trim()))) {
            UI.error(parent, "The From date cannot be after the To date.");
            return false;
        }
        return true;
    }

    /** Runs {@code action} after every edit, so a search box can filter as the user types. */
    static void onTextChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    /** Saves the rows currently on screen to a CSV file the user chooses. */
    static void exportCsv(Component parent, JTable table, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export to CSV");
        chooser.setSelectedFile(new File(suggestedName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        TableModel model = table.getModel();
        try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
            out.println(csvLine(model, -1));
            for (int row = 0; row < model.getRowCount(); row++) {
                out.println(csvLine(model, row));
            }
        } catch (IOException e) {
            UI.error(parent, "Could not write the file. " + e.getMessage());
            return;
        }
        UI.info(parent, model.getRowCount() + " row(s) exported to " + file.getName() + ".");
    }

    /** One CSV line: the column names when {@code row} is -1, otherwise that row's values. */
    private static String csvLine(TableModel model, int row) {
        StringBuilder line = new StringBuilder();
        for (int column = 0; column < model.getColumnCount(); column++) {
            if (column > 0) {
                line.append(',');
            }
            Object value = row < 0 ? model.getColumnName(column) : model.getValueAt(row, column);
            line.append(csvValue(value == null ? "" : value.toString()));
        }
        return line.toString();
    }

    /** Quotes a value that would otherwise break the line, such as "Rs. 1,250.00". */
    private static String csvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Sends the table to the printer under the report title, numbering each page. */
    static void print(Component parent, JTable table, String reportTitle) {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(reportTitle),
                    new MessageFormat("Page {0}"));
        } catch (PrinterException e) {
            UI.error(parent, "Could not print the report. " + e.getMessage());
        }
    }
}
