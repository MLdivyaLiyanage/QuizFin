package com.perisic.fish.engine;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ScoreEntry {
    public enum Mode {
        FISH_ONLY("Fish only"), QUIZ_AND_FISH("Quiz + Fish"), UNKNOWN("Not recorded");

        public final String label;

        Mode(String label) { this.label = label; }
    }

    public final String player;
    public final int score;
    public final long time;
    public final int fishScore;
    public final Integer quizScore;
    public final Mode mode;

    /** Older records only stored fish points, without recording the game mode. */
    public ScoreEntry(String player, int score, long time) {
        this(player, null, score, time, Mode.UNKNOWN);
    }

    /** A null quiz score means the player started Fish Challenge directly. */
    public ScoreEntry(String player, Integer quizScore, int fishScore, long time) {
        this(player, quizScore, fishScore, time, quizScore == null ? Mode.FISH_ONLY : Mode.QUIZ_AND_FISH);
    }

    /** Used when the stored mode must be preserved exactly (database rows). */
    public ScoreEntry(String player, Integer quizScore, int fishScore, long time, Mode mode) {
        this.player = player;
        this.quizScore = quizScore;
        this.fishScore = fishScore;
        this.score = (quizScore == null ? 0 : quizScore) + fishScore;
        this.mode = mode;
        this.time = time;
    }

    /** When the score was recorded (epoch millis of the game-end moment). */
    public long getPlayedAt() {
        return time;
    }

    /** When the score was recorded, as a Java time value. */
    public Instant getTime() {
        return Instant.ofEpochMilli(time);
    }

    /** When the score was recorded, as a user-readable date and time string. */
    public String getTimeDisplay() {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"));
    }
}
