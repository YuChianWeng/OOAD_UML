package uml.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import javax.swing.JToggleButton;
import uml.model.DiagramModel;
import uml.tool.AssociationMode;
import uml.tool.RectMode;
import uml.tool.ToolMode;

/**
 * Smoke test for FR-005: after a transient Rect/Oval creation completes,
 * both the active tool and toolbar highlight must return to the previous
 * persistent tool.
 */
public class ToolbarTransientModeTest {

    public static void main(String[] args) throws Exception {
        DiagramModel model = new DiagramModel();
        CanvasPanel canvas = new CanvasPanel(model);
        ToolBar toolBar = new ToolBar(canvas);

        JToggleButton association = buttonNamed(toolBar, "Association");
        JToggleButton rect = buttonNamed(toolBar, "Rect");

        association.doClick();
        assertTrue(currentMode(canvas) instanceof AssociationMode,
            "Association click should activate AssociationMode");
        assertHighlighted(association, "Association should be highlighted after click");

        rect.doClick();
        assertTrue(currentMode(canvas) instanceof RectMode,
            "Rect click should activate RectMode");
        assertHighlighted(rect, "Rect should be highlighted while drawing");

        ToolMode mode = currentMode(canvas);
        mode.onMousePressed(mouse(canvas, MouseEvent.MOUSE_PRESSED, 10, 10), model, canvas);
        mode.onMouseReleased(mouse(canvas, MouseEvent.MOUSE_RELEASED, 80, 80), model, canvas);

        assertTrue(currentMode(canvas) instanceof AssociationMode,
            "RectMode should revert active tool to previous AssociationMode");
        assertHighlighted(association,
            "Toolbar highlight should revert to Association after Rect creation");
        assertNotHighlighted(rect,
            "Rect button should no longer be highlighted after transient creation");

        System.out.println("TOOLBAR_TRANSIENT_OK");
    }

    private static JToggleButton buttonNamed(ToolBar toolBar, String text) {
        for (Component component : toolBar.getComponents()) {
            if (component instanceof JToggleButton) {
                JToggleButton button = (JToggleButton) component;
                if (text.equals(button.getText())) {
                    return button;
                }
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    private static ToolMode currentMode(CanvasPanel canvas) throws Exception {
        Field field = CanvasPanel.class.getDeclaredField("activeMode");
        field.setAccessible(true);
        return (ToolMode) field.get(canvas);
    }

    private static MouseEvent mouse(CanvasPanel canvas, int id, int x, int y) {
        return new MouseEvent(canvas, id, System.currentTimeMillis(), 0, x, y, 1, false);
    }

    private static void assertHighlighted(JToggleButton button, String message) {
        assertTrue(Color.DARK_GRAY.equals(button.getBackground()), message);
        assertTrue(Color.WHITE.equals(button.getForeground()), message);
    }

    private static void assertNotHighlighted(JToggleButton button, String message) {
        assertTrue(!Color.DARK_GRAY.equals(button.getBackground()), message);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
