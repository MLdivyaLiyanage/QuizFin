package com.perisic.fish.engine;

import java.util.List;

/**
 * One multiple-choice trivia question (from Open Trivia DB).
 */
public class Question {
    public final String category;
    public final String difficulty;
    public final String text;
    public final String correct;
    /** All answers (correct + wrong) in shiffled order.*/
    public final List<String> options;

    public Question(String category, String difficulty, String text, String correct, List<String> options) {
        this.category = category;
        this.difficulty = difficulty;
        this.text = text;
        this.correct = correct;
        this.options = options;
    }
}
