package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.auth.Session;
import com.perisic.fish.engine.GameEngine;
import com.perisic.fish.engine.QuizEngine;
import com.perisic.fish.engine.ScoreEntry;

/**
 * Main QuizFin window: navy sidebar (Dashboard, General Quiz, Fish Challenge,
 * Leaderboard) and a content area that shows one screen at a time.
 */
public class GameGUI extends JFrame {

    private static final long serialVersionUID = -107785653906635L;

    private final String player;
    private final transient Session session;
    private final transient AppServices services;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel title = Theme.label("", Theme.display(28), Theme.TEXT);
    private final JLabel subtitle = Theme.label("", Theme.nunito(Font.PLAIN, 14), Theme.MUTED);
    private final Map<String, RoundButton> nav = new LinkedHashMap<>();
    private final RoundButton back = new RoundButton("Back");
    private Runnable backAction;

    /** Default player (testing without login). */
    public GameGUI() {
        this(Session.guest(), new AppServices());
    }

    /** Use this to start the GUI after login. */
    public GameGUI(Session session, AppServices services) {
        super("QuizFin");
        this.session = session;
        this.services = services;
        this.player = session.username;
        setSize(1200, 840);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setBackground(Theme.BG);
        getContentPane().add(buildSidebar(), BorderLayout.WEST);
        getContentPane().add(buildMain(), BorderLayout.CENTER);
        showDashboard();
    }

    String getPlayer() {
        return player;
    }

    AppServices services() {
        return services;
    }

    /** Logout: revoke the session (deletes the remember-me cookie), back to login. */
    private void logout() {
        services.auth.logout(session);
        new LoginGUI(services);
        dispose();
    }

    // ------------------------------------------------------------ layout

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.NAVY);
        side.setPreferredSize(new Dimension(216, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 14, 18, 14));

        JLabel brand = Theme.label("QUIZFIN", Theme.display(34), Color.WHITE);
        brand.setIcon(Theme.symbolIcon("●", Color.WHITE, 16));
        brand.setAlignmentX(0f);
        JLabel tag = Theme.label("Learn • Play • Win", Theme.nunito(Font.PLAIN, 13), new Color(0x9F, 0xB4, 0xCC));
        tag.setAlignmentX(0f);
        side.add(brand);
        side.add(Box.createVerticalStrut(4));
        side.add(tag);
        side.add(Box.createVerticalStrut(28));

        addNav(side, "dashboard", "▦ Dashboard", this::showDashboard);
        addNav(side, "quiz", "◇ General Quiz", this::showQuizSetup);
        addNav(side, "fish", "◉ Fish Challenge", this::startFish);
        addNav(side, "board", "★ Leaderboard", this::showLeaderboard);

        side.add(Box.createVerticalGlue());
        RoundButton out = new RoundButton("Log out");
        out.setHorizontalAlignment(SwingConstants.LEFT);
        out.setAlignmentX(0f);
        out.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        out.setColors(Theme.NAVY, null, new Color(0xFF, 0xB4, 0xB4));
        out.addActionListener(e -> logout());
        side.add(out);
        side.add(Box.createVerticalStrut(6));
        side.add(userChip(false));
        return side;
    }

    /** Clicking a sidebar item opens its page; the current page stays selected. */
    private void addNav(JPanel side, String key, String text, Runnable action) {
        int symbolEnd = text.offsetByCodePoints(0, 1);
        RoundButton b = new RoundButton(text.substring(symbolEnd).trim());
        b.setIcon(Theme.symbolIcon(text.substring(0, symbolEnd), Color.WHITE, 12));
        b.setIconTextGap(6);
        b.setFont(Theme.font(Font.BOLD, 15));
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(0f);
        b.setPreferredSize(new Dimension(188, 46));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        b.setColors(Theme.NAVY, null, Color.WHITE);
        b.addActionListener(e -> {
            if (!b.isSelected()) action.run();
        });
        nav.put(key, b);
        side.add(b);
        side.add(Box.createVerticalStrut(6));
    }

    private JComponent buildMain() {
        JPanel main = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0xE7F2FA));
                g.fillOval(getWidth() - 320, -85, 260, 260);
                g.fillOval(getWidth() - 210, getHeight() - 235, 360, 360);
                g.dispose();
            }
        };
        main.setBackground(Theme.BG);
        main.setBorder(new EmptyBorder(24, 34, 24, 34));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);
        back.setIcon(Theme.symbolIcon("←", Theme.PRIMARY, 18));
        back.setIconTextGap(8);
        back.setFont(Theme.medium(14));
        back.setBorder(new EmptyBorder(8, 14, 8, 14));
        back.setColors(Theme.CARD, Theme.CHIP_BORDER, Theme.PRIMARY);
        back.getAccessibleContext().setAccessibleName("Go back");
        back.addActionListener(e -> {
            if (backAction != null) backAction.run();
        });
        JPanel backHolder = new JPanel(new BorderLayout());
        backHolder.setOpaque(false);
        backHolder.add(back, BorderLayout.NORTH);
        JPanel heading = new JPanel(new BorderLayout(14, 0));
        heading.setOpaque(false);
        heading.add(backHolder, BorderLayout.WEST);
        heading.add(titles, BorderLayout.CENTER);
        head.add(heading, BorderLayout.CENTER);
        JPanel greeting = new JPanel(new BorderLayout());
        greeting.setOpaque(false);
        greeting.add(userChip(true), BorderLayout.NORTH);
        head.add(greeting, BorderLayout.EAST);
        head.setBorder(new EmptyBorder(0, 0, 18, 0));

        content.setOpaque(false);
        main.add(head, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    /** Avatar + name. Light version for the header, dark for the sidebar. */
    private JComponent userChip(boolean header) {
        RoundPanel chip = new RoundPanel(header ? Theme.CARD : new Color(0x1A, 0x3A, 0x63), 18,
                new BorderLayout(10, 0));
        chip.setBorder(new EmptyBorder(8, 10, 8, 14));
        JComponent avatar = new JComponent() {
            private static final long serialVersionUID = 1L;
            {
                setPreferredSize(new Dimension(26, 26));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.AVATAR);
                g2.fillOval(0, 0, 25, 25);
                g2.dispose();
            }
        };
        JPanel av = new JPanel(new GridBagLayout());
        av.setOpaque(false);
        av.add(avatar);
        chip.add(av, BorderLayout.WEST);
        if (header) {
            chip.add(Theme.label("Hello, " + player + "!", Font.BOLD, 13, Theme.TEXT), BorderLayout.CENTER);
        } else {
            JPanel names = new JPanel();
            names.setOpaque(false);
            names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
            names.add(Theme.label(player, Font.BOLD, 14, Color.WHITE));
            names.add(Theme.label("Player account", Theme.nunito(Font.PLAIN, 13), new Color(0x9F, 0xB4, 0xCC)));
            chip.add(names, BorderLayout.CENTER);
            chip.setAlignmentX(0f);
            chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        }
        return chip;
    }

    // -------------------------------------------------------- navigation

    private void setPage(String navKey, String t, String sub, JComponent page) {
        setPage(navKey, t, sub, page, "dashboard".equals(navKey) ? null : this::showDashboard);
    }

    private void setPage(String navKey, String t, String sub, JComponent page, Runnable parentPage) {
        backAction = parentPage;
        back.setVisible(parentPage != null);
        title.setText(t);
        subtitle.setText(sub);
        for (Map.Entry<String, RoundButton> e : nav.entrySet()) {
            boolean on = e.getKey().equals(navKey);
            e.getValue().setSelected(on);
            e.getValue().getAccessibleContext().setAccessibleDescription(on ? "Current page" : "Open page");
            e.getValue().setColors(on ? Theme.NAV_ACTIVE : Theme.NAVY, null, Color.WHITE);
        }
        content.removeAll();
        content.add(page, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    /** Show the dashboard with the current player's saved best score. */
    public void showDashboard() {
        setPage(
                "dashboard", "Dashboard", "Choose your next challenge and start collecting points",
                new DashboardPanel(
                        services.scores.best(player), this::showQuizSetup, this::startFish
                )
        );
    }


    public void showQuizSetup() {
        setPage("quiz", "General Quiz Setup", "Customize your quiz before you start", new QuizSetupPanel(this));
    }

    void startQuiz(QuizEngine engine, String categoryName) {
        setPage("quiz", "General Quiz", "Answer the question and earn points",
                new QuizPanel(this, engine), this::showQuizSetup);
    }

    void showQuizResult(QuizEngine engine) {
        if (!engine.isFinished()) {
            return;
        }
        startFish(engine.getTotal(), engine.getScore());
    }

    public void startFish() {
        startFish(GameEngine.ROUNDS, null);
    }

    private void startFish(int rounds, Integer quizScore) {
        setPage("fish", "Fish Challenge", "Count the fish and earn bonus points • " + rounds + " rounds",
                new FishPanel(this, rounds, quizScore));
    }

    void showFishResult(GameEngine engine, Integer quizScore) {
        if (!engine.isFinished()) return;
        ScoreEntry entry = new ScoreEntry(engine.getPlayer(), quizScore, engine.getScore(), System.currentTimeMillis());
        services.scores.add(entry);
        int pct = engine.getCorrectCount() * 100 / engine.getTotalRounds();
        String detail = entry.mode.label + " • "
                + (quizScore == null ? "" : "Quiz: " + quizScore + " pts • ")
                + "Fish: " + engine.getScore() + " pts";
        setPage("fish", "Challenge Complete", "Your score has been added to the leaderboard",
                new ResultPanel(pct, entry.score + " pts", detail,
                        "Start New Quiz", this::showQuizSetup, "Leaderboard", this::showLeaderboard));
    }

    public void showLeaderboard() {
        setPage("board", "Leaderboard", "Fish only and Quiz + Fish scores", new LeaderboardPanel(this));
    }

    // -------------------------------------------------------------- main

    /** Entry point for testing without login. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameGUI().setVisible(true));
    }
}
