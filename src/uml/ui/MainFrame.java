package uml.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Collections;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import uml.model.BasicObject;
import uml.model.CompositeObject;
import uml.model.DiagramModel;
import uml.model.GraphicObject;

/**
 * The application window.
 *
 * Responsibilities (Principle IV):
 *   - Construct and wire the three primary UI components:
 *     CanvasPanel (CENTER), ToolBar (WEST), and JMenuBar (top).
 *   - Own the DiagramModel and pass it to CanvasPanel.
 *   - Wire Edit menu actions to model and canvas (Phase 7/8: T046, T047, T051).
 */
public class MainFrame extends JFrame {

    final DiagramModel model;
    final CanvasPanel  canvas;
    final ToolBar      toolBar;

    public MainFrame() {
        super("UML Editor");

        model   = new DiagramModel();
        canvas  = new CanvasPanel(model);
        toolBar = new ToolBar(canvas);

        setLayout(new BorderLayout());
        add(canvas,  BorderLayout.CENTER);
        add(toolBar, BorderLayout.WEST);

        JMenuBar bar = buildMenuBar();
        setJMenuBar(bar);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    // ── Menu construction ─────────────────────────────────────────────────────

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        // File — empty for now (persistence is out of scope)
        bar.add(new JMenu("File"));

        // Edit — Group and Ungroup wired here (Phase 7); Label wired in Phase 8
        JMenu edit = new JMenu("Edit");
        edit.add(namedItem("Group",   this::onGroup));
        edit.add(namedItem("Ungroup", this::onUngroup));
        edit.add(namedItem("Label",   this::onLabel));
        bar.add(edit);

        return bar;
    }

    /**
     * Edit → Group action (T046, FR-030).
     *
     * Guard: requires at least 2 objects selected; silently does nothing otherwise.
     * When the guard passes:
     *   1. The model replaces the selected objects with a new CompositeObject.
     *   2. The canvas selection is updated to contain only the composite.
     *   3. The canvas is repainted.
     */
    private void onGroup() {
        List<GraphicObject> sel = canvas.getSelection();
        if (sel.size() < 2) return;           // FR-030: no-op if fewer than 2

        CompositeObject composite = model.group(sel);
        canvas.setSelection(Collections.singletonList(composite));
        canvas.repaint();
    }

    /**
     * Edit → Ungroup action (T047, FR-033).
     *
     * Guard: requires exactly one CompositeObject to be selected; silently does
     * nothing if the selection contains anything else (or is empty, or has more
     * than one element).
     * When the guard passes:
     *   1. The model dissolves only the outermost composite layer.
     *   2. The canvas selection is updated to the restored children.
     *   3. The canvas is repainted.
     */
    private void onUngroup() {
        List<GraphicObject> sel = canvas.getSelection();
        if (sel.size() != 1) return;           // must be exactly one object
        if (!(sel.get(0) instanceof CompositeObject)) return; // must be composite

        List<GraphicObject> children = model.ungroup((CompositeObject) sel.get(0));
        canvas.setSelection(Collections.unmodifiableList(children));
        canvas.repaint();
    }

    /**
     * Edit → Label action (T051, FR-037).
     *
     * Guard: requires exactly one BasicObject to be selected.
     *   - Empty selection, multi-selection, or a CompositeObject → silent no-op.
     *
     * When the guard passes:
     *   1. Open a LabelDialog pre-filled with the object's current label and color.
     *   2. If the user presses OK:  apply the new name and color to the model object,
     *      then repaint the canvas so the change is immediately visible.
     *   3. If the user presses Cancel or closes the window: leave the object unchanged.
     */
    private void onLabel() {
        List<GraphicObject> sel = canvas.getSelection();
        if (sel.size() != 1) return;                         // must be exactly one object
        if (!(sel.get(0) instanceof BasicObject)) return;    // must be a basic (non-composite) object

        BasicObject target = (BasicObject) sel.get(0);

        LabelDialog dialog = new LabelDialog(
                (Frame) this,
                target.getLabel(),
                target.getLabelColor());

        if (dialog.isConfirmed()) {
            target.setLabel(dialog.getLabelName());
            target.setLabelColor(dialog.getLabelColor());
            canvas.repaint();
        }
    }

    /** Create an enabled JMenuItem whose action calls the given Runnable. */
    private static JMenuItem namedItem(String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }
}
