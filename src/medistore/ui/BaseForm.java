package medistore.ui;

import java.awt.*;
import javax.swing.*;
import medistore.util.UI;

/**
 * The shared skeleton of every screen in the application.
 *
 * A form supplies a title, an optional row of action buttons and its body; the
 * heading, spacing and background are handled here so all eleven screens line up
 * pixel for pixel.
 */
public abstract class BaseForm extends JPanel {

    private final String title;
    private final JPanel header = new JPanel(new BorderLayout());
    private final JPanel content = new JPanel(new BorderLayout());

    protected BaseForm(String title, String subtitle) {
        this.title = title;
        setLayout(new BorderLayout());
        setBackground(UI.BACKGROUND);
        setBorder(UI.padding(24, 28, 24, 28));

        JPanel captions = new JPanel();
        captions.setLayout(new BoxLayout(captions, BoxLayout.Y_AXIS));
        captions.setOpaque(false);
        JLabel heading = UI.title(title);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        captions.add(heading);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel sub = UI.muted(subtitle);
            sub.setAlignmentX(LEFT_ALIGNMENT);
            sub.setBorder(UI.padding(4, 0, 0, 0));
            captions.add(sub);
        }

        header.setOpaque(false);
        header.setBorder(UI.padding(0, 0, 20, 0));
        header.add(captions, BorderLayout.WEST);

        content.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    /** Puts the screen's own controls into the area below the heading. */
    protected void setBody(JComponent body) {
        content.removeAll();
        content.add(body, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    /** Buttons that sit on the right of the heading, such as New or Print. */
    protected void setHeaderActions(JComponent actions) {
        header.add(actions, BorderLayout.EAST);
    }

    /** Called every time the screen is brought to the front; reload data here. */
    public void onShow() {
    }

    public String getTitle() {
        return title;
    }
}
