package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.ScoreEntry;

/**
 * "Hall of Champions": top 10 Fish Challenge scores.
 */
class LeaderboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    LeaderboardPanel(GameGUI frame) {
        super(new BorderLayout(0, 18));
        setOpaque(false);

        RoundPanel banner = new RoundPanel(Theme.SKY, new BorderLayout());
        banner.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel bt = new JPanel();
        bt.setOpaque(false);
        bt.setLayout(new BoxLayout(bt, BoxLayout.Y_AXIS));
        bt.add(Theme.label("Hall of Champions", Font.BOLD, 20, Theme.TEXT));
        bt.add(Box.createVerticalStrut(6));
        bt.add(Theme.label("Every fish counts. Climb to the top!", Font.PLAIN, 13, Theme.MUTED));
        banner.add(bt, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        RoundPanel table = new RoundPanel(Theme.CARD, new BorderLayout(0, 8));
        table.setBorder(new EmptyBorder(18, 24, 18, 24));
        table.add(row("RANK", "PLAYER", "SCORE", "RECORD", true, false), BorderLayout.NORTH);

        List<ScoreEntry> top = frame.services().scores.top(10);
        JPanel rows = new JPanel(new GridLayout(Math.max(top.size(), 1), 1, 0, 4));
        rows.setOpaque(false);
        if (top.isEmpty()) {
            rows.add(Theme.label("No scores yet – finish a Fish Challenge to set your record!", Font.PLAIN, 14, Theme.MUTED));
        }
        int rank = 1;
        for (ScoreEntry e : top) {
            rows.add(row(String.format("%02d", rank++), e.player, e.score + " pts", ago(e.time), false,
                    e.player.equals(frame.getPlayer())));
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(rows, BorderLayout.NORTH);
        table.add(holder, BorderLayout.CENTER);
        add(table, BorderLayout.CENTER);
    }

    private static JPanel row(String a, String b, String c, String d, boolean header, boolean me) {
        JPanel r = new JPanel(new GridLayout(1, 4));
        r.setOpaque(me);
        r.setBackground(new Color(0xE9, 0xF3, 0xFC));
        r.setBorder(new EmptyBorder(header ? 4 : 11, 12, header ? 4 : 11, 12));
        for (String s : new String[] { a, b, c, d }) {
            JLabel l = header ? Theme.label(s, Font.BOLD, 11, Theme.MUTED)
                    : Theme.label(s, s.equals(b) ? Font.BOLD : Font.PLAIN, 14, Theme.TEXT);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            r.add(l);
        }
        return r;
    }

    private static String ago(long time) {
        long mins = Math.max(0, (System.currentTimeMillis() - time) / 60000);
        if (mins < 1)
            return "just now";
        if (mins < 60)
            return mins + " min ago";
        if (mins < 60 * 24)
            return (mins / 60) + " hr ago";
        return (mins / (60 * 24)) + " days ago";
    }
}
