package com.perisic.fish.peripherals;

// AI-assisted focus/pressed-state updates (OpenAI Codex, 2026-11-11). See SOURCES.md.

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
    private boolean keyboardFocus;
    private boolean interactionFeedback = true;
    private int arc = 14;

    RoundButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFont(Theme.font(java.awt.Font.BOLD, 16));
        setForeground(Color.WHITE);
        setBorder(new EmptyBorder(10, 18, 10, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                keyboardFocus = e.getCause() != java.awt.event.FocusEvent.Cause.MOUSE_EVENT;
                repaint();
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) { keyboardFocus = false; repaint(); }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) { keyboardFocus = true; repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                keyboardFocus = false;
                repaint();
            }
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

    /** Keep scored answer colours steady while leaving the control accessible. */
    void setInteractionFeedback(boolean enabled) {
        interactionFeedback = enabled;
        repaint();
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
        } else if (interactionFeedback && (over || getModel().isPressed())) {
            f = fill.darker();
            if (fill.equals(Theme.CHIP) || fill.equals(Theme.CHIP_SEL) || fill.equals(Theme.CARD)) {
                f = new Color(0xE4, 0xF0, 0xFB);
            }
        }
        g2.setColor(f);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        // Focus strengthens the existing outer edge; never add an inner outline.
        boolean focused = hasFocus() && keyboardFocus && interactionFeedback && isEnabled();
        if (isEnabled() && (border != null || focused)) {
            g2.setStroke(new java.awt.BasicStroke(focused ? 2f : 1f));
            g2.setColor(border != null ? border : getForeground());
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
