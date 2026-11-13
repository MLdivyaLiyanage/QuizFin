package com.perisic.fish.peripherals;

// AI-assisted UI wording and layout updates (OpenAI Codex, 2026-11-11).
// See SOURCES.md for the assistance record.

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.GameEngine;

/**
 * Main menu: banner, three stat cards and the two game cards.
 */
class DashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    

    DashboardPanel(int best, Runnable openQuiz, Runnable openFish) {
        super(new BorderLayout(0, 18));
        setOpaque(false);

        // banner
        RoundPanel banner = new RoundPanel(new Color(0xD6F0F8), 28, new BorderLayout(12, 0));
        banner.setPreferredSize(new Dimension(0, 150));
        banner.setBorder(new EmptyBorder(22, 26, 18, 26));
        JPanel text = vbox();
        text.add(Theme.label("A little practice. A new high score.", Theme.display(24), Theme.TEXT));
        text.add(Box.createVerticalStrut(6));
        text.add(Theme.label("Choose a challenge below to get started.", Theme.medium(14),
                Theme.MUTED));
        banner.add(text, BorderLayout.CENTER);
        JComponent art = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double scale = Math.min(getWidth() / 200.0, getHeight() / 140.0);
                g.translate((getWidth() - 200 * scale) / 2, (getHeight() - 140 * scale) / 2);
                g.scale(scale, scale);
                Theme.paintBannerArt(g);
                g.dispose();
            }
        };
        art.setPreferredSize(new Dimension(150, 110));
        banner.add(art, BorderLayout.EAST);
        add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);

        // stats
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 110));
        stats.add(stat("Games available", "02"));
        JComponent fishRounds = stat("Fish rounds",
                "<html>" + GameEngine.ROUNDS + " rounds by default<br>Matches quiz length</html>", 14);
        fishRounds.setToolTipText("Play directly for 10 rounds, or match your completed quiz's question count.");
        stats.add(fishRounds);
        stats.add(stat("Your best score", best + " pts"));
        center.add(stats, BorderLayout.NORTH);

        // game cards
        JPanel games = new JPanel(new GridLayout(1, 2, 16, 0));
        games.setOpaque(false);
        games.setPreferredSize(new Dimension(0, 320));
        games.add(gameCard(false, "Quiz + Fish", "<html>Choose a topic and answer 5–15 questions.<br>Then play the same number of fish rounds.<br>Both scores count toward your total.</html>",
                "Set up my quiz", openQuiz));
        games.add(gameCard(true, "Fish Challenge", "<html>Study each puzzle, then answer from memory.<br>Play " + GameEngine.ROUNDS + " rounds and build your score.<br>No general knowledge quiz required.</html>",
                "Start fish challenge", openFish));
        JPanel gameArea = new JPanel(new BorderLayout());
        gameArea.setOpaque(false);
        gameArea.add(games, BorderLayout.NORTH);
        center.add(gameArea, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private static JPanel vbox() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JComponent stat(String label, String value) {
        return stat(label, value, 32);
    }

    private static JComponent stat(String label, String value, int valueFontSize) {
        RoundPanel card = new RoundPanel(Theme.CARD, 16, new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel p = vbox();
        p.add(Theme.label(label, Theme.medium(14), Theme.MUTED));
        p.add(Box.createVerticalStrut(4));
        p.add(Theme.label(value, valueFontSize >= 28 ? Theme.display(valueFontSize) : Theme.medium(valueFontSize), Theme.TEXT));
        card.add(p, BorderLayout.CENTER);
        return card;
    }

    private static JComponent gameCard(boolean fish, String name, String desc, String button, Runnable action) {
        RoundPanel card = new RoundPanel(Theme.CARD, new BorderLayout());
        card.setBorder(new EmptyBorder(24, 24, 24, 24));
        JPanel p = vbox();
        JComponent badge = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(fish ? 0xDCF7F9 : 0xE0ECFF));
                g.fillOval(0, 0, 52, 52);
                g.setColor(fish ? new Color(0x237CAD) : Theme.PRIMARY);
                if (fish) {
                    g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawOval(17, 12, 20, 20);
                    g.drawLine(33, 29, 43, 39);
                    g.drawArc(7, 24, 20, 16, 20, 140);
                    g.drawArc(25, 21, 18, 18, 190, 120);
                } else {
                    g.setFont(Theme.font(Font.BOLD, 25));
                    g.drawString("?", 19, 35);
                }
                g.dispose();
            }
        };
        badge.setMaximumSize(new Dimension(52, 52));
        badge.setPreferredSize(new Dimension(52, 52));
        badge.setMinimumSize(new Dimension(52, 52));
        badge.setAlignmentX(0f);
        JPanel badgeRow = new JPanel(new BorderLayout());
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(0f);
        badgeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        badgeRow.add(badge, BorderLayout.WEST);
        p.add(badgeRow);
        p.add(Box.createVerticalStrut(14));
        JComponent t = Theme.label(name, Theme.display(22), Theme.TEXT);
        t.setAlignmentX(0f);
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        JComponent d = Theme.label(desc, Theme.medium(13), Theme.MUTED);
        d.setAlignmentX(0f);
        p.add(d);
        p.add(Box.createVerticalStrut(20));
        RoundButton b = new RoundButton(button);
        b.setFont(Theme.font(Font.BOLD, 16));
        b.setAlignmentX(0f);
        b.addActionListener(e -> action.run());
        p.add(b);
        card.add(p, BorderLayout.NORTH);
        return card;
    }
}
