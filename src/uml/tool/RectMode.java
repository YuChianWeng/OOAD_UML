package uml.tool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import uml.model.DiagramModel;
import uml.model.RectObject;
import uml.ui.CanvasPanel;
import uml.util.GeomUtils;

/**
 * Transient tool mode for drawing rectangles.
 *
 * Responsibility (Principle IV): capture a single press-drag-release gesture,
 * create a RectObject from the normalized bounding box, add it to the model,
 * then revert the canvas back to the previous persistent tool.
 *
 * "Transient" means this mode is never stored as {@code previousMode} — multiple
 * Rect presses in a row always revert to the same base tool (usually Select).
 */
public class RectMode implements ToolMode {

    private Point pressPoint;

    // ── ToolMode contract ─────────────────────────────────────────────────────

    @Override
    public void onMousePressed(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        pressPoint = e.getPoint();
    }

    @Override
    public void onMouseDragged(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        if (pressPoint == null) return;
        canvas.setPreviewBounds(GeomUtils.normalizeRect(pressPoint, e.getPoint()), false);
        canvas.repaint();
    }

    @Override
    public void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        if (pressPoint == null) return;

        Rectangle bounds = GeomUtils.normalizeRect(pressPoint, e.getPoint());
        // Enforce 20×20 minimum so ports and labels are always interactable.
        bounds.width  = Math.max(bounds.width,  20);
        bounds.height = Math.max(bounds.height, 20);

        model.addObject(new RectObject(bounds));
        canvas.clearPreviewBounds();
        // Revert to the persistent tool that was active before Rect was selected.
        canvas.setActiveTool(canvas.getPreviousMode());
        canvas.repaint();
        pressPoint = null;
    }

    @Override
    public void onMouseMoved(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        // No hover behavior for a creation tool.
    }

    // ── Transient flag ────────────────────────────────────────────────────────

    /**
     * Returns {@code true}: RectMode must never be stored as {@code previousMode}
     * so that repeated Rect creations always revert to the same base tool.
     */
    @Override
    public boolean isTransient() {
        return true;
    }
}
