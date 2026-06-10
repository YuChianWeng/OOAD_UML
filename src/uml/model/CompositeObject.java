package uml.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uml.util.GeomUtils;

/**
 * A composite (grouped) graphic object that contains one or more child GraphicObjects.
 *
 * Children may be BasicObjects or other CompositeObjects — nesting is fully supported.
 * The composite has no position of its own: its bounding box is derived from its
 * children, and moving it means translating all children by the same delta.
 *
 * Responsibilities (Principle IV):
 *   - Store the ordered list of child GraphicObjects.
 *   - Derive getBoundingBox() dynamically as the union of all children's boxes.
 *   - Render children first, then draw a dashed outline around the computed bbox.
 *   - Implement moveTo(newX, newY) in absolute coordinates, propagating the delta
 *     to every child (BasicObjects and nested CompositeObjects alike).
 *
 * Design notes:
 *   - CompositeObject is NOT resizable (no resize() method — spec FR-032).
 *   - Links belong to BasicObjects only; this class never appears in a Link endpoint.
 *   - getLabel() returns "" and getLabelColor() returns LIGHT_GRAY (unused, but required
 *     by GraphicObject contract).
 */
public class CompositeObject extends GraphicObject {

    /** Ordered child list; reflects the z-order within the group. */
    private final List<GraphicObject> children;

    /**
     * Create a composite from the given list of children.
     *
     * A defensive copy is made so the caller's list and the composite's list
     * remain independent.  Requires at least 2 children (enforced by
     * {@link DiagramModel#group} before this constructor is called).
     *
     * @param children the objects to group; must not be null or empty
     */
    public CompositeObject(List<GraphicObject> children) {
        this.children = new ArrayList<>(children);
    }

    // ── GraphicObject contract ────────────────────────────────────────────────

    /**
     * Bounding box = union of every child's bounding box.
     *
     * Computed dynamically on each call so the box always tracks the current
     * positions of all descendants (including deeply nested composites).
     * Returns a zero-size rectangle at (0,0) only if the children list is
     * empty, which the constructor invariant prevents in normal use.
     */
    @Override
    public Rectangle getBoundingBox() {
        Rectangle bbox = null;
        for (GraphicObject child : children) {
            Rectangle cb = child.getBoundingBox();
            bbox = (bbox == null) ? new Rectangle(cb) : GeomUtils.union(bbox, cb);
        }
        return (bbox != null) ? bbox : new Rectangle(0, 0, 0, 0);
    }

    /** A point is inside the composite if it falls within its bounding box. */
    @Override
    public boolean contains(Point p) {
        return getBoundingBox().contains(p);
    }

    /**
     * Draw all children in order, then overlay a dashed bounding-box outline.
     *
     * The dashed outline serves as the composite's visual identity:
     * it signals grouping without obscuring the children inside.
     */
    @Override
    public void draw(Graphics2D g) {
        // 1. Paint each child (preserves their individual render styles)
        for (GraphicObject child : children) {
            child.draw(g);
        }

        // 2. Dashed bounding-box outline on top
        Rectangle bbox = getBoundingBox();
        Stroke saved = g.getStroke();
        g.setStroke(new BasicStroke(
            1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10f, new float[]{6f, 4f}, 0f
        ));
        g.setColor(Color.DARK_GRAY);
        g.drawRect(bbox.x, bbox.y, bbox.width, bbox.height);
        g.setStroke(saved);
    }

    /** Composites carry no user-visible label. */
    @Override
    public String getLabel() {
        return "";
    }

    /** Unused fill colour — satisfies the GraphicObject contract. */
    @Override
    public Color getLabelColor() {
        return Color.LIGHT_GRAY;
    }

    // ── Movement ──────────────────────────────────────────────────────────────

    /**
     * Move the composite to a new absolute top-left position.
     *
     * Because CompositeObject has no stored x/y, this method:
     *   1. Computes the current bounding-box origin (bbox.x, bbox.y).
     *   2. Derives the delta: dx = newX − bbox.x, dy = newY − bbox.y.
     *   3. Applies the delta to each child recursively:
     *      - BasicObject  → absolute moveTo(child.bbox.x + dx, child.bbox.y + dy)
     *      - CompositeObject → recursive moveTo using the same absolute-coord API
     *
     * This mirrors BasicObject.moveTo's absolute-coordinate contract, so
     * SelectMode can call moveTo on both types with identical arguments.
     *
     * @param newX new left edge of the composite bounding box
     * @param newY new top edge of the composite bounding box
     */
    public void moveTo(int newX, int newY) {
        Rectangle bbox = getBoundingBox();
        int dx = newX - bbox.x;
        int dy = newY - bbox.y;
        moveBy(dx, dy);
    }

    /**
     * Translate all children by {@code (dx, dy)} pixels.
     *
     * Used internally by {@link #moveTo} and by nested CompositeObject recursion.
     * Kept package-private so DiagramModel can call it if needed, but external
     * callers should always use the absolute {@link #moveTo} API.
     */
    void moveBy(int dx, int dy) {
        for (GraphicObject child : children) {
            if (child instanceof BasicObject) {
                BasicObject bo = (BasicObject) child;
                Rectangle bb = bo.getBoundingBox();
                bo.moveTo(bb.x + dx, bb.y + dy);
            } else if (child instanceof CompositeObject) {
                ((CompositeObject) child).moveBy(dx, dy);
            }
        }
    }

    // ── Children access ───────────────────────────────────────────────────────

    /**
     * Return an unmodifiable view of the direct children of this composite.
     *
     * Used by {@link DiagramModel#ungroup} to restore children to the top-level
     * object list when the composite is dissolved.
     *
     * @return unmodifiable list; never null; contains at least 2 elements
     */
    public List<GraphicObject> getChildren() {
        return Collections.unmodifiableList(children);
    }
}
