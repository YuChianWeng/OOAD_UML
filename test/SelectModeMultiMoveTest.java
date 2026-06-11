package uml.ui;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import uml.model.DiagramModel;
import uml.model.GraphicObject;
import uml.model.RectObject;
import uml.tool.SelectMode;

/**
 * Regression tests for TA-required multi-selection movement behavior.
 */
public class SelectModeMultiMoveTest {

    public static void main(String[] args) {
        testDraggingSelectedObjectMovesWholeSelection();
        testDraggingUnselectedObjectKeepsSingleObjectMoveBehavior();
        System.out.println("SELECT_MODE_MULTI_MOVE_OK");
    }

    private static void testDraggingSelectedObjectMovesWholeSelection() {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        SelectMode mode = new SelectMode();

        RectObject first = new RectObject(new Rectangle(10, 10, 50, 40));
        RectObject second = new RectObject(new Rectangle(100, 10, 50, 40));
        model.addObject(first);
        model.addObject(second);

        dragSelect(mode, model, canvas, 0, 0, 180, 80);
        assertEquals(2, canvas.getSelection().size(), "drag-select should select both objects");

        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 20, 20), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 50, 60), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 50, 60), model, canvas);

        assertBounds(first, 40, 50, 50, 40, "dragged selected object should move by delta");
        assertBounds(second, 130, 50, 50, 40, "other selected object should move by same delta");
        assertEquals(2, canvas.getSelection().size(), "batch move should preserve multi-selection");
    }

    private static void testDraggingUnselectedObjectKeepsSingleObjectMoveBehavior() {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        SelectMode mode = new SelectMode();

        RectObject first = new RectObject(new Rectangle(10, 10, 50, 40));
        RectObject second = new RectObject(new Rectangle(100, 10, 50, 40));
        RectObject third = new RectObject(new Rectangle(220, 10, 50, 40));
        model.addObject(first);
        model.addObject(second);
        model.addObject(third);

        dragSelect(mode, model, canvas, 0, 0, 180, 80);
        assertEquals(2, canvas.getSelection().size(), "drag-select should select first two objects");

        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 230, 20), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, 260, 60), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 260, 60), model, canvas);

        assertBounds(first, 10, 10, 50, 40, "unselected-object drag should not move previous selection");
        assertBounds(second, 100, 10, 50, 40, "unselected-object drag should not move previous selection");
        assertBounds(third, 250, 50, 50, 40, "unselected-object drag should move only hit object");

        List<GraphicObject> selection = canvas.getSelection();
        assertEquals(1, selection.size(), "unselected-object drag should collapse selection to hit object");
        assertTrue(selection.get(0) == third, "third object should be the only selected object");
    }

    private static void dragSelect(SelectMode mode, DiagramModel model, CanvasPanel canvas,
                                   int x1, int y1, int x2, int y2) {
        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, x1, y1), model, canvas);
        mode.onMouseDragged(mouse(canvas, MouseEvent.MOUSE_DRAGGED, x2, y2), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, x2, y2), model, canvas);
    }

    private static MouseEvent mouse(CanvasPanel canvas, int id, int x, int y) {
        return new MouseEvent(canvas, id, System.currentTimeMillis(), 0, x, y, 1, false);
    }

    private static void assertBounds(RectObject obj, int x, int y, int width, int height, String message) {
        Rectangle actual = obj.getBoundingBox();
        Rectangle expected = new Rectangle(x, y, width, height);
        assertTrue(expected.equals(actual), message + ": expected " + expected + " but got " + actual);
    }

    private static void assertEquals(int expected, int actual, String message) {
        assertTrue(expected == actual, message + ": expected " + expected + " but got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
