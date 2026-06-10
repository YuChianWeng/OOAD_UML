# Data Model: UML / Workflow Editor

**Branch**: `001-uml-editor` | **Date**: 2026-04-14  
**Phase**: 1 — Class design with responsibilities

---

## Package Overview

```
uml.app         — Entry point only
uml.model       — Domain model (no Swing imports)
uml.tool        — ToolMode strategy implementations
uml.ui          — Swing view classes
uml.util        — Stateless geometry helpers
```

---

## uml.model

### `GraphicObject` (abstract)

**Responsibility**: Common contract for everything that can be drawn on the canvas and
interacted with by the user.

**Fields**: none (subclasses own their state)

**Methods** (all abstract unless noted):

| Method | Return | Notes |
|--------|--------|-------|
| `draw(Graphics2D g)` | void | Paints the object |
| `contains(Point p)` | boolean | True if p is inside this object |
| `getBoundingBox()` | Rectangle | Minimum enclosing rectangle |
| `getLabel()` | String | Display name (may be empty) |
| `getLabelColor()` | Color | Fill/background color of the object |

**Design note**: No `getDepth()` method — z-order is entirely encoded in list position
inside `DiagramModel`. No Swing import anywhere in this class.

---

### `BasicObject` (abstract, extends `GraphicObject`)

**Responsibility**: Shared state and behavior for Rect and Oval: bounding box storage,
label/color management, port computation delegation, and resize logic.

**Fields**:

| Field | Type | Notes |
|-------|------|-------|
| `x` | int | Bounding box origin x |
| `y` | int | Bounding box origin y |
| `width` | int | Always ≥ 20 |
| `height` | int | Always ≥ 20 |
| `label` | String | Display text |
| `labelColor` | Color | Fill color (default: white or light gray) |

**Methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `getPorts()` | `List<Point>` | Computed from bounding box; abstract |
| `getShapeType()` | `ShapeType` | RECT or OVAL (enum) |
| `resize(Rectangle newBounds)` | void | Sets x,y,w,h; clamps to min 20×20 |
| `moveTo(int x, int y)` | void | Translates origin |
| `contains(Point p)` | boolean | `getBoundingBox().contains(p)` |
| `getBoundingBox()` | Rectangle | `new Rectangle(x, y, width, height)` |
| `getLabel()` | String | returns `label` |
| `getLabelColor()` | Color | returns `labelColor` |
| `setLabel(String)` | void | |
| `setLabelColor(Color)` | void | |

---

### `RectObject` (extends `BasicObject`)

**Responsibility**: A rectangle shape with 8 connection ports.

**Additional methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `draw(Graphics2D g)` | void | Fill rect with `labelColor`; draw border; draw centered label |
| `getPorts()` | `List<Point>` | Delegates to `PortUtils.computeRectPorts(getBoundingBox())` |

**Port layout** (indices 0–7, clockwise from top-left):

```
0(TL)  1(TM)  2(TR)
7(ML)         3(MR)
6(BL)  5(BM)  4(BR)
```

---

### `OvalObject` (extends `BasicObject`)

**Responsibility**: An ellipse shape with 4 connection ports.

**Additional methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `draw(Graphics2D g)` | void | Fill oval with `labelColor`; draw border; draw centered label |
| `getPorts()` | `List<Point>` | Delegates to `PortUtils.computeOvalPorts(getBoundingBox())` |

**Port layout** (indices 0–3, clockwise from top):

```
     0(Top)
3(L)       1(R)
     2(Bot)
```

---

### `CompositeObject` (extends `GraphicObject`)

**Responsibility**: Owns a tree of child `GraphicObject`s. Its bounding box is the
minimum enclosing rectangle of all children (computed on demand). Not resizable; not a
link endpoint.

**Fields**:

| Field | Type | Notes |
|-------|------|-------|
| `children` | `List<GraphicObject>` | Exclusively owned; never shared |

**Methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `getChildren()` | `List<GraphicObject>` | Unmodifiable view; mutation via DiagramModel |
| `getBoundingBox()` | Rectangle | Union of all `child.getBoundingBox()` results |
| `contains(Point p)` | boolean | `getBoundingBox().contains(p)` |
| `draw(Graphics2D g)` | void | Draws bounding box outline; delegates draw to each child |
| `getLabel()` | String | `""` (composites have no label in spec) |
| `getLabelColor()` | Color | Not applicable; returns default |
| `moveTo(int dx, int dy)` | void | Translates all children recursively |

**Design note**: `children` is package-private; `DiagramService` (or `DiagramModel`)
adds/removes children only during group/ungroup operations.

---

### `Link`

**Responsibility**: Represents a directed connection between two `BasicObject` instances.
Draws itself by recomputing endpoints lazily from port indices.

**Fields**:

| Field | Type | Notes |
|-------|------|-------|
| `type` | `LinkType` | ASSOCIATION, GENERALIZATION, COMPOSITION |
| `source` | `BasicObject` | Source object reference |
| `sourcePortIndex` | int | Index into `source.getPorts()` |
| `target` | `BasicObject` | Target object reference |
| `targetPortIndex` | int | Index into `target.getPorts()` |

**Methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `draw(Graphics2D g)` | void | Calls `source.getPorts().get(sourcePortIndex)` and `target.getPorts().get(targetPortIndex)`; draws line + arrowhead |
| `getSource()` | `BasicObject` | |
| `getTarget()` | `BasicObject` | |
| `getType()` | `LinkType` | |

**Arrowhead rendering** (by `LinkType`):
- `ASSOCIATION`: open arrowhead at target end
- `GENERALIZATION`: hollow triangle at target end
- `COMPOSITION`: filled diamond at source end

---

### `LinkType` (enum)

```java
public enum LinkType { ASSOCIATION, GENERALIZATION, COMPOSITION }
```

---

### `DiagramModel`

**Responsibility**: Single source of truth for the entire diagram. Owns the ordered list
of top-level `GraphicObject`s (z-ordered by list position) and all `Link`s. Provides all
mutating operations; keeps invariants.

**Fields**:

| Field | Type | Notes |
|-------|------|-------|
| `objects` | `List<GraphicObject>` | Index 0 = deepest; last = front |
| `links` | `List<Link>` | All links in the diagram |

**Methods**:

| Method | Return | Notes |
|--------|--------|-------|
| `addObject(GraphicObject)` | void | Appends to end of list (new objects start at front) |
| `removeObject(GraphicObject)` | void | Removes from list |
| `getObjects()` | `List<GraphicObject>` | Unmodifiable view for painting (index 0 → last) |
| `moveToFront(GraphicObject)` | void | `list.remove(obj); list.add(obj)` |
| `getObjectAt(Point p)` | `GraphicObject` | Reverse-iterate list; first `contains(p)` wins; null if none |
| `getBasicObjectAt(Point p)` | `BasicObject` | Same as above but returns only BasicObject; null for composite/empty |
| `getObjectsCompletelyInside(Rectangle r)` | `List<GraphicObject>` | Forward-iterate; include only if `r.contains(obj.getBoundingBox())` |
| `group(List<GraphicObject> selected)` | `CompositeObject` | Remove selected from list, create CompositeObject, add to end |
| `ungroup(CompositeObject c)` | `List<GraphicObject>` | Remove c from list; insert children at c's former index |
| `addLink(Link)` | void | Append to links list |
| `removeLink(Link)` | void | Remove from links list |
| `getLinks()` | `List<Link>` | Unmodifiable view for painting |

---

## uml.tool

### `ToolMode` (interface)

**Responsibility**: Strategy interface; one implementation per toolbar button.

```java
public interface ToolMode {
    void onMousePressed (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseDragged (MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseReleased(MouseEvent e, DiagramModel model, CanvasPanel canvas);
    void onMouseMoved   (MouseEvent e, DiagramModel model, CanvasPanel canvas);
}
```

---

### `SelectMode` (implements `ToolMode`)

**Responsibility**: Handles all three sub-gestures of Select mode: resize (port-hit),
move (object-hit), and drag-select (empty canvas hit). Implements FR-023 priority order.

**State**:
```java
private enum State { IDLE, MOVING, RESIZING, DRAG_SELECTING }
private State state = IDLE;
private GraphicObject moveTarget;
private BasicObject resizeTarget;
private int resizePortIndex;
private Point moveOffset;
private Point dragStart;
private List<GraphicObject> selection = new ArrayList<>();
private GraphicObject hoveredObject;  // for port/bbox display
```

**Key logic points**:
- `onMouseMoved`: update `hoveredObject`; trigger `canvas.repaint()` if changed
- `onMousePressed`: evaluate FR-023 priority (port → object → empty)
- `onMouseDragged`: dispatch to MOVING / RESIZING / DRAG_SELECTING branch
- `onMouseReleased`: finalize; DRAG_SELECTING computes enclosed objects

---

### `RectMode` / `OvalMode` (implements `ToolMode`)

**Responsibility**: Transient creation modes. Capture press-to-release bounding box,
create the shape, add to model, revert `CanvasPanel` to previous mode.

**State**: `pressPoint: Point`, `previewBounds: Rectangle`

---

### `AssociationMode` / `GeneralizationMode` / `CompositionMode` (implements `ToolMode`)

**Responsibility**: Link creation modes. Validate press inside a `BasicObject`; draw
preview line during drag; validate release inside a different `BasicObject`; create `Link`.

**State**: `state: {IDLE, DRAWING, INVALID}`, `source: BasicObject`,
`sourcePortIndex: int`, `previewEnd: Point`

---

## uml.ui

### `MainFrame` (extends `JFrame`)

**Responsibility**: Constructs the entire window layout (BorderLayout: North = empty,
West = ToolBar, Center = CanvasPanel). Creates JMenuBar. Wires Edit menu actions to
`DiagramModel` operations via `DiagramService`. Passes selected `ToolMode` instances to
`CanvasPanel`.

**Key wiring**:
- Edit → Group: `if selection.size() >= 2 → model.group(selection); canvas.repaint()`
- Edit → Ungroup: `if selection.size()==1 && selection[0] instanceof CompositeObject → model.ungroup(composite); canvas.repaint()`
- Edit → Label: `if selection.size()==1 && selection[0] instanceof BasicObject → new LabelDialog(...).show()`

---

### `ToolBar` (extends `JPanel`)

**Responsibility**: Renders 6 tool buttons vertically. Manages the active-button highlight
(exactly one button dark at all times). On button press, creates the corresponding
`ToolMode` instance and notifies `CanvasPanel`.

**Design note**: Buttons are `JButton` or `JToggleButton` in a `ButtonGroup` to enforce
single-selection. Color toggling on press is handled in ActionListener.

---

### `CanvasPanel` (extends `JPanel`)

**Responsibility**: Sole Swing View. Owns `DiagramModel` and active `ToolMode`. Delegates
all four mouse events to `activeMode`. Implements `paintComponent` as described in the
interaction flow. Stores `previousMode` for transient tool revert.

**Fields**:

| Field | Type | Notes |
|-------|------|-------|
| `model` | `DiagramModel` | Single shared instance |
| `activeMode` | `ToolMode` | Current tool strategy |
| `previousMode` | `ToolMode` | For transient Rect/Oval revert |
| `dragRect` | `Rectangle` | Null when not drag-selecting |
| `previewBounds` | `Rectangle` | Null when not drawing shape preview |
| `linkPreviewEnd` | `Point` | Null when not in link-drawing gesture |

**Key method**:
```java
public void setActiveTool(ToolMode mode) {
    if (!(activeMode instanceof RectMode) && !(activeMode instanceof OvalMode))
        previousMode = activeMode;
    activeMode = mode;
}
```

---

### `LabelDialog` (extends `JDialog`)

**Responsibility**: Modal dialog for editing label name and fill color of a `BasicObject`.
Pre-fills with current values. OK applies; Cancel discards.

**Components**: `JTextField` (label name), `JButton` "Choose Color" (opens
`JColorChooser`), OK `JButton`, Cancel `JButton`.

---

## uml.util

### `PortUtils`

```java
// Returns 8 Points for Rect: TL, TM, TR, MR, BR, BM, BL, ML (clockwise)
public static List<Point> computeRectPorts(Rectangle bbox)

// Returns 4 Points for Oval: Top, Right, Bottom, Left (clockwise)
public static List<Point> computeOvalPorts(Rectangle bbox)
```

---

### `GeomUtils`

```java
// Returns index of port in ports closest to point p (Euclidean distance)
public static int nearestPortIndex(List<Point> ports, Point p)

// Returns minimum enclosing Rectangle of two rectangles
public static Rectangle union(Rectangle a, Rectangle b)

// Normalizes a rectangle defined by two corners (handles reversed drag)
public static Rectangle normalizeRect(Point p1, Point p2)
```

---

## Class Relationship Diagram (textual)

```
MainFrame
  ├── ToolBar ──────────────────────────────► CanvasPanel.setActiveTool(ToolMode)
  └── CanvasPanel
        ├── DiagramModel
        │     ├── List<GraphicObject>
        │     │     ├── RectObject  (extends BasicObject extends GraphicObject)
        │     │     ├── OvalObject  (extends BasicObject extends GraphicObject)
        │     │     └── CompositeObject (extends GraphicObject)
        │     │           └── List<GraphicObject> children  [OWNS]
        │     └── List<Link>
        │           └── Link → source: BasicObject, target: BasicObject
        │
        └── ToolMode (interface)
              ├── SelectMode
              ├── RectMode
              ├── OvalMode
              ├── AssociationMode
              ├── GeneralizationMode
              └── CompositionMode

PortUtils ◄── used by RectObject, OvalObject
GeomUtils ◄── used by SelectMode, AssociationMode, GeneralizationMode, CompositionMode
LabelDialog ◄── opened by MainFrame Edit→Label action
```
