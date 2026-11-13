package com.perisic.fish.peripherals;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** A single selectable option; Swing lays out all text instead of painting it at fixed positions. */
final class QuizChoiceButton extends JToggleButton {
    private static final long serialVersionUID = 1L;
    private final JLabel title;
    private final JLabel description;
    private final JLabel check = new JLabel("", SwingConstants.CENTER);
    private boolean keyboardFocus;

    QuizChoiceButton(String text, String detail) {
        super(text);
        setFont(Theme.font(Font.BOLD, 16));
        setLayout(new BorderLayout(4, 0));
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        check.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        check.setPreferredSize(new Dimension(24, 24));
        add(check, BorderLayout.WEST);
        add(Box.createHorizontalStrut(24), BorderLayout.EAST);

        JPanel textPanel = new JPanel(new GridLayout(detail == null ? 1 : 2, 1, 0, 4));
        textPanel.setOpaque(false);
        title = Theme.label(text, Theme.font(Font.BOLD, 16), Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        textPanel.add(title);
        description = detail == null ? null : Theme.label(detail, Theme.nunito(Font.PLAIN, 13), Theme.MUTED);
        if (description != null) {
            description.setHorizontalAlignment(SwingConstants.CENTER);
            textPanel.add(description);
            setToolTipText(detail);
        }
        add(textPanel, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName(detail == null ? text : text + ", " + detail);

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                keyboardFocus = event.getCause() != FocusEvent.Cause.MOUSE_EVENT;
                repaint();
            }
            @Override public void focusLost(FocusEvent event) { keyboardFocus = false; repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { keyboardFocus = false; repaint(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) { keyboardFocus = true; repaint(); }
        });
        addChangeListener(event -> updateAppearance());
        updateAppearance();
    }

    private void updateAppearance() {
        Color color = !isEnabled() ? Theme.MUTED : isSelected() ? Theme.PRIMARY : Theme.TEXT;
        title.setForeground(color);
        check.setText(isSelected() ? "\u2713" : "");
        check.setForeground(color);
        if (description != null) description.setForeground(!isEnabled() ? Theme.MUTED
                : isSelected() ? Theme.PRIMARY : Theme.MUTED);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Math.max(size.height, description == null ? 52 : 80));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = isSelected() ? Theme.CHIP_SEL : Theme.CHIP;
        if (isEnabled() && getModel().isRollover()) fill = new Color(0xE4F0FB);
        g.setColor(fill);
        g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
        boolean focused = hasFocus() && keyboardFocus && isEnabled();
        g.setStroke(new BasicStroke(focused ? 2f : 1f));
        g.setColor(isEnabled() && (isSelected() || focused) ? Theme.PRIMARY : Theme.CHIP_BORDER);
        g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
        g.dispose();
        // Child labels render once through paintChildren, using their layout bounds.
    }
}
