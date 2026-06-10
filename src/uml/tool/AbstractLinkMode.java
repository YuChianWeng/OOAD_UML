package uml.tool;

import java.awt.event.MouseEvent;
import uml.model.BasicObject;
import uml.model.DiagramModel;
import uml.model.Link;
import uml.model.LinkType;
import uml.ui.CanvasPanel;
import uml.util.GeomUtils;

/**
 * Shared state machine for Association, Generalization, and Composition modes.
 *
 * All three link-creation tools have identical interaction logic:
 *   1. mousePressed on a BasicObject port → enter DRAWING state, show preview line.
 *   2. mouseDragged → update preview line endpoint.
 *   3. mouseReleased on a different BasicObject → create Link, clear preview.
 *   4. mouseReleased elsewhere / on same object → abort silently, clear preview.
 *
 * Subclasses differ only in the LinkType they produce, so they each override only
 * getLinkType() — Open-Closed Principle (Principle III): adding a new link type
 * means adding one new subclass with one method override, nothing else changes.
 *
 * Responsibilities (Principle IV):
 *   - Own the IDLE / DRAWING / INVALID state machine.
 *   - Handle all four mouse events and enforce the link-creation rules:
 *       • press on null (empty canvas or composite) → INVALID, no link
 *       • release on null → abort, no link
 *       • release on same object as source → abort, no link
 *   - Delegate arrowhead rendering entirely to Link.draw().
 */
abstract class AbstractLinkMode implements ToolMode {

    // ── Internal state ─────────────────────────────────────────────────────

    protected enum State { IDLE, DRAWING, INVALID }

    private State       state           = State.IDLE;
    private BasicObject source          = null;
    private int         sourcePortIndex = 0;

    // ── Subclass contract ──────────────────────────────────────────────────

    /** Return the LinkType constant that this mode creates. */
    protected abstract LinkType getLinkType();

    // ── ToolMode implementation ────────────────────────────────────────────

    /**
     * Determine whether the press lands on a valid BasicObject port.
     *
     * Hit-testing logic:
     *   - model.getBasicObjectAt(p) returns the frontmost BasicObject whose
     *     bounding box contains p, or null for composites and empty canvas.
     *   - If null → INVALID (no preview is shown; drag and release are ignored).
     *   - If non-null → record source and nearest port, enter DRAWING, show preview.
     *
     * Port selection: GeomUtils.nearestPortIndex finds the port geometrically
     * closest to the press point.  The press need not be exactly on a port handle —
     * "nearest port" is resolved automatically so the user only has to click
     * somewhere inside the source object.
     */
    @Override
    public void onMousePressed(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        BasicObject hit = model.getBasicObjectAt(e.getPoint());
        if (hit == null) {
            // Press on empty canvas or composite — no link can start here
            state = State.INVALID;
            return;
        }
        source          = hit;
        sourcePortIndex = GeomUtils.nearestPortIndex(source.getPorts(), e.getPoint());
        state           = State.DRAWING;
        canvas.setLinkPreview(source.getPorts().get(sourcePortIndex), e.getPoint());
        canvas.repaint();
    }

    /**
     * While dragging, keep the preview line endpoint under the cursor.
     * No-op when state is IDLE or INVALID.
     */
    @Override
    public void onMouseDragged(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        if (state == State.DRAWING) {
            canvas.setLinkPreview(source.getPorts().get(sourcePortIndex), e.getPoint());
            canvas.repaint();
        }
    }

    /**
     * Validate and commit (or abort) the link on mouse release.
     *
     * Rejection conditions (each aborts silently — no link is created):
     *   1. state != DRAWING (press was on empty canvas or composite)
     *   2. Release lands on empty canvas / composite (getBasicObjectAt returns null)
     *   3. Release is on the same object as the press (self-loop not allowed)
     *
     * When all conditions pass:
     *   - targetPortIndex is resolved as the port nearest to the release point.
     *   - A new Link is created and added to the model.
     */
    @Override
    public void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        canvas.clearLinkPreview();
        canvas.repaint();

        if (state != State.DRAWING) {
            state = State.IDLE;
            return;
        }

        BasicObject target = model.getBasicObjectAt(e.getPoint());

        // Reject: no valid target, or target is the same object as source
        if (target == null || target == source) {
            state = State.IDLE;
            return;
        }

        int targetPortIndex = GeomUtils.nearestPortIndex(target.getPorts(), e.getPoint());
        Link link = new Link(getLinkType(), source, sourcePortIndex, target, targetPortIndex);
        model.addLink(link);
        canvas.repaint();
        state = State.IDLE;
    }

    /** Unused for link modes; hover feedback is handled by SelectMode. */
    @Override
    public void onMouseMoved(MouseEvent e, DiagramModel model, CanvasPanel canvas) {}
}
