package uml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Modal dialog for editing the label name and fill color of a BasicObject.
 *
 * Responsibilities (Phase 8 / T051):
 *   - Display the object's current label text and color pre-filled in controls.
 *   - Let the user change the label text via a JTextField.
 *   - Let the user choose a new fill color via JColorChooser.
 *   - Report confirmation status so the caller can apply or discard the changes.
 *
 * This class is purely presentational:
 *   - It reads the initial state from the caller's constructor arguments.
 *   - It performs no model mutations itself.
 *   - MainFrame reads {@link #isConfirmed()}, {@link #getLabelName()}, and
 *     {@link #getLabelColor()} after the dialog closes and applies them to the model.
 *
 * Design notes:
 *   - Calling {@code setVisible(true)} inside the constructor blocks the EDT
 *     until the user dismisses the dialog (standard Swing modal pattern).
 *   - The color swatch panel acts as a live preview: it updates immediately
 *     when the user picks a color in the chooser.
 */
public class LabelDialog extends JDialog {

    private final JTextField nameField;
    private Color            chosenColor;
    private final JPanel     colorSwatch;
    private boolean          confirmed = false;

    /**
     * Construct, populate, and show the dialog modally.
     *
     * The constructor blocks until the user presses OK, Cancel, or closes the
     * window.  Callers should read {@link #isConfirmed()} immediately after
     * construction to determine whether to apply the edits.
     *
     * @param owner        the parent frame (used for modal centering)
     * @param initialName  current label text of the selected BasicObject
     * @param initialColor current fill color of the selected BasicObject
     */
    public LabelDialog(Frame owner, String initialName, Color initialColor) {
        super(owner, "Customize Label Style", true /* modal */);

        this.chosenColor = initialColor;

        // ── Name row ─────────────────────────────────────────────────────────
        nameField = new JTextField(initialName, 20);

        // ── Color row ────────────────────────────────────────────────────────
        // The swatch is a small filled panel that previews the chosen color.
        // Clicking "Choose…" opens JColorChooser; the swatch updates on selection.
        colorSwatch = new JPanel();
        colorSwatch.setBackground(initialColor);
        colorSwatch.setPreferredSize(new Dimension(60, 24));
        colorSwatch.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JButton chooseBtn = new JButton("Choose\u2026");
        chooseBtn.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(
                    LabelDialog.this, "Choose Fill Color", chosenColor);
            if (picked != null) {
                chosenColor = picked;
                colorSwatch.setBackground(picked);
            }
        });

        // ── Form layout (GridBagLayout keeps rows aligned neatly) ─────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 4, 4);

        // Row 0: "Label name:" + text field (spans 2 columns)
        c.gridx = 0; c.gridy = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Label name:"), c);
        c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(nameField, c);

        // Row 1: "Label color:" + color swatch + "Choose…" button
        c.gridx = 0; c.gridy = 1; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Label color:"), c);
        c.gridx = 1;
        form.add(colorSwatch, c);
        c.gridx = 2;
        form.add(chooseBtn, c);

        // ── Button row ────────────────────────────────────────────────────────
        JButton okBtn     = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e     -> { confirmed = true;  dispose(); });
        cancelBtn.addActionListener(e -> { confirmed = false; dispose(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(okBtn);
        buttons.add(cancelBtn);

        // ── Assemble ──────────────────────────────────────────────────────────
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form,    BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okBtn);  // Enter key activates OK
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        setVisible(true);   // blocks the EDT until the dialog is dismissed
    }

    // ── Result accessors ──────────────────────────────────────────────────────

    /**
     * @return {@code true} if the user pressed OK; {@code false} for Cancel or
     *         window-close — the caller must not apply any changes in that case
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return the label name currently in the text field (never {@code null});
     *         meaningful only when {@link #isConfirmed()} is {@code true}
     */
    public String getLabelName() {
        return nameField.getText();
    }

    /**
     * @return the fill color selected by the user; meaningful only when
     *         {@link #isConfirmed()} is {@code true}
     */
    public Color getLabelColor() {
        return chosenColor;
    }
}
