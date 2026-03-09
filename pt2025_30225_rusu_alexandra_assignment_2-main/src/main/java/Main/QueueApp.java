package Main;

import GUI.SimulationFrame;

import javax.swing.SwingUtilities;

public class QueueApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulationFrame frame = new SimulationFrame();
            frame.setVisible(true);
        });
    }
}