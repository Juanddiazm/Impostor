package com.impostor.juego;

/** Palabra elegida al azar junto con su categoría (y, en modo Undercover, la palabra parecida). */
public class WordPick {
    public final String category;
    public final String word;
    /** Palabra del undercover: parecida a {@link #word} pero distinta. Null en modo Impostor. */
    public final String undercoverWord;

    public WordPick(String category, String word) {
        this(category, word, null);
    }

    public WordPick(String category, String word, String undercoverWord) {
        this.category = category;
        this.word = word;
        this.undercoverWord = undercoverWord;
    }
}
