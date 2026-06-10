# Research: UML / Workflow Editor

**Branch**: `001-uml-editor` | **Date**: 2026-04-14  
**Phase**: 0 — Decisions consolidated before design

---

## Decision 1 — Language and Runtime

**Decision**: Java 17 LTS with standard JDK Swing library.  
**Rationale**: Assignment mandates Java; Java 17 is the current LTS, offers `sealed classes`
(useful for `LinkType` if desired), and is widely available on lab machines. Java Swing is
the only GUI toolkit allowed by the constitution.  
**Alternatives considered**:
- Java 11: also LTS, but older; 17 adds no breaking Swing changes and is preferable for
  longevity.
- JavaFX: explicitly prohibited by constitution.
- AWT-only: Swing is built on AWT and adds double-buffering, layout managers, and
  JColorChooser — using AWT alone would require re-implementing these.

---

## Decision 2 — Architecture Pattern: Strategy for Tool Modes

**Decision**: `ToolMode` interface (Strategy pattern); one concrete class per tool button;
`CanvasPanel` holds `activeMode` reference and delegates all four mouse events to it.  
**Rationale**:
- Eliminates one large switch/if-else block in CanvasPanel (Principle V, Principle III).
- Adding a new tool = new class only (Open-Closed, Principle III).
- Each ToolMode is independently readable and testable (Principle IV).
- Matches the clarified answer Q1/Q11 from `/speckit.clarify`.  
**Alternatives considered**:
- Command pattern per event: overkill; tools are stateful (they track press/drag/release
  state across three events), so a per-event command does not naturally encapsulate that.
- Enum with switch: violates Principle V (instanceof/switch on type in shared logic).

---

## Decision 3 — Domain Model: GraphicObject Hierarchy

**Decision**:
```
GraphicObject (abstract)
├── BasicObject (abstract)
│   ├── RectObject
│   └── OvalObject
└── CompositeObject
Link (separate — NOT a GraphicObject)
```
**Rationale**: PDF classifies objects into exactly three categories: Basic Object, Link,
Composite. Link is not a "graphic object" in the PDF's terminology — it connects two basic
objects but is not selectable, groupable, or moveable as an independent entity. Keeping
Link outside the hierarchy prevents polymorphic misuse.  
**Alternatives considered**:
- Link extends GraphicObject: would allow Link to appear in the model's top-level list
  accidentally and be selected/grouped, both of which are spec violations.
- Single `Shape` class with a type field: violates Principle V and Principle III.

---

## Decision 4 — Z-Order: List Position in DiagramModel

**Decision**: Z-order is represented solely by the position of each `GraphicObject` in
`DiagramModel.objects` (a `List<GraphicObject>`). Index 0 = deepest (painted first);
last index = front (painted last, hit-tested first). No explicit depth integer field.  
**Rationale**:
- Simple, correct, and O(n) for all operations (paint, hit-test, bring-to-front).
- No need to maintain consistency between a depth field and list position.
- Matches clarified answer Q4 from `/speckit.clarify`.  
**Alternatives considered**:
- Explicit `depth` int field: requires a second sort key; two sources of truth for z-order.
- `TreeMap<Integer, GraphicObject>`: depths must be unique; reassignment on select is
  complex.

---

## Decision 5 — Port Representation: Computed, Not Stored

**Decision**: Ports are computed lazily by `BasicObject.getPorts()`, which derives port
positions from the current bounding box at call time. Ports are `java.awt.Point` objects.
No port list is stored as a field.  
**Rationale**:
- Ports always reflect current bounding box (no stale-cache bug after resize or move).
- Computation is trivial arithmetic: 8 operations for Rect, 4 for Oval.
- Links store `sourcePortIndex` (int) rather than a `Point`; the absolute position is
  recovered by calling `source.getPorts().get(sourcePortIndex)` at repaint time.  
**Alternatives considered**:
- Stored port list updated on resize: introduces a cache-coherence problem; easy to
  forget updating on move.
- Port objects as first-class entities: over-engineered for this scope.

---

## Decision 6 — Link Endpoint Update: Lazy Recomputation

**Decision**: `Link.draw(Graphics2D)` recomputes start and end positions by calling
`source.getPorts().get(sourcePortIndex)` and `target.getPorts().get(targetPortIndex)` on
every repaint.  
**Rationale**:
- Zero observer wiring; no listener registration/deregistration needed.
- Repaint is already triggered on every move/resize; link endpoints are always current.
- At the expected scale (≤ 100 objects), the overhead is negligible.  
**Alternatives considered**:
- Observer/PropertyChangeListener: more infrastructure; same result.
- Storing absolute Point in Link and updating on each move: requires every move code path
  to also update all affected links — error-prone.

---

## Decision 7 — CompositeObject Bounding Box: Computed on Demand

**Decision**: `CompositeObject.getBoundingBox()` iterates its children and returns the
union of their bounding boxes each time it is called. No cached bounding box field.  
**Rationale**:
- Children can themselves be composites with their own dynamic bounding boxes.
- Caching would require cache invalidation on every child move/resize.
- `getBoundingBox()` is only called during repaint and hit-test — a small number of calls
  relative to the object count.  
**Alternatives considered**:
- Cached bbox with dirty flag: correct but adds complexity for minimal benefit at this
  scale.

---

## Decision 8 — Transient Tool Mode (Rect / Oval)

**Decision**: `CanvasPanel` stores `previousMode` (updated each time a non-transient tool
is activated). When `RectMode` or `OvalMode` finishes a creation gesture (`mouseReleased`),
it calls `canvas.setActiveTool(canvas.getPreviousMode())`.  
**Rationale**: PDF Use Case A step 6 states "回到之前的 mode" (return to the previous
mode). Select is the default previousMode on launch.  
**Alternatives considered**:
- Always revert to Select: violates the spec (previous mode could be Association, etc.).
- Stack-based history: overkill for one level of transience.

---

## Decision 9 — SelectMode Sub-State Machine

**Decision**: `SelectMode` maintains an internal enum `State { IDLE, MOVING, RESIZING,
DRAG_SELECTING }`. Each mouse event method switches on `state`.  
**Rationale**: The three branches on `mousePressed` (port-hit / object-hit / empty) are
mutually exclusive and must carry through to `mouseDragged` and `mouseReleased`. An enum
state is the simplest and most readable approach.  
**Alternatives considered**:
- Separate sub-mode objects for move/resize/drag-select: over-engineered; the three sub-
  states share the `SelectMode` context (hover object, selection list).

---

## Decision 10 — Grouping / Ungrouping Ownership

**Decision**: True tree ownership in `CompositeObject`. On `group(selected)`:
(a) remove selected objects from `DiagramModel.objects`; (b) add them as children of a
new `CompositeObject`; (c) add the composite to `DiagramModel.objects`.
On `ungroup(composite)`: (a) remove composite from `DiagramModel.objects`; (b) insert
its direct children into `DiagramModel.objects` at the same position.  
**Rationale**: Matches clarified answer Q3 + Q7 from `/speckit.clarify`. "解構最外一層"
(outermost layer only) maps exactly to re-inserting only `composite.getChildren()`, not
their descendants.  
**Alternatives considered**:
- Flat reference set (children remain in top-level list, tagged with a group ID): makes
  bounding box computation fragile; hard to implement nested groups correctly.
