package uml.ui;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import uml.model.DiagramModel;
import uml.model.OvalObject;
import uml.model.RectObject;
import uml.tool.SelectMode;

/**
 * Regression tests for reverse resize with minimum-size clamping.
 */
public class ResizeAnchorClampTest {

    public static void main(String[] args) {
        testReverseCornerResizeKeepsOppositeAnchorStable();
        testReverseHorizontalEdgeResizeKeepsFixedAxisStable();
        testReverseVerticalEdgeResizeKeepsFixedAxisStable();
        System.out.println("RESIZE_ANCHOR_CLAMP_OK");
    }

    private static void testReverseCornerResizeKeepsOppositeAnchorStable() {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        SelectMode mode = new SelectMode();
        RectObject rect = new RectObject(new Rectangle(100, 100, 100, 100));
        model.addObject(rect);

        mode.onMouseMoved(mouse(canvas, MouseEvent.MOUSE_MOVED, 196, 196), model, canvas);
        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 200, 200), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 95, 95), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 95, 95), model, canvas);

        assertBounds(rect, 80, 80, 20, 20,
            "reverse corner resize should clamp size without drifting the opposite anchor");
    }

    private static void testReverseHorizontalEdgeResizeKeepsFixedAxisStable() {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        SelectMode mode = new SelectMode();
        OvalObject oval = new OvalObject(new Rectangle(100, 100, 100, 80));
        model.addObject(oval);

        mode.onMouseMoved(mouse(canvas, MouseEvent.MOUSE_MOVED, 199, 140), model, canvas);
        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 200, 140), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 95, 140), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 95, 140), model, canvas);

        assertBounds(oval, 80, 100, 20, 80,
            "reverse horizontal edge resize should clamp width without vertical drift");
    }

    private static void testReverseVerticalEdgeResizeKeepsFixedAxisStable() {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        SelectMode mode = new SelectMode();
        OvalObject oval = new OvalObject(new Rectangle(100, 100, 100, 80));
        model.addObject(oval);

        mode.onMouseMoved(mouse(canvas, MouseEvent.MOUSE_MOVED, 150, 179), model, canvas);
        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 150, 180), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 150, 95), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 150, 95), model, canvas);

        assertBounds(oval, 100, 80, 100, 20,
            "reverse vertical edge resize should clamp height without horizontal drift");
    }

    private static MouseEvent mouse(CanvasPanel canvas, int id, int x, int y) {
        return new MouseEvent(canvas, id, System.currentTimeMillis(), 0, x, y, 1, false);
    }

    private static void assertBounds(Object obj, int x, int y, int width, int height, String message) {
        Rectangle actual;
        if (obj instanceof RectObject) {
            actual = ((RectObject) obj).getBoundingBox();
        } else if (obj instanceof OvalObject) {
            actual = ((OvalObject) obj).getBoundingBox();
        } else {
            throw new AssertionError("Unsupported object type: " + obj);
        }
        Rectangle expected = new Rectangle(x, y, width, height);
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
