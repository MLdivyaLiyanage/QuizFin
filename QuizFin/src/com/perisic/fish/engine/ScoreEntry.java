package com.perisic.fish.engine;

public class ScoreEntry {
    public enum Mode {
        FISH_ONLY("Fish only"), QUIZ_AND_FISH("Quiz + Fish"), UNKNOWN("Not recorded");

        public final String label;

        Mode(String label) { this.label = label; }
    }

    public final String player;
    public final int score;
    public final long time;
    public final Integer quizScore;
    public final int fishScore;
    public final Mode mode;

    /** Older records only stored fish points, without recording the game mode. */
    public ScoreEntry(String player, int score, long time) {
        this(player, null, score, time, Mode.UNKNOWN);
    }

    /** A null quiz score means the player started Fish Challenge directly. */
    public ScoreEntry(String player, Integer quizScore, int fishScore, long time) {
        this(player, quizScore, fishScore, time, quizScore == null ? Mode.FISH_ONLY : Mode.QUIZ_AND_FISH);
    }

    private ScoreEntry(String player, Integer quizScore, int fishScore, long time, Mode mode) {
        this.player = player;
        this.quizScore = quizScore;
        this.fishScore = fishScore;
        this.score = (quizScore == null ? 0 : quizScore) + fishScore;
        this.mode = mode;
        this.time = time;
    }
}
