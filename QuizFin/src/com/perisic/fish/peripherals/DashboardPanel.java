package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
        RoundPanel banner = new RoundPanel(new Color(0, 0, 0), 28, new BorderLayout(12, 0));
        banner.setPreferredSize(new Dimension(0, 150));
        banner.setBorder(new EmptyBorder(22, 26, 18, 26));
        JPanel text = vbox();
        text.add(Theme.label("Choose your next challenge", Font.BOLD, 20, Theme.TEXT));
        text.add(Box.createVerticalStrut(6));
        text.add(Theme.label("Play either game, or complete a quiz followed by matching fish rounds!", Font.PLAIN, 12,
                Theme.MUTED));
        banner.add(text, BorderLayout.WEST);
        JComponent art = new JComponent() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                int width = Math.min(getWidth(), getHeight() * 317 / 225);
                int height = width * 225 / 317;
                BufferedImage illustration = new BufferedImage(317, 225, BufferedImage.TYPE_INT_RGB);
                g.drawImage(illustration, (getWidth() - width) / 2, (getHeight() - height) / 2,
                        (getWidth() + width) / 2, (getHeight() + height) / 2, 48, 45, 365, 270, null);
                g.dispose();
            }
        };
        art.setPreferredSize(new Dimension(190, 110));
        banner.add(art, BorderLayout.EAST);
        add(banner, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);

        // stats
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setPreferredSize(new Dimension(0, 88));
        stats.add(stat("Games available", "02"));
        JComponent fishRounds = stat("Fish rounds",
                "<html>10 rounds by default<br>Matches quiz length</html>", 14);
        fishRounds.setToolTipText("Play directly for 10 rounds, or match your completed quiz's question count.");
        stats.add(fishRounds);
        stats.add(stat("Your best score", best + " pts"));
        center.add(stats, BorderLayout.NORTH);

        // game cards
        JPanel games = new JPanel(new GridLayout(1, 2, 16, 0));
        games.setOpaque(false);
        games.setPreferredSize(new Dimension(0, 232));
        games.add(gameCard(false, "General Knowledge Quiz", "Categories • Difficulty • Timed questions",
                "Start General Quiz", openQuiz));
        games.add(gameCard(true, "Fish Challenge", "Count the fish and earn bonus points!",
                "Play Fish Game", openFish));
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
        return stat(label, value, 26);
    }

    private static JComponent stat(String label, String value, int valueFontSize) {
        RoundPanel card = new RoundPanel(Theme.CARD, 16, new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel p = vbox();
        p.add(Theme.label(label, Font.PLAIN, 11, Theme.MUTED));
        p.add(Box.createVerticalStrut(4));
        p.add(Theme.label(value, Font.BOLD, valueFontSize, Theme.TEXT));
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
        badgeRow.add(badge, fish ? BorderLayout.EAST : BorderLayout.WEST);
        p.add(badgeRow);
        p.add(Box.createVerticalStrut(14));
        JComponent t = Theme.label(name, Font.BOLD, 17, Theme.TEXT);
        t.setAlignmentX(0f);
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        JComponent d = Theme.label(desc, Font.PLAIN, 11, Theme.MUTED);
        d.setAlignmentX(0f);
        p.add(d);
        p.add(Box.createVerticalStrut(20));
        RoundButton b = new RoundButton(button);
        b.setFont(Theme.font(Font.BOLD, 11));
        b.setAlignmentX(0f);
        b.addActionListener(e -> action.run());
        p.add(b);
        card.add(p, BorderLayout.NORTH);
        return card;
    }
}
