package com.impostor.juego;

/** Palabra elegida al azar junto con su categoría. */
public class WordPick {
    public final String category;
    public final String word;

    public WordPick(String category, String word) {
        this.category = category;
        this.word = word;
    }
}
