package com.perisic.fish.peripherals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
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
    private final JLabel summary = Theme.label("", Font.BOLD, 15, Theme.TEXT);
    private boolean loading;

    private final String[] difficulties = { "Easy", "Medium", "Hard" };
    private final int[] counts = { 5, 10, 15 };
    private int difficultyIdx = 0;
    private int countIdx = 1;
    private final List<QuizChoiceButton> diffButtons = new ArrayList<>();
    private final List<QuizChoiceButton> countButtons = new ArrayList<>();

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
        bt.add(Theme.label("Ready for a brain challenge?", Font.BOLD, 22, Theme.TEXT));
        bt.add(Box.createVerticalStrut(6));
        bt.add(Theme.label("Choose your quiz length, then play matching fish rounds.", Theme.nunito(Font.PLAIN, 14), Theme.MUTED));
        banner.add(bt, BorderLayout.CENTER);
        add(banner, BorderLayout.NORTH);

        // form card
        RoundPanel card = new RoundPanel(Theme.CARD, new GridBagLayout());
        card.setBorder(new EmptyBorder(16, 24, 16, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);

        card.add(Theme.label("Build your quiz", Font.BOLD, 22, Theme.TEXT), c);
        c.gridy++;
        c.insets = new Insets(4, 0, 0, 0);
        card.add(Theme.label("Choose a topic and a challenge level that suits you.", Theme.nunito(Font.PLAIN, 14), Theme.MUTED), c);
        c.gridy++;
        c.insets = new Insets(16, 0, 0, 0);
        card.add(fieldHeading("Category", "Choose a topic you enjoy"), c);
        c.gridy++;
        c.insets = new Insets(6, 0, 0, 0);
        categoryBox.setFont(Theme.font(Font.PLAIN, 15));
        categoryBox.setForeground(Theme.TEXT);
        categoryBox.setBackground(Theme.CHIP);
        categoryBox.setPreferredSize(new Dimension(300, 44));
        categoryBox.setMaximumRowCount(10);
        categoryBox.getAccessibleContext().setAccessibleName("Quiz category");
        categoryBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setBorder(new EmptyBorder(7, 10, 7, 10));
                label.setBackground(index >= 0 && selected ? Theme.CHIP_SEL : Theme.CHIP);
                label.setForeground(Theme.TEXT);
                label.setFont(Theme.font(Font.PLAIN, 15));
                return label;
            }
        });
        categoryBox.addItem(new Category(9, "General Knowledge"));
        card.add(categoryBox, c);
        c.gridy++;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(fieldHeading("Difficulty", "Choose one level"), c);
        c.gridy++;
        c.insets = new Insets(6, 0, 0, 0);
        card.add(chipRow(difficulties, diffButtons, 0, i -> { difficultyIdx = i; updateSummary(); },
                new String[] { "Correct answer: +10", "Correct answer: +15", "Correct answer: +20" }), c);
        c.gridy++;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(fieldHeading("Number of questions", "Choose a short or longer quiz"), c);
        c.gridy++;
        c.insets = new Insets(6, 0, 0, 0);
        String[] countLabels = { "5 Questions", "10 Questions", "15 Questions" };
        card.add(chipRow(countLabels, countButtons, 1, i -> { countIdx = i; updateSummary(); }), c);
        c.gridy++;
        c.insets = new Insets(18, 0, 0, 0);
        RoundPanel footer = new RoundPanel(Theme.CHIP, 16, new BorderLayout(12, 0));
        footer.setBorder(new EmptyBorder(10, 14, 10, 10));
        JPanel details = new JPanel(new GridLayout(2, 1, 0, 3));
        details.setOpaque(false);
        details.add(Theme.label("Your quiz", Theme.nunito(Font.PLAIN, 13), Theme.MUTED));
        details.add(summary);
        footer.add(details, BorderLayout.CENTER);
        updateSummary();
        start.setPreferredSize(new Dimension(190, 46));
        start.setFocusPainted(true);
        start.addActionListener(e -> startQuiz());
        footer.add(start, BorderLayout.EAST);
        card.add(footer, c);

        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setOpaque(false);
        GridBagConstraints placement = new GridBagConstraints();
        placement.weightx = 1;
        placement.weighty = 1;
        placement.fill = GridBagConstraints.HORIZONTAL;
        placement.anchor = GridBagConstraints.NORTH;
        wrap.add(card, placement);
        add(wrap, BorderLayout.CENTER);

        loadCategories();
    }

    private static JPanel fieldHeading(String title, String hint) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.add(Theme.label(title, Font.BOLD, 20, Theme.TEXT), BorderLayout.WEST);
        row.add(Theme.label(hint, Theme.nunito(Font.PLAIN, 13), Theme.MUTED), BorderLayout.EAST);
        return row;
    }

    private void updateSummary() {
        summary.setText(counts[countIdx] + " questions  \u00b7  " + difficulties[difficultyIdx]);
    }

    private interface IndexHandler {
        void chosen(int i);
    }

    private JComponent chipRow(String[] labels, List<QuizChoiceButton> store, int selected, IndexHandler h) {
        return chipRow(labels, store, selected, h, null);
    }

    private JComponent chipRow(String[] labels, List<QuizChoiceButton> store, int selected, IndexHandler h,
            String[] descriptions) {
        JPanel row = new JPanel(new GridLayout(1, labels.length, 12, 0));
        row.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < labels.length; i++) {
            String description = descriptions == null ? null : descriptions[i];
            QuizChoiceButton b = new QuizChoiceButton(labels[i], description);
            group.add(b);
            if (description != null) {
                b.getAccessibleContext().setAccessibleName(labels[i] + ", " + description);
                b.setToolTipText(description);
            }
            final int idx = i;
            b.addActionListener(e -> {
                h.chosen(idx);
            });
            store.add(b);
            row.add(b);
            for (String key : new String[] { "LEFT", "RIGHT", "UP", "DOWN" }) {
                int direction = key.equals("LEFT") || key.equals("UP") ? -1 : 1;
                b.getInputMap().put(KeyStroke.getKeyStroke(key), key);
                b.getActionMap().put(key, new AbstractAction() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        QuizChoiceButton next = store.get(Math.floorMod(idx + direction, store.size()));
                        if (next.isEnabled()) {
                            next.requestFocusInWindow();
                            next.doClick(0);
                        }
                    }
                });
            }
        }
        store.get(selected).setSelected(true);
        return row;
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
                    if (cats == null || cats.isEmpty() || loading) return;
                    Category selected = (Category) categoryBox.getSelectedItem();
                    categoryBox.removeAllItems();
                    for (Category c : cats) {
                        categoryBox.addItem(c);
                        if (selected != null && c.id == selected.id) {
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
        if (loading) return;
        Category cat = (Category) categoryBox.getSelectedItem();
        if (cat == null) {
            return;
        }
        final String difficulty = difficulties[difficultyIdx];
        final int amount = counts[countIdx];
        setLoading(true);
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
                    if (!javax.swing.SwingUtilities.isDescendingFrom(QuizSetupPanel.this, frame)) return;
                    frame.startQuiz(new QuizEngine(qs, difficulty), cat.name);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    setLoading(false);
                    start.setText("Start Quiz");
                    if (javax.swing.SwingUtilities.isDescendingFrom(QuizSetupPanel.this, frame)) {
                        JOptionPane.showMessageDialog(frame, "Could not load questions:\n" + cause.getMessage()
                                + "\nCheck your connection or try another category. Your settings have been kept.",
                                "Quiz unavailable", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    private void setLoading(boolean busy) {
        loading = busy;
        start.setEnabled(!busy);
        categoryBox.setEnabled(!busy);
        for (QuizChoiceButton b : diffButtons) b.setEnabled(!busy);
        for (QuizChoiceButton b : countButtons) b.setEnabled(!busy);
    }
}
