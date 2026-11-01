package com.perisic.fish.engine;

/** Where Fish Challenge games come from (the Fish API in production). */
public interface GameSource {
    /** @return a game, or null if none could be loaded. */
    Game getRandomGame();
}