# Contract: DiagramModel Public API

**Package**: `uml.model`  
**Type**: Domain model / single source of truth  
**Date**: 2026-04-14

---

## Purpose

`DiagramModel` is the only class that owns and mutates the list of top-level
`GraphicObject`s and the list of `Link`s. All tool modes and menu actions operate
through this API — never by directly accessing the lists. This ensures that z-order
invariants, group invariants, and link integrity are maintained in one place.

---

## Invariants

1. **Z-order via list position**: `objects.get(0)` is the deepest (painted first);
   `objects.get(objects.size()-1)` is the frontmost (painted last, hit-tested first).
2. **Exclusive ownership**: A `GraphicObject` appears in exactly one place in the tree —
   either in the top-level `objects` list OR as a child of exactly one `CompositeObject`.
   It MUST NOT appear in both simultaneously.
3. **Link referential integrity**: Every `Link` in `links` references `BasicObject`
   instances that exist somewhere in the object tree (directly in `objects` or nested
   in a composite). Orphaned links (referencing removed objects) are removed when the
   referenced object is removed from the model.
4. **Minimum size**: `BasicObject.resize()` enforces ≥ 20 × 20 px before the model
   stores the updated bounding box.

---

## API

### Object Management

```java
// Add a new object to the front (end of list)
void addObject(GraphicObject obj)

// Remove object from the top-level list.
// Also removes any Link referencing this object (if obj is BasicObject).
void removeObject(GraphicObject obj)

// Read-only ordered view for painting (index 0 = deepest, last = front)
List<GraphicObject> getObjects()
```

### Z-Order

```java
// Move obj to the end of the list (bring to front).
// Precondition: obj is in the top-level list.
// Postcondition: obj == objects.get(objects.size()-1)
void moveToFront(GraphicObject obj)
```

### Hit-Testing

```java
// Iterate top-level list in reverse; return first obj where obj.contains(p) is true.
// Returns null if no object contains p.
GraphicObject getObjectAt(Point p)

// Same as getObjectAt but returns only BasicObject (skips CompositeObject).
// Returns null if no BasicObject contains p.
BasicObject getBasicObjectAt(Point p)

// Return all top-level objects whose bounding box is COMPLETELY inside rect r.
// Partial containment is excluded.
List<GraphicObject> getObjectsCompletelyInside(Rectangle r)
```

### Group / Ungroup

```java
// Precondition: selected.size() >= 2; all objects in selected are in top-level list.
// Removes selected from top-level list; creates CompositeObject with those as children;
// adds the new composite to end of top-level list.
// Postcondition: invariants 1 and 2 hold; the new composite is returned.
CompositeObject group(List<GraphicObject> selected)

// Precondition: composite is in the top-level list.
// Removes composite from top-level list; inserts composite.getChildren() at the
// composite's former index in the list (preserving relative z-order of children).
// Postcondition: invariants 1 and 2 hold; direct children are now top-level objects.
// Nested sub-composites within those children remain intact.
List<GraphicObject> ungroup(CompositeObject composite)
```

### Link Management

```java
// Add a link. Precondition: link.source and link.target are in the object tree.
void addLink(Link link)

// Remove a specific link.
void removeLink(Link link)

// Read-only view of all links (for painting).
List<Link> getLinks()
```

---

## Thread Safety

`DiagramModel` is NOT thread-safe. All calls MUST be made from the Swing Event Dispatch
Thread (EDT). `CanvasPanel` and all `ToolMode` implementations run on the EDT by default
(mouse event callbacks are always on the EDT in Swing).

---

## Error Handling

- Calling `group()` with `selected.size() < 2`: throws `IllegalArgumentException` or
  returns null (caller — `MainFrame` menu action — is responsible for precondition check).
- Calling `ungroup()` with a `CompositeObject` not in the top-level list: no-op or
  `IllegalArgumentException` (caller checks `selection == 1 composite` before calling).
- Callers (menu actions in `MainFrame`) are responsible for all precondition guards before
  invoking model mutation methods.
