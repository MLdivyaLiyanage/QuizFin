package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.Question;
import com.perisic.fish.engine.QuizEngine;

/**
 * One General Quiz question at a time: select answer, check, update score,
 * next question, final score.
 */
class QuizPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final GameGUI frame;
    private final QuizEngine engine;

    private final JLabel counter = Theme.label("", Font.BOLD, 15, Theme.TEXT);
    private final JLabel scoreLabel = Theme.label("", Font.BOLD, 15, Theme.PRIMARY);
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel meta = Theme.label("", Font.BOLD, 11, Theme.PRIMARY);
    private final JTextArea questionText = new JTextArea();
    private final JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 14, 14));
    private final JLabel feedback = Theme.label(" ", Font.PLAIN, 13, Theme.MUTED);
    private final RoundButton next = new RoundButton("Next Question");
    private final List<RoundButton> optionButtons = new ArrayList<>();
    private boolean answered = false;

    QuizPanel(GameGUI frame, QuizEngine engine) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        this.engine = engine;
        setOpaque(false);

        // progress card
        RoundPanel top = new RoundPanel(Theme.CARD, 16, new BorderLayout(0, 8));
        top.setBorder(new EmptyBorder(14, 20, 14, 20));
        JPanel line = new JPanel(new BorderLayout());
        line.setOpaque(false);
        line.add(counter, BorderLayout.WEST);
        line.add(scoreLabel, BorderLayout.EAST);
        progress.setBorderPainted(false);
        progress.setForeground(new Color(0x3F, 0xB4, 0xDC));
        progress.setBackground(new Color(0xE3, 0xEE, 0xF6));
        progress.setPreferredSize(new Dimension(100, 8));
        top.add(line, BorderLayout.NORTH);
        top.add(progress, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // question card
        RoundPanel card = new RoundPanel(Theme.CARD, new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(24, 26, 20, 26));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        meta.setAlignmentX(0f);
        head.add(meta);
        head.add(Box.createVerticalStrut(10));
        questionText.setEditable(false);
        questionText.setOpaque(false);
        questionText.setLineWrap(true);
        questionText.setWrapStyleWord(true);
        questionText.setFont(Theme.font(Font.BOLD, 20));
        questionText.setForeground(Theme.TEXT);
        questionText.setAlignmentX(0f);
        head.add(questionText);
        card.add(head, BorderLayout.NORTH);

        optionsGrid.setOpaque(false);
        JPanel optionsHolder = new JPanel(new BorderLayout());
        optionsHolder.setOpaque(false);
        optionsHolder.add(optionsGrid, BorderLayout.NORTH);
        card.add(optionsHolder, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        foot.add(feedback, BorderLayout.WEST);
        next.addActionListener(e -> onNext());
        foot.add(next, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        bindKeys();
        showQuestion();
    }

    /** Keyboard events: A-D choose an answer, Enter goes to the next question. */
    private void bindKeys() {
        javax.swing.InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getActionMap();
        for (int i = 0; i < 4; i++) {
            final int n = i;
            im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A + i, 0), "opt" + i);
            im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1 + i, 0), "opt" + i);
            am.put("opt" + i, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (n < optionButtons.size()) {
                        optionButtons.get(n).doClick(80);
                    }
                }
            });
        }
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "next");
        am.put("next", new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (next.isEnabled()) {
                    next.doClick(80);
                }
            }
        });
    }

    private void showQuestion() {
        Question q = engine.current();
        answered = false;
        counter.setText(String.format("Question %02d / %02d", engine.getNumber(), engine.getTotal()));
        scoreLabel.setText("Score: " + engine.getScore() + " pts");
        progress.setValue((engine.getNumber() - 1) * 100 / engine.getTotal());
        meta.setText((q.category + " • " + q.difficulty).toUpperCase());
        questionText.setText(q.text);
        feedback.setText("Choose an answer to continue.");
        feedback.setForeground(Theme.MUTED);
        next.setEnabled(false);
        next.setText(engine.isLastQuestion() ? "Play Fish Game" : "Next Question");

        optionsGrid.removeAll();
        optionButtons.clear();
        char letter = 'A';
        for (String opt : q.options) {
            RoundButton b = new RoundButton(
                    "<html><body style='width:300px'><b>" + letter + "</b> &nbsp;&nbsp; " + Theme.esc(opt)
                            + "</body></html>");
            b.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            b.setFont(Theme.font(Font.PLAIN, 14));
            b.setColors(Theme.CHIP, Theme.CHIP_BORDER, Theme.TEXT);
            b.setPreferredSize(new Dimension(300, 60));
            b.addActionListener(e -> onAnswer(opt, b));
            optionButtons.add(b);
            optionsGrid.add(b);
            letter++;
        }
        optionsGrid.revalidate();
        optionsGrid.repaint();
    }

    private void onAnswer(String chosen, RoundButton button) {
        if (answered) {
            return;
        }
        answered = true;
        Question q = engine.current();
        boolean ok = engine.submit(chosen);

        // colour the chosen button, and always reveal the right one
        for (int i = 0; i < optionButtons.size(); i++) {
            if (q.options.get(i).equals(q.correct)) {
                optionButtons.get(i).setColors(Theme.GREEN_BG, Theme.GREEN, Theme.TEXT);
            }
        }
        if (!ok) {
            button.setColors(Theme.RED_BG, Theme.RED, Theme.TEXT);
        }
        scoreLabel.setText("Score: " + engine.getScore() + " pts");
        progress.setValue(engine.getNumber() * 100 / engine.getTotal());
        if (engine.unlocksFishChallenge()) {
            next.setText("Play Fish Game");
        }
        if (ok) {
            feedback.setText("Correct! +" + engine.pointsPerQuestion() + " pts");
            feedback.setForeground(Theme.GREEN);
        } else {
            feedback.setText("Not quite – the answer was: " + q.correct);
            feedback.setForeground(Theme.RED);
        }
        next.setEnabled(true);
    }

    private void onNext() {
        if (!answered) {
            return;
        }
        if (engine.isLastQuestion()) {
            frame.showQuizResult(engine);
        } else {
            engine.next();
            showQuestion();
        }
    }
}
