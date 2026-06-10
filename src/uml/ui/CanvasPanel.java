package uml.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import uml.model.BasicObject;
import uml.model.CompositeObject;
import uml.model.DiagramModel;
import uml.model.GraphicObject;
import uml.model.Link;
import uml.tool.ToolMode;

/**
 * The drawing surface.
 *
 * Responsibilities (Principle IV):
 *   - Own the DiagramModel reference for rendering.
 *   - Hold the active and previous ToolMode references.
 *   - Forward all four mouse event types to the active ToolMode (Strategy pattern).
 *   - Render objects and transient overlays in paintComponent.
 *
 * This class intentionally contains NO interaction logic.  All decisions about
 * what a mouse event means are delegated entirely to the current ToolMode
 * (Principle V: no switch/instanceof for tool behaviour).
 *
 * paintComponent is expanded incrementally per phase:
 *   Phase 2 (T010): placeholder "Canvas" text.
 *   Phase 3 (T022/T023): full object rendering + preview overlay.
 *   Phase 4 (T026/T027): hover and selection indicators.
 *   Phase 5 (T035/T036): link rendering + link preview line.
 *   Phase 7 (T048): drag-select rectangle overlay.
 */
public class CanvasPanel extends JPanel {

    /**
     * Observer used by ToolBar to keep button highlighting synchronized with
     * programmatic tool changes (for example when RectMode/OvalMode revert to
     * the previous persistent tool after mouse release).
     */
    public interface ToolChangeListener {
        void toolChanged(ToolMode mode);
    }

    private final DiagramModel model;
    private ToolMode activeMode;
    private ToolMode previousMode;
    private ToolChangeListener toolChangeListener;

    // ── Transient overlay state ───────────────────────────────────────────────

    // Phase 3 (T023): shape-creation preview
    private Rectangle previewBounds  = null;
    private boolean   previewIsOval  = false;

    // Phase 4 (T026): hovered object — triggers port-handle display on BasicObjects
    private GraphicObject hoveredObject = null;

    // Phase 4 (T027): current selection — drives port handles + menu enable state
    private List<GraphicObject> selection = Collections.emptyList();

    // Phase 5 (T035): dashed link-preview line shown while dragging a link tool
    private Point linkPreviewStart = null;
    private Point linkPreviewEnd   = null;

    // Phase 7 (T048): dashed drag-select rectangle overlay
    private Rectangle dragRect = null;

    public CanvasPanel(DiagramModel model) {
        this.model = model;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));

        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mousePressed (MouseEvent e) {
                if (activeMode != null) activeMode.onMousePressed(e, model, CanvasPanel.this);
            }
            @Override public void mouseDragged (MouseEvent e) {
                if (activeMode != null) activeMode.onMouseDragged(e, model, CanvasPanel.this);
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (activeMode != null) activeMode.onMouseReleased(e, model, CanvasPanel.this);
            }
            @Override public void mouseMoved   (MouseEvent e) {
                if (activeMode != null) activeMode.onMouseMoved(e, model, CanvasPanel.this);
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    // ── Tool management ───────────────────────────────────────────────────────

    /**
     * Switch the active tool.
     *
     * Only saves the outgoing tool as {@code previousMode} when that tool is
     * non-transient — this prevents RectMode/OvalMode from overwriting the
     * persistent tool that the user explicitly chose.
     */
    public void setActiveTool(ToolMode mode) {
        if (activeMode != null && !activeMode.isTransient()) {
            previousMode = activeMode;
        }
        activeMode = mode;
        if (toolChangeListener != null) {
            toolChangeListener.toolChanged(activeMode);
        }
    }

    /**
     * Register a listener for active-tool changes.
     *
     * CanvasPanel remains the owner of the active ToolMode, while ToolBar uses
     * this callback only for view synchronization.  This keeps transient-mode
     * reversion centralized in CanvasPanel and prevents RectMode/OvalMode from
     * knowing anything about toolbar buttons.
     */
    public void setToolChangeListener(ToolChangeListener listener) {
        this.toolChangeListener = listener;
    }

    /** Return the last non-transient tool that was active before the current one. */
    public ToolMode getPreviousMode() {
        return previousMode;
    }

    // ── Hover and selection mutators (T026 / T027) ───────────────────────────

    /**
     * Update the hovered object and repaint only if it actually changed.
     *
     * Called by SelectMode.onMouseMoved on every mouse-move event.  The changed
     * guard prevents continuous repaints while the mouse glides over empty canvas.
     *
     * @param obj the GraphicObject under the cursor, or {@code null} if none
     */
    public void setHoveredObject(GraphicObject obj) {
        if (hoveredObject != obj) {
            hoveredObject = obj;
            repaint();
        }
    }

    /**
     * Replace the current selection with the given list and repaint.
     *
     * SelectMode passes an unmodifiable snapshot so CanvasPanel cannot mutate
     * the list through this reference.  The canvas stores it directly — no
     * defensive copy needed because the source is already unmodifiable.
     *
     * @param newSelection unmodifiable list of selected objects; may be empty
     */
    public void setSelection(List<GraphicObject> newSelection) {
        this.selection = newSelection;
        repaint();
    }

    /**
     * Return the current selection for use by MainFrame Edit-menu actions.
     *
     * @return unmodifiable view; never {@code null}
     */
    public List<GraphicObject> getSelection() {
        return selection;
    }

    /**
     * Return the object currently under the mouse cursor, or {@code null}.
     *
     * Used by SelectMode.onMousePressed (branch 1 — port-hit / resize) to
     * identify which BasicObject's port handles are currently visible so it
     * can test whether the press landed on one of them.
     *
     * @return the hovered GraphicObject, or {@code null} if the cursor is over
     *         empty canvas
     */
    public GraphicObject getHoveredObject() {
        return hoveredObject;
    }

    // ── Preview state mutators (T023) ─────────────────────────────────────────

    /**
     * Set the shape-creation preview overlay.
     *
     * Called by RectMode/OvalMode on each drag event so the user sees a live
     * outline of the shape being drawn before the mouse is released.
     *
     * @param bounds    normalized bounding box (always positive w/h)
     * @param isOval    {@code true} → draw oval outline; {@code false} → rect outline
     */
    public void setPreviewBounds(Rectangle bounds, boolean isOval) {
        this.previewBounds = bounds;
        this.previewIsOval = isOval;
    }

    /** Clear the shape-creation preview overlay (called on mouse release). */
    public void clearPreviewBounds() {
        this.previewBounds = null;
    }

    // ── Link preview mutators (T035) ──────────────────────────────────────────

    /**
     * Set the dashed link-preview line shown while a link tool is being dragged.
     *
     * {@code start} is the source port anchor (fixed during the drag);
     * {@code end} follows the cursor.  Called by AbstractLinkMode on each drag event.
     *
     * @param start source port point (never null)
     * @param end   current cursor position (never null)
     */
    public void setLinkPreview(Point start, Point end) {
        this.linkPreviewStart = start;
        this.linkPreviewEnd   = end;
        repaint();
    }

    /**
     * Clear the link-preview line (called on mouse release, successful or not).
     */
    public void clearLinkPreview() {
        this.linkPreviewStart = null;
        this.linkPreviewEnd   = null;
        repaint();
    }

    // ── Drag-select rectangle mutators (T048) ─────────────────────────────────

    /**
     * Set the drag-select rectangle overlay shown while the user rubber-bands a
     * multi-selection in empty canvas space.
     *
     * Called by SelectMode during DRAG_SELECTING state on each mouse-drag event.
     * The rectangle is already normalized (positive width/height) by GeomUtils.normalizeRect.
     *
     * @param rect normalized drag rectangle; must not be null
     */
    public void setDragRect(Rectangle rect) {
        this.dragRect = rect;
        repaint();
    }

    /**
     * Clear the drag-select rectangle overlay (called on mouse release in SelectMode).
     */
    public void clearDragRect() {
        this.dragRect = null;
        repaint();
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Full paint routine (T022 / T023 / T026 / T027).
     *
     * Layer order:
     *   1. White background       (super.paintComponent)
     *   2. All GraphicObjects     in z-order (index 0 = back → last = front)
     *   3. Hover / selection      indicators per object (port squares or bbox outline)
     *   4. Links                  (Phase 5 — T036)
     *   5. Link preview line      (Phase 5 — T035)
     *   6. Shape-creation preview (dashed rect/oval outline, Phase 3)
     *   7. Drag-select rectangle  (Phase 7 — T048)
     *
     * No interaction logic lives here — indicators are driven entirely by the
     * {@code hoveredObject} and {@code selection} fields set by SelectMode.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ── Layer 1: all diagram objects in z-order ───────────────────────────
        for (GraphicObject obj : model.getObjects()) {
            obj.draw(g2);
        }

        // ── Layer 2: hover and selection indicators ───────────────────────────
        // Drawn after all objects so handles always appear on top of shapes.
        for (GraphicObject obj : model.getObjects()) {
            boolean isHovered  = (obj == hoveredObject);
            boolean isSelected = selection.contains(obj);
            if (isHovered || isSelected) {
                drawIndicators(g2, obj);
            }
        }

        // ── Layer 3: links (shaft + arrowhead, on top of all shapes) ─────────
        // Link.draw() re-derives endpoints from connected ports each frame,
        // so links automatically follow objects that have been moved or resized.
        for (Link link : model.getLinks()) {
            link.draw(g2);
        }

        // ── Layer 4: link-creation preview (dashed line while dragging) ───────
        if (linkPreviewStart != null && linkPreviewEnd != null) {
            Stroke savedStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(
                1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{6f, 4f}, 0f
            ));
            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(linkPreviewStart.x, linkPreviewStart.y,
                        linkPreviewEnd.x,   linkPreviewEnd.y);
            g2.setStroke(savedStroke);
        }

        // ── Layer 5: shape-creation preview (dashed outline) ─────────────────
        if (previewBounds != null) {
            Stroke saved = g2.getStroke();
            g2.setStroke(new BasicStroke(
                1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{6f, 4f}, 0f
            ));
            g2.setColor(Color.DARK_GRAY);
            if (previewIsOval) {
                g2.drawOval(previewBounds.x, previewBounds.y,
                            previewBounds.width, previewBounds.height);
            } else {
                g2.drawRect(previewBounds.x, previewBounds.y,
                            previewBounds.width, previewBounds.height);
            }
            g2.setStroke(saved);
        }

        // ── Layer 6: drag-select rectangle (Phase 7 — T048) ──────────────────
        // Drawn last so it appears above all objects and overlays.
        // The dashed border uses a shorter dash pattern ({4f,4f}) to visually
        // distinguish it from the link-preview line ({6f,4f}).
        if (dragRect != null) {
            Stroke saved = g2.getStroke();
            g2.setStroke(new BasicStroke(
                1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{4f, 4f}, 0f
            ));
            g2.setColor(Color.BLUE);
            g2.drawRect(dragRect.x, dragRect.y, dragRect.width, dragRect.height);
            g2.setStroke(saved);
        }
    }

    /**
     * Draw the appropriate interaction indicator for {@code obj}.
     *
     * BasicObject   → 8×8 black squares centred on each port point.
     * CompositeObject → dashed bounding-box outline (Phase 7: T043 adds CompositeObject).
     *
     * Using a dedicated helper keeps paintComponent's layer structure readable
     * and makes it easy to extend indicators per type without changing the loop.
     */
    private void drawIndicators(Graphics2D g2, GraphicObject obj) {
        if (obj instanceof BasicObject) {
            // Draw 8×8 filled black squares centred on each port anchor point.
            g2.setColor(Color.BLACK);
            for (Point port : ((BasicObject) obj).getPorts()) {
                g2.fillRect(port.x - 4, port.y - 4, 8, 8);
            }
        } else if (obj instanceof CompositeObject) {
            // Draw a solid blue bounding-box outline to show the composite is selected
            // or hovered.  No port handles — composites are not resizable (FR-032).
            Rectangle bbox = obj.getBoundingBox();
            Stroke saved = g2.getStroke();
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(bbox.x, bbox.y, bbox.width, bbox.height);
            g2.setStroke(saved);
        }
    }
}
