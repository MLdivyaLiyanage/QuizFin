package com.perisic.fish.peripherals;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;

/**
 * Panel with a rounded, filled background (the "cards" of the design).
 */
class RoundPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final Color fill;
    private final int arc;

    RoundPanel(Color fill, LayoutManager layout) {
        this(fill, 22, layout);
    }

    RoundPanel(Color fill, int arc, LayoutManager layout) {
        super(layout);
        this.fill = fill;
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
