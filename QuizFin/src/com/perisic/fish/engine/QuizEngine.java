package com.perisic.fish.engine;

import java.util.List;

/**
 * General Quiz engine. Correct answer = points by difficulty (easy 10, medium
 * 15, hard 20). Wrong answer = 0 points.
 */
public class QuizEngine {
    private final List<Question> questions;
    private final String difficulty;
    private int index = 0;
    private int score = 0;
    private int correctCount = 0;
    private boolean answered = false;

    public QuizEngine(List<Question> questions, String difficulty) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("A quiz must contain at least one question.");
        }
        this.questions = questions;
        this.difficulty = difficulty;
    }

    public int pointsPerQuestion() {
        switch (difficulty.toLowerCase()) {
            case "hard":
                return 20;
            case "medium":
                return 15;
            default:
                return 10;
        }
    }

    public Question current() {
        return questions.get(index);
    }

    /** Checks an answer for the current question. Only the first answer counts. */
    public boolean submit(String answer) {
        if (answered) {
            return false;
        }
        answered = true;
        boolean ok = current().correct.equals(answer);
        if (ok) {
            score += pointsPerQuestion();
            correctCount++;
        }
        return ok;
    }

    public boolean isLastQuestion() {
        return index >= questions.size() - 1;
    }

    /** Moves to the next question. */
    public void next() {
        if (answered && !isLastQuestion()) {
            index++;
            answered = false;
        }
    }

    public int getNumber() {
        return index + 1;
    }

    public int getTotal() {
        return questions.size();
    }

    public int getScore() {
        return score;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    /** A quiz is complete after the player answers its final question. */
    public boolean isFinished() {
        return isLastQuestion() && answered;
    }

    /** Completing a quiz unlocks the fish challenge, regardless of score. */
    public boolean unlocksFishChallenge() {
        return isFinished();
    }

    public String getDifficulty() {
        return difficulty;
    }
}
