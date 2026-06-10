package uml.tool;

import java.awt.event.MouseEvent;
import uml.model.DiagramModel;
import uml.ui.CanvasPanel;

/**
 * Strategy interface for canvas mouse interaction.
 *
 * Each tool (Select, Rect, Oval, Association, Generalization, Composition) is a
 * separate class that implements this interface.  CanvasPanel holds the currently
 * active ToolMode and forwards every mouse event to it — no switch or instanceof
 * needed in the canvas itself (Principle V).
 *
 * All four handler methods receive the same three arguments so that any tool can
 * read model state, mutate it, and repaint the canvas without reaching back through
 * a global reference.
 */
public interface ToolMode {

    void onMousePressed (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseDragged (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseMoved   (MouseEvent e, DiagramModel model, CanvasPanel canvas);

    /**
     * Returns {@code true} for tools that revert to the previous tool automatically
     * after a single use (currently RectMode and OvalMode).
     *
     * CanvasPanel.setActiveTool() uses this flag to decide whether to update
     * {@code previousMode}: transient tools are never saved as the previous mode,
     * so repeated Rect/Oval creation always reverts to the same persistent tool.
     */
    default boolean isTransient() {
        return false;
    }
}
