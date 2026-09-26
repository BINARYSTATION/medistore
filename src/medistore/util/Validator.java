package medistore.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

/**
 * Collects every problem with a form and reports them together.
 *
 * Showing one dialog listing all the mistakes is far less irritating than
 * making the user rediscover them one message box at a time.
 */
public final class Validator {

    private final List<String> problems = new ArrayList<>();

    /** Fails when the field is empty or only spaces. */
    public Validator required(JTextComponent field, String label) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            problems.add(label + " is required.");
        }
        return this;
    }

    /** Fails when nothing is picked in a combo box. */
    public Validator selected(JComponent combo, Object value, String label) {
        if (value == null) {
            problems.add("Please select a " + label + ".");
        }
        return this;
    }

    /** Fails unless the text is a whole number of at least {@code minimum}. */
    public Validator wholeNumber(JTextComponent field, String label, int minimum) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            problems.add(label + " is required.");
            return this;
        }
        try {
            if (Integer.parseInt(text) < minimum) {
                problems.add(label + " cannot be less than " + minimum + ".");
            }
        } catch (NumberFormatException e) {
            problems.add(label + " must be a whole number.");
        }
        return this;
    }

    /** Fails unless the text is a number of at least {@code minimum}. */
    public Validator decimal(JTextComponent field, String label, double minimum) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            problems.add(label + " is required.");
            return this;
        }
        try {
            if (Double.parseDouble(text) < minimum) {
                problems.add(label + " cannot be less than " + minimum + ".");
            }
        } catch (NumberFormatException e) {
            problems.add(label + " must be a number.");
        }
        return this;
    }

    /** Fails unless the text is a real yyyy-MM-dd date. */
    public Validator date(JTextComponent field, String label) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            problems.add(label + " is required.");
            return this;
        }
        try {
            LocalDate.parse(text);
        } catch (RuntimeException e) {
            problems.add(label + " must be a date in yyyy-MM-dd form, for example 2027-03-31.");
        }
        return this;
    }

    /** Fails unless the text is a ten digit phone number. */
    public Validator phone(JTextComponent field, String label) {
        String text = field.getText().trim();
        if (!text.isEmpty() && !text.matches("\\d{10}")) {
            problems.add(label + " must be exactly 10 digits.");
        }
        return this;
    }

    /** Fails on anything that is clearly not an email address. */
    public Validator email(JTextComponent field, String label) {
        String text = field.getText().trim();
        if (!text.isEmpty() && !text.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}")) {
            problems.add(label + " does not look like a valid email address.");
        }
        return this;
    }

    /** Adds a rule the generic checks above cannot express. */
    public Validator check(boolean mustBeTrue, String problem) {
        if (!mustBeTrue) {
            problems.add(problem);
        }
        return this;
    }

    public boolean isValid() {
        return problems.isEmpty();
    }

    /** The collected problems as one bulleted message ready for a dialog. */
    public String message() {
        StringBuilder sb = new StringBuilder("Please correct the following:\n\n");
        for (String problem : problems) {
            sb.append("  •  ").append(problem).append('\n');
        }
        return sb.toString();
    }

    /** Shows the problems if there are any; returns true when the form is good to save. */
    public boolean showIfInvalid(java.awt.Component parent) {
        if (problems.isEmpty()) {
            return true;
        }
        UI.error(parent, message());
        return false;
    }
}
