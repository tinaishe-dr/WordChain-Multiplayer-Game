package wordchain.client;

import javax.swing.SwingUtilities;

import wordchain.client.ui.MainFrame;
import wordchain.client.ui.Theme;

/** Desktop client entry point. */
public final class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
