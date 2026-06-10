package uml.app;

import javax.swing.SwingUtilities;
import uml.ui.MainFrame;

/**
 * Application entry point.
 *
 * Schedules MainFrame creation on the Event Dispatch Thread as required by Swing.
 * All further interaction is event-driven from this point forward.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
