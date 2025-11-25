package org.example;

import javax.swing.SwingUtilities;

public class Main{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RouteService app = new RouteService();
            app.setVisible(true);
        });
    }
}