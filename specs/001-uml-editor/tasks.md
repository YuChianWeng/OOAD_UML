# Tasks: UML / Workflow Editor

**Branch**: `001-uml-editor`  
**Input**: Design documents from `/specs/001-uml-editor/`  
**Prerequisites**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/ ✅  
**Tests**: No automated test tasks (not requested). Manual acceptance criteria are embedded in each phase checkpoint.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks in same phase)
- **[Story]**: Spec user story this task belongs to (US1–US6)
- Each task includes the exact file path in `src/uml/`

## Path Conventions

```
src/uml/app/       — Entry point
src/uml/model/     — Domain model (no Swing)
src/uml/tool/      — ToolMode strategy implementations
src/uml/ui/        — Swing view classes
src/uml/util/      — Stateless geometry helpers
```

---

## Phase 1: Setup

**Purpose**: Create project layout; ensure the application compiles and launches.

- [ ] T001 Create directory structure: `src/uml/{app,model,tool,ui,util}/` under repository root
- [ ] T002 Create build script: `Makefile` (or `pom.xml`) that compiles all `.java` files under `src/` and produces a runnable JAR with main class `uml.app.Main`

**Checkpoint**: `make run` (or `mvn exec:java`) opens an empty JFrame without errors.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish all abstract base classes, utility classes, and the application shell.  
**⚠️ CRITICAL**: No user-story work begins until this phase is complete — all later tasks depend on these classes.

- [ ] T003 Create `GraphicObject` abstract class with abstract methods `draw(Graphics2D)`, `contains(Point)`, `getBoundingBox():Rectangle`, `getLabel():String`, `getLabelColor():Color` — no Swing imports in `src/uml/model/GraphicObject.java`
- [ ] T004 [P] Create `LinkType` enum with constants `ASSOCIATION`, `GENERALIZATION`, `COMPOSITION` in `src/uml/model/LinkType.java`
- [ ] T005 Create `BasicObject` abstract class extending `GraphicObject` with fields `x`, `y`, `width`, `height` (all int), `label` (String, default `""`), `labelColor` (Color, default `Color.WHITE`); implement `getBoundingBox()`, `contains()`, `getLabel()`, `getLabelColor()`, `setLabel(String)`, `setLabelColor(Color)`, `moveTo(int x, int y)`, `resize(Rectangle r)` (clamps w/h to ≥ 20); leave `draw()` and `getPorts()` abstract in `src/uml/model/BasicObject.java`
- [ ] T006 Create `DiagramModel` with private `List<GraphicObject> objects` (ArrayList) and private `List<Link> links` (ArrayList); provide empty-body placeholder methods to be filled per phase in `src/uml/model/DiagramModel.java`
- [ ] T007 Create `ToolMode` interface with four method signatures: `onMousePressed`, `onMouseDragged`, `onMouseReleased`, `onMouseMoved` — all with parameters `(MouseEvent e, DiagramModel model, CanvasPanel canvas)` in `src/uml/tool/ToolMode.java`
- [ ] T008 [P] Create `PortUtils` with two static methods: `computeRectPorts(Rectangle bbox)` returning `List<Point>` of 8 points (TL, TM, TR, MR, BR, BM, BL, ML — clockwise starting top-left); `computeOvalPorts(Rectangle bbox)` returning `List<Point>` of 4 points (top, right, bottom, left) in `src/uml/util/PortUtils.java`
- [ ] T009 [P] Create `GeomUtils` with three static methods: `nearestPortIndex(List<Point> ports, Point p)` returning the index of the port closest to `p` (Euclidean); `union(Rectangle a, Rectangle b)` returning their minimum enclosing `Rectangle`; `normalizeRect(Point p1, Point p2)` returning a `Rectangle` with non-negative width/height regardless of corner order in `src/uml/util/GeomUtils.java`
- [ ] T010 Create `CanvasPanel` extending `JPanel`: holds `DiagramModel model` and `ToolMode activeMode` and `ToolMode previousMode`; implement `setActiveTool(ToolMode mode)` (updates `previousMode` when switching away from a non-transient mode, sets `activeMode`); implement `getPreviousMode()`; add `MouseAdapter` that delegates all four events to `activeMode`; `paintComponent` draws placeholder "Canvas" text for now in `src/uml/ui/CanvasPanel.java`
- [ ] T011 Create `ToolBar` extending `JPanel` with `BoxLayout` (Y-axis): add 6 `JToggleButton`s in a `ButtonGroup` labeled Select, Association, Generalization, Composition, Rect, Oval; on each button press call `canvas.setActiveTool(correspondingModeInstance)`; highlight active button by setting background to `Color.DARK_GRAY` / foreground to `Color.WHITE`; Select is highlighted on construction in `src/uml/ui/ToolBar.java`
- [ ] T012 Create `MainFrame` extending `JFrame`: `BorderLayout`; `CanvasPanel` in CENTER; `ToolBar` in WEST; `JMenuBar` with `JMenu("File")` (empty) and `JMenu("Edit")` containing disabled `JMenuItem`s Group, Ungroup, Label; title "UML Editor"; `setDefaultCloseOperation(EXIT_ON_CLOSE)`; pack and center on screen in `src/uml/ui/MainFrame.java`
- [ ] T013 Create `Main` entry point: `SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true))` in `src/uml/app/Main.java`

**Checkpoint**: Application launches showing menu bar, 6-button toolbar (Select highlighted), and blank canvas. Clicking each toolbar button highlights it and de-highlights the others.

---

## Phase 3: User Story 1 — Launch and Draw Basic Shapes (Priority: P1) 🎯 MVP

**Goal**: The user can draw rectangles and ovals on the canvas; mode reverts automatically after creation.

**Independent Test**: Press Rect, drag on canvas → rectangle appears with ≥ 20×20 px minimum; mode returns to Select. Press Oval, drag → oval appears. Canvas repaints correctly.

- [X] T014 [US1] Create `RectObject` extending `BasicObject`: implement `draw(Graphics2D g)` — fill bounding box with `labelColor`, draw black border (`g.drawRect`), draw `label` centered in black using `FontMetrics`; implement `getPorts()` delegating to `PortUtils.computeRectPorts(getBoundingBox())` in `src/uml/model/RectObject.java`
- [X] T015 [P] [US1] Create `OvalObject` extending `BasicObject`: same structure as `RectObject` but uses `g.fillOval` / `g.drawOval`; `getPorts()` delegates to `PortUtils.computeOvalPorts(getBoundingBox())` in `src/uml/model/OvalObject.java`
- [X] T016 [US1] Implement in `DiagramModel`: `addObject(GraphicObject obj)` appends to `objects`; `removeObject(GraphicObject obj)` removes from `objects`; `getObjects()` returns `Collections.unmodifiableList(objects)` in `src/uml/model/DiagramModel.java`
- [X] T017 [US1] Create `RectMode` implementing `ToolMode`: `onMousePressed` stores `pressPoint`; `onMouseDragged` calls `canvas.setPreviewBounds(GeomUtils.normalizeRect(pressPoint, e.getPoint()))` then `canvas.repaint()`; `onMouseReleased` computes final bounds (clamp w/h to ≥ 20), creates `new RectObject(bounds)`, calls `model.addObject(obj)`, calls `canvas.clearPreviewBounds()`, calls `canvas.setActiveTool(canvas.getPreviousMode())`, then `canvas.repaint()` in `src/uml/tool/RectMode.java`
- [X] T018 [P] [US1] Create `OvalMode` implementing `ToolMode`: identical structure to `RectMode` but creates `new OvalObject(bounds)` in `src/uml/tool/OvalMode.java`
- [X] T019 [US1] Create stub `SelectMode` implementing `ToolMode`: all four methods are empty bodies (no-op) for now — will be filled in Phase 4 and beyond in `src/uml/tool/SelectMode.java`
- [X] T020 [P] [US1] Create stub `AssociationMode`, `GeneralizationMode`, `CompositionMode`: each implements `ToolMode` with empty method bodies for now in `src/uml/tool/AssociationMode.java`, `GeneralizationMode.java`, `CompositionMode.java`
- [X] T021 [US1] Update `ToolBar` to instantiate one concrete `ToolMode` object per button and call `canvas.setActiveTool(mode)` on each press; Select button instantiates `SelectMode`; Rect/Oval buttons instantiate `RectMode`/`OvalMode`; link buttons instantiate their stub modes in `src/uml/ui/ToolBar.java`
- [X] T022 [US1] Implement `CanvasPanel.paintComponent(Graphics g)` fully: cast to `Graphics2D`; enable antialiasing (`RenderingHints.VALUE_ANTIALIAS_ON`); paint background white; iterate `model.getObjects()` from index 0 to last calling `obj.draw(g2)`; draw preview shape if `previewBounds != null` (dashed outline) in `src/uml/ui/CanvasPanel.java`
- [X] T023 [US1] Add preview state to `CanvasPanel`: fields `previewBounds` (Rectangle), `previewIsOval` (boolean); methods `setPreviewBounds(Rectangle, boolean)` and `clearPreviewBounds()`; update `paintComponent` to draw preview shape (dashed rect or oval) when `previewBounds != null`; update `RectMode` to pass `false` and `OvalMode` to pass `true` in `src/uml/ui/CanvasPanel.java`

**Checkpoint**: Rect and Oval creation fully working. Mode reverts to previous after creation. All created shapes are painted correctly and persist on canvas.

---

## Phase 4: User Story 3 (Partial) — Select, Hover, and Move (Priority: P3)

**Goal**: Objects can be hovered (showing ports/bbox), clicked to select, and dragged to move. Z-order (depth) is correct.

**Note**: Implemented before link creation (Phase 5) so that link-anchor behavior can be visually verified immediately.

**Independent Test**: Draw two overlapping shapes. Hover → ports appear on basic objects, bbox outline on composites. Click one → selected (ports stay visible), moves to front. Drag → object follows mouse; releases at correct position.

- [X] T024 [US3] Implement in `DiagramModel`: `moveToFront(GraphicObject obj)` — `objects.remove(obj); objects.add(obj)`; `getObjectAt(Point p)` — iterate `objects` in reverse, return first where `obj.contains(p)`, or `null`; `getBasicObjectAt(Point p)` — same but `instanceof BasicObject` filter in `src/uml/model/DiagramModel.java`
- [X] T025 [US3] Implement `SelectMode` object-hit branch (FR-023 priority 2 and 3): add fields `State { IDLE, MOVING }`, `GraphicObject moveTarget`, `Point moveOffset`, `List<GraphicObject> selection`; `onMousePressed`: call `model.getObjectAt(e.getPoint())`; if non-null → `model.moveToFront(hit)`, set `selection = [hit]`, set `state = MOVING`, compute `moveOffset = new Point(e.getX() - hit.getBoundingBox().x, e.getY() - hit.getBoundingBox().y)`, `canvas.repaint()`; if null → `state = IDLE`, clear selection, `canvas.repaint()`; `onMouseDragged` MOVING: call `moveTarget.moveTo(e.getX() - moveOffset.x, e.getY() - moveOffset.y)`, `canvas.repaint()`; `onMouseReleased` MOVING: `state = IDLE` in `src/uml/tool/SelectMode.java`
- [X] T026 [US3] Add hover tracking to `CanvasPanel`: field `hoveredObject` (GraphicObject, nullable); method `setHoveredObject(GraphicObject obj)` sets field and calls `repaint()` if changed; update `paintComponent` — after drawing each object, if `obj == hoveredObject && obj instanceof BasicObject` draw 8×8 black squares at each port point; if `obj == hoveredObject && obj instanceof CompositeObject` draw dashed bounding box outline in `src/uml/ui/CanvasPanel.java`
- [X] T027 [US3] Add selection rendering to `CanvasPanel`: field `List<GraphicObject> selection`; method `setSelection(List<GraphicObject>)`; update `paintComponent` — after drawing each object, if `obj` is in `selection && obj instanceof BasicObject` draw port handles; if `obj` is in `selection && obj instanceof CompositeObject` draw bounding box outline; add `getSelection()` for use by menu actions in `src/uml/ui/CanvasPanel.java`
- [X] T028 [US3] Update `SelectMode.onMouseMoved()`: call `model.getObjectAt(e.getPoint())`; call `canvas.setHoveredObject(result)`; update `SelectMode` so that `setSelection()` is called on canvas whenever selection changes in `src/uml/tool/SelectMode.java`

**Checkpoint**: Hover shows port squares on basic objects and bbox outline on composites. Click selects an object (ports remain visible) and brings it to front. Drag moves selected object. Click on empty canvas deselects all.

---

## Phase 5: User Story 2 — Link Creation (Priority: P2)

**Goal**: All three link types can be created between basic objects with correct arrowheads. Links redraw when objects move.

**Independent Test**: Draw two rectangles. Select Association → press inside rect A, drag to rect B, release → arrow appears connecting nearest ports. Repeat with Generalization and Composition. Move either rect → link follows. Press on composite or empty canvas → no link created.

- [X] T029 [US2] Implement in `DiagramModel`: `addLink(Link link)`, `removeLink(Link link)`, `getLinks()` returning unmodifiable view in `src/uml/model/DiagramModel.java`
- [X] T030 [US2] Create `Link` class with fields `LinkType type`, `BasicObject source`, `int sourcePortIndex`, `BasicObject target`, `int targetPortIndex`; constructor; getters; implement `draw(Graphics2D g)`: compute `startPt = source.getPorts().get(sourcePortIndex)` and `endPt = target.getPorts().get(targetPortIndex)`; draw line from `startPt` to `endPt` in `src/uml/model/Link.java`
- [X] T031 [US2] Implement arrowheads in `Link.draw()`: ASSOCIATION — open arrowhead (two lines at ~30°) at `endPt` pointing away from `startPt`; GENERALIZATION — hollow equilateral triangle at `endPt`; COMPOSITION — filled diamond (rhombus) at `startPt`; use `Graphics2D` path/polygon for each in `src/uml/model/Link.java`
- [X] T032 [US2] Implement `AssociationMode` fully: add fields `State { IDLE, DRAWING, INVALID }`, `BasicObject source`, `int sourcePortIndex`, `Point previewEnd`; `onMousePressed`: call `model.getBasicObjectAt(e.getPoint())`; if null → `state = INVALID`; else `source = hit`, `sourcePortIndex = GeomUtils.nearestPortIndex(source.getPorts(), e.getPoint())`, `state = DRAWING`, `canvas.setLinkPreview(source.getPorts().get(sourcePortIndex), e.getPoint())`, `canvas.repaint()`; `onMouseDragged` DRAWING: update `canvas.setLinkPreview(..., e.getPoint())`, `canvas.repaint()`; `onMouseReleased` DRAWING: get `target = model.getBasicObjectAt(e.getPoint())`; if null or `target == source` → abort, clear preview; else compute `targetPortIndex`, create `new Link(LinkType.ASSOCIATION, source, sourcePortIndex, target, targetPortIndex)`, `model.addLink(link)`, clear preview, `canvas.repaint()`; `state = IDLE` in `src/uml/tool/AssociationMode.java`
- [X] T033 [P] [US2] Implement `GeneralizationMode` with same structure as `AssociationMode` using `LinkType.GENERALIZATION` in `src/uml/tool/GeneralizationMode.java`
- [X] T034 [P] [US2] Implement `CompositionMode` with same structure using `LinkType.COMPOSITION` in `src/uml/tool/CompositionMode.java`
- [X] T035 [US2] Add link preview state to `CanvasPanel`: fields `linkPreviewStart` and `linkPreviewEnd` (both `Point`, nullable); methods `setLinkPreview(Point start, Point end)` and `clearLinkPreview()`; in `paintComponent` draw a dashed line from `linkPreviewStart` to `linkPreviewEnd` when both are non-null in `src/uml/ui/CanvasPanel.java`
- [X] T036 [US2] Update `CanvasPanel.paintComponent()` to iterate `model.getLinks()` after all objects and call `link.draw(g2)` for each; links always render on top of shapes in `src/uml/ui/CanvasPanel.java`

**Checkpoint**: All three link types create correctly. Arrowheads are visually distinct. Links redraw when connected objects are moved. All failure conditions (press on composite, release on same object, release on empty canvas) silently produce no link.

---

## Phase 6: User Story 3 (Full) — Resize; User Story 6 — Depth Verification (Priority: P3 / P6)

**Goal**: Port-drag resizes basic objects (reversed drag supported; 20 px minimum enforced). Z-order is fully verified.

**Independent Test**: Draw a rectangle; hover → 8 ports appear; drag a corner port → shape resizes live. Drag corner past opposite corner → shape mirrors correctly. Drag to tiny size → stops at 20×20. Draw two overlapping rects; click the one underneath → it comes to front; overlap area now selects it.

- [ ] T037 [US3] Extend `SelectMode` with RESIZING sub-state: add `State.RESIZING`, `BasicObject resizeTarget`, `int resizePortIndex`, `int oppositePortIndex`; update `onMousePressed` to check port-hit FIRST (FR-023 priority 1): if `canvas.hoveredObject instanceof BasicObject`, iterate that object's ports, if `distanceSq(e.getPoint(), port) <= PORT_RADIUS_SQ (16px²)` → `state = RESIZING`, store `resizeTarget`, `resizePortIndex`, compute `oppositePortIndex` (the port diagonally opposite for corners, or axis-opposite for midpoints), `return`; this check runs before the object-hit check in `src/uml/tool/SelectMode.java`
- [ ] T038 [US3] Implement `SelectMode.onMouseDragged()` RESIZING branch: compute opposite corner point from `resizeTarget` (the port at `oppositePortIndex`); call `GeomUtils.normalizeRect(e.getPoint(), oppositeCorner)` to get new bounds (handles reversed drag by producing positive w/h); call `resizeTarget.resize(newBounds)` (clamp enforced inside BasicObject); `canvas.repaint()` in `src/uml/tool/SelectMode.java`
- [ ] T039 [US3] Verify `BasicObject.resize(Rectangle r)` clamps: `this.width = Math.max(20, r.width)`, `this.height = Math.max(20, r.height)`, `this.x = r.x`, `this.y = r.y`; write a comment explaining the clamping invariant in `src/uml/model/BasicObject.java`
- [ ] T040 [US6] Verify depth ordering end-to-end: draw two overlapping `RectObject`s A and B; click B → `model.moveToFront(B)` moves B to end of list; `paintComponent` draws A first then B (B on top); `model.getObjectAt(overlap point)` iterates in reverse and returns B; add rationale comment to `DiagramModel.getObjectAt()` explaining the reverse-iteration contract in `src/uml/model/DiagramModel.java`

**Checkpoint**: Resize by port drag works from all 8 handle positions. Reversed drag mirrors the shape correctly. Minimum size enforced. Overlapping objects render and hit-test in correct z-order.

---

## Phase 7: User Story 4 — Multi-Selection and Group/Ungroup (Priority: P4)

**Goal**: Drag-rectangle selects multiple objects; Edit → Group combines them into a composite; Edit → Ungroup dissolves one layer.

**Independent Test**: Draw 3 shapes. Drag empty canvas → dashed rect appears → release → all enclosed shapes selected. Edit → Group → one composite. Click composite → selected as unit. Edit → Ungroup → 3 shapes restored. Nested composite remains intact after one Ungroup.

- [ ] T041 [US4] Extend `SelectMode` with `DRAG_SELECTING` sub-state: `onMousePressed` on empty canvas (after failing object-hit check) → `state = DRAG_SELECTING`, `dragStart = e.getPoint()`, clear selection; `onMouseDragged` DRAG_SELECTING: `canvas.setDragRect(GeomUtils.normalizeRect(dragStart, e.getPoint()))`, `canvas.repaint()`; `onMouseReleased` DRAG_SELECTING: `List<GraphicObject> enclosed = model.getObjectsCompletelyInside(dragRect)`, `canvas.setSelection(enclosed)`, `canvas.clearDragRect()`, `state = IDLE`, `canvas.repaint()` in `src/uml/tool/SelectMode.java`
- [ ] T042 [US4] Implement `DiagramModel.getObjectsCompletelyInside(Rectangle r)`: iterate `objects` forward; include `obj` if `r.contains(obj.getBoundingBox())`; return list in `src/uml/model/DiagramModel.java`
- [ ] T043 [US4] Create `CompositeObject` extending `GraphicObject`: field `List<GraphicObject> children` (package-private ArrayList); `getBoundingBox()` computes union of all `child.getBoundingBox()` using `GeomUtils.union()`; `contains(Point p)` returns `getBoundingBox().contains(p)`; `draw(Graphics2D g)` draws dashed bounding box outline then calls `child.draw(g)` for each child; `moveTo(int dx, int dy)` calls `moveTo(dx, dy)` on each child recursively (BasicObject.moveTo uses absolute coords — pass delta as new position = `child.bbox.x + dx`); `getChildren()` returns unmodifiable list; `getLabel()` returns `""`; `getLabelColor()` returns `Color.LIGHT_GRAY` in `src/uml/model/CompositeObject.java`
- [ ] T044 [US4] Implement `DiagramModel.group(List<GraphicObject> selected)`: validate `selected.size() >= 2` (throw `IllegalArgumentException` if not); remove all elements of `selected` from `objects`; create `CompositeObject composite = new CompositeObject(selected)`; `objects.add(composite)`; return `composite` in `src/uml/model/DiagramModel.java`
- [ ] T045 [US4] Implement `DiagramModel.ungroup(CompositeObject composite)`: find `idx = objects.indexOf(composite)`; `objects.remove(composite)`; `objects.addAll(idx, composite.getChildren())` — inserts children at composite's former index, preserving their relative order; return `new ArrayList<>(composite.getChildren())` in `src/uml/model/DiagramModel.java`
- [ ] T046 [US4] Wire Edit → Group in `MainFrame`: `ActionListener` reads `canvas.getSelection()`; if `selection.size() < 2` → return (no-op, FR-030); call `CompositeObject c = model.group(selection)`; call `canvas.setSelection(List.of(c))`; `canvas.repaint()` in `src/uml/ui/MainFrame.java`
- [ ] T047 [US4] Wire Edit → Ungroup in `MainFrame`: `ActionListener` reads `canvas.getSelection()`; if `selection.size() != 1 || !(selection.get(0) instanceof CompositeObject)` → return (no-op, FR-033); call `List<GraphicObject> children = model.ungroup((CompositeObject) selection.get(0))`; call `canvas.setSelection(children)`; `canvas.repaint()` in `src/uml/ui/MainFrame.java`
- [ ] T048 [US4] Add drag-rect state to `CanvasPanel`: field `dragRect` (Rectangle, nullable); `setDragRect(Rectangle)` and `clearDragRect()`; in `paintComponent` draw dashed rectangle (use `BasicStroke` with dash pattern `{4f, 4f}`) when `dragRect != null` in `src/uml/ui/CanvasPanel.java`
- [ ] T049 [US4] Verify composite move: select a composite; drag it → all children move; links attached to any child inside the composite redraw to the new positions (lazy recomputation in `Link.draw()` picks up the new `getPorts()` values automatically); add a comment in `Link.draw()` documenting this lazy-update contract in `src/uml/model/Link.java`

**Checkpoint**: Multi-selection drag works. Group creates a composite selectable as a unit. Ungroup dissolves one layer only; nested composites remain intact. Composite move updates all link endpoints.

---

## Phase 8: User Story 5 — Label Name and Fill Color (Priority: P5)

**Goal**: Edit → Label opens a dialog for the selected basic object; OK updates label text and fill color immediately.

**Independent Test**: Draw a rectangle. Select it. Edit → Label → dialog opens with empty name field and default color. Type "MyClass", pick yellow, click OK → rectangle fills yellow, shows "MyClass" centered inside. Click Cancel on a second open → no change.

- [ ] T050 [US5] Create `LabelDialog` extending `JDialog`: constructor `(Frame owner, BasicObject target, CanvasPanel canvas)`; modal; `JTextField nameField` pre-filled with `target.getLabel()`; `JButton chooseColorButton` pre-filled color swatch; clicking it opens `JColorChooser` and stores chosen color in `currentColor`; `JButton ok` calls `target.setLabel(nameField.getText())`, `target.setLabelColor(currentColor)`, `canvas.repaint()`, `dispose()`; `JButton cancel` calls `dispose()`; lay out using `GridBagLayout` or `BoxLayout`; title "Customize Label Style" in `src/uml/ui/LabelDialog.java`
- [ ] T051 [US5] Wire Edit → Label in `MainFrame`: `ActionListener` reads `canvas.getSelection()`; if `selection.size() != 1 || !(selection.get(0) instanceof BasicObject)` → return (no-op, FR-034); create `new LabelDialog(this, (BasicObject) selection.get(0), canvas).setVisible(true)` in `src/uml/ui/MainFrame.java`
- [ ] T052 [US5] Verify label and fill rendering: `RectObject.draw()` and `OvalObject.draw()` fill with `getLabelColor()` before drawing the border; render `getLabel()` centered using `FontMetrics.stringWidth` and `getAscent()` in black regardless of fill color; test with a filled yellow rectangle showing centered black text in `src/uml/model/RectObject.java`, `src/uml/model/OvalObject.java`

**Checkpoint**: Label dialog opens correctly only for basic objects. OK updates fill color and label text live on canvas. Cancel leaves everything unchanged.

---

## Phase 9: Polish and Cross-Cutting Concerns

**Purpose**: Edge case verification, cleanup, and full end-to-end demo readiness.

- [ ] T053 [P] Verify FR-023 SelectMode priority order: with a basic object hovered (ports visible), press on a port handle → resize activates (not move); press on object body (not a port) → move activates; press on empty canvas → drag-select activates; write inline comments in `SelectMode.onMousePressed()` labeling the three branches in `src/uml/tool/SelectMode.java`
- [ ] T054 [P] Verify all link failure conditions (FR-014, FR-015): press on composite → `getBasicObjectAt` returns null → state = INVALID → no link; release on same object as press → guard `target == source` → no link; release on composite or empty canvas → `getBasicObjectAt` returns null → no link; add a comment in each `onMouseReleased` guard clause in `src/uml/tool/AssociationMode.java`
- [ ] T055 [P] Verify reversed-drag resize (FR-026): drag top-left corner below and to the right of the bottom-right corner; confirm shape mirrors correctly (bounding box uses absolute delta); drag to produce w < 20 or h < 20 → confirm clamped to exactly 20 in `src/uml/tool/SelectMode.java`
- [ ] T056 [P] Verify Group/Ungroup edge cases: Group with 1 object selected → no action; Group with 0 selected → no action; Ungroup with basic object selected → no action; Ungroup with 2 objects selected → no action; confirm menu action guards match FR-030 and FR-033 in `src/uml/ui/MainFrame.java`
- [ ] T057 [P] Verify Rect/Oval click (no drag): press and release at same point → shape created at that point with minimum 20×20 px size; confirm `GeomUtils.normalizeRect` returns 0×0 which then gets clamped in `RectMode.onMouseReleased()` in `src/uml/tool/RectMode.java`, `OvalMode.java`
- [ ] T058 Remove all `System.out.println`, `e.printStackTrace()`, and debug print statements from every source file; verify no console output during normal operation (all files under `src/uml/`)
- [ ] T059 Demonstrate all 7 PDF use cases end-to-end: A (shape creation + transient mode), B (link creation + failure conditions), C (multi-select + group), D (ungroup), E (move composite), F (resize with port drag), G (label + fill color); record any failure and fix before closing this milestone

**Checkpoint**: All 7 use cases pass with zero incorrect behaviors. Application is demo-ready.

---

## Dependencies and Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
  └──► Phase 2 (Foundational) ← BLOCKS all user story phases
         ├──► Phase 3 (US1: Shape Creation)
         │      └──► Phase 4 (US3 partial: Select/Hover/Move)
         │             └──► Phase 5 (US2: Link Creation)
         │                    └──► Phase 6 (US3 full: Resize + US6: Depth)
         │                           └──► Phase 7 (US4: Multi-Select + Group/Ungroup)
         │                                  └──► Phase 8 (US5: Label)
         │                                         └──► Phase 9 (Polish)
```

**Note on US3/US2 ordering**: Select/Move (Phase 4) is implemented before Link Creation
(Phase 5) so that link anchor behavior is immediately visible and verifiable after creation.
This is a technical dependency, not a priority re-ordering; US2 tasks retain their [US2] labels.

### User Story Dependencies

| User Story | Can start after | Notes |
|------------|----------------|-------|
| US1 (P1) | Phase 2 complete | No other US dependency |
| US3 partial (P3) | US1 complete | Needs shapes on canvas to select |
| US2 (P2) | US3 partial complete | Needs select/hover for link testing |
| US3 full (P3) | US2 complete | Resize adds RESIZING branch to SelectMode |
| US4 (P4) | US3 full complete | Drag-select adds DRAG_SELECTING branch to SelectMode |
| US5 (P5) | Phase 2 complete | Label dialog only depends on BasicObject |
| US6 (P6) | US3 complete | Depth ordering verified alongside select/move |

### Parallel Opportunities Within Each Phase

**Phase 2 (Foundational)**:
- T004, T008, T009 can be written in parallel (distinct utility files)

**Phase 3 (US1)**:
- T014 (RectObject) and T015 (OvalObject) can be written in parallel
- T017 (RectMode) and T018 (OvalMode) can be written in parallel
- T019 (SelectMode stub) and T020 (link mode stubs) can be written in parallel

**Phase 5 (US2)**:
- T033 (GeneralizationMode) and T034 (CompositionMode) can be written in parallel after T032

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational — builds the entire class skeleton
3. Complete Phase 3: User Story 1 — shapes can be drawn and displayed
4. **STOP and VALIDATE**: The canvas shows rectangles and ovals; mode reverts; repaint is correct
5. This is the minimum runnable demo checkpoint

### Incremental Delivery (Recommended for Course Demo)

1. **Demo 1** (after Phase 3): Shape creation working — "Here's the canvas with shape drawing"
2. **Demo 2** (after Phase 4): Selection and movement — "I can select and move objects"
3. **Demo 3** (after Phase 5): Links — "I can connect objects with Association / Generalization / Composition"
4. **Demo 4** (after Phase 6): Resize — "Port drag resizes; reversed drag works; minimum size enforced"
5. **Demo 5** (after Phase 7): Group/Ungroup — "Multi-select, group into composite, ungroup one layer"
6. **Demo 6** (after Phase 8): Labels — "Name and fill color dialog working"
7. **Final** (after Phase 9): All 7 PDF use cases demonstrable

### SelectMode Is Built Incrementally

`SelectMode` grows across four phases — this is intentional:

| Phase | Branch added to SelectMode |
|-------|---------------------------|
| Phase 3 | Stub (no-op) |
| Phase 4 | Object-hit → select + move (MOVING state) |
| Phase 6 | Port-hit → resize (RESIZING state) |
| Phase 7 | Empty canvas → drag-select (DRAG_SELECTING state) |

Each addition is isolated to a new `if` branch in `onMousePressed` and a new case in
`onMouseDragged`/`onMouseReleased` — no existing branches are touched.

---

## Notes

- `[P]` tasks = different source files, no incomplete dependencies → can be written in parallel
- `[Story]` label maps each task to the user story it fulfills for traceability
- Commit after each phase checkpoint to preserve a runnable state at every milestone
- Never leave a phase partially complete before the next begins — honor Principle IX
- `SelectMode` is the most complex class; its incremental growth across phases keeps it manageable
- Total tasks: **59** (2 Setup + 11 Foundational + 10 US1 + 5 US3-partial + 8 US2 + 4 US3-full/US6 + 9 US4 + 3 US5 + 7 Polish)
