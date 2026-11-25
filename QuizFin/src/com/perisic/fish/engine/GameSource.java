package com.perisic.fish.engine;

/** Where Fish Challenge games come from Fish API. there we used the low cuping */
public interface GameSource {
    /** a game, or null if none could be loaded. */
    Game getRandomGame();
}