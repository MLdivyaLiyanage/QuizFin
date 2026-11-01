package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.perisic.fish.engine.QuizEngine;
import com.perisic.fish.engine.Category;
import com.perisic.fish.engine.QuestionSource;
import com.perisic.fish.engine.Question;

/**
 * General Quiz setup: category (from OpenTDB), difficulty, number of
 * questions, then Start Quiz.
 */
class QuizSetupPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final GameGUI frame;
    private final transient QuestionSource server;
    private final JComboBox<Category> categoryBox = new JComboBox<>();
    private final RoundButton start = new RoundButton("Start Quiz");

    private final String[] difficulties = { "Easy", "Medium", "Hard" };
    private final int[] counts = { 5, 10, 15 };
    private int difficultyIdx = 0;
    private int countIdx = 1;
    private final List<RoundButton> diffButtons = new ArrayList<>();
    private final List<RoundButton> countButtons = new ArrayList<>();

    QuizSetupPanel(GameGUI frame) {
        super(new BorderLayout(0, 18));
        this.frame = frame;
        this.server = frame.services().questions;
        setOpaque(false);

        RoundPanel banner = new RoundPanel(Theme.SKY, new BorderLayout());
        banner.setBorder(new EmptyBorder(20, 26, 20, 26));
        JPanel bt = new JPanel();
        bt.setOpaque(false);
        bt.setLayout(new BoxLayout(bt, BoxLayout.Y_AXIS));
        bt.add(Theme.label("Ready for a brain challenge?", Font.BOLD, 20, Theme.TEXT));
        bt.add(Box.createVerticalStrut(6));
        bt.add(Theme.label("Choose 5, 10 or 15 questions, then play the same number of fish rounds.", Font.PLAIN, 13, Theme.MUTED));
        banner.add(bt, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        // form card
        RoundPanel card = new RoundPanel(Theme.CARD, new GridBagLayout());
        card.setBorder(new EmptyBorder(26, 30, 26, 30));
        card.setPreferredSize(new Dimension(640, 360));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        card.add(Theme.label("Configure your General Quiz", Font.BOLD, 18, Theme.TEXT), c);
        c.insets = new Insets(16, 0, 4, 0);
        card.add(Theme.label("Category", Font.BOLD, 12, Theme.TEXT), c);
        c.insets = new Insets(4, 0, 4, 0);
        categoryBox.setPreferredSize(new Dimension(300, 34));
        categoryBox.addItem(new Category(9, "General Knowledge"));
        card.add(categoryBox, c);
        c.insets = new Insets(14, 0, 4, 0);
        card.add(Theme.label("Difficulty", Font.BOLD, 12, Theme.TEXT), c);
        c.insets = new Insets(4, 0, 4, 0);
        card.add(chipRow(difficulties, diffButtons, 0, i -> difficultyIdx = i), c);
        c.insets = new Insets(14, 0, 4, 0);
        card.add(Theme.label("Number of questions", Font.BOLD, 12, Theme.TEXT), c);
        c.insets = new Insets(4, 0, 4, 0);
        String[] countLabels = { "5 Questions", "10 Questions", "15 Questions" };
        card.add(chipRow(countLabels, countButtons, 1, i -> countIdx = i), c);
        c.insets = new Insets(20, 0, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        start.addActionListener(e -> startQuiz());
        card.add(start, c);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        wrap.add(card);
        add(wrap, BorderLayout.CENTER);

        loadCategories();
    }

    private interface IndexHandler {
        void chosen(int i);
    }

    private JComponent chipRow(String[] labels, List<RoundButton> store, int selected, IndexHandler h) {
        JPanel row = new JPanel(new GridLayout(1, labels.length, 12, 0));
        row.setOpaque(false);
        for (int i = 0; i < labels.length; i++) {
            RoundButton b = new RoundButton(labels[i]);
            b.setFont(Theme.font(Font.BOLD, 13));
            final int idx = i;
            b.addActionListener(e -> {
                h.chosen(idx);
                paintChips(store, idx);
            });
            store.add(b);
            row.add(b);
        }
        paintChips(store, selected);
        return row;
    }

    private static void paintChips(List<RoundButton> chips, int selected) {
        for (int i = 0; i < chips.size(); i++) {
            boolean on = i == selected;
            chips.get(i).setColors(on ? Theme.CHIP_SEL : Theme.CHIP, on ? Theme.PRIMARY : Theme.CHIP_BORDER,
                    Theme.TEXT);
        }
    }

    private void loadCategories() {
        new SwingWorker<List<Category>, Void>() {
            @Override
            protected List<Category> doInBackground() {
                return server.getCategories();
            }

            @Override
            protected void done() {
                try {
                    List<Category> cats = get();
                    categoryBox.removeAllItems();
                    for (Category c : cats) {
                        categoryBox.addItem(c);
                        if (c.id == 9) {
                            categoryBox.setSelectedItem(c);
                        }
                    }
                } catch (Exception ignored) {
                    // keep the default General Knowledge entry
                }
            }
        }.execute();
    }

    private void startQuiz() {
        Category cat = (Category) categoryBox.getSelectedItem();
        if (cat == null) {
            return;
        }
        final String difficulty = difficulties[difficultyIdx];
        final int amount = counts[countIdx];
        start.setEnabled(false);
        start.setText("Loading questions...");

        new SwingWorker<List<Question>, Void>() {
            @Override
            protected List<Question> doInBackground() throws Exception {
                return server.getQuestions(amount, cat.id, difficulty);
            }

            @Override
            protected void done() {
                try {
                    List<Question> qs = get();
                    frame.startQuiz(new QuizEngine(qs, difficulty), cat.name);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    JOptionPane.showMessageDialog(frame, "Could not load questions:\n" + cause.getMessage(),
                            "Quiz", JOptionPane.WARNING_MESSAGE);
                    start.setEnabled(true);
                    start.setText("Start Quiz");
                }
            }
        }.execute();
    }
}
