package com.perisic.fish.engine;

import java.awt.image.BufferedImage;

/**
 * Fish Challenge engine. One engine per player per play-through.
 *
 * Scoring (from the design document):
 * <ul>
 * <li>Correct answer: +10</li>
 * <li>Speed bonus: +5 if answered within 5 seconds</li>
 * <li>Streak bonus: +5 when 3 or more correct answers in a row</li>
 * <li>Wrong answer: 0 points, streak resets</li>
 * </ul>
 */
public class GameEngine {

    public static final int ROUNDS = 10;
    public static final int CORRECT_POINTS = 10;
    public static final int SPEED_BONUS = 5;
    public static final long SPEED_LIMIT_MS = 5000;
    public static final int STREAK_BONUS = 5;
    public static final int STREAK_MIN = 3;

    /** Outcome of one answer. */
    public static class AnswerResult {
        public final boolean correct;
        public final int solution;
        public final int base, speed, streak;

        AnswerResult(boolean correct, int solution, int base, int speed, int streak) {
            this.correct = correct;
            this.solution = solution;
            this.base = base;
            this.speed = speed;
            this.streak = streak;
        }

        public int total() {
            return base + speed + streak;
        }
    }

    private final String thePlayer;
    private final GameSource theGames;
    private final int totalRounds;
    private Game current = null;
    private long roundStart = 0;

    private int round = 0; // rounds started
    private int answered = 0; // rounds answered
    private int score = 0;
    private int streak = 0;
    private int bestStreak = 0;
    private int correctCount = 0;

    /**
     * Each player has their own game engine.
     */
    public GameEngine(String player) {
        this(player, new GameServer());
    }

    /** Dependency injection: any GameSource (Fish API, test stub, ...). */
    public GameEngine(String player, GameSource source) {
        this(player, source, ROUNDS);
    }

    /** Use the completed quiz's question count for this challenge. */
    public GameEngine(String player, GameSource source, int totalRounds) {
        if (totalRounds <= 0) {
            throw new IllegalArgumentException("A fish challenge must contain at least one round.");
        }
        thePlayer = player;
        theGames = source;
        this.totalRounds = totalRounds;
    }

    public String getPlayer() {
        return thePlayer;
    }

    /**
     * Loads the next fish image from the Fish API (blocking - call off the UI
     * thread).
     *
     * @return the image, or null if the API could not be reached. In that case
     *         the round counter is not advanced, so it can simply be retried.
     */
    public BufferedImage nextGame() {
        if (isFinished()) {
            return null;
        }
        if (current != null) {
            return current.getImage();
        }
        Game g = theGames.getRandomGame();
        if (g == null || g.getImage() == null) {
            return null;
        }
        current = g;
        round++;
        roundStart = System.currentTimeMillis();
        return current.getImage();
    }

    /**
     * Checks the player's answer for the current round and updates score and
     * streak. Answering the same round twice is ignored (returns null).
     */
    public AnswerResult checkSolution(int i) {
        if (current == null) {
            return null;
        }
        int solution = current.getSolution();
        current = null; // a round can be answered only once
        answered++;

        if (i != solution) {
            streak = 0;
            return new AnswerResult(false, solution, 0, 0, 0);
        }
        long took = System.currentTimeMillis() - roundStart;
        streak++;
        correctCount++;
        bestStreak = Math.max(bestStreak, streak);
        int speed = took <= SPEED_LIMIT_MS ? SPEED_BONUS : 0;
        int streakPts = streak >= STREAK_MIN ? STREAK_BONUS : 0;
        AnswerResult r = new AnswerResult(true, solution, CORRECT_POINTS, speed, streakPts);
        score += r.total();
        return r;
    }

    public boolean isFinished() {
        return answered >= totalRounds;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getScore() {
        return score;
    }

    public int getStreak() {
        return streak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public int getRound() {
        return round;
    }

    public int getCorrectCount() {
        return correctCount;
    }
}
