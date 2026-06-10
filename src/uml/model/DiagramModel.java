package uml.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The single source of truth for all diagram state.
 *
 * Owns two ordered lists:
 *   - objects: every GraphicObject on the canvas, in z-order (index 0 = back).
 *   - links:   every Link between BasicObjects.
 *
 * Responsibilities (Principle IV):
 *   - Maintain z-order for objects.
 *   - Expose safe, read-only views to the canvas renderer.
 *   - Implement all structural operations (add, remove, move-to-front, group, ungroup).
 *
 * Methods are added per phase — empty stubs mark slots reserved for later phases.
 */
public class DiagramModel {

    private final List<GraphicObject> objects = new ArrayList<>();
    private final List<Link>          links   = new ArrayList<>();

    // ── Phase 3 (T016): object CRUD ──────────────────────────────────────────

    /** Append {@code obj} to the end of the z-order list (new object lands on top). */
    public void addObject(GraphicObject obj) {
        objects.add(obj);
    }

    /** Remove {@code obj} from the z-order list. No-op if not present. */
    public void removeObject(GraphicObject obj) {
        objects.remove(obj);
    }

    /**
     * Return a read-only view of all objects in z-order (index 0 = back, last = front).
     * CanvasPanel iterates this list in paintComponent; the view is safe to read during
     * painting because mutations happen only on the EDT outside paintComponent.
     */
    public List<GraphicObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    // ── Phase 4 (T024): z-order and hit-testing ──────────────────────────────

    /**
     * Move {@code obj} to the front of the z-order (painted last = visually on top).
     *
     * Implementation: remove from its current position, re-append to end of list.
     * This is O(n) in list size; acceptable for the ≤ 100-object target scale.
     */
    public void moveToFront(GraphicObject obj) {
        objects.remove(obj);
        objects.add(obj);
    }

    /**
     * Return the frontmost object whose bounding area contains {@code p}, or
     * {@code null} if no object covers that point.
     *
     * Depth-ordering contract (US6 / T040):
     *   - {@code objects} is maintained in z-order: index 0 is the backmost object
     *     and the last index is the frontmost (painted last = visually on top).
     *   - This method iterates in reverse (highest index first) so that when two
     *     objects overlap, the one painted on top is always returned first.
     *   - {@link #moveToFront} keeps this contract consistent by re-appending a
     *     clicked object to the end of the list, which both repaints it on top and
     *     ensures the next call to this method returns it for the overlap region.
     *   - {@link uml.ui.CanvasPanel#paintComponent} iterates the same list forward
     *     (index 0 → last), so paint order and hit-test order are always in sync.
     */
    public GraphicObject getObjectAt(Point p) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            GraphicObject obj = objects.get(i);
            if (obj.contains(p)) return obj;
        }
        return null;
    }

    /**
     * Same as {@link #getObjectAt} but returns only {@link BasicObject} instances.
     *
     * Used by link modes: links may only connect to single (non-composite) shapes.
     * Clicking on a CompositeObject or empty canvas returns {@code null}.
     */
    public BasicObject getBasicObjectAt(Point p) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            GraphicObject obj = objects.get(i);
            if (obj instanceof BasicObject && obj.contains(p)) {
                return (BasicObject) obj;
            }
        }
        return null;
    }

    // ── Phase 5 (T029): link CRUD ────────────────────────────────────────────

    /** Append {@code link} to the links list. */
    public void addLink(Link link) {
        links.add(link);
    }

    /** Remove {@code link} from the links list. No-op if not present. */
    public void removeLink(Link link) {
        links.remove(link);
    }

    /**
     * Return a read-only view of all links.
     * CanvasPanel iterates this list in paintComponent to render link shafts
     * and arrowheads on top of all shapes.
     */
    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    // ── Phase 7 (T042, T044, T045): group / ungroup ──────────────────────────

    /**
     * Return every top-level object whose bounding box is completely inside {@code r}.
     *
     * "Completely inside" uses {@link Rectangle#contains(Rectangle)}: the object's
     * entire bounding box must fit within {@code r}.  Objects that only partially
     * overlap the drag rectangle are excluded.
     *
     * Called by SelectMode after the DRAG_SELECTING gesture completes (T041).
     *
     * @param r the drag-select rectangle (already normalized to positive w/h)
     * @return objects whose bounding boxes are fully enclosed by {@code r}; may be empty
     */
    public List<GraphicObject> getObjectsCompletelyInside(Rectangle r) {
        List<GraphicObject> result = new ArrayList<>();
        for (GraphicObject obj : objects) {
            if (r.contains(obj.getBoundingBox())) {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * Group the given objects into a new {@link CompositeObject}.
     *
     * Ownership transfer:
     *   1. All objects in {@code selected} are removed from the top-level list.
     *   2. A new {@code CompositeObject} is created with those objects as children.
     *   3. The composite is appended to the end of the z-order (visually on top).
     *
     * The caller is responsible for ensuring {@code selected.size() >= 2}.
     *
     * @param selected two or more objects to group; must all be present in the model
     * @return the newly created composite
     * @throws IllegalArgumentException if fewer than 2 objects are given
     */
    public CompositeObject group(List<GraphicObject> selected) {
        if (selected.size() < 2) {
            throw new IllegalArgumentException(
                "Group requires at least 2 objects; got " + selected.size());
        }
        objects.removeAll(selected);
        CompositeObject composite = new CompositeObject(selected);
        objects.add(composite);
        return composite;
    }

    /**
     * Dissolve the outermost layer of {@code composite}, restoring its direct children
     * to the top-level object list at the composite's former z-order position.
     *
     * Only the outermost layer is removed: if any child is itself a CompositeObject,
     * it remains intact as a composite and is simply re-inserted into the top-level list.
     *
     * Ownership transfer:
     *   1. The composite is removed; its index in the z-order is recorded.
     *   2. The children are inserted at that same index in their original relative order.
     *      This preserves depth ordering from before the group was formed as closely
     *      as possible.
     *
     * @param composite the composite to dissolve; must be present in the model
     * @return a new list containing the direct children that were restored
     */
    public List<GraphicObject> ungroup(CompositeObject composite) {
        int idx = objects.indexOf(composite);
        objects.remove(idx);
        List<GraphicObject> children = new ArrayList<>(composite.getChildren());
        objects.addAll(idx, children);
        return children;
    }

}
