package com.perisic.fish.peripherals;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/**
 * Flat rounded button that looks the same on every look-and-feel.
 */
class RoundButton extends JButton {
    private static final long serialVersionUID = 1L;

    private Color fill = Theme.PRIMARY;
    private Color border = null;
    private boolean over = false;
    private int arc = 14;

    RoundButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.font(java.awt.Font.BOLD, 14));
        setForeground(Color.WHITE);
        setBorder(new EmptyBorder(10, 18, 10, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                over = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                over = false;
                repaint();
            }
        });
    }

    /** Sets fill, border (may be null) and text colour. */
    void setColors(Color fill, Color border, Color text) {
        this.fill = fill;
        this.border = border;
        setForeground(text);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color f = fill;
        if (!isEnabled()) {
            f = new Color(0xD5, 0xDF, 0xEA);
        } else if (over) {
            f = fill.darker();
            if (fill.equals(Theme.CHIP) || fill.equals(Theme.CHIP_SEL) || fill.equals(Theme.CARD)) {
                f = new Color(0xE4, 0xF0, 0xFB);
            }
        }
        g2.setColor(f);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (border != null && isEnabled()) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
