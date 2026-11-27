package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import com.perisic.fish.engine.ScoreEntry;
import com.perisic.fish.engine.ScoreStore;

/** Top ten completed runs with score details and background refresh. */
class LeaderboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final ScoreStore scores;
    private final String player;
    private final JPanel rows = panel(new GridLayout(1, 1));
    private final JLabel status = Theme.label("Loading scores...", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel leading = Theme.label("—", Font.BOLD, 24, Theme.TEXT);
    private final JLabel personal = Theme.label("—", Font.BOLD, 24, Theme.PRIMARY);
    private final JLabel runCount = Theme.label("—", Font.BOLD, 24, Theme.TEXT);
    private final RoundButton refresh = new RoundButton("Refresh");
    private final Timer refreshTimer = new Timer(5000, e -> refreshScores());
    private boolean refreshing;
    private boolean hasResults;

    LeaderboardPanel(GameGUI frame) { this(frame.services().scores, frame.getPlayer()); }

    /** Sample data can be supplied for a preview without a database. */
    LeaderboardPanel(ScoreStore scores, String player) {
        super(new BorderLayout(0, 16));
        this.scores = scores;
        this.player = player;
        setOpaque(false);
        JPanel intro = panel(new BorderLayout(0, 14));
        RoundPanel banner = new RoundPanel(Theme.SKY, 24, new BorderLayout(16, 0));
        banner.setBorder(new EmptyBorder(20, 24, 20, 24));
        banner.add(stack(Theme.label("Hall of Champions", Theme.display(28), Theme.TEXT),
                Theme.label("Your next personal best starts with one more challenge.", Font.PLAIN, 13, Theme.MUTED)));
        refresh.setFont(Theme.font(Font.BOLD, 13));
        refresh.setColors(Theme.CARD, Theme.CHIP_BORDER, Theme.PRIMARY);
        refresh.getAccessibleContext().setAccessibleDescription("Reload the top ten completed runs");
        refresh.addActionListener(e -> refreshScores());
        JPanel actions = panel(new GridBagLayout());
        actions.add(refresh);
        banner.add(actions, BorderLayout.EAST);
        intro.add(banner, BorderLayout.NORTH);
        JPanel stats = panel(new GridLayout(1, 3, 12, 0));
        stats.add(stat("Leading score", leading, "Highest total in the top 10"));
        stats.add(stat("Your best in top 10", personal, "Your highest listed total"));
        stats.add(stat("Ranked runs", runCount, "Completed runs shown below"));
        intro.add(stats);
        add(intro, BorderLayout.NORTH);

        RoundPanel table = new RoundPanel(Theme.CARD, 24, new BorderLayout(0, 12));
        table.setBorder(new EmptyBorder(18, 18, 16, 18));
        table.add(stack(Theme.label("Top 10 runs", Font.BOLD, 19, Theme.TEXT),
                Theme.label("Highest total first · Players can appear more than once · Your runs are marked YOU",
                        Theme.nunito(Font.PLAIN, 12), Theme.MUTED)), BorderLayout.NORTH);
        showMessage("Loading the leaderboard", "Your latest scores will appear here shortly.");
        JPanel header = columns(true, Theme.label("RANK", Font.BOLD, 11, Theme.MUTED),
                Theme.label("PLAYER / MODE", Font.BOLD, 11, Theme.MUTED),
                Theme.label("SCORE DETAILS", Font.BOLD, 11, Theme.MUTED),
                Theme.label("TOTAL", Font.BOLD, 11, Theme.MUTED),
                Theme.label("PLAYED", Font.BOLD, 11, Theme.MUTED));
        header.setOpaque(true);
        header.setBackground(Theme.CARD);
        JPanel topAligned = panel(new BorderLayout());
        topAligned.add(rows, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(topAligned);
        scroll.setColumnHeaderView(header);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        table.add(scroll);
        status.getAccessibleContext().setAccessibleName("Leaderboard refresh status");
        table.add(status, BorderLayout.SOUTH);
        add(table);
    }

    @Override public void addNotify() {
        super.addNotify();
        refreshScores();
        refreshTimer.start();
    }

    @Override public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    private void refreshScores() {
        if (refreshing) return;
        refreshing = true;
        refresh.setEnabled(false);
        refresh.setText("Refreshing...");
        new SwingWorker<List<ScoreEntry>, Void>() {
            @Override protected List<ScoreEntry> doInBackground() { return scores.top(10); }
            @Override protected void done() {
                try {
                    displayScores(get());
                    hasResults = true;
                    status.setForeground(Theme.MUTED);
                    status.setText("Updated " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            + " · Refreshes automatically every 5 seconds");
                } catch (Exception ex) {
                    if (!hasResults) showMessage("Scores are unavailable", "Check the database connection, then choose Refresh.");
                    status.setForeground(Theme.RED);
                    status.setText(hasResults ? "Could not refresh · Showing last loaded scores · Retrying automatically"
                            : "Could not load scores · Retrying automatically");
                } finally {
                    refreshing = false;
                    refresh.setEnabled(true);
                    refresh.setText("Refresh");
                }
            }
        }.execute();
    }

    private void displayScores(List<ScoreEntry> top) {
        leading.setText(top.isEmpty() ? "—" : top.get(0).score + " pts");
        var best = top.stream().filter(e -> e.player.equalsIgnoreCase(player)).mapToInt(e -> e.score).max();
        personal.setText(best.isPresent() ? best.getAsInt() + " pts" : "Not listed yet");
        personal.setFont(Theme.font(Font.BOLD, best.isPresent() ? 24 : 17));
        runCount.setText(top.size() + " / 10");
        rows.removeAll();
        rows.setLayout(new GridLayout(Math.max(1, top.size()), 1, 0, 7));
        if (top.isEmpty()) showMessage("Be the first on the board",
                "Finish a Fish Challenge or a Quiz + Fish game to record a score.");
        for (int i = 0; i < top.size(); i++) rows.add(scoreRow(i + 1, top.get(i)));
        rows.revalidate();
        rows.repaint();
    }

    private JPanel scoreRow(int rank, ScoreEntry entry) {
        boolean me = entry.player.equalsIgnoreCase(player);
        Color rankFill = rank == 1 ? new Color(0xFFF0C9) : rank == 2 ? new Color(0xE5EBF3)
                : rank == 3 ? new Color(0xF7E6D8) : Theme.CARD;
        RoundPanel badge = new RoundPanel(rankFill, 16, new BorderLayout());
        JLabel rankLabel = Theme.label(String.format("%02d", rank), Font.BOLD, 16, Theme.TEXT);
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badge.add(rankLabel);
        badge.setPreferredSize(new Dimension(42, 38));
        badge.getAccessibleContext().setAccessibleName("Rank " + rank);
        JLabel name = Theme.label(entry.player, Font.BOLD, 14, me ? Theme.PRIMARY : Theme.TEXT);
        name.setToolTipText(entry.player);
        JPanel identity = panel(new BorderLayout(6, 0));
        identity.add(name);
        if (me) identity.add(Theme.label("YOU", Font.BOLD, 10, Theme.PRIMARY), BorderLayout.EAST);
        JPanel details = stack(Theme.label("Quiz  " + (entry.quizScore == null ? "—" : entry.quizScore), Font.PLAIN, 12, Theme.MUTED),
                Theme.label("Fish  " + entry.fishScore, Font.PLAIN, 12, Theme.MUTED));
        details.setToolTipText("Total = quiz points + fish points. A dash means no quiz score was recorded.");
        var played = entry.getTime().atZone(ZoneId.systemDefault());
        JPanel date = stack(Theme.label(played.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), Font.PLAIN, 12, Theme.TEXT),
                Theme.label(played.format(DateTimeFormatter.ofPattern("hh:mm a")), Font.PLAIN, 11, Theme.MUTED));
        date.setToolTipText(entry.getTimeDisplay());
        RoundPanel card = new RoundPanel(me ? Theme.CHIP_SEL : Theme.CHIP, 16, new BorderLayout());
        card.add(columns(false, badge, stack(identity, Theme.label(entry.mode.label, Font.PLAIN, 11, Theme.MUTED)),
                details, Theme.label(entry.score + " pts", Font.BOLD, 16, Theme.PRIMARY), date));
        return card;
    }

    private void showMessage(String title, String hint) {
        rows.removeAll();
        rows.setLayout(new GridLayout(1, 1));
        JPanel message = stack(Theme.label(title, Font.BOLD, 17, Theme.TEXT),
                Theme.label(hint, Theme.nunito(Font.PLAIN, 13), Theme.MUTED));
        message.setBorder(new EmptyBorder(32, 12, 32, 12));
        rows.add(message);
        rows.revalidate();
        rows.repaint();
    }

    private static JPanel stat(String title, JLabel value, String hint) {
        RoundPanel card = new RoundPanel(Theme.CARD, 20, new BorderLayout());
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.add(stack(Theme.label(title, Font.BOLD, 12, Theme.MUTED), value,
                Theme.label(hint, Theme.nunito(Font.PLAIN, 11), Theme.MUTED)));
        return card;
    }

    private static JPanel columns(boolean header, JComponent... cells) {
        JPanel row = panel(new GridBagLayout());
        row.setBorder(new EmptyBorder(header ? 2 : 10, 10, header ? 2 : 10, 10));
        int[] widths = { 52, 170, 112, 92, 116 };
        for (int i = 0; i < cells.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.weightx = i == 1 ? 1 : 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, i == cells.length - 1 ? 0 : 10);
            cells[i].setPreferredSize(new Dimension(widths[i], cells[i].getPreferredSize().height));
            row.add(cells[i], c);
        }
        return row;
    }

    private static JPanel stack(JComponent... children) {
        JPanel result = panel(null);
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        for (int i = 0; i < children.length; i++) {
            if (i > 0) result.add(Box.createVerticalStrut(4));
            children[i].setAlignmentX(LEFT_ALIGNMENT);
            result.add(children[i]);
        }
        return result;
    }

    private static JPanel panel(LayoutManager layout) {
        JPanel result = new JPanel(layout);
        result.setOpaque(false);
        return result;
    }
}
