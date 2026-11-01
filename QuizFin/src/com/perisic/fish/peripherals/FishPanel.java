package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.GameEngine;
import com.perisic.fish.engine.GameEngine.AnswerResult;

/**
 * Fish Challenge: Fish API image, pick how many fish, check, score + bonus,
 * next round, final score, leaderboard.
 */
class FishPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final GameGUI frame;
    private final GameEngine engine;

    private final JLabel roundLabel = Theme.label("", Font.BOLD, 15, Theme.TEXT);
    private final JLabel streakLabel = Theme.label("Streak: 0", Font.BOLD, 15, Theme.ORANGE);
    private final JLabel scoreLabel = Theme.label("Score: 0 pts", Font.BOLD, 15, Theme.PRIMARY);
    private final JLabel imageLabel = new JLabel("Loading fish...", SwingConstants.CENTER);
    private final JLabel feedback = Theme.label(" ", Font.BOLD, 14, Theme.MUTED);
    private final RoundButton[] digits = new RoundButton[10];
    private final RoundButton retry = new RoundButton("Retry");
    private boolean accepting = false;

    FishPanel(GameGUI frame, int totalRounds) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        this.engine = new GameEngine(frame.getPlayer(), frame.services().games, totalRounds);
        roundLabel.setText(String.format("Round %02d / %02d", 1, engine.getTotalRounds()));
        setOpaque(false);

        // status bar
        RoundPanel bar = new RoundPanel(Theme.CARD, 16, new GridLayout(1, 3));
        bar.setBorder(new EmptyBorder(14, 20, 14, 20));
        streakLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bar.add(roundLabel);
        bar.add(streakLabel);
        bar.add(scoreLabel);
        add(bar, BorderLayout.NORTH);

        // image area
        RoundPanel water = new RoundPanel(Theme.SKY, new BorderLayout());
        water.setBorder(new EmptyBorder(14, 20, 14, 20));
        water.add(Theme.label("FISH API • COUNTING CHALLENGE", Font.BOLD, 11, Theme.PRIMARY),
                BorderLayout.NORTH);
        imageLabel.setFont(Theme.font(Font.PLAIN, 14));
        imageLabel.setForeground(Theme.MUTED);
        water.add(imageLabel, BorderLayout.CENTER);
        add(water, BorderLayout.CENTER);

        // answer card
        RoundPanel answers = new RoundPanel(Theme.CARD, new BorderLayout(0, 12));
        answers.setBorder(new EmptyBorder(18, 22, 16, 22));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Theme.label("How many fish can you see?", Font.BOLD, 17, Theme.TEXT), BorderLayout.WEST);
        retry.setVisible(false);
        retry.addActionListener(e -> loadNext());
        head.add(retry, BorderLayout.EAST);
        answers.add(head, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridLayout(1, 10, 10, 0));
        row.setOpaque(false);
        for (int i = 0; i < 10; i++) {
            RoundButton b = new RoundButton(String.valueOf(i));
            b.setFont(Theme.font(Font.BOLD, 16));
            b.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.TEXT);
            b.setPreferredSize(new Dimension(50, 46));
            final int n = i;
            b.addActionListener(e -> onAnswer(n, b));
            digits[i] = b;
            row.add(b);
        }
        answers.add(row, BorderLayout.CENTER);

        JPanel foot = new JPanel();
        foot.setOpaque(false);
        foot.setLayout(new BoxLayout(foot, BoxLayout.Y_AXIS));
        feedback.setAlignmentX(0f);
        foot.add(feedback);
        foot.add(Box.createVerticalStrut(4));
        JLabel rules = Theme.label("Correct +" + GameEngine.CORRECT_POINTS + " • Speed bonus +"
                        + GameEngine.SPEED_BONUS + " (within " + GameEngine.SPEED_LIMIT_MS / 1000 + "s) • Streak bonus +"
                        + GameEngine.STREAK_BONUS + " (" + GameEngine.STREAK_MIN + "+ in a row) • Wrong = 0",
                Font.PLAIN, 11, Theme.MUTED);
        rules.setAlignmentX(0f);
        foot.add(rules);
        answers.add(foot, BorderLayout.SOUTH);
        add(answers, BorderLayout.SOUTH);

        bindKeys();
        loadNext();
    }

    /** Keyboard events: press 0-9 (main row or numpad) to answer. */
    private void bindKeys() {
        javax.swing.InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getActionMap();
        for (int i = 0; i < 10; i++) {
            final int n = i;
            im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + i, 0), "digit" + i);
            im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_NUMPAD0 + i, 0), "digit" + i);
            am.put("digit" + i, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    digits[n].doClick(80);
                }
            });
        }
    }

    /** Loads the next fish image from the API without freezing the UI. */
    private void loadNext() {
        accepting = false;
        retry.setVisible(false);
        feedback.setText("Loading fish...");
        feedback.setForeground(Theme.MUTED);
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return engine.nextGame();
            }

            @Override
            protected void done() {
                BufferedImage img = null;
                try {
                    img = get();
                } catch (Exception ignored) {
                    // handled below
                }
                if (img == null) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Could not reach the Fish API.");
                    feedback.setText("Check your internet connection and press Retry.");
                    feedback.setForeground(Theme.RED);
                    retry.setVisible(true);
                    return;
                }
                imageLabel.setText(null);
                imageLabel.setIcon(new ImageIcon(img));
                roundLabel.setText(String.format("Round %02d / %02d", engine.getRound(), engine.getTotalRounds()));
                feedback.setText("Pick your answer.");
                accepting = true;
            }
        }.execute();
    }

    private void onAnswer(int n, RoundButton button) {
        if (!accepting) {
            return;
        }
        accepting = false;
        AnswerResult r = engine.checkSolution(n);
        if (r == null) {
            return;
        }
        scoreLabel.setText("Score: " + engine.getScore() + " pts");
        streakLabel.setText("Streak: " + engine.getStreak());
        if (r.correct) {
            button.setColors(Theme.GREEN_BG, Theme.GREEN, Theme.TEXT);
            StringBuilder sb = new StringBuilder("Correct! +" + r.base);
            if (r.speed > 0)
                sb.append("  +" + r.speed + " speed");
            if (r.streak > 0)
                sb.append("  +" + r.streak + " streak");
            sb.append("  = +" + r.total());
            feedback.setText(sb.toString());
            feedback.setForeground(Theme.GREEN);
        } else {
            button.setColors(Theme.RED_BG, Theme.RED, Theme.TEXT);
            digits[r.solution].setColors(Theme.GREEN_BG, Theme.GREEN, Theme.TEXT);
            feedback.setText("Wrong – there were " + r.solution + " fish. +0");
            feedback.setForeground(Theme.RED);
        }

        Timer t = new Timer(1300, e -> {
            resetDigits();
            if (engine.isFinished()) {
                frame.services().scores.add(engine.getPlayer(), engine.getScore());
                frame.showFishResult(engine);
            } else {
                loadNext();
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private void resetDigits() {
        for (RoundButton b : digits) {
            b.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.TEXT);
        }
    }
}
