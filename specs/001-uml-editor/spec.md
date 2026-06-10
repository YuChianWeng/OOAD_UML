# Feature Specification: UML / Workflow Editor

**Feature Branch**: `001-uml-editor`
**Created**: 2026-04-14
**Status**: Draft (Revised — aligned with official PDF requirements)
**Source**: "Oops UML Editor - requirements.pdf" (authoritative spec)

## Clarifications

### Session 2026-04-14

- Q: What is the correct type hierarchy for graphical objects? → A: Option A — `GraphicObject`
  abstract class for BasicObject and CompositeObject; `Link` is a separate top-level
  drawable class outside that hierarchy. Confirmed by PDF three-way classification:
  Basic Object, Link, Composite.
- Q: Are Rect/Oval persistent or transient tool modes? → A: Transient. After object
  creation, mode reverts to the previously active mode automatically.
- Q: Is link creation hit-tested on the whole object body or on port ranges? → A: The
  mouse press must land inside the bounding box of a basic object; the system then
  selects the nearest port as the connection anchor. Composites are excluded entirely.
- Q: Does hover show ports, or only selection? → A: Hover alone shows ports (or
  bounding box for composites). Selection also shows them.
- Q: What does "Label Color" change — fill color or text color? → A: Fill/background
  color of the object. The label text is rendered on top of the filled object.
- Q: Can composites be included in a Group operation? → A: Yes. Group requires ≥ 2
  objects selected of any type (basic or composite). The resulting composite may
  contain other composites (nesting is supported).
- Q: Does Ungroup dissolve all layers or just the outermost? → A: One layer only.
  Ungroup deconstructs the outermost composite; nested composites inside remain intact.
- Q: How is depth ordering defined? → A: Each object has a depth value 0–99; lower
  value = rendered on top and receives mouse events first. The last-selected object
  gets its depth set to the minimum value (moves to front).
- Q: How is z-order stored — List position or explicit depth field? → A: Option A —
  List position in `DiagramModel`'s top-level list IS the z-order. Index 0 = deepest
  (painted first); last index = front (painted last, hit-tested first). On select:
  `list.remove(obj); list.add(obj)`. No integer depth field on any class.
- Q: What is the hit-test priority order in SelectMode on mouse press? → A: Option A —
  Port first, then object body, then empty canvas. (1) If press lands on a visible
  port handle of any basic object → initiate resize for that port. (2) Else if press
  lands inside any object (checked in reverse z-order) → select it and initiate move.
  (3) Else → initiate drag-selection rectangle. Port is checked first because it
  occupies a small area entirely on the object boundary; checking object body first
  would make resize unreachable.
- Q: Tool mode / MVC separation strategy? → A: Option A — Strategy pattern per tool.
  `ToolMode` interface; one concrete class per tool button; `CanvasPanel` delegates all
  mouse events to the active `ToolMode` instance; `DiagramModel` is pure POJO with no
  Swing imports; `CanvasPanel.paintComponent()` reads model, never mutates it.
- Q: How should composite child ownership work? → A: Option A — True tree ownership.
  On Group, selected objects are removed from `DiagramModel`'s top-level list and
  become exclusive children of the new `CompositeObject`, which is added to the
  top-level list. On Ungroup, the composite is removed and its direct children are
  re-inserted into the top-level list; nested sub-composites remain intact.

---

## Definitions (Canonical — from PDF)

- **Basic Object**: A Rect or Oval shape. Has a bounding box, 8 ports (Rect) or 4 ports
  (Oval), a label name, and a fill color. Can be source and target of links.
- **Link**: An Association, Generalization, or Composition connection between two distinct
  Basic Objects, anchored at one port on each end. Not a subtype of GraphicObject.
- **Composite Object**: A tree-shaped container formed by grouping ≥ 2 objects. Its
  bounding box is the minimum enclosing rectangle of all its constituent objects.
  A composite may contain other composites (nesting is permitted).
- **Depth**: Each object holds a depth value (integer 0–99). Lower depth = painted on
  top and intercepts mouse events before objects with higher depth. On selection, the
  selected object's depth is set to the minimum among all objects (brought to front).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Launch, Draw Basic Shapes (Priority: P1)

A user opens the application. A menu bar (File | Edit), a vertical toolbar on the left
(Select, Association, Generalization, Composition, Rect, Oval), and a canvas area are
visible. Select is active by default.

The user presses and holds the Rect button. The previous tool button restores its color;
the Rect button turns black. The user drags on the canvas, releases, and a rectangle is
drawn. The mode reverts to the previously active tool (Select). The user repeats with
Oval.

**Why this priority**: All other features require objects to exist on the canvas.

**Independent Test**: Launch the app. Press Rect, drag on canvas → rectangle appears,
mode returns to Select. Press Oval, drag → oval appears. No other features needed.

**Acceptance Scenarios**:

1. **Given** the Rect button is pressed in any mode, **When** the user drags on the
   canvas and releases, **Then** a rectangle is drawn at the dragged bounding box
   (minimum 20×20 px enforced), and the active mode reverts to the mode that was active
   before Rect was pressed.
2. **Given** the Oval button is pressed in any mode, **When** the user drags on the
   canvas and releases, **Then** an oval is drawn with the dragged bounding box, and
   mode reverts to the previous mode.
3. **Given** a shape was drawn, **When** the canvas repaints, **Then** the shape remains
   at the correct position with no visual artifacts.
4. **Given** the Rect button is active, **When** the user presses Rect again, **Then**
   the Rect button turns black (active), and the previous mode button restores color.

---

### User Story 2 - Create Links Between Basic Objects (Priority: P2)

With Association, Generalization, or Composition active, the user presses the mouse
inside a basic object's bounding box. The system identifies the nearest port. The user
drags to a different basic object and releases. A link is drawn from the nearest port of
the source to the nearest port of the destination, with the appropriate arrowhead at the
destination.

**Why this priority**: Links are the semantic core of a UML diagram.

**Independent Test**: Draw two rectangles. Select Association. Press inside rect A,
drag to rect B, release → an association arrow connects them via nearest ports.
Repeat with Generalization and Composition.

**Acceptance Scenarios**:

1. **Given** a link tool is active and two basic objects exist, **When** the user presses
   inside object A and releases inside a different object B, **Then** a link is created
   from A's nearest port to B's nearest port with the correct arrowhead for the link type.
2. **Given** Association is selected, **When** the link is created, **Then** it shows a
   plain arrow (→) at the destination end.
3. **Given** Generalization is selected, **When** the link is created, **Then** it shows
   a hollow triangle arrowhead (△) at the destination end.
4. **Given** Composition is selected, **When** the link is created, **Then** it shows a
   filled diamond (◆) at the source end.
5. **Given** a link tool is active, **When** the mouse press lands outside all basic
   objects (on empty canvas or on a composite), **Then** no action occurs for the entire
   press-drag-release sequence.
6. **Given** a link tool is active, **When** the mouse press is inside basic object A but
   the release is outside all basic objects OR on the same object A, **Then** no link
   is created.

---

### User Story 3 - Hover, Select, Move, and Resize (Priority: P3)

In Select mode, moving the mouse over a basic object shows its ports; moving over a
composite shows its bounding box outline. Clicking an object selects it (deselects
others) and promotes it to front (minimum depth). Dragging a selected object moves it;
all attached links redraw. Dragging a port handle on a selected basic object resizes it,
supporting reversed dragging and enforcing a 20 px minimum.

**Why this priority**: Hover, selection, movement, and resize are the foundational
interaction layer for all editing.

**Independent Test**: Draw a rectangle. Hover → 8 port squares appear. Click → it is
selected, rendered on top. Drag body → object moves, any links follow. Drag a corner
port → object resizes. Drag corner past opposite corner → shape mirrors correctly.
Drag to very small size → clamps at 20×20 px.

**Acceptance Scenarios**:

1. **Given** Select mode is active, **When** the user moves the mouse over a basic
   object, **Then** all of its port handles are displayed.
2. **Given** Select mode is active, **When** the user moves the mouse over a composite,
   **Then** only the composite's bounding box outline is displayed.
3. **Given** the user clicks a basic or composite object, **Then** it is selected, all
   previously selected objects are deselected, and the clicked object's depth is set to
   the minimum (front).
4. **Given** an object is selected, **When** the user clicks on empty canvas (not on any
   object), **Then** all selections are cleared.
5. **Given** an object is selected and the user drags it, **When** the mouse is released,
   **Then** the object has moved to the new position; all connected links are redrawn.
6. **Given** a basic object is hovered (ports visible) and the user clicks and drags a
   port handle, **When** the mouse is released at (x, y), **Then** the dragged port's
   new position is (x, y) and the object's bounding box is recalculated accordingly.
7. **Given** the user drags a port past the opposite corner (reversed drag), **Then** the
   system treats the drag endpoint as the new origin corner and recalculates the bounding
   box using absolute width/height values (no negative dimensions).
8. **Given** the calculated new bounding box has width or height < 20 px, **Then** the
   object is drawn at exactly 20×20 px minimum.
9. **Given** the user attempts to drag a port on a composite object, **Then** no resize
   occurs (F.1: composites are not resizable).

---

### User Story 4 - Multi-Selection and Group / Ungroup (Priority: P4)

In Select mode, dragging on empty canvas draws a dashed selection rectangle. On release,
all objects entirely enclosed become selected. With ≥ 2 objects selected, Group (Edit
menu) combines them into a composite. With exactly 1 composite selected, Ungroup
deconstructs its outermost layer only.

**Why this priority**: Grouping is a core feature of the workflow editor.

**Independent Test**: Draw three shapes. Drag a selection box — all three selected.
Edit → Group → they become one composite. Click composite → selected as unit. Edit →
Ungroup → three shapes restored.

**Acceptance Scenarios**:

1. **Given** Select mode is active and the user drags starting from empty canvas (not
   inside any object), **When** the mouse is released, **Then** all objects whose
   bounding boxes are completely enclosed by the drag rectangle become selected; partially
   overlapping objects are NOT selected.
2. **Given** ≥ 2 objects are selected (any combination of basic and composite objects),
   **When** the user chooses Edit → Group, **Then** all selected objects combine into a
   single composite; the composite's bounding box is the union of all children.
3. **Given** exactly 1 composite object is selected, **When** the user chooses
   Edit → Ungroup, **Then** the outermost composite dissolves; its direct children
   become individually selectable at their current positions; nested sub-composites
   remain intact.
4. **Given** only 1 object is selected, **When** the user chooses Edit → Group,
   **Then** no action occurs (D.1: minimum 2 objects required).
5. **Given** > 1 object is selected (or 0), **When** the user chooses Edit → Ungroup,
   **Then** no action occurs (D.2: Ungroup requires exactly 1 composite selected).
6. **Given** a composite is one of the selected objects, **When** the user groups with
   another object, **Then** a new composite is created containing both (composite nesting
   is supported).
7. **Given** a composite is selected, **When** it is rendered, **Then** only its outer
   bounding box outline is shown; internal structure is not individually highlighted.

---

### User Story 5 - Customize Label Name and Fill Color (Priority: P5)

With a basic object selected, the user opens Edit → Label. A "Customize Label Style"
dialog appears, pre-filled with the current label name and current fill color. The user
types a new name and/or picks a new color. Clicking OK immediately applies both changes.
Clicking Cancel leaves the object unchanged.

**Why this priority**: Labels give semantic meaning to diagram elements.

**Independent Test**: Draw a rectangle. Select it. Edit → Label → dialog appears with
empty name and default color. Enter "MyClass", pick yellow. Click OK. The rectangle
now has yellow fill and shows "MyClass" inside.

**Acceptance Scenarios**:

1. **Given** a single basic object is selected, **When** the user opens Edit → Label,
   **Then** a "Customize Label Style" dialog opens showing the current label name and
   current fill color.
2. **Given** the user changes the label name and clicks OK, **Then** the object
   immediately displays the new name centered within its bounding box.
3. **Given** the user changes the fill color and clicks OK, **Then** the object's
   background fill is immediately rendered in the chosen color.
4. **Given** the user clicks Cancel, **Then** the object retains its original name and
   fill color unchanged.
5. **Given** no basic object is selected (or a composite is selected), **When** the user
   opens the Edit menu, **Then** the Label option is either disabled or produces no
   dialog (precondition not met).

---

### User Story 6 - Depth Ordering and Hit-Testing (Priority: P6)

Every object has a depth (0–99). Lower depth = painted on top and receives mouse events
first. When two objects overlap, only the topmost one (lowest depth) receives a mouse
event. Selecting an object sets its depth to the minimum among all objects (brings it to
front).

**Why this priority**: Correct z-order is required for predictable mouse interaction
when objects overlap.

**Independent Test**: Draw two overlapping rectangles. Click the overlap area → the
topmost one is selected. Select the other by clicking its non-overlapping area → it
comes to front. The overlap area now selects the newly-front object.

**Acceptance Scenarios**:

1. **Given** two objects overlap, **When** the user clicks the overlap area, **Then**
   only the object with the lower depth value (front) receives the click and is selected.
2. **Given** an object is selected, **Then** its depth is set to the minimum value among
   all objects on the canvas (it is now rendered on top of all others).
3. **Given** objects overlap, **When** the canvas repaints, **Then** objects are drawn in
   depth order (higher depth painted first, lower depth painted last = on top).

---

### Edge Cases

- Rect/Oval button pressed with no subsequent drag (click only): object is created at
  the press point with the minimum visible size (20×20 px).
- Link tool: mouse press inside a composite object → no link created (composites have
  no ports; treated as Alt B.1).
- Link tool: mouse press on a port-bearing basic object, then released on a composite →
  no link created (Alt B.2).
- Multi-select drag encloses zero objects: all current selections are cleared, no error.
- Group called with 1 object selected: no action (D.1).
- Group called with 0 objects selected: no action.
- Ungroup called with 0 objects, 2+ objects, or 1 basic object selected: no action (D.2).
- Resize drag within the same side (zero or near-zero delta): minimum size (20 px)
  clamp applies; object does not disappear.
- Moving a composite object: all links attached to any basic object inside the composite
  must be redrawn at the updated positions.
- Overlapping objects during multi-select: an object partially outside the drag
  rectangle is NOT included in the selection.
- Clicking on empty canvas in any mode other than Select: the canvas-specific interaction
  of that mode applies (e.g., pressing Rect/Oval starts the creation drag).

## Requirements *(mandatory)*

### Functional Requirements

**Application Shell**

- **FR-001**: The application MUST display a main window with three regions: a menu bar
  at the top, a vertical toolbar on the left, and a canvas area occupying the rest.
- **FR-002**: The menu bar MUST contain a File menu and an Edit menu.
- **FR-003**: The Edit menu MUST contain at minimum: Group, Ungroup, and Label items.
- **FR-004**: The toolbar MUST display six buttons in this top-to-bottom order: Select,
  Association, Generalization, Composition, Rect, Oval. Exactly one button MUST appear
  active (dark/highlighted) at all times; Select is active on launch.
- **FR-005**: When the user presses Rect or Oval, the currently active button restores
  its default appearance and the pressed button turns active (dark). After the creation
  gesture completes (mouse released on canvas), the Rect or Oval button restores and the
  mode that was active BEFORE the press becomes active again (transient mode behavior).

**Shape Creation**

- **FR-006**: When Rect is pressed and the user drags on the canvas, a rectangle MUST be
  created at the dragged bounding box on mouse release; width and height MUST each be at
  least 20 px.
- **FR-007**: When Oval is pressed and the user drags on the canvas, an oval MUST be
  created at the dragged bounding box on mouse release; width and height MUST each be at
  least 20 px.
- **FR-008**: Newly created objects MUST have an empty label, a default fill color, and
  be rendered immediately on the canvas.

**Port Display**

- **FR-009**: In Select mode, when the mouse cursor enters a basic object's bounding box
  (hover), the object's port handles MUST be displayed immediately (before any click).
  Rect shows 8 ports (4 corners + 4 edge midpoints); Oval shows 4 ports (top, bottom,
  left, right).
- **FR-010**: When the mouse cursor enters a composite object's bounding box (hover), the
  composite's bounding box outline MUST be displayed.
- **FR-011**: Port handles MUST be hidden when the mouse leaves an unselected basic
  object's bounding box.

**Link Creation**

- **FR-012**: When a link tool (Association, Generalization, Composition) is active and
  the user presses the mouse inside a basic object's bounding box, the system MUST begin
  a link creation gesture using the nearest port of that object as the start anchor.
- **FR-013**: A link MUST be created only when the mouse is released inside a DIFFERENT
  basic object's bounding box; the nearest port of the destination object is the end
  anchor.
- **FR-014**: If the mouse press lands outside all basic objects (empty canvas or on a
  composite), the entire press-drag-release sequence MUST produce no action (Alt B.1).
- **FR-015**: If the mouse release lands outside all basic objects, or on the same object
  as the press, no link MUST be created (Alt B.2).
- **FR-016**: The three link types MUST be visually distinct: Association shows a plain
  arrow at the destination; Generalization shows a hollow triangular arrowhead at the
  destination; Composition shows a filled diamond at the source.

**Selection**

- **FR-017**: In Select mode, clicking inside a basic or composite object MUST select
  that object, deselect all others, and set the clicked object's depth to the minimum
  (front). Clicking on empty canvas MUST deselect all.
- **FR-018**: A selected basic object MUST continue to show its port handles. A selected
  composite MUST show only its bounding box outline (no port handles).

**Multi-Selection**

- **FR-019**: In Select mode, pressing the mouse on empty canvas (not inside any object)
  and dragging MUST render a live dashed selection rectangle.
- **FR-020**: On mouse release, all objects whose bounding boxes are COMPLETELY ENCLOSED
  within the drag rectangle MUST become selected; partially enclosed objects MUST NOT.
- **FR-021**: If the drag rectangle encloses no objects, all prior selections MUST be
  cleared (Alt C.3).

**Moving**

- **FR-022**: In Select mode, dragging a selected object (basic or composite) MUST move
  it so that its position on release equals the drag endpoint; all links connected to
  any basic object within the moved object MUST be redrawn immediately.

**Select Mode Mouse Press Priority**

- **FR-023**: On `mousePressed` in Select mode, the system MUST evaluate the press
  location in this exact priority order:
  1. **Port hit**: If the press point falls within the hit area of a visible port handle
     on any basic object → enter resize mode for that port (see FR-024–FR-027).
  2. **Object hit**: Else if the press point is inside any object (checked in reverse
     z-order, frontmost first) → select that object and enter move mode (see FR-022).
  3. **Empty canvas**: Else → enter drag-selection rectangle mode (see FR-019–FR-021).
  These three branches are mutually exclusive; only the first matching branch executes.

**Resizing**

- **FR-024**: In Select mode, when a basic object's ports are visible (hover or
  selection) and the user clicks and drags a port handle, the object MUST resize
  continuously with the drag.
- **FR-025**: The mouse release coordinates become the new position of the dragged port;
  the system MUST recalculate the object's bounding box accordingly.
- **FR-026**: Reversed dragging (dragging a port past the opposite edge) MUST be handled:
  the system recalculates and swaps the origin corner; width and height are the absolute
  values of the drag delta (Alt F.2).
- **FR-027**: If the resulting width or height is less than 20 px, the system MUST clamp
  to 20 px (Alt F.3).
- **FR-028**: Composite objects MUST NOT be resizable; dragging a composite produces no
  size change (Alt F.1).

**Grouping**

- **FR-029**: Edit → Group MUST be available when ≥ 2 objects of any type are selected.
  Executing it removes all selected objects from `DiagramModel`'s top-level list,
  transfers them as exclusive children of a new `CompositeObject`, and adds the
  composite to the top-level list. The composite's bounding box is the union of all
  children's bounding boxes.
- **FR-030**: Edit → Group with fewer than 2 objects selected MUST produce no action
  (Alt D.1).
- **FR-031**: Composites may be nested: a composite may be grouped with other objects to
  form a larger composite.

**Ungrouping**

- **FR-032**: Edit → Ungroup MUST be available only when exactly 1 composite is selected.
  Executing it removes the composite from `DiagramModel`'s top-level list and
  re-inserts its direct children into the top-level list at the composite's former
  position. Nested sub-composites within those children remain intact as their own
  `CompositeObject` instances (Alt D, Case 2: "解構最外一層").
- **FR-033**: Edit → Ungroup when more than 1 object is selected (or when the single
  selected object is a basic object) MUST produce no action (Alt D.2).

**Label Customization**

- **FR-034**: Edit → Label MUST be accessible when exactly 1 basic object is selected.
- **FR-035**: Invoking Edit → Label MUST open a "Customize Label Style" dialog pre-filled
  with the object's current label name and current fill color.
- **FR-036**: The dialog MUST provide a text field for Label Name and a color chooser for
  Label Color (background fill color of the object).
- **FR-037**: Clicking OK MUST immediately update the object's label text and fill color
  on the canvas. Clicking Cancel MUST leave the object unchanged.

**Depth Ordering and Hit-Testing**

- **FR-038**: Z-order MUST be represented by the position of each `GraphicObject` in
  `DiagramModel`'s top-level `List<GraphicObject>`. Index 0 = deepest; last index =
  front. No explicit depth integer field is stored on any object class.
- **FR-039**: When the canvas repaints, `CanvasPanel` MUST iterate the top-level list
  from index 0 to the last index, drawing each object in that order so that the last
  object in the list (front) is painted on top of all others.
- **FR-040**: When a mouse event occurs at a point where objects overlap, `DiagramModel`
  MUST iterate the top-level list in REVERSE (last index first) and return the first
  object whose `contains(point)` returns true — this is the frontmost object.
- **FR-041**: Selecting an object MUST move it to the end of `DiagramModel`'s top-level
  list (`list.remove(obj); list.add(obj)`), making it the frontmost object without
  altering any other object's position in the list.

### Key Entities

- **GraphicObject** (abstract): Common base for BasicObject and CompositeObject.
  Provides: `draw(Graphics2D)`, `contains(Point)`, `getBoundingBox(): Rectangle`,
  `getLabel(): String`, `getLabelColor(): Color`, `getDepth(): int`. Does not include
  Link.
- **BasicObject** extends GraphicObject: Represents a Rect or Oval. Adds: `getPorts():
  List<Point>` (computed from bounding box), `getShapeType()`. Ports for Rect are
  computed at the 4 corners and 4 edge midpoints; ports for Oval at top/bottom/left/right
  of the bounding box.
- **CompositeObject** extends GraphicObject: OWNS its children via an exclusive child
  list (`List<GraphicObject>`). On Group, selected objects are removed from
  `DiagramModel`'s top-level list and transferred into this child list. The composite
  is then added to the top-level list. On Ungroup, the composite is removed from the
  top-level list and its direct children are re-inserted there; nested sub-composites
  remain intact as their own `CompositeObject` instances. Bounding box is the union of
  all children's bounding boxes (recomputed on demand). Cannot host links; not resizable.
- **Link**: NOT a GraphicObject subtype. Stores: link type (Association/Generalization/
  Composition), reference to source `BasicObject`, source port index, reference to
  target `BasicObject`, target port index. Endpoint positions are recomputed lazily
  from the referenced objects' current port positions on every repaint — no listener
  or observer wiring required.
- **DiagramModel**: The single source of truth. Owns:
  (1) an ordered `List<GraphicObject>` of top-level objects where list position IS the
  z-order (last element = lowest depth = front; painted last = on top); and
  (2) a `List<Link>` of all links. On selection, the selected object is moved to the
  end of the top-level list. Hit-testing iterates the list in reverse (end → start) so
  the frontmost object is found first.
- **ToolMode** (Strategy interface): Declares `onMousePressed(MouseEvent, DiagramModel,
  CanvasPanel)`, `onMouseDragged(...)`, `onMouseReleased(...)`, `onMouseMoved(...)`.
  One concrete implementation per tool button (e.g., `SelectMode`, `RectMode`,
  `OvalMode`, `AssociationMode`, `GeneralizationMode`, `CompositionMode`). Adding a
  new tool = new class only, zero changes to existing classes.
- **CanvasPanel** (View): Extends `JPanel`. Holds a reference to the active `ToolMode`
  and the `DiagramModel`. `paintComponent(Graphics g)` iterates `DiagramModel`'s
  top-level list front-to-back (index 0 first, last index on top), drawing each
  `GraphicObject` and then all `Link`s. All mouse events are delegated to the active
  `ToolMode`. Never mutates the model directly.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a multi-shape diagram with 5+ shapes, 3+ link types, one
  group, and one label change within 5 minutes of first launch.
- **SC-002**: All seven use cases (A through G) can be demonstrated sequentially with
  zero incorrect behaviors during a 10-minute TA demo.
- **SC-003**: Creating, grouping, and ungrouping 20 objects completes with no perceptible
  delay.
- **SC-004**: Moving objects and repainting attached links occurs in real time with no
  visible lag during drag.
- **SC-005**: All link-creation failure conditions (Alt B.1, B.2) produce no visible
  error state and no exception visible to the user.
- **SC-006**: The application launches to a working canvas in under 3 seconds.
- **SC-007**: Port display on hover responds within one repaint cycle (no noticeable
  latency) when the mouse enters a basic object's bounds.

## Assumptions

- Minimum visible size for a basic object is 20×20 px (as stated in the PDF spec).
- Port handles are rendered as solid black squares (~8×8 px) centered on the computed
  port position, consistent with the reference diagrams in the PDF.
- Depth values start at 0 for the first object created; each subsequent object gets
  depth = current_max + 1 (or wrapped within 0–99). On selection, depth is set to 0
  and all others increment by 1 to maintain relative order.
- File operations (save, load, new) are out of scope; the File menu exists structurally
  but its items need not function for the initial submission.
- Label text is rendered centered within the object's bounding box; the text color is
  always black regardless of fill color.
- The color chooser for Label Color uses any standard color selection mechanism (exact
  UI not mandated by the PDF).
- Only one diagram is open at a time; no multi-tab or multi-window required.
- Undo/redo is out of scope.
- Keyboard shortcuts are not required; all interactions are mouse-driven via toolbar,
  canvas, and menu.
- The "Label" menu item under Edit applies only to basic objects. Composite label
  customization is not mentioned in the PDF and is therefore out of scope.
