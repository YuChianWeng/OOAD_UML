# UML Editor — Architecture Reference

Java 17 + Swing term project for Object-Oriented Analysis & Design.  
No third-party libraries. Compiles and runs entirely offline.

---

## Table of Contents

1. [How to Build and Run](#1-how-to-build-and-run)
2. [Package & File Map](#2-package--file-map)
3. [Layer Architecture](#3-layer-architecture)
4. [Class Hierarchy](#4-class-hierarchy)
5. [Model Layer — Detail](#5-model-layer--detail)
6. [Tool Layer — Detail](#6-tool-layer--detail)
7. [UI Layer — Detail](#7-ui-layer--detail)
8. [Util Layer — Detail](#8-util-layer--detail)
9. [Key Design Patterns](#9-key-design-patterns)
10. [Data-Flow Walkthroughs](#10-data-flow-walkthroughs)
11. [Port Index Conventions](#11-port-index-conventions)
12. [Paint Layer Order](#12-paint-layer-order)
13. [Feature Phase Summary](#13-feature-phase-summary)

---

## 1. How to Build and Run

```bash
# Compile (from project root)
make compile

# Run from compiled classes
java -cp bin uml.app.Main

# Run the smoke tests
make test

# Or build and run the jar
make run
```

Requires JDK 17+. No third-party libraries are needed.

---

## 2. Package & File Map

```
src/
├── uml/app/
│   └── Main.java                  Entry point — schedules MainFrame on EDT
│
├── uml/model/                     Domain model (pure Java, no Swing)
│   ├── GraphicObject.java         Abstract root of all canvas objects
│   ├── BasicObject.java           Abstract single shape (Rect, Oval)
│   ├── RectObject.java            Concrete rectangle
│   ├── OvalObject.java            Concrete ellipse
│   ├── CompositeObject.java       Composite pattern — grouped objects
│   ├── Link.java                  Directed relationship between two BasicObjects
│   ├── LinkType.java              Enum: ASSOCIATION | GENERALIZATION | COMPOSITION
│   └── DiagramModel.java          Single source of truth; owns object + link lists
│
├── uml/tool/                      Strategy pattern — one class per interaction mode
│   ├── ToolMode.java              Strategy interface (4 mouse events + isTransient)
│   ├── SelectMode.java            Move / resize / drag-select (state machine)
│   ├── RectMode.java              Transient: draw a rectangle then revert
│   ├── OvalMode.java              Transient: draw an oval then revert
│   ├── AbstractLinkMode.java      Shared state machine for all three link tools
│   ├── AssociationMode.java       getLinkType() → ASSOCIATION
│   ├── GeneralizationMode.java    getLinkType() → GENERALIZATION
│   └── CompositionMode.java       getLinkType() → COMPOSITION
│
├── uml/ui/                        Swing presentation layer
│   ├── MainFrame.java             Application window; owns model + canvas + toolbar
│   ├── CanvasPanel.java           Drawing surface; forwards mouse events to ToolMode
│   ├── ToolBar.java               Vertical toggle-button panel; wires buttons → tools
│   └── LabelDialog.java           Modal dialog: edit label name + fill color (Phase 8)
│
└── uml/util/                      Stateless geometry helpers
    ├── GeomUtils.java             normalizeRect, union, nearestPortIndex
    └── PortUtils.java             computeRectPorts (8 pts), computeOvalPorts (4 pts)
```

---

## 3. Layer Architecture

```
┌─────────────────────────────────────────────────────────┐
│  uml.app — Entry Point                                  │
│  Main: invokeLater → new MainFrame().setVisible(true)   │
└────────────────────────┬────────────────────────────────┘
                         │ creates
┌────────────────────────▼────────────────────────────────┐
│  uml.ui — Presentation Layer                            │
│                                                         │
│  MainFrame                                              │
│   ├── DiagramModel (owns)                               │
│   ├── CanvasPanel  (owns, passes model reference)       │
│   ├── ToolBar      (owns, passes canvas reference)      │
│   └── Edit menu actions: onGroup / onUngroup / onLabel  │
│                                                         │
│  CanvasPanel                                            │
│   ├── Holds active ToolMode (Strategy)                  │
│   ├── Forwards mouse events → ToolMode                  │
│   ├── Owns all transient overlay state                  │
│   └── paintComponent: renders model + overlays          │
│                                                         │
│  ToolBar                                                │
│   └── 6 JToggleButtons → canvas.setActiveTool(mode)    │
│                                                         │
│  LabelDialog                                            │
│   └── Modal; reads initial state; returns confirmed     │
│        name + color to MainFrame                        │
└───────┬────────────────────────────┬────────────────────┘
        │ reads/mutates              │ delegates events
┌───────▼──────────┐     ┌──────────▼──────────────────────┐
│  uml.model       │     │  uml.tool — Strategy Layer      │
│                  │◄────│                                  │
│  DiagramModel    │     │  ToolMode (interface)            │
│  GraphicObject   │     │  SelectMode  (state machine)    │
│  BasicObject     │     │  RectMode    (transient)        │
│  RectObject      │     │  OvalMode    (transient)        │
│  OvalObject      │     │  AbstractLinkMode (template)    │
│  CompositeObject │     │  AssociationMode                │
│  Link            │     │  GeneralizationMode             │
│  LinkType        │     │  CompositionMode                │
└──────────────────┘     └─────────────────────────────────┘
        ▲
        │ pure geometry
┌───────┴───────────────────────────────────────────────────┐
│  uml.util — Utility Layer (stateless, no model coupling)  │
│  GeomUtils: normalizeRect | union | nearestPortIndex       │
│  PortUtils: computeRectPorts | computeOvalPorts            │
└────────────────────────────────────────────────────────────┘
```

**Dependency rules (enforced by package structure):**
- `uml.model` → `uml.util` only (no UI, no tool imports)
- `uml.tool` → `uml.model`, `uml.ui`, `uml.util`
- `uml.ui`   → `uml.model`, `uml.tool`, `uml.util`
- `uml.util` → nothing (pure Java geometry)

---

## 4. Class Hierarchy

### Graphic Object Hierarchy (Composite Pattern)

```
GraphicObject  (abstract)
│  draw(Graphics2D)
│  contains(Point) → boolean
│  getBoundingBox() → Rectangle
│  getLabel() → String
│  getLabelColor() → Color
│
├── BasicObject  (abstract)
│   │  x, y, width, height          ← positional state
│   │  label, labelColor            ← label state (Phase 8)
│   │  setLabel(String)
│   │  setLabelColor(Color)
│   │  moveTo(int, int)             ← absolute move
│   │  resize(Rectangle)            ← clamped to 20×20 minimum
│   │  getPorts() → List<Point>     ← abstract; supplied by subclass
│   │
│   ├── RectObject
│   │    draw(): fillRect + drawRect + centered label text
│   │    getPorts(): PortUtils.computeRectPorts (8 pts)
│   │
│   └── OvalObject
│        draw(): fillOval + drawOval + centered label text
│        getPorts(): PortUtils.computeOvalPorts (4 pts)
│
└── CompositeObject
     children: List<GraphicObject>   ← ordered child list
     getBoundingBox(): union of all children's boxes (dynamic)
     contains(Point): bounding-box test
     draw(): paint children → dashed bbox outline
     moveTo(int, int): computes delta → moveBy(dx, dy) recursively
     moveBy(int, int): propagates delta to every descendant
     getChildren() → unmodifiable list
```

### Tool Mode Hierarchy (Strategy Pattern)

```
ToolMode  (interface)
│  onMousePressed(MouseEvent, DiagramModel, CanvasPanel)
│  onMouseDragged(...)
│  onMouseReleased(...)
│  onMouseMoved(...)
│  isTransient() → boolean  (default: false)
│
├── SelectMode
│    State: IDLE | MOVING | RESIZING | DRAG_SELECTING
│    onMousePressed: 3-branch priority dispatcher
│    onMouseDragged: delegates to active state
│    onMouseReleased: commits or finalizes gesture
│    onMouseMoved: updates hoveredObject
│
├── RectMode   (isTransient = true)
│    press → store pressPoint
│    drag  → setPreviewBounds(rect, false)
│    release → addObject(new RectObject) → revert to previousMode
│
├── OvalMode   (isTransient = true)
│    press → store pressPoint
│    drag  → setPreviewBounds(oval, true)
│    release → addObject(new OvalObject) → revert to previousMode
│
└── AbstractLinkMode  (abstract)
     State: IDLE | DRAWING | INVALID
     getLinkType() → LinkType  (abstract; supplied by subclass)
     press   → getBasicObjectAt → DRAWING or INVALID
     drag    → setLinkPreview(start, cursor)
     release → validate target → addLink or abort
     │
     ├── AssociationMode     getLinkType() → ASSOCIATION
     ├── GeneralizationMode  getLinkType() → GENERALIZATION
     └── CompositionMode     getLinkType() → COMPOSITION
```

---

## 5. Model Layer — Detail

### `DiagramModel`

The single source of truth. Owned by `MainFrame`; passed by reference to `CanvasPanel` and to every `ToolMode` call.

| Field | Type | Description |
|-------|------|-------------|
| `objects` | `List<GraphicObject>` | z-ordered list; index 0 = backmost, last = frontmost |
| `links` | `List<Link>` | all directed relationships |

| Method | Phase | Description |
|--------|-------|-------------|
| `addObject(obj)` | 3 | append to z-order end |
| `removeObject(obj)` | 3 | remove from z-order |
| `getObjects()` | 3 | unmodifiable view |
| `moveToFront(obj)` | 4 | remove + re-append to end |
| `getObjectAt(p)` | 4 | reverse-iterate, first hit wins |
| `getBasicObjectAt(p)` | 5 | same but only BasicObject instances |
| `addLink(link)` | 5 | append to link list |
| `removeLink(link)` | 5 | remove from link list |
| `getLinks()` | 5 | unmodifiable view |
| `getObjectsCompletelyInside(r)` | 7 | drag-select helper |
| `group(selected)` | 7 | remove members → new CompositeObject → add |
| `ungroup(composite)` | 7 | remove composite → re-insert children at same z-index |

**Z-order invariant:** `getObjectAt` iterates in reverse (last → index 0) so the frontmost painted object wins in a click ambiguity. `moveToFront` re-appends to the end of the list, which keeps paint order and hit-test order in sync.

---

### `BasicObject`

Holds all state for a single shape:

| Field | Type | Default | Mutated by |
|-------|------|---------|------------|
| `x, y` | `int` | at creation | `moveTo()`, `resize()` |
| `width, height` | `int` | at creation | `resize()` (clamped ≥ 20) |
| `label` | `String` | `""` | `setLabel()` via `LabelDialog` → `MainFrame` |
| `labelColor` | `Color` | `Color.WHITE` | `setLabelColor()` via `LabelDialog` → `MainFrame` |

`resize(Rectangle r)` clamps width and height to at least 20 px to keep port handles interactable and labels legible.

---

### `CompositeObject`

Implements the **Composite** pattern. Has no stored `x, y`. Instead:

- `getBoundingBox()` — recomputed every call as the union of all children's bounding boxes. This means it automatically tracks moved/resized children with no listener needed.
- `moveTo(newX, newY)` — computes `dx = newX − bbox.x`, `dy = newY − bbox.y`, then calls `moveBy(dx, dy)` recursively on all descendants.
- `moveBy(dx, dy)` — package-private; propagates the delta through the tree. `BasicObject` children receive an absolute `moveTo`; nested `CompositeObject` children receive a recursive `moveBy`.

---

### `Link`

Stores its connection as `(source, sourcePortIndex, target, targetPortIndex)`.

**Lazy-update contract:** endpoints are re-derived each frame via `source.getPorts().get(sourcePortIndex)`. When a connected shape moves or resizes its `getPorts()` values change, so the link follows with zero explicit coupling — no listener, no stored Point that needs updating.

| LinkType | Arrowhead | Location |
|----------|-----------|----------|
| `ASSOCIATION` | Open two-line chevron | Target end |
| `GENERALIZATION` | Hollow equilateral triangle (white fill erases shaft inside) | Target end |
| `COMPOSITION` | Filled black diamond (16 px long × 12 px wide) | Source end |

All arrowhead geometry is computed from a unit vector along the shaft and its 90° perpendicular. No trigonometry lookup tables are needed.

---

## 6. Tool Layer — Detail

### `SelectMode` — State Machine

```
              press on port handle
IDLE ─────────────────────────────────────► RESIZING
  │                                              │
  │ press on object                              │ release
  ▼                                              ▼
MOVING ◄──────────────────────────── release → IDLE
  │
  │ press on empty canvas
  ▼
DRAG_SELECTING
  │ release
  └──► finalize selection → IDLE
```

**Branch priority in `onMousePressed`:**

1. **Port hit** — checks `getHoveredObject()` first (only the hovered BasicObject shows port handles). Iterates ports; enters RESIZING if cursor is within `PORT_RADIUS_SQ` (64 px² = 8 px radius) of any port.
2. **Object hit** — calls `model.getObjectAt(p)`. Enters MOVING.
3. **Empty canvas** — clears selection; enters DRAG_SELECTING.

**Resize axis constraints** are computed once at press time by comparing the dragged port's position with the diametrically opposite port:
- Same X → `resizeFreeY = true` → only top/bottom edge moves (TM, BM, oval top/bottom)
- Same Y → `resizeFreeX = true` → only left/right edge moves (ML, MR, oval left/right)
- Neither → corner port → both axes free

The anchor point and initial bounds are frozen at press time to prevent drift under the 20 px clamp.

---

### `RectMode` / `OvalMode` — Transient Tools

Both override `isTransient() → true`. `CanvasPanel.setActiveTool()` skips storing transient modes as `previousMode`, so repeated shape creation always reverts to the same persistent tool. Lifecycle:

```
press  → store pressPoint
drag   → setPreviewBounds (live dashed outline)
release→ create object, clearPreviewBounds, setActiveTool(previousMode)
```

---

### `AbstractLinkMode` — Template Method

State machine with three states: IDLE, DRAWING, INVALID.

```
press on BasicObject  →  DRAWING (record source + nearest port)
press on null/composite →  INVALID (drag and release ignored)

drag (DRAWING)  →  update setLinkPreview(fixedStart, cursor)

release (DRAWING):
  - getBasicObjectAt(release point)
  - reject if null, or same object as source
  - else: addLink(new Link(...))
  always: clearLinkPreview, → IDLE
```

Subclasses (`AssociationMode`, `GeneralizationMode`, `CompositionMode`) each override only `getLinkType()`. This is the Open-Closed Principle in action: adding a fourth link type requires one new class and one new `LinkType` constant — zero changes elsewhere.

---

## 7. UI Layer — Detail

### `MainFrame`

Owns `DiagramModel`, `CanvasPanel`, and `ToolBar`. Wires the Edit menu:

| Menu item | Guard | Action |
|-----------|-------|--------|
| **Group** | `sel.size() >= 2` | `model.group(sel)` → update selection → repaint |
| **Ungroup** | `sel.size() == 1 && sel[0] instanceof CompositeObject` | `model.ungroup(composite)` → update selection → repaint |
| **Label** | `sel.size() == 1 && sel[0] instanceof BasicObject` | open `LabelDialog` → on OK: `setLabel` + `setLabelColor` → repaint |

All three actions are silent no-ops when their guard fails (no dialog, no error).

---

### `CanvasPanel`

The drawing surface. Contains **no interaction logic** — all decisions are delegated to the active `ToolMode`.

**Transient overlay state fields** (all set by ToolMode via canvas setters):

| Field | Set by | Cleared by | Purpose |
|-------|--------|------------|---------|
| `previewBounds` + `previewIsOval` | RectMode/OvalMode drag | release | Dashed shape-creation preview |
| `hoveredObject` | SelectMode.onMouseMoved | next move event | Port handles display |
| `selection` | SelectMode, MainFrame | SelectMode press | Selection indicators |
| `linkPreviewStart/End` | AbstractLinkMode drag | release | Dashed link-preview line |
| `dragRect` | SelectMode DRAG_SELECTING drag | release | Blue rubber-band rectangle |

**`setActiveTool(mode)`** — the transient guard:
```java
if (activeMode != null && !activeMode.isTransient()) {
    previousMode = activeMode;  // only save persistent tools
}
activeMode = mode;
```

---

### `ToolBar`

Six `JToggleButton`s in a `ButtonGroup` (mutually exclusive). Each button press calls `canvas.setActiveTool(modes[idx])`. Button ordering:

| Index | Label | Mode |
|-------|-------|------|
| 0 | Select | `SelectMode` (persistent) |
| 1 | Association | `AssociationMode` (persistent) |
| 2 | Generalization | `GeneralizationMode` (persistent) |
| 3 | Composition | `CompositionMode` (persistent) |
| 4 | Rect | `RectMode` (transient) |
| 5 | Oval | `OvalMode` (transient) |

Select is pre-selected and active at startup.

---

### `LabelDialog`

A modal `JDialog`. Constructor:

1. Builds a `JTextField` pre-filled with `initialName`.
2. Builds a color-swatch `JPanel` pre-filled with `initialColor`.
3. "Choose…" button opens `JColorChooser`; updates `chosenColor` and the swatch background on selection.
4. OK sets `confirmed = true`; Cancel / window-close leaves it `false`.
5. Calls `setVisible(true)` (blocks EDT until dismissed, standard Swing modal).

After the constructor returns, the caller reads:

```java
if (dialog.isConfirmed()) {
    target.setLabel(dialog.getLabelName());
    target.setLabelColor(dialog.getLabelColor());
    canvas.repaint();
}
```

The dialog performs **zero model mutations** — it only reads initial state from constructor arguments and exposes results via getters.

---

## 8. Util Layer — Detail

### `GeomUtils`

| Method | Used by | Purpose |
|--------|---------|---------|
| `normalizeRect(p1, p2)` | RectMode, OvalMode, SelectMode | Produce a Rectangle with non-negative w/h regardless of drag direction |
| `union(a, b)` | CompositeObject | Minimum enclosing rectangle of two rectangles |
| `nearestPortIndex(ports, p)` | AbstractLinkMode, SelectMode | Squared-distance search; returns index of closest port |

### `PortUtils`

| Method | Returns | Consumer |
|--------|---------|----------|
| `computeRectPorts(bbox)` | 8 Points (clockwise TL→TM→TR→MR→BR→BM→BL→ML) | `RectObject.getPorts()` |
| `computeOvalPorts(bbox)` | 4 Points (clockwise top→right→bottom→left) | `OvalObject.getPorts()` |

Both methods are pure functions: given a `Rectangle` bounding box they return a `List<Point>`. No object state is involved. The results are consumed by:
- `CanvasPanel.drawIndicators` — renders port handle squares
- `AbstractLinkMode` — finds nearest port for link creation
- `SelectMode` — detects port-handle press for resize
- `Link.draw()` — re-derives endpoints each frame (lazy-update)

---

## 9. Key Design Patterns

| Pattern | Where | Effect |
|---------|-------|--------|
| **Strategy** | `ToolMode` interface + 7 implementations | `CanvasPanel` never branches on tool type; adding a tool = one new class |
| **Template Method** | `AbstractLinkMode` + 3 subclasses | Shared link-creation state machine; each subclass overrides only `getLinkType()` |
| **Composite** | `GraphicObject` / `BasicObject` / `CompositeObject` | `CanvasPanel` iterates `List<GraphicObject>` uniformly; no instanceof in the render loop |
| **Observer (manual)** | `CanvasPanel.setHoveredObject`, `setSelection` | SelectMode pushes state to canvas; canvas decides when to repaint |
| **Lazy Update** | `Link.draw()` re-reads `getPorts()` each frame | Links follow moved/resized objects automatically with no event wiring |
| **Transient Tool Flag** | `isTransient()` in `ToolMode` | RectMode/OvalMode revert after one use without knowing what the previous tool was |
| **Separation of Concerns** | Dialog vs. model vs. canvas | `LabelDialog` only presents UI; `MainFrame` applies changes; `BasicObject` stores state |

---

## 10. Data-Flow Walkthroughs

### Drawing a Rectangle

```
User clicks "Rect" button
  → ToolBar ActionListener → canvas.setActiveTool(new RectMode())
  → CanvasPanel saves old SelectMode as previousMode (not transient)

User press-drags on canvas
  → CanvasPanel.onMousePressed → RectMode.onMousePressed → stores pressPoint
  → CanvasPanel.onMouseDragged → RectMode.onMouseDragged
      → GeomUtils.normalizeRect(pressPoint, cursor)
      → canvas.setPreviewBounds(rect, false) → canvas.repaint()
      → paintComponent draws dashed rect outline (Layer 5)

User releases
  → RectMode.onMouseReleased
      → GeomUtils.normalizeRect → clamp to ≥ 20×20
      → model.addObject(new RectObject(bounds))
      → canvas.clearPreviewBounds()
      → canvas.setActiveTool(previousMode)  ← reverts to Select
      → canvas.repaint() → RectObject.draw() renders the shape (Layer 1)
```

### Moving an Object

```
User (in SelectMode) presses on a shape
  → SelectMode.onMousePressed branch 2
      → model.getObjectAt(point) → hit
      → model.moveToFront(hit)
      → selection ← [hit]; canvas.setSelection(...)
      → moveOffset ← (cursor − bbox.origin)
      → state = MOVING

User drags
  → SelectMode.onMouseDragged (MOVING branch)
      → hit.moveTo(cursor.x − offsetX, cursor.y − offsetY)
      → canvas.repaint()
      → paintComponent: obj.draw(g2) at new position (Layer 1)
      → Links auto-update: Link.draw() re-calls getPorts() (Layer 3)

User releases
  → SelectMode.onMouseReleased → state = IDLE
```

### Editing a Label

```
User (in SelectMode) clicks one BasicObject (e.g., RectObject)
  → selection = [rectObj]

User clicks Edit → Label
  → MainFrame.onLabel()
      → guard: sel.size() == 1 && sel[0] instanceof BasicObject  ✓
      → target = (BasicObject) sel.get(0)
      → new LabelDialog(this, target.getLabel(), target.getLabelColor())
          → dialog shows JTextField + color swatch pre-filled
          → blocks EDT until OK/Cancel

User edits name, picks color, presses OK
  → dialog.isConfirmed() == true
  → target.setLabel(dialog.getLabelName())
  → target.setLabelColor(dialog.getLabelColor())
  → canvas.repaint()
      → RectObject.draw(g2):
           g2.setColor(labelColor); g2.fillRect(...)    ← new fill color
           g2.drawString(label, tx, ty)                 ← new name
```

### Creating a Link

```
User clicks "Association" button → AssociationMode becomes active

User presses inside source shape
  → AbstractLinkMode.onMousePressed
      → model.getBasicObjectAt(point) → sourceObj
      → sourcePortIndex ← GeomUtils.nearestPortIndex(...)
      → state = DRAWING
      → canvas.setLinkPreview(sourcePort, cursor)

User drags toward target
  → AbstractLinkMode.onMouseDragged
      → canvas.setLinkPreview(sourcePort, cursor) → repaint
      → paintComponent draws dashed preview line (Layer 4)

User releases over target shape
  → AbstractLinkMode.onMouseReleased
      → canvas.clearLinkPreview()
      → model.getBasicObjectAt(releasePoint) → targetObj
      → guard: targetObj != null && targetObj != sourceObj  ✓
      → targetPortIndex ← GeomUtils.nearestPortIndex(...)
      → model.addLink(new Link(ASSOCIATION, src, srcPort, tgt, tgtPort))
      → canvas.repaint() → link.draw(g2) renders open-chevron arrow (Layer 3)
```

### Grouping Objects

```
User drag-selects two shapes (SelectMode DRAG_SELECTING)
  → release → model.getObjectsCompletelyInside(dragRect)
  → selection = [rectObj, ovalObj]

User clicks Edit → Group
  → MainFrame.onGroup()
      → guard: sel.size() >= 2  ✓
      → model.group(sel)
          → objects.removeAll([rectObj, ovalObj])
          → composite = new CompositeObject([rectObj, ovalObj])
          → objects.add(composite)
      → canvas.setSelection([composite])
      → canvas.repaint()
          → composite.draw(g2): draws children + dashed bbox outline (Layer 1)
          → drawIndicators: blue solid border (Layer 2)
```

---

## 11. Port Index Conventions

These indices are used consistently by `PortUtils`, `Link`, and `SelectMode`.

**Rectangle — 8 ports (clockwise from top-left):**

```
0(TL) ──── 1(TM) ──── 2(TR)
  │                      │
7(ML)                  3(MR)
  │                      │
6(BL) ──── 5(BM) ──── 4(BR)
```

Opposite-port formula: `opp = (i + 4) % 8`

**Oval — 4 ports (clockwise from top):**

```
       0(top)
         │
3(left)──┼──1(right)
         │
       2(bottom)
```

Opposite-port formula: `opp = (i + 2) % 4`

---

## 12. Paint Layer Order

`CanvasPanel.paintComponent` renders in this exact order (later layers appear on top):

| Layer | Content | Driven by |
|-------|---------|-----------|
| 0 | White background | `super.paintComponent(g)` |
| 1 | All `GraphicObject`s in z-order | `model.getObjects()` (index 0 → last) |
| 2 | Hover / selection indicators | `hoveredObject`, `selection` fields |
| 3 | All `Link`s (shaft + arrowhead) | `model.getLinks()` |
| 4 | Link-creation preview (dashed line) | `linkPreviewStart/End` |
| 5 | Shape-creation preview (dashed outline) | `previewBounds` |
| 6 | Drag-select rectangle (blue dashed) | `dragRect` |

**Indicator rendering** (`drawIndicators`):
- `BasicObject` → 8×8 filled black squares centred on each port point
- `CompositeObject` → solid blue 2 px border around bounding box

---

## 13. Feature Phase Summary

| Phase | User Story | Key Classes Added / Modified |
|-------|------------|------------------------------|
| 1 | Project setup | `Main`, `MainFrame` skeleton |
| 2 | Tool bar + empty canvas | `ToolBar`, `CanvasPanel` (placeholder), `ToolMode` stubs |
| 3 | Draw Rect / Oval | `RectObject`, `OvalObject`, `BasicObject`, `GraphicObject`, `RectMode`, `OvalMode`, `DiagramModel` (add/get), `CanvasPanel` (render + preview) |
| 4 | Select, move, depth, hover | `SelectMode` (IDLE/MOVING), `DiagramModel` (moveToFront, getObjectAt), `CanvasPanel` (hover + selection indicators) |
| 5 | Association / Generalization / Composition links | `Link`, `LinkType`, `AbstractLinkMode`, 3 concrete link modes, `DiagramModel` (link CRUD), `CanvasPanel` (link + preview render) |
| 6 | Resize via port handles | `SelectMode` (RESIZING state, port-hit detection, axis constraints), `BasicObject.resize()`, `PortUtils` |
| 7 | Group / Ungroup, drag-select | `CompositeObject`, `DiagramModel` (group, ungroup, getObjectsCompletelyInside), `SelectMode` (DRAG_SELECTING), `GeomUtils.union`, `MainFrame` (Group/Ungroup actions) |
| 8 | Customize Label Style | `LabelDialog` (new), `MainFrame.onLabel()`, `BasicObject.setLabel/setLabelColor` (pre-existing), `RectObject`/`OvalObject` render (pre-existing) |
