# Contract: ToolMode Interface

**Package**: `uml.tool`  
**Type**: Strategy interface  
**Date**: 2026-04-14

---

## Purpose

`ToolMode` is the Strategy abstraction that decouples canvas mouse events from tool-
specific behavior. `CanvasPanel` holds one active `ToolMode` instance and delegates all
four mouse event types to it. This is the primary extensibility point of the application:
adding a new tool requires only a new `ToolMode` implementation class — zero changes to
`CanvasPanel` or any existing tool.

---

## Interface Definition

```java
package uml.tool;

import java.awt.event.MouseEvent;
import uml.model.DiagramModel;
import uml.ui.CanvasPanel;

public interface ToolMode {

    /**
     * Called on MouseEvent.MOUSE_PRESSED on the canvas.
     * Implementations begin their gesture here (e.g., store press point,
     * identify hit object, enter sub-state).
     */
    void onMousePressed(MouseEvent e, DiagramModel model, CanvasPanel canvas);

    /**
     * Called on MouseEvent.MOUSE_DRAGGED on the canvas.
     * Implementations update transient preview state and call canvas.repaint().
     */
    void onMouseDragged(MouseEvent e, DiagramModel model, CanvasPanel canvas);

    /**
     * Called on MouseEvent.MOUSE_RELEASED on the canvas.
     * Implementations finalize any gesture started in onMousePressed.
     * Must call canvas.repaint() if the model was modified.
     */
    void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas);

    /**
     * Called on MouseEvent.MOUSE_MOVED on the canvas (no button pressed).
     * Implementations update hover state and call canvas.repaint() if display
     * state changed (e.g., port handles appear/disappear on hover).
     */
    void onMouseMoved(MouseEvent e, DiagramModel model, CanvasPanel canvas);
}
```

---

## Contracts (Preconditions / Postconditions)

| Method | Preconditions | Postconditions |
|--------|--------------|----------------|
| `onMousePressed` | `e`, `model`, `canvas` are non-null | Implementation stores any necessary gesture state; may mutate model (e.g., select, move to front); calls `canvas.repaint()` if visual state changed |
| `onMouseDragged` | `e`, `model`, `canvas` are non-null; a prior `onMousePressed` has been called (gesture is in progress) | Updates preview state in canvas (drag rect, shape preview, link preview); calls `canvas.repaint()` |
| `onMouseReleased` | `e`, `model`, `canvas` are non-null; a prior `onMousePressed` has been called | Finalizes gesture; resets internal state to IDLE; model mutation complete; calls `canvas.repaint()` |
| `onMouseMoved` | `e`, `model`, `canvas` are non-null; no mouse button is held | Updates hover state only; does NOT mutate model; calls `canvas.repaint()` if hover target changed |

---

## Implementations

| Class | Tool Button | Behavior Summary |
|-------|------------|-----------------|
| `SelectMode` | Select | Port-hit → resize; object-hit → select+move; empty → drag-select rect |
| `RectMode` | Rect | Drag to create RectObject; transient — reverts to previous mode on release |
| `OvalMode` | Oval | Drag to create OvalObject; transient — reverts to previous mode on release |
| `AssociationMode` | Association | Link creation: press in basic object → drag → release in different basic object |
| `GeneralizationMode` | Generalization | Same as AssociationMode; different arrowhead |
| `CompositionMode` | Composition | Same as AssociationMode; different arrowhead |

---

## Extension Guide

To add a new tool (e.g., `NoteMode` for sticky notes):

1. Create `uml/tool/NoteMode.java` implementing `ToolMode`.
2. Add a "Note" button to `ToolBar`.
3. In `ToolBar`'s button listener, call `canvas.setActiveTool(new NoteMode())`.
4. Add `NoteObject` extending `BasicObject` if new shape is needed.

**Zero changes to `CanvasPanel`, `DiagramModel`, or any existing ToolMode class.**
