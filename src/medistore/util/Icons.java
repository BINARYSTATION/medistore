package medistore.util;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * The navigation icons, drawn with Java2D instead of loaded from image files.
 *
 * Drawing them keeps the project to a single folder with no image assets to
 * lose, and they stay sharp at any size on any screen. Unicode symbols were the
 * other option but they render differently on every machine.
 */
public final class Icons {

    public enum Glyph { GRID, PILL, BOX, PERSON, CART, TRUCK, CHART, CLOCK, PAGE, KEY, EXIT }

    private Icons() {
    }

    public static Icon of(Glyph glyph, int size, Color colour) {
        return new GlyphIcon(glyph, size, colour);
    }

    private static final class GlyphIcon implements Icon {
        private final Glyph glyph;
        private final int size;
        private final Color colour;

        GlyphIcon(Glyph glyph, int size, Color colour) {
            this.glyph = glyph;
            this.size = size;
            this.colour = colour;
        }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            // Every glyph below is drawn on a 24x24 grid and then scaled to fit.
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(colour);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2);
            g2.dispose();
        }

        private void draw(Graphics2D g2) {
            switch (glyph) {
                case GRID -> {
                    g2.fill(new RoundRectangle2D.Double(3, 3, 8, 8, 2, 2));
                    g2.fill(new RoundRectangle2D.Double(13, 3, 8, 8, 2, 2));
                    g2.fill(new RoundRectangle2D.Double(3, 13, 8, 8, 2, 2));
                    g2.fill(new RoundRectangle2D.Double(13, 13, 8, 8, 2, 2));
                }
                case PILL -> {
                    AffineTransform old = g2.getTransform();
                    g2.rotate(Math.toRadians(-45), 12, 12);
                    g2.fill(new RoundRectangle2D.Double(4, 8.5, 16, 7, 7, 7));
                    g2.setColor(new Color(255, 255, 255, 140));
                    g2.fill(new Rectangle2D.Double(11.5, 8.5, 1.6, 7));
                    g2.setTransform(old);
                }
                case BOX -> {
                    g2.draw(new Polygon(
                            new int[]{12, 21, 12, 3}, new int[]{3, 8, 13, 8}, 4));
                    g2.draw(new Line2D.Double(3, 8, 3, 17));
                    g2.draw(new Line2D.Double(21, 8, 21, 17));
                    g2.draw(new Line2D.Double(3, 17, 12, 22));
                    g2.draw(new Line2D.Double(21, 17, 12, 22));
                    g2.draw(new Line2D.Double(12, 13, 12, 22));
                }
                case PERSON -> {
                    g2.fill(new Ellipse2D.Double(8, 3, 8, 8));
                    g2.fill(new Arc2D.Double(3.5, 13, 17, 18, 0, 180, Arc2D.PIE));
                }
                case CART -> {
                    g2.draw(new Line2D.Double(2, 4, 5, 4));
                    g2.draw(new Polygon(
                            new int[]{5, 22, 19, 8}, new int[]{4, 7, 15, 15}, 4));
                    g2.fill(new Ellipse2D.Double(8, 18, 4, 4));
                    g2.fill(new Ellipse2D.Double(16, 18, 4, 4));
                }
                case TRUCK -> {
                    g2.draw(new RoundRectangle2D.Double(2, 7, 12, 9, 1.5, 1.5));
                    g2.draw(new Polygon(
                            new int[]{14, 19, 22, 22, 14}, new int[]{10, 10, 13, 16, 16}, 5));
                    g2.fill(new Ellipse2D.Double(5, 15, 4.5, 4.5));
                    g2.fill(new Ellipse2D.Double(15, 15, 4.5, 4.5));
                }
                case CHART -> {
                    g2.fill(new RoundRectangle2D.Double(3, 13, 4.5, 8, 1.5, 1.5));
                    g2.fill(new RoundRectangle2D.Double(9.75, 8, 4.5, 13, 1.5, 1.5));
                    g2.fill(new RoundRectangle2D.Double(16.5, 3, 4.5, 18, 1.5, 1.5));
                }
                case CLOCK -> {
                    g2.draw(new Ellipse2D.Double(3, 3, 18, 18));
                    g2.draw(new Line2D.Double(12, 7.5, 12, 12));
                    g2.draw(new Line2D.Double(12, 12, 16, 14.5));
                }
                case PAGE -> {
                    g2.draw(new Polygon(
                            new int[]{5, 14, 19, 19, 5}, new int[]{2, 2, 7, 22, 22}, 5));
                    g2.draw(new Line2D.Double(14, 2, 14, 7));
                    g2.draw(new Line2D.Double(14, 7, 19, 7));
                    g2.draw(new Line2D.Double(8.5, 12, 15.5, 12));
                    g2.draw(new Line2D.Double(8.5, 16, 15.5, 16));
                }
                case KEY -> {
                    g2.draw(new Ellipse2D.Double(3, 8, 9, 9));
                    g2.draw(new Line2D.Double(11.5, 12.5, 21, 12.5));
                    g2.draw(new Line2D.Double(18, 12.5, 18, 16.5));
                    g2.draw(new Line2D.Double(21, 12.5, 21, 15.5));
                }
                case EXIT -> {
                    g2.draw(new Polygon(
                            new int[]{13, 4, 4, 13}, new int[]{3, 3, 21, 21}, 4));
                    g2.draw(new Line2D.Double(10, 12, 21, 12));
                    g2.draw(new Line2D.Double(17.5, 8.5, 21, 12));
                    g2.draw(new Line2D.Double(17.5, 15.5, 21, 12));
                }
            }
        }
    }
}
