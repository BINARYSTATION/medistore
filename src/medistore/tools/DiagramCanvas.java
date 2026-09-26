package medistore.tools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * A small drawing surface for the diagrams that go into the project report.
 *
 * The diagrams are drawn in code rather than in a drawing package so they can be
 * regenerated the moment the database design changes, and so they come out at
 * print resolution every time. All coordinates are given in ordinary units and
 * scaled up on the way to the image.
 */
class DiagramCanvas {

    /** Everything is drawn at three times size so the images stay sharp in print. */
    private static final double SCALE = 3.0;

    static final Color INK        = new Color(0x1E, 0x29, 0x3B);
    static final Color LINE       = new Color(0x47, 0x55, 0x69);
    static final Color PROCESS    = new Color(0xCC, 0xFB, 0xF1);
    static final Color PROCESS_ED = new Color(0x0D, 0x94, 0x88);
    static final Color ENTITY     = new Color(0xE0, 0xE7, 0xFF);
    static final Color ENTITY_ED  = new Color(0x46, 0x4F, 0xC7);
    static final Color STORE      = new Color(0xFE, 0xF3, 0xC7);
    static final Color STORE_ED   = new Color(0xD9, 0x77, 0x06);
    static final Color TABLE_HEAD = new Color(0x0F, 0x76, 0x6E);
    static final Color WHITE      = Color.WHITE;

    private final BufferedImage image;
    private final Graphics2D g;
    private final int width;
    private final int height;

    DiagramCanvas(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage((int) (width * SCALE), (int) (height * SCALE),
                BufferedImage.TYPE_INT_RGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.scale(SCALE, SCALE);
        g.setStroke(stroke(1.4f));
    }

    static BasicStroke stroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    static Font font(int size, int style) {
        return new Font("Helvetica Neue", style, size);
    }

    /** The caption printed across the top of every diagram. */
    void heading(String text) {
        g.setColor(INK);
        g.setFont(font(20, Font.BOLD));
        centreText(text, width / 2.0, 34);
    }

    void subheading(String text) {
        g.setColor(LINE);
        g.setFont(font(12, Font.PLAIN));
        centreText(text, width / 2.0, 54);
    }

    /** A square-cornered box: an external entity in a data flow diagram. */
    void box(double x, double y, double w, double h, String label, Color fill, Color edge) {
        g.setColor(fill);
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(edge);
        g.setStroke(stroke(1.6f));
        g.draw(new Rectangle2D.Double(x, y, w, h));
        g.setColor(INK);
        g.setFont(font(12, Font.BOLD));
        centreWrapped(label, x + w / 2, y + h / 2, w - 12);
    }

    /** A rounded box: a process in a data flow diagram. */
    void process(double x, double y, double w, double h, String number, String label) {
        g.setColor(PROCESS);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 22, 22));
        g.setColor(PROCESS_ED);
        g.setStroke(stroke(1.8f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 22, 22));

        g.setColor(PROCESS_ED);
        g.setFont(font(12, Font.BOLD));
        centreText(number, x + w / 2, y + 20);

        g.setColor(INK);
        g.setFont(font(12, Font.BOLD));
        centreWrapped(label, x + w / 2, y + h / 2 + 6, w - 14);
    }

    /** An open-ended bar: a data store in a data flow diagram. */
    void store(double x, double y, double w, double h, String id, String label) {
        g.setColor(STORE);
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(STORE_ED);
        g.setStroke(stroke(1.6f));
        g.draw(new Line2D.Double(x, y, x + w, y));
        g.draw(new Line2D.Double(x, y + h, x + w, y + h));
        g.draw(new Line2D.Double(x + 30, y, x + 30, y + h));

        g.setColor(STORE_ED);
        g.setFont(font(12, Font.BOLD));
        centreText(id, x + 15, y + h / 2 + 4);
        g.setColor(INK);
        g.setFont(font(11, Font.PLAIN));
        centreWrapped(label, x + 30 + (w - 30) / 2, y + h / 2 + 4, w - 42);
    }

    /** A diamond: a relationship in an entity relationship diagram. */
    void diamond(double cx, double cy, double w, double h, String label) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(cx, cy - h / 2);
        shape.lineTo(cx + w / 2, cy);
        shape.lineTo(cx, cy + h / 2);
        shape.lineTo(cx - w / 2, cy);
        shape.closePath();
        g.setColor(new Color(0xFC, 0xE7, 0xF3));
        g.fill(shape);
        g.setColor(new Color(0xBE, 0x18, 0x5D));
        g.setStroke(stroke(1.5f));
        g.draw(shape);
        g.setColor(INK);
        g.setFont(font(11, Font.BOLD));
        centreText(label, cx, cy + 4);
    }

    /** An ellipse: an attribute in an entity relationship diagram, underlined when it is the key. */
    void attribute(double cx, double cy, double w, double h, String label, boolean key) {
        Ellipse2D shape = new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h);
        g.setColor(new Color(0xF0, 0xFD, 0xF4));
        g.fill(shape);
        g.setColor(new Color(0x15, 0x80, 0x3D));
        g.setStroke(stroke(1.2f));
        g.draw(shape);
        g.setColor(INK);
        g.setFont(font(10, Font.PLAIN));
        FontMetrics fm = g.getFontMetrics();
        double textX = cx - fm.stringWidth(label) / 2.0;
        double baseline = cy + 3.5;
        g.drawString(label, (float) textX, (float) baseline);
        if (key) {
            g.setStroke(stroke(0.9f));
            g.draw(new Line2D.Double(textX, baseline + 2, textX + fm.stringWidth(label), baseline + 2));
        }
    }

    /** Moves everything drawn after this call, e.g. to close up space under the heading. */
    void shift(double dx, double dy) {
        g.translate(dx, dy);
    }

    /** A plain line between two points. */
    void line(double x1, double y1, double x2, double y2) {
        g.setColor(LINE);
        g.setStroke(stroke(1.3f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /** A line with an arrowhead, optionally captioned with the data that flows along it. */
    void arrow(double x1, double y1, double x2, double y2, String label) {
        arrow(x1, y1, x2, y2, label, 0.5);
    }

    /**
     * Same, with the caption placed {@code along} of the way down the line
     * (0 at the tail, 1 at the head) so crowded arrows can keep their captions apart.
     */
    void arrow(double x1, double y1, double x2, double y2, String label, double along) {
        g.setColor(LINE);
        g.setStroke(stroke(1.3f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        arrowHead(x1, y1, x2, y2);
        if (label != null && !label.isBlank()) {
            g.setColor(new Color(0x33, 0x41, 0x55));
            g.setFont(font(10, Font.PLAIN));
            double mx = x1 + (x2 - x1) * along;
            double my = y1 + (y2 - y1) * along;
            labelOnLine(label, mx, my);
        }
    }

    private void arrowHead(double x1, double y1, double x2, double y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double size = 9;
        Path2D head = new Path2D.Double();
        head.moveTo(x2, y2);
        head.lineTo(x2 - size * Math.cos(angle - Math.PI / 7),
                    y2 - size * Math.sin(angle - Math.PI / 7));
        head.lineTo(x2 - size * Math.cos(angle + Math.PI / 7),
                    y2 - size * Math.sin(angle + Math.PI / 7));
        head.closePath();
        g.fill(head);
    }

    /** Draws the caption on a white pad so the line does not run through the words. */
    private void labelOnLine(String text, double cx, double cy) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(cx - w / 2.0 - 3, cy - 8, w + 6, 14));
        g.setColor(new Color(0x33, 0x41, 0x55));
        g.drawString(text, (float) (cx - w / 2.0), (float) (cy + 3));
    }

    /** A cardinality marker such as 1 or M placed beside a relationship line. */
    void cardinality(String text, double x, double y) {
        g.setColor(new Color(0xBE, 0x18, 0x5D));
        g.setFont(font(11, Font.BOLD));
        centreText(text, x, y);
    }

    /**
     * A table in the relational schema diagram: a coloured caption bar above a
     * list of columns, with the key columns marked.
     */
    void tableBox(double x, double y, double w, String name, String[] columns) {
        double headerHeight = 26;
        double rowHeight = 17;
        double h = headerHeight + columns.length * rowHeight;

        g.setColor(new Color(0, 0, 0, 22));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 3, w, h, 8, 8));

        g.setColor(WHITE);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 8, 8));

        g.setColor(TABLE_HEAD);
        g.fill(new RoundRectangle2D.Double(x, y, w, headerHeight, 8, 8));
        g.fill(new Rectangle2D.Double(x, y + headerHeight - 8, w, 8));

        g.setColor(WHITE);
        g.setFont(font(12, Font.BOLD));
        g.drawString(name, (float) (x + 10), (float) (y + 18));

        double textY = y + headerHeight + 12;
        for (String column : columns) {
            boolean primary = column.startsWith("PK ");
            boolean foreign = column.startsWith("FK ");
            String text = primary || foreign ? column.substring(3) : column;

            if (primary || foreign) {
                g.setColor(primary ? new Color(0xB4, 0x53, 0x09) : new Color(0x46, 0x4F, 0xC7));
                g.setFont(font(8, Font.BOLD));
                g.drawString(primary ? "PK" : "FK", (float) (x + 8), (float) textY);
            }
            g.setColor(INK);
            g.setFont(font(10, primary ? Font.BOLD : Font.PLAIN));
            g.drawString(text, (float) (x + 28), (float) textY);
            textY += rowHeight;
        }

        g.setColor(new Color(0xCB, 0xD5, 0xE1));
        g.setStroke(stroke(1.1f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 8, 8));
    }

    /** Height a table box will occupy, so callers can lay the page out. */
    static double tableHeight(int columnCount) {
        return 26 + columnCount * 17;
    }

    /** A one-to-many connector drawn in crow's foot notation. */
    void relation(double x1, double y1, double x2, double y2) {
        g.setColor(ENTITY_ED);
        g.setStroke(stroke(1.4f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));

        // The single end carries a bar, the many end carries the three-pronged foot.
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double bx = x1 + 11 * Math.cos(angle);
        double by = y1 + 11 * Math.sin(angle);
        g.draw(new Line2D.Double(bx + 5 * Math.sin(angle), by - 5 * Math.cos(angle),
                                 bx - 5 * Math.sin(angle), by + 5 * Math.cos(angle)));

        double fx = x2 - 11 * Math.cos(angle);
        double fy = y2 - 11 * Math.sin(angle);
        g.draw(new Line2D.Double(fx, fy, x2, y2));
        g.draw(new Line2D.Double(fx, fy, x2 + 6 * Math.sin(angle), y2 - 6 * Math.cos(angle)));
        g.draw(new Line2D.Double(fx, fy, x2 - 6 * Math.sin(angle), y2 + 6 * Math.cos(angle)));
    }

    /** A small colour key so the reader knows what each shape means. */
    void legend(double x, double y, String[][] entries) {
        g.setFont(font(10, Font.PLAIN));
        double cy = y;
        for (String[] entry : entries) {
            Color fill = Color.decode(entry[1]);
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(x, cy - 8, 16, 11, 4, 4));
            g.setColor(LINE);
            g.setStroke(stroke(1.1f));
            g.draw(new RoundRectangle2D.Double(x, cy - 8, 16, 11, 4, 4));
            g.setColor(INK);
            g.drawString(entry[0], (float) (x + 23), (float) cy);
            cy += 17;
        }
    }

    private void centreText(String text, double cx, double baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) baselineY);
    }

    /** Centres a caption, breaking it over several lines when it will not fit. */
    private void centreWrapped(String text, double cx, double centreY, double maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String attempt = current.isEmpty() ? word : current + " " + word;
            if (fm.stringWidth(attempt) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(attempt);
            }
        }
        lines.add(current.toString());

        double lineHeight = fm.getHeight() - 2;
        double y = centreY - (lines.size() - 1) * lineHeight / 2;
        for (String line : lines) {
            g.drawString(line, (float) (cx - fm.stringWidth(line) / 2.0), (float) y);
            y += lineHeight;
        }
    }

    void save(String path) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            g.dispose();
            ImageIO.write(image, "png", file);
            System.out.println("  wrote " + path + "  (" + image.getWidth() + "x"
                    + image.getHeight() + ")");
        } catch (IOException e) {
            throw new IllegalStateException("Could not write " + path, e);
        }
    }
}
