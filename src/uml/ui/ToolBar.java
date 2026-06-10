package uml.ui;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import uml.tool.AssociationMode;
import uml.tool.CompositionMode;
import uml.tool.GeneralizationMode;
import uml.tool.OvalMode;
import uml.tool.RectMode;
import uml.tool.SelectMode;
import uml.tool.ToolMode;

/**
 * The vertical tool-selection panel on the left edge of the window.
 *
 * Responsibilities (Principle IV):
 *   - Render 6 toggle buttons in a ButtonGroup (only one active at a time).
 *   - Keep track of which button is visually highlighted.
 *   - Notify CanvasPanel of the newly selected tool on each button press.
 *
 * Tool-mode wiring:
 *   Phase 2 (T011): anonymous no-op stubs (compile- and demo-safe).
 *   Phase 3 (T021): concrete SelectMode, RectMode, OvalMode, link-mode instances.
 *
 * The {@code modes} array is package-private so that T021 can replace entries
 * with concrete implementations without restructuring the button/event wiring.
 */
public class ToolBar extends JPanel {

    // Button ordering: 0=Select 1=Association 2=Generalization 3=Composition 4=Rect 5=Oval
    // T021 replaces these stubs with concrete ToolMode instances.
    ToolMode[] modes;

    private final JToggleButton[] buttons;

    private static final String[] LABELS = {
        "Select", "Association", "Generalization", "Composition", "Rect", "Oval"
    };

    public ToolBar(CanvasPanel canvas) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        modes   = buildStubModes();
        buttons = new JToggleButton[LABELS.length];
        ButtonGroup group = new ButtonGroup();

        for (int i = 0; i < LABELS.length; i++) {
            final int idx = i;
            JToggleButton btn = new JToggleButton(LABELS[i]);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
            btn.addActionListener(e -> {
                canvas.setActiveTool(modes[idx]);
            });
            group.add(btn);
            add(btn);
            buttons[i] = btn;
        }

        // Keep visual button state synchronized with CanvasPanel's active tool.
        // This also covers programmatic changes, especially transient Rect/Oval
        // modes reverting to the previous persistent mode after object creation.
        canvas.setToolChangeListener(this::highlightMode);

        // Select is highlighted and active at startup
        buttons[0].setSelected(true);
        canvas.setActiveTool(modes[0]);
    }

    // ── Visual state ─────────────────────────────────────────────────────────

    private void highlightButton(JToggleButton active) {
        for (JToggleButton btn : buttons) {
            btn.setBackground(null);
            btn.setForeground(null);
        }
        active.setBackground(Color.DARK_GRAY);
        active.setForeground(Color.WHITE);
        active.setSelected(true);
    }

    /** Highlight the button whose mode instance is currently active. */
    private void highlightMode(ToolMode activeMode) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == activeMode) {
                highlightButton(buttons[i]);
                return;
            }
        }
    }

    // ── Concrete mode factory (Phase 3 / T021) ───────────────────────────────

    /**
     * Build one concrete ToolMode instance per toolbar button.
     *
     * Button order: 0=Select  1=Association  2=Generalization
     *               3=Composition  4=Rect  5=Oval
     *
     * Each button press in the ActionListener calls canvas.setActiveTool(modes[idx]),
     * which internally updates previousMode when the outgoing tool is non-transient.
     * RectMode and OvalMode override isTransient() → true so they are never stored
     * as previousMode, ensuring repeated shape creation always reverts to Select
     * (or whichever persistent tool the user last chose).
     */
    private static ToolMode[] buildStubModes() {
        return new ToolMode[]{
            new SelectMode(),        // 0 — Select
            new AssociationMode(),   // 1 — Association
            new GeneralizationMode(),// 2 — Generalization
            new CompositionMode(),   // 3 — Composition
            new RectMode(),          // 4 — Rect   (transient)
            new OvalMode(),          // 5 — Oval   (transient)
        };
    }
}
