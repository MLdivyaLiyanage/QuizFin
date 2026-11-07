package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;

import com.perisic.fish.engine.GameEngine;
import com.perisic.fish.engine.GameEngine.AnswerResult;

/**
 * Fish Memory Challenge: memorize the image, then answer after it is hidden.
 */
class FishPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    static final int MEMORY_SECONDS = 5;
    private static final int MEMORY_MILLIS = MEMORY_SECONDS * 1000;
    private static final Color BONUS_INK = new Color(0x0A, 0x70, 0x64);

    private final GameGUI frame;
    private final GameEngine engine;
    private final Integer quizScore;

    private final JLabel roundLabel = Theme.label("", Theme.display(26), Theme.TEXT);
    private final JLabel streakLabel = Theme.label("0 in a row", Theme.display(26), BONUS_INK);
    private final JLabel scoreLabel = Theme.label("0 pts", Theme.display(26), Color.WHITE);
    private final JLabel imageLabel = new JLabel("Loading fish...", SwingConstants.CENTER);
    private final JLabel feedback = Theme.label(" ", Theme.nunitoSemiBold(15), Theme.MUTED);
    private final RoundButton[] digits = new RoundButton[10];
    private final RoundButton retry = new RoundButton("Retry");
    private final JLabel phaseLabel = Theme.label("LOADING FISH", Theme.nunito(Font.BOLD, 14), Theme.NAVY);
    private final JLabel countdownLabel = Theme.label(" ", Theme.nunito(Font.BOLD, 15), Theme.NAVY);
    private final JProgressBar countdown = new JProgressBar();
    private final JLabel prompt = Theme.label("Memorize the fish before answering", Theme.display(24), Theme.TEXT);
    private final Timer countdownTimer = new Timer(50, e -> updateCountdown());
    private Timer nextRoundTimer;
    private SwingWorker<BufferedImage, Void> imageWorker;
    private long previewEndsAt;
    private boolean accepting = false;

    FishPanel(GameGUI frame, int totalRounds) {
        this(frame, totalRounds, null);
    }

    FishPanel(GameGUI frame, int totalRounds, Integer quizScore) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        this.quizScore = quizScore;
        this.engine = new GameEngine(frame.getPlayer(), frame.services().games, totalRounds);
        updateRoundLabel(1);
        setOpaque(false);

        // status bar
        JPanel bar = new JPanel(new GridLayout(1, 3, 12, 0));
        bar.setOpaque(false);
        bar.add(statCard("ROUND", roundLabel, Theme.CARD, Theme.NAV_ACTIVE));
        bar.add(statCard("CURRENT STREAK", streakLabel, Theme.GREEN_BG, BONUS_INK));
        bar.add(statCard("TOTAL SCORE", scoreLabel, Theme.NAVY, Theme.AVATAR));
        add(bar, BorderLayout.NORTH);

        // image area
        RoundPanel water = new RoundPanel(Theme.SKY, new BorderLayout(0, 12));
        water.setBorder(new EmptyBorder(14, 16, 16, 16));
        JPanel phase = new JPanel(new BorderLayout(12, 6));
        phase.setOpaque(false);
        phase.add(phaseLabel, BorderLayout.WEST);
        phase.add(countdownLabel, BorderLayout.EAST);
        countdown.setUI(new BasicProgressBarUI());
        countdown.setBorderPainted(false);
        countdown.setBackground(Theme.CARD);
        countdown.setPreferredSize(new Dimension(0, 7));
        countdown.getAccessibleContext().setAccessibleName("Phase time remaining");
        phase.add(countdown, BorderLayout.SOUTH);
        water.add(phase, BorderLayout.NORTH);
        RoundPanel imageCard = new RoundPanel(Theme.CARD, 18, new BorderLayout());
        imageLabel.setFont(Theme.display(28));
        imageLabel.setForeground(Theme.NAV_ACTIVE);
        imageCard.add(imageLabel, BorderLayout.CENTER);
        water.add(imageCard, BorderLayout.CENTER);
        add(water, BorderLayout.CENTER);

        // answer card
        RoundPanel answers = new RoundPanel(Theme.CARD, new BorderLayout(0, 12));
        answers.setBorder(new EmptyBorder(18, 22, 16, 22));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(prompt, BorderLayout.WEST);
        retry.setVisible(false);
        retry.addActionListener(e -> loadNext());
        head.add(retry, BorderLayout.EAST);
        answers.add(head, BorderLayout.NORTH);

        JPanel row = new JPanel(new GridLayout(1, 10, 10, 0));
        row.setOpaque(false);
        for (int i = 0; i < 10; i++) {
            RoundButton b = new RoundButton(String.valueOf(i));
            b.setFont(Theme.display(22));
            b.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.TEXT);
            b.setPreferredSize(new Dimension(50, 46));
            b.setEnabled(false);
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
        JLabel rules = Theme.label("+" + GameEngine.CORRECT_POINTS + " correct  •  +"
                        + GameEngine.SPEED_BONUS + " speed bonus  •  +"
                        + GameEngine.STREAK_BONUS + " streak bonus at " + GameEngine.STREAK_MIN + "+ in a row",
                Theme.nunito(Font.PLAIN, 13), Theme.NAV_ACTIVE);
        rules.setAlignmentX(0f);
        foot.add(rules);
        answers.add(foot, BorderLayout.SOUTH);
        add(answers, BorderLayout.SOUTH);

        bindKeys();
        loadNext();
    }

    private static JPanel statCard(String caption, JLabel value, Color background, Color captionColor) {
        RoundPanel card = new RoundPanel(background, 16, new BorderLayout(0, 2));
        card.setBorder(new EmptyBorder(9, 16, 9, 16));
        card.add(Theme.label(caption, Theme.nunito(Font.BOLD, 12), captionColor), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void updateRoundLabel(int round) {
        roundLabel.setText(round + " of " + engine.getTotalRounds());
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
                    if (accepting) digits[n].doClick(0);
                }
            });
        }
    }

    /** Loads the next fish image from the API without freezing the UI. */
    private void loadNext() {
        accepting = false;
        setAnswersEnabled(false);
        imageLabel.setIcon(null);
        imageLabel.setText("Loading fish...");
        phaseLabel.setText("LOADING FISH");
        clearCountdown();
        prompt.setText("Memorize the fish before answering");
        retry.setVisible(false);
        feedback.setText("Loading fish...");
        feedback.setForeground(Theme.MUTED);
        imageWorker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                return engine.nextGame();
            }

            @Override
            protected void done() {
                if (isCancelled() || !SwingUtilities.isDescendingFrom(FishPanel.this, frame)) return;
                BufferedImage img = null;
                try {
                    img = get();
                } catch (Exception ignored) {
                    // handled below
                }
                if (img == null) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Fish image unavailable");
                    feedback.setText("Check your internet connection and press Retry.");
                    feedback.setForeground(Theme.RED);
                    phaseLabel.setText("FISH UNAVAILABLE");
                    retry.setVisible(true);
                    return;
                }
                imageLabel.setText(null);
                imageLabel.setIcon(new ImageIcon(img));
                updateRoundLabel(engine.getRound());
                startPreview();
            }
        };
        imageWorker.execute();
    }

    private void startPreview() {
        phaseLabel.setText("MEMORIZE THE FISH");
        feedback.setText("Count the fish before the picture disappears.");
        countdown.setForeground(Theme.PRIMARY);
        countdown.setMaximum(MEMORY_MILLIS);
        showCountdown(MEMORY_MILLIS, " to look");
        previewEndsAt = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(MEMORY_MILLIS);
        countdownTimer.start();
    }

    private void updateCountdown() {
        if (!SwingUtilities.isDescendingFrom(this, frame)) {
            countdownTimer.stop();
            return;
        }
        if (accepting) {
            long remaining = engine.getSpeedBonusRemainingMillis();
            if (remaining > 0) {
                showCountdown(remaining, " for +" + GameEngine.SPEED_BONUS + " pts");
            } else {
                countdown.setValue(0);
                countdownLabel.setText("Speed bonus ended");
                countdownTimer.stop();
            }
            return;
        }
        long remaining = previewEndsAt - System.nanoTime();
        if (remaining > 0) {
            showCountdown(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)), " to look");
            return;
        }
        imageLabel.setIcon(null);
        imageLabel.setText("Picture hidden");
        phaseLabel.setText("ANSWER FROM MEMORY");
        prompt.setText("How many fish did you see?");
        feedback.setText("Pick a number below or press 0–9 on your keyboard.");
        countdown.setForeground(BONUS_INK);
        countdown.setMaximum((int) GameEngine.SPEED_LIMIT_MS);
        engine.startAnswering();
        accepting = true;
        showCountdown(engine.getSpeedBonusRemainingMillis(), " for +" + GameEngine.SPEED_BONUS + " pts");
        setAnswersEnabled(true);
    }

    private void showCountdown(long millis, String suffix) {
        countdown.setValue((int) millis);
        countdownLabel.setText(((millis + 999) / 1000) + "s" + suffix);
    }

    private void clearCountdown() {
        countdownTimer.stop();
        countdown.setValue(0);
        countdownLabel.setText(" ");
    }

    private void setAnswersEnabled(boolean enabled) {
        for (RoundButton digit : digits) digit.setEnabled(enabled);
    }

    @Override
    public void removeNotify() {
        accepting = false;
        countdownTimer.stop();
        if (nextRoundTimer != null) nextRoundTimer.stop();
        if (imageWorker != null) imageWorker.cancel(true);
        super.removeNotify();
    }

    private void onAnswer(int n, RoundButton button) {
        if (!accepting) {
            return;
        }
        accepting = false;
        clearCountdown();
        AnswerResult r = engine.checkSolution(n);
        if (r == null) {
            return;
        }
        scoreLabel.setText(engine.getScore() + " pts");
        phaseLabel.setText("ROUND COMPLETE");
        imageLabel.setText(r.correct ? "Correct!" : "Correct answer: " + r.solution);
        streakLabel.setText(engine.getStreak() + " in a row");
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
            feedback.setText("You picked " + n + ". No points this round.");
            feedback.setForeground(Theme.RED);
        }

        nextRoundTimer = new Timer(1300, e -> {
            if (!SwingUtilities.isDescendingFrom(FishPanel.this, frame)) return;
            resetDigits();
            if (engine.isFinished()) {
                frame.showFishResult(engine, quizScore);
            } else {
                loadNext();
            }
        });
        nextRoundTimer.setRepeats(false);
        nextRoundTimer.start();
    }

    private void resetDigits() {
        for (RoundButton b : digits) {
            b.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.TEXT);
        }
    }
}
