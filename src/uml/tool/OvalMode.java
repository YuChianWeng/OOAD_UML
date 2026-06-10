package uml.tool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import uml.model.DiagramModel;
import uml.model.OvalObject;
import uml.ui.CanvasPanel;
import uml.util.GeomUtils;

/**
 * Transient tool mode for drawing ovals.
 *
 * Identical gesture lifecycle to RectMode; only the created object type differs.
 *
 * Responsibility (Principle IV): capture a press-drag-release gesture, create
 * an OvalObject from the normalized bounding box, add it to the model, then
 * revert the canvas to the previous persistent tool.
 */
public class OvalMode implements ToolMode {

    private Point pressPoint;

    // ── ToolMode contract ─────────────────────────────────────────────────────

    @Override
    public void onMousePressed(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        pressPoint = e.getPoint();
    }

    @Override
    public void onMouseDragged(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        if (pressPoint == null) return;
        canvas.setPreviewBounds(GeomUtils.normalizeRect(pressPoint, e.getPoint()), true);
        canvas.repaint();
    }

    @Override
    public void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas) {
        if (pressPoint == null) return;

        Rectangle bounds = GeomUtils.normalizeRect(pressPoint, e.getPoint());
        // Enforce 20×20 minimum so ports and labels are always interactable.
        bounds.width  = Math.max(bounds.width,  20);
        bounds.height = Math.max(bounds.height, 20);

        model.addObject(new OvalObject(bounds));
        canvas.clearPreviewBounds();
        // Revert to the persistent tool that was active before Oval was selected.
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
     * Returns {@code true}: OvalMode must never be stored as {@code previousMode}
     * so that repeated Oval creations always revert to the same base tool.
     */
    @Override
    public boolean isTransient() {
        return true;
    }
}
