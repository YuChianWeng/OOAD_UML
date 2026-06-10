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
 * A rectangular shape with 8 connection ports.
 *
 * Responsibility (Principle IV): render a filled, bordered rectangle and
 * expose 8 clockwise port anchor points via PortUtils.
 *
 * Rendering: fill with labelColor → black border → centered black label text.
 * The label is rendered in black regardless of fill color so it stays legible
 * on any background shade chosen by the user (US5 / FR-037).
 */
public class RectObject extends BasicObject {

    public RectObject(Rectangle r) {
        super(r);
    }

    // ── GraphicObject contract ────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g) {
        // 1. Fill interior with label color (white by default)
        g.setColor(labelColor);
        g.fillRect(x, y, width, height);

        // 2. Black border
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, width, height);

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
     * 8-port layout (clockwise from top-left):
     * 0=TL  1=TM  2=TR  3=MR  4=BR  5=BM  6=BL  7=ML
     */
    @Override
    public List<Point> getPorts() {
        return PortUtils.computeRectPorts(getBoundingBox());
    }
}
