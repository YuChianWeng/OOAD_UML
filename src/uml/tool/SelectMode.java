package uml.tool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uml.model.BasicObject;
import uml.model.CompositeObject;
import uml.model.DiagramModel;
import uml.model.GraphicObject;
import uml.ui.CanvasPanel;
import uml.util.GeomUtils;

/**
 * Handles all three sub-gestures of the Select tool (FR-023 priority order):
 *
 *   Priority 1 — PORT HIT:   press on a port handle of a hovered BasicObject
 *                             → resize (Phase 6, T037).
 *   Priority 2 — OBJECT HIT: press inside any GraphicObject
 *                             → move (Phase 4, T025).
 *   Priority 3 — EMPTY:      press on empty canvas
 *                             → drag-select (Phase 7, T041).
 *
 * Responsibilities (Principle IV):
 *   - Own the authoritative selection list.
 *   - Implement move logic (compute offset on press; apply absolute coords on drag).
 *   - Implement resize logic (port-hit detection; opposite-anchor drag normalization).
 *   - Notify CanvasPanel of selection and hover changes via its setter methods;
 *     all rendering decisions stay in CanvasPanel (no Swing in this class).
 */
public class SelectMode implements ToolMode {

    // ── State machine ─────────────────────────────────────────────────────────

    private enum State { IDLE, MOVING, RESIZING, DRAG_SELECTING }

    private State state = State.IDLE;

    // ── Resize hit-detection constant ─────────────────────────────────────────

    /**
     * Squared-distance threshold for port-handle hit detection (FR-023 priority 1).
     *
     * 64 px² → radius of 8 px around each port centre.  The port handle is drawn
     * as an 8×8 square (±4 px from centre), so this gives a comfortable click
     * margin that covers the full square plus a small buffer around it.
     */
    private static final int PORT_RADIUS_SQ = 64;

    // ── Resize gesture fields ─────────────────────────────────────────────────

    /** The BasicObject being resized; valid only while state == RESIZING. */
    private BasicObject resizeTarget;

    /**
     * Canvas position of the diametrically opposite port, captured once at press
     * time and held fixed for the entire drag gesture.
     *
     * This point is the immovable anchor of the resize: for corner ports, both axes
     * of this point are used; for edge-midpoint ports, only one axis is used (the
     * free axis).  Capturing it at press time prevents the anchor from drifting when
     * the 20 px minimum-size clamp is active on the moving side.
     */
    private Point resizeAnchorPoint;

    /**
     * Bounding box of the resize target captured once at press time.
     *
     * Edge-midpoint ports can only change one dimension; the other dimension must
     * remain exactly as it was when the gesture started.  Re-reading the bounds on
     * every drag event would accumulate floating-point drift, so we freeze them here.
     */
    private Rectangle resizeInitialBounds;

    /**
     * Axis constraint for edge-midpoint ports.
     *
     * {@code resizeFreeY} = true  → only the top or bottom edge moves (TM / BM / oval top / oval bottom).
     * {@code resizeFreeX} = true  → only the left or right edge moves (ML / MR / oval left / oval right).
     * Both false                  → corner port; both axes are free.
     *
     * Determined at press time by comparing the pressed-port position with the anchor
     * position: if they share the same X coordinate the port pair is horizontal (Y is
     * free); if they share Y the pair is vertical (X is free).
     */
    private boolean resizeFreeY;
    private boolean resizeFreeX;

    // ── Move gesture fields ───────────────────────────────────────────────────

    /** The object being dragged; valid only while state == MOVING. */
    private GraphicObject moveTarget;

    /**
     * Offset from the object's top-left corner to the mouse press point.
     * Subtracted on each drag event to keep the object anchored under the cursor.
     */
    private Point moveOffset;

    // ── Drag-select gesture fields (T041) ─────────────────────────────────────

    /**
     * Canvas point where the drag-select gesture began.
     * Captured on press (branch 3); combined with the current cursor position
     * each drag event to produce the rubber-band rectangle via GeomUtils.normalizeRect.
     * Valid only while state == DRAG_SELECTING.
     */
    private Point dragStart;

    // ── Selection ─────────────────────────────────────────────────────────────

    /** Canonical selection list — drives both CanvasPanel rendering and menu state. */
    private final List<GraphicObject> selection = new ArrayList<>();

    // ── ToolMode contract ─────────────────────────────────────────────────────

    /**
     * Three-branch priority dispatcher (FR-023).
     *
     * Branch 1 (port hit / resize) — T037: checks ports on the hovered BasicObject.
     * Branch 2 (object hit / move) — T025: hit-tests every object.
     * Branch 3 (empty canvas)      — T025: deselects; drag-select added in Phase 7.
     */
    @Override
    public void onMousePressed(MouseEvent e, DiagramModel model, CanvasPanel canvas) {

        // ── Branch 1: PORT HIT → resize (FR-023 priority 1) ──────────────────
        // Port handles are visible only on the hovered BasicObject, so we check
        // canvas.getHoveredObject() rather than hit-testing the full object list.
        // If the press falls within PORT_RADIUS_SQ of any port centre, we enter
        // RESIZING state immediately and return — the object-hit branch is skipped.
        GraphicObject hovered = canvas.getHoveredObject();
        if (hovered instanceof BasicObject) {
            BasicObject bo = (BasicObject) hovered;
            List<Point> ports = bo.getPorts();
            for (int i = 0; i < ports.size(); i++) {
                Point port = ports.get(i);
                long dx = e.getX() - port.x;
                long dy = e.getY() - port.y;
                if (dx * dx + dy * dy <= PORT_RADIUS_SQ) {
                    // Port hit confirmed — set up resize state.
                    resizeTarget = bo;

                    // Opposite port index: add half the port count, wrap around.
                    // rect:  0(TL)↔4(BR), 1(TM)↔5(BM), 2(TR)↔6(BL), 3(MR)↔7(ML)
                    // oval:  0(top)↔2(bottom), 1(right)↔3(left)
                    int opp = (i + ports.size() / 2) % ports.size();

                    // Freeze anchor and initial bounds before the first resize call.
                    resizeAnchorPoint  = new Point(ports.get(opp));
                    resizeInitialBounds = bo.getBoundingBox();

                    // Determine axis constraint by comparing pressed-port and anchor
                    // coordinates.  Edge-midpoint pairs share one coordinate:
                    //   same X  →  TM/BM or oval top/bottom  →  only Y is free
                    //   same Y  →  ML/MR or oval left/right  →  only X is free
                    //   neither →  corner                    →  both axes free
                    resizeFreeY = (port.x == resizeAnchorPoint.x);
                    resizeFreeX = (port.y == resizeAnchorPoint.y);

                    // Bring to front and mark as selected so port handles remain visible.
                    model.moveToFront(bo);
                    selection.clear();
                    selection.add(bo);
                    canvas.setSelection(Collections.unmodifiableList(selection));

                    state = State.RESIZING;
                    canvas.repaint();
                    return;
                }
            }
        }

        // ── Branch 2: OBJECT HIT → move (FR-023 priority 2) ──────────────────
        GraphicObject hit = model.getObjectAt(e.getPoint());
        if (hit != null) {
            model.moveToFront(hit);

            selection.clear();
            selection.add(hit);
            canvas.setSelection(Collections.unmodifiableList(selection));

            moveTarget = hit;
            moveOffset = new Point(
                e.getX() - hit.getBoundingBox().x,
                e.getY() - hit.getBoundingBox().y
            );
            state = State.MOVING;
            canvas.repaint();
            return;
        }

        // ── Branch 3: EMPTY CANVAS → drag-select (FR-023 priority 3) ────────
        // No object was hit: start a rubber-band drag-select gesture (T041).
        // The current selection is cleared immediately; objects are added to the
        // selection on release once the final drag rectangle is known.
        selection.clear();
        canvas.setSelection(Collections.unmodifiableList(selection));
        dragStart = e.getPoint();
        state = State.DRAG_SELECTING;
        canvas.repaint();
    }

    @Override
    public void onMouseDragged(MouseEvent e, DiagramModel model, CanvasPanel canvas) {

        // ── RESIZING drag (T038) ──────────────────────────────────────────────
        if (state == State.RESIZING && resizeTarget != null) {
            Rectangle newBounds;

            if (resizeFreeY) {
                // Edge-midpoint: TM, BM, oval top, oval bottom.
                // Only the top/bottom edge moves; left edge and width are fixed.
                int newY = Math.min(e.getY(), resizeAnchorPoint.y);
                int newH = Math.abs(e.getY() - resizeAnchorPoint.y);
                newBounds = new Rectangle(
                    resizeInitialBounds.x, newY,
                    resizeInitialBounds.width, newH
                );
            } else if (resizeFreeX) {
                // Edge-midpoint: ML, MR, oval left, oval right.
                // Only the left/right edge moves; top edge and height are fixed.
                int newX = Math.min(e.getX(), resizeAnchorPoint.x);
                int newW = Math.abs(e.getX() - resizeAnchorPoint.x);
                newBounds = new Rectangle(
                    newX, resizeInitialBounds.y,
                    newW, resizeInitialBounds.height
                );
            } else {
                // Corner port: both axes are free — standard two-point normalisation.
                newBounds = GeomUtils.normalizeRect(e.getPoint(), resizeAnchorPoint);
            }

            // resize() applies the new bounds and clamps width/height to ≥ 20 px.
            resizeTarget.resize(newBounds);
            canvas.repaint();
            return;
        }

        // ── MOVING drag (T025 / T043) ─────────────────────────────────────────
        if (state == State.MOVING && moveTarget != null) {
            // Both BasicObject and CompositeObject expose moveTo(absoluteX, absoluteY).
            // BasicObject stores its own x/y directly.
            // CompositeObject computes the delta from its current bbox origin and
            // propagates it to all children — the call site is identical for both types.
            if (moveTarget instanceof BasicObject) {
                ((BasicObject) moveTarget).moveTo(
                    e.getX() - moveOffset.x,
                    e.getY() - moveOffset.y
                );
            } else if (moveTarget instanceof CompositeObject) {
                ((CompositeObject) moveTarget).moveTo(
                    e.getX() - moveOffset.x,
                    e.getY() - moveOffset.y
                );
            }
            canvas.repaint();
            return;
        }

        // ── DRAG_SELECTING drag (T041) ────────────────────────────────────────
        if (state == State.DRAG_SELECTING && dragStart != null) {
            // Update the rubber-band rectangle and pass it to the canvas for rendering.
            // GeomUtils.normalizeRect always produces a rectangle with positive w/h
            // regardless of drag direction, so the displayed box is well-formed.
            canvas.setDragRect(GeomUtils.normalizeRect(dragStart, e.getPoint()));
        }
    }

    @Override
    public void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        // MOVING / RESIZING — finalized incrementally during drag; nothing extra on release.
        if (state == State.MOVING || state == State.RESIZING) {
            state = State.IDLE;
            return;
        }

        // ── DRAG_SELECTING release (T041) ─────────────────────────────────────
        // Compute the final rubber-band rectangle and collect every top-level object
        // whose bounding box is completely inside it.  Then replace the current
        // selection, clear the overlay, and return to IDLE.
        if (state == State.DRAG_SELECTING && dragStart != null) {
            Rectangle dragRect = GeomUtils.normalizeRect(dragStart, e.getPoint());
            List<GraphicObject> enclosed = model.getObjectsCompletelyInside(dragRect);
            selection.clear();
            selection.addAll(enclosed);
            canvas.setSelection(Collections.unmodifiableList(selection));
            canvas.clearDragRect();
            dragStart = null;
            state = State.IDLE;
            canvas.repaint();
        }
    }

    /**
     * Track the hovered object and notify the canvas so it can draw port handles.
     *
     * Delegating the repaint decision to {@link CanvasPanel#setHoveredObject}
     * avoids redundant repaints when the mouse moves over empty canvas (the canvas
     * only repaints when the hovered object actually changes).
     */
    @Override
    public void onMouseMoved(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        canvas.setHoveredObject(model.getObjectAt(e.getPoint()));
    }
}
