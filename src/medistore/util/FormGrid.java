package medistore.util;

import java.awt.*;
import javax.swing.*;

/**
 * Builds the label-above-input grids that every data entry screen uses.
 *
 * Written as a small builder so a form reads like the form itself:
 * new FormGrid(2).add("Medicine Name", nameField).add("Company", companyField).panel()
 */
public final class FormGrid {

    private final JPanel panel = new JPanel(new GridBagLayout());
    private final int columns;
    private int index;

    public FormGrid(int columns) {
        this.columns = Math.max(1, columns);
        panel.setOpaque(false);
    }

    /** Adds one labelled field in the next free cell. */
    public FormGrid add(String label, JComponent field) {
        return add(label, field, 1);
    }

    /** Adds a field that stretches across {@code span} columns, for addresses and the like. */
    public FormGrid add(String label, JComponent field, int span) {
        int row = index / columns;
        int column = index % columns;
        int width = Math.min(span, columns - column);

        GridBagConstraints labelAt = new GridBagConstraints();
        labelAt.gridx = column;
        labelAt.gridy = row * 2;
        labelAt.gridwidth = width;
        labelAt.anchor = GridBagConstraints.WEST;
        labelAt.insets = new Insets(0, 0, 5, 18);
        panel.add(UI.fieldLabel(label.toUpperCase()), labelAt);

        GridBagConstraints fieldAt = new GridBagConstraints();
        fieldAt.gridx = column;
        fieldAt.gridy = row * 2 + 1;
        fieldAt.gridwidth = width;
        fieldAt.fill = GridBagConstraints.HORIZONTAL;
        fieldAt.weightx = 1;
        fieldAt.anchor = GridBagConstraints.WEST;
        fieldAt.insets = new Insets(0, 0, 16, 18);
        panel.add(field, fieldAt);

        index += width;
        return this;
    }

    /** Leaves the next cell empty so the following field lands where you want it. */
    public FormGrid skip() {
        index++;
        return this;
    }

    public JPanel panel() {
        return panel;
    }
}
