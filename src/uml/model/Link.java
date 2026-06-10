package uml.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;

/**
 * A directed UML relationship between two BasicObjects.
 *
 * Owns the link type and its two endpoints as (object + port-index) pairs.
 * Endpoints are re-derived each frame in draw() by calling
 * source/target.getPorts() — this is the lazy-update contract:
 * when a connected object moves or resizes, its getPorts() values change
 * and the next repaint picks up the new position automatically with no
 * explicit listener or notification required (Phase 7 / T049).
 *
 * Responsibilities (Principle IV):
 *   - Store type, source, sourcePortIndex, target, targetPortIndex.
 *   - Render the shaft line and the correct type-specific arrowhead.
 *
 * Arrowhead styles (T031):
 *   ASSOCIATION    — open two-line chevron at the target end
 *   GENERALIZATION — hollow equilateral triangle at the target end (superclass side)
 *   COMPOSITION    — filled black diamond at the source end (owner side)
 */
public class Link {

    private final LinkType    type;
    private final BasicObject source;
    private final int         sourcePortIndex;
    private final BasicObject target;
    private final int         targetPortIndex;

    public Link(LinkType type,
                BasicObject source, int sourcePortIndex,
                BasicObject target, int targetPortIndex) {
        this.type            = type;
        this.source          = source;
        this.sourcePortIndex = sourcePortIndex;
        this.target          = target;
        this.targetPortIndex = targetPortIndex;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public LinkType    getType()            { return type; }
    public BasicObject getSource()          { return source; }
    public int         getSourcePortIndex() { return sourcePortIndex; }
    public BasicObject getTarget()          { return target; }
    public int         getTargetPortIndex() { return targetPortIndex; }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Draw the shaft and arrowhead onto {@code g}.
     *
     * Lazy-update contract: startPt and endPt are derived from
     * source.getPorts().get(sourcePortIndex) and target.getPorts().get(targetPortIndex)
     * on every call, so moved or resized objects automatically update the link
     * position on the next repaint — no mutation of this object is ever needed.
     */
    public void draw(Graphics2D g) {
        Point sp = source.getPorts().get(sourcePortIndex);
        Point ep = target.getPorts().get(targetPortIndex);

        double dx  = ep.x - sp.x;
        double dy  = ep.y - sp.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return; // degenerate: source and target port are identical

        // Unit vector along the shaft (source → target direction)
        double ux = dx / len;
        double uy = dy / len;
        // Perpendicular unit vector (90° CCW rotation)
        double px = -uy;
        double py =  ux;

        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Color.BLACK);

        // Draw the shaft first; arrowhead shapes are drawn on top
        g.drawLine(sp.x, sp.y, ep.x, ep.y);

        switch (type) {
            case ASSOCIATION:
                drawOpenArrow(g, ep, ux, uy, px, py);
                break;
            case GENERALIZATION:
                drawHollowTriangle(g, ep, ux, uy, px, py);
                break;
            case COMPOSITION:
                drawFilledDiamond(g, sp, ux, uy, px, py);
                break;
        }

        g.setStroke(saved);
    }

    /**
     * ASSOCIATION — open two-line chevron at {@code tip} (target end).
     *
     * Two wing lines radiate from {@code tip} back along the shaft (~30° angle).
     * No fill; purely a line drawing.
     */
    private void drawOpenArrow(Graphics2D g, Point tip,
                               double ux, double uy, double px, double py) {
        // Back point: 15 px from tip along shaft toward source
        int backX = (int) Math.round(tip.x - 15 * ux);
        int backY = (int) Math.round(tip.y - 15 * uy);
        // Wing points: ±7 px perpendicular from back point
        int w1x = (int) Math.round(backX + 7 * px);
        int w1y = (int) Math.round(backY + 7 * py);
        int w2x = (int) Math.round(backX - 7 * px);
        int w2y = (int) Math.round(backY - 7 * py);
        g.setColor(Color.BLACK);
        g.drawLine(tip.x, tip.y, w1x, w1y);
        g.drawLine(tip.x, tip.y, w2x, w2y);
    }

    /**
     * GENERALIZATION — hollow equilateral triangle at {@code tip} (superclass / target end).
     *
     * Tip points at the target port.  The base is 15 px back along the shaft.
     * The interior is filled white so the shaft line is visually erased inside the triangle.
     */
    private void drawHollowTriangle(Graphics2D g, Point tip,
                                    double ux, double uy, double px, double py) {
        // Base centre: 15 px back from tip along shaft
        int baseX = (int) Math.round(tip.x - 15 * ux);
        int baseY = (int) Math.round(tip.y - 15 * uy);
        // Two base corners: ±8 px perpendicular from base centre
        int b1x = (int) Math.round(baseX + 8 * px);
        int b1y = (int) Math.round(baseY + 8 * py);
        int b2x = (int) Math.round(baseX - 8 * px);
        int b2y = (int) Math.round(baseY - 8 * py);

        Polygon tri = new Polygon();
        tri.addPoint(tip.x, tip.y); // tip toward target
        tri.addPoint(b1x,   b1y);   // base corner 1
        tri.addPoint(b2x,   b2y);   // base corner 2

        // White fill first erases the shaft inside the triangle
        g.setColor(Color.WHITE);
        g.fillPolygon(tri);
        // Black outline for the visible hollow-triangle style
        g.setColor(Color.BLACK);
        g.drawPolygon(tri);
    }

    /**
     * COMPOSITION — filled black diamond at {@code base} (source / owner end).
     *
     * The diamond is 16 px long (along the shaft) and 12 px wide (perpendicular).
     * The back tip sits at the source port; the front tip points toward the target.
     * Black fill ensures the shaft segment inside the diamond is not visible.
     */
    private void drawFilledDiamond(Graphics2D g, Point base,
                                   double ux, double uy, double px, double py) {
        // Midpoint of the diamond (8 px from base tip toward target)
        int midX  = (int) Math.round(base.x + 8 * ux);
        int midY  = (int) Math.round(base.y + 8 * uy);
        // Front tip of the diamond (16 px from base tip)
        int frontX = (int) Math.round(base.x + 16 * ux);
        int frontY = (int) Math.round(base.y + 16 * uy);
        // Side points: ±6 px perpendicular from midpoint
        int s1x = (int) Math.round(midX + 6 * px);
        int s1y = (int) Math.round(midY + 6 * py);
        int s2x = (int) Math.round(midX - 6 * px);
        int s2y = (int) Math.round(midY - 6 * py);

        Polygon diamond = new Polygon();
        diamond.addPoint(base.x, base.y); // back tip (at source port)
        diamond.addPoint(s1x,    s1y);    // side 1
        diamond.addPoint(frontX, frontY); // front tip (toward target)
        diamond.addPoint(s2x,    s2y);    // side 2

        g.setColor(Color.BLACK);
        g.fillPolygon(diamond);
    }
}
