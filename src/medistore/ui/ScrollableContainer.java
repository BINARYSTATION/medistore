package medistore.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A panel that stretches to fill its scroll viewport like a plain panel, and only
 * lets a scrollbar appear when the viewport is too small for it (small screens).
 */
public class ScrollableContainer extends JPanel implements Scrollable {

    private final int unitIncrement;

    public ScrollableContainer(int unitIncrement) {
        this.unitIncrement = unitIncrement;
    }

    public ScrollableContainer(LayoutManager layout, int unitIncrement) {
        super(layout);
        this.unitIncrement = unitIncrement;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return unitIncrement;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    // Fill the viewport when it is at least as big as either the reference size or the
    // content's own preferred size; otherwise keep the preferred size and scroll.
    @Override
    public boolean getScrollableTracksViewportWidth() {
        Component viewport = getParent();
        return viewport == null
                || viewport.getWidth() >= Math.min(trackingReferenceSize().width, getPreferredSize().width);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        Component viewport = getParent();
        return viewport == null
                || viewport.getHeight() >= Math.min(trackingReferenceSize().height, getPreferredSize().height);
    }

    /**
     * The size above which the viewport is always filled. A panel holding a JTable
     * reports an oversized preferred size (JTable's default 450x400 viewport), so such
     * a panel overrides this with the size it really needs.
     */
    protected Dimension trackingReferenceSize() {
        return getPreferredSize();
    }
}
