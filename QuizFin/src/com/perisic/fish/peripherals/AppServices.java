package com.perisic.fish.peripherals;

import com.perisic.fish.auth.AuthService;
import com.perisic.fish.engine.GameServer;
import com.perisic.fish.engine.GameSource;
import com.perisic.fish.engine.Leaderboard;
import com.perisic.fish.engine.QuestionSource;
import com.perisic.fish.engine.QuizServer;
import com.perisic.fish.engine.ScoreStore;

/**
 * Composition root: the ONLY place that chooses the concrete implementations
 * (Open Trivia DB, Fish API, file leaderboard, file-based auth). Screens get
 * these through interfaces, so they stay decoupled and testable.
 */
public class AppServices {
    public final QuestionSource questions;
    public final GameSource games;
    public final ScoreStore scores;
    public final AuthService auth;

    public AppServices(QuestionSource questions, GameSource games, ScoreStore scores, AuthService auth) {
        this.questions = questions;
        this.games = games;
        this.scores = scores;
        this.auth = auth;
    }

    /** Production wiring. */
    public AppServices() {
        this(new QuizServer(), new GameServer(), new Leaderboard(), new AuthService());
    }
}
