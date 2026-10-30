package com.perisic.fish.engine;

public class Category {
    public final int id;
    public final String name;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String toString()
    {
        return name;
    }
}
