package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.ScoreEntry;

/**
 * "Hall of Champions": top 10 total scores with game mode and score breakdown.
 */
class LeaderboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final GameGUI frame;
    private final JPanel rows = new JPanel(new GridLayout(1, 1, 0, 4));
    private final Timer refreshTimer = new Timer(1000, e -> refreshScores());
    private static final DateTimeFormatter PLAYED_AT = DateTimeFormatter
            .ofPattern("dd MMM yyyy, hh:mm:ss a").withZone(ZoneId.systemDefault());

    LeaderboardPanel(GameGUI frame) {
        super(new BorderLayout(0, 18));
        this.frame = frame;
        setOpaque(false);

        RoundPanel banner = new RoundPanel(Theme.SKY, new BorderLayout());
        banner.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel bt = new JPanel();
        bt.setOpaque(false);
        bt.setLayout(new BoxLayout(bt, BoxLayout.Y_AXIS));
        bt.add(Theme.label("Hall of Champions", Theme.display(28), Theme.TEXT));
        bt.add(Box.createVerticalStrut(6));
        bt.add(Theme.label("Completed games • Scores and times update automatically", Font.BOLD, 14, Theme.MUTED));
        banner.add(bt, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        RoundPanel table = new RoundPanel(Theme.CARD, new BorderLayout(0, 8));
        table.setBorder(new EmptyBorder(18, 24, 18, 24));
        table.add(row(true, false, "RANK", "PLAYER", "GAMES PLAYED", "QUIZ", "FISH", "TOTAL", "PLAYED"), BorderLayout.NORTH);

        rows.setOpaque(false);
        refreshScores();
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(rows, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        table.add(scroll, BorderLayout.CENTER);
        add(table, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshScores();
        refreshTimer.start();
    }

    @Override
    public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    private void refreshScores() {
        List<ScoreEntry> top = frame.services().scores.top(10);
        rows.removeAll();
        rows.setLayout(new GridLayout(Math.max(top.size(), 1), 1, 0, 4));
        if (top.isEmpty()) {
            rows.add(Theme.label("No scores yet – finish Fish Challenge alone or after a quiz!", Font.BOLD, 14, Theme.MUTED));
        }
        int rank = 1;
        for (ScoreEntry entry : top) {
            JPanel scoreRow = row(false, entry.player.equals(frame.getPlayer()), String.format("%02d", rank++),
                    entry.player, entry.mode.label, entry.quizScore == null ? "—" : String.valueOf(entry.quizScore),
                    String.valueOf(entry.fishScore), entry.score + " pts", ago(entry.time));
            ((JLabel) scoreRow.getComponent(6)).setToolTipText(PLAYED_AT.format(Instant.ofEpochMilli(entry.time)));
            rows.add(scoreRow);
        }
        rows.revalidate();
        rows.repaint();
    }

    private static JPanel row(boolean header, boolean me, String... values) {
        JPanel r = new JPanel(new GridBagLayout());
        r.setOpaque(me);
        r.setBackground(new Color(0xE9, 0xF3, 0xFC));
        r.setBorder(new EmptyBorder(header ? 4 : 11, 12, header ? 4 : 11, 12));
        int[] widths = { 44, 115, 140, 55, 55, 80, 120 };
        for (int i = 0; i < values.length; i++) {
            String s = values[i];
            JLabel l = header ? Theme.label(s, Font.BOLD, 12, Theme.MUTED)
                    : Theme.label(s, Font.BOLD, 13, Theme.TEXT);
            l.setPreferredSize(new Dimension(widths[i], l.getPreferredSize().height));
            l.setToolTipText(s);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.weightx = i == 1 || i == 2 || i == 6 ? 1 : 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            r.add(l, c);
        }
        return r;
    }

    private static String ago(long time) {
        long seconds = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (seconds < 5) return "just now";
        if (seconds < 60) return seconds + " seconds ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
