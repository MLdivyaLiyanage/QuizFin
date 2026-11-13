package com.perisic.fish.peripherals;

// AI-assisted UI updates (OpenAI Codex, 2026-11-11): background refresh, scrolling,
// error feedback, empty state, and case-insensitive player highlighting. See SOURCES.md.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.ScoreEntry;

/** "Hall of Champions": top 10 total scores with game mode and score breakdown. */
class LeaderboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final GameGUI frame;
    private final JPanel rows = new JPanel(new GridLayout(1, 1, 0, 4));
    private final Timer refreshTimer = new Timer(5000, e -> refreshScores());
    private boolean refreshing;

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
        bt.add(Theme.label("Top 10 runs • Your scores are highlighted • Refreshes every 5 seconds", Font.PLAIN, 13, Theme.MUTED));
        banner.add(bt, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        RoundPanel table = new RoundPanel(Theme.CARD, new BorderLayout(0, 8));
        table.setBorder(new EmptyBorder(18, 24, 18, 24));

        rows.setOpaque(false);
        refreshScores();
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(row(true, false, "RANK", "PLAYER", "MODE", "QUIZ", "FISH", "TOTAL", "PLAYED"), BorderLayout.NORTH);
        holder.add(rows, BorderLayout.CENTER);
        JPanel topAligned = new JPanel(new BorderLayout());
        topAligned.setOpaque(false);
        topAligned.add(holder, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(topAligned);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        if (refreshing) return;
        refreshing = true;
        new SwingWorker<List<ScoreEntry>, Void>() {
            @Override protected List<ScoreEntry> doInBackground() {
                return frame.services().scores.top(10);
            }
            @Override protected void done() {
                refreshing = false;
                try {
                    displayScores(get());
                } catch (Exception ex) {
                    rows.removeAll();
                    rows.setLayout(new GridLayout(1, 1));
                    rows.add(Theme.label("Scores unavailable. Retrying automatically...", Font.PLAIN, 14, Theme.MUTED));
                    rows.revalidate();
                    rows.repaint();
                }
            }
        }.execute();
    }

    private void displayScores(List<ScoreEntry> top) {
        rows.removeAll();
        rows.setLayout(new GridLayout(Math.max(top.size(), 1), 1, 0, 4));
        if (top.isEmpty()) {
            JLabel empty = Theme.label("<html>No scores yet.<br>Finish a fish challenge to set the first score!</html>", Font.PLAIN, 15, Theme.MUTED);
            empty.setBorder(new EmptyBorder(28, 12, 28, 12));
            rows.add(empty);
        }
        int rank = 1;
        for (ScoreEntry entry : top) {
            rows.add(row(false, entry.player.equalsIgnoreCase(frame.getPlayer()),
                    String.format("%02d", rank++),
                    entry.player,
                    entry.mode.label,
                    entry.quizScore == null ? "—" : String.valueOf(entry.quizScore),
                    String.valueOf(entry.fishScore),
                    entry.score + " pts",
                    entry.getTimeDisplay()));
        }
        rows.revalidate();
        rows.repaint();
    }

    private static JPanel row(boolean header, boolean me, String... values) {
        JPanel r = new JPanel(new GridBagLayout());
        r.setOpaque(me);
        r.setBackground(new Color(0xE9, 0xF3, 0xFC));
        r.setBorder(new EmptyBorder(header ? 4 : 11, 12, header ? 4 : 11, 12));
        // The PLAYED column holds a full date + time, e.g. "09 Nov 2026, 10:28:48 pm".
        int[] widths = { 40, 110, 130, 50, 50, 80, 190 };
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
}
