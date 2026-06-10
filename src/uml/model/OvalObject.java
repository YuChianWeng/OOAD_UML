package uml.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import uml.util.PortUtils;

/**
 * An elliptical shape with 4 connection ports.
 *
 * Responsibility (Principle IV): render a filled, bordered ellipse and
 * expose 4 clockwise port anchor points via PortUtils.
 *
 * Rendering: fill with labelColor → black border → centered black label text.
 * Identical render contract to RectObject; only the geometry differs.
 */
public class OvalObject extends BasicObject {

    public OvalObject(Rectangle r) {
        super(r);
    }

    // ── GraphicObject contract ────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g) {
        // 1. Fill interior with label color (white by default)
        g.setColor(labelColor);
        g.fillOval(x, y, width, height);

        // 2. Black border
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawOval(x, y, width, height);

        // 3. Centered label — always black so it's legible on any fill
        if (!label.isEmpty()) {
            g.setColor(Color.BLACK);
            FontMetrics fm = g.getFontMetrics();
            int tx = x + (width  - fm.stringWidth(label)) / 2;
            int ty = y + (height + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, tx, ty);
        }
    }

    // ── Ports ─────────────────────────────────────────────────────────────────

    /**
     * 4-port layout (clockwise from top):
     * 0=top  1=right  2=bottom  3=left
     */
    @Override
    public List<Point> getPorts() {
        return PortUtils.computeOvalPorts(getBoundingBox());
    }
}
