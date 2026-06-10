package uml.model;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Abstract base for all single (non-composite) graphic objects.
 *
 * Owns the positional state (x, y, width, height) and label state (text + fill colour).
 * Concrete subclasses (RectObject, OvalObject) supply only draw() and getPorts().
 *
 * Responsibilities (Principle IV):
 *   - Store and expose position / size.
 *   - Implement generic hit-testing via bounding box.
 *   - Enforce the 20×20 px minimum-size invariant on resize.
 *   - Declare getPorts() so the port-rendering and link-anchor logic can be
 *     applied polymorphically by CanvasPanel and Link respectively.
 */
public abstract class BasicObject extends GraphicObject {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected String label      = "";
    protected Color  labelColor = Color.WHITE;

    protected BasicObject(int x, int y, int width, int height) {
        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;
    }

    /** Convenience constructor used by RectMode / OvalMode after drag normalization. */
    protected BasicObject(Rectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    // ── GraphicObject contract ────────────────────────────────────────────────

    @Override
    public Rectangle getBoundingBox() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public boolean contains(Point p) {
        return getBoundingBox().contains(p);
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public Color getLabelColor() {
        return labelColor;
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    public void setLabel(String label) {
        this.label = (label != null) ? label : "";
    }

    public void setLabelColor(Color color) {
        this.labelColor = (color != null) ? color : Color.WHITE;
    }

    /**
     * Move the top-left corner to the given absolute canvas coordinates.
     * Width and height are unchanged.
     */
    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    /**
     * Apply a new bounding rectangle, clamping width and height to at least 20 px.
     *
     * Clamping invariant: a BasicObject must always occupy at least 20×20 px so
     * that port handles remain interactable and labels remain legible at minimum size.
     *
     * The top-left corner ({@code r.x}, {@code r.y}) is applied without clamping
     * because SelectMode passes an already-normalised rectangle from
     * {@link uml.util.GeomUtils#normalizeRect}: when the user drags past the
     * opposite corner the anchor and cursor swap roles, so the origin is always
     * the smaller coordinate and the width/height are always non-negative before
     * this method is called.  The clamp here is a final safety net that prevents
     * the shape from collapsing below one visible port square.
     */
    public void resize(Rectangle r) {
        this.x      = r.x;
        this.y      = r.y;
        this.width  = Math.max(20, r.width);   // minimum 20 px wide
        this.height = Math.max(20, r.height);  // minimum 20 px tall
    }

    // ── Ports (implemented per shape) ─────────────────────────────────────────

    /**
     * Return the port anchor points for this object.
     * Used by CanvasPanel to draw port handles and by Link to compute endpoints.
     * Delegated to PortUtils by concrete subclasses.
     */
    public abstract List<Point> getPorts();
}
