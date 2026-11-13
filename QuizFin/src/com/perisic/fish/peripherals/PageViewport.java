package com.perisic.fish.peripherals;

// AI-assisted implementation (OpenAI Codex, 2026-11-11). See SOURCES.md.

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/** Keeps game controls reachable when the window is shorter than a page. */
final class PageViewport extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;

    PageViewport(JComponent page) {
        super(new BorderLayout());
        setOpaque(false);
        add(page, BorderLayout.CENTER);
    }

    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(800, size.width), Math.max(610, size.height));
    }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int axis, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int axis, int direction) {
        return Math.max(24, (axis == javax.swing.SwingConstants.VERTICAL ? r.height : r.width) - 24);
    }
    @Override public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport viewport && viewport.getWidth() >= getPreferredSize().width;
    }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
    }
}
