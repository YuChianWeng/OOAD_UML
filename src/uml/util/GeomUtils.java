package uml.util;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Stateless geometry helpers used across tool modes and model operations.
 *
 * All methods are pure functions; no state is stored (Principle II).
 */
public final class GeomUtils {

    private GeomUtils() {}

    /**
     * Return the index of the port in {@code ports} whose centre is closest to
     * {@code p}, measured by squared Euclidean distance (avoids sqrt).
     *
     * Used by link modes to snap a press/release point to the nearest port on a
     * BasicObject, and by SelectMode to detect which port handle was dragged.
     */
    public static int nearestPortIndex(List<Point> ports, Point p) {
        int  best     = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < ports.size(); i++) {
            long dx   = ports.get(i).x - p.x;
            long dy   = ports.get(i).y - p.y;
            long dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best     = i;
            }
        }
        return best;
    }

    /**
     * Return the minimum enclosing rectangle of {@code a} and {@code b}.
     * Delegates to {@link Rectangle#union} which handles empty rectangles correctly.
     */
    public static Rectangle union(Rectangle a, Rectangle b) {
        return a.union(b);
    }

    /**
     * Return a Rectangle with non-negative width and height regardless of
     * which corner was the drag-start and which was the drag-end.
     *
     * Used by RectMode, OvalMode, and SelectMode (drag-select) so that dragging
     * in any direction always produces a valid, paint-able bounding box.
     */
    public static Rectangle normalizeRect(Point p1, Point p2) {
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        int w = Math.abs(p2.x - p1.x);
        int h = Math.abs(p2.y - p1.y);
        return new Rectangle(x, y, w, h);
    }

    /**
     * Build corner-resize bounds while keeping the opposite corner stable.
     *
     * Use Case F.2 allows the dragged port to cross over the opposite anchor;
     * Use Case F.3 requires a visible minimum size.  The minimum is applied
     * before choosing the new origin, so when the cursor crosses the anchor the
     * rectangle grows away from that anchor instead of visually drifting past it.
     */
    public static Rectangle anchoredResizeBounds(Point anchor, Point cursor,
                                                  int minWidth, int minHeight) {
        int w = Math.max(minWidth, Math.abs(cursor.x - anchor.x));
        int h = Math.max(minHeight, Math.abs(cursor.y - anchor.y));
        int x = (cursor.x < anchor.x) ? anchor.x - w : anchor.x;
        int y = (cursor.y < anchor.y) ? anchor.y - h : anchor.y;
        return new Rectangle(x, y, w, h);
    }

    /**
     * Build horizontal edge-resize bounds with a fixed opposite vertical edge.
     * The fixed Y/height axis is preserved exactly for oval left/right ports and
     * rectangle middle-left/middle-right ports.
     */
    public static Rectangle anchoredHorizontalResizeBounds(int anchorX, int cursorX,
                                                            int fixedY, int fixedHeight,
                                                            int minWidth) {
        int w = Math.max(minWidth, Math.abs(cursorX - anchorX));
        int x = (cursorX < anchorX) ? anchorX - w : anchorX;
        return new Rectangle(x, fixedY, w, fixedHeight);
    }

    /**
     * Build vertical edge-resize bounds with a fixed opposite horizontal edge.
     * The fixed X/width axis is preserved exactly for oval top/bottom ports and
     * rectangle top-middle/bottom-middle ports.
     */
    public static Rectangle anchoredVerticalResizeBounds(int anchorY, int cursorY,
                                                          int fixedX, int fixedWidth,
                                                          int minHeight) {
        int h = Math.max(minHeight, Math.abs(cursorY - anchorY));
        int y = (cursorY < anchorY) ? anchorY - h : anchorY;
        return new Rectangle(fixedX, y, fixedWidth, h);
    }
}
