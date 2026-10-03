package com.impostor.juego;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Estado de la partida en curso. Se comparte entre pantallas mediante {@link #current}. */
public class GameState {

    public static GameState current;

    /** Modos de juego. */
    public static final int MODE_IMPOSTOR = 0;
    public static final int MODE_UNDERCOVER = 1;

    public final List<String> players;
    public final int mode;
    /** Impostores (modo Impostor) o undercovers (modo Undercover). */
    public final int impostorCount;
    /** Jugadores sin palabra en modo Undercover. Siempre 0 en modo Impostor. */
    public final int mrWhiteCount;
    /** El impostor (o Mr. White) recibe la categoría como pista. */
    public final boolean hintForImpostor;
    public final int timerMinutes;
    public final List<String> categories;

    /** Jugadores que no son civiles: impostores, undercovers y Mr. White. */
    public boolean[] isImpostor;
    public boolean[] isMrWhite;
    public boolean[] alive;
    /** Palabra de los civiles. */
    public String word;
    /** Palabra parecida que reciben los undercovers (solo modo Undercover). */
    public String undercoverWord;
    public String category;
    public int round;
    public int starterIndex;

    private final Random random = new Random();

    public GameState(List<String> players, int impostorCount, boolean hintForImpostor,
                     int timerMinutes, List<String> categories) {
        this(players, MODE_IMPOSTOR, impostorCount, 0, hintForImpostor, timerMinutes, categories);
    }

    public GameState(List<String> players, int mode, int impostorCount, int mrWhiteCount,
                     boolean hintForImpostor, int timerMinutes, List<String> categories) {
        this.players = new ArrayList<>(players);
        this.mode = mode;
        this.impostorCount = impostorCount;
        this.mrWhiteCount = mode == MODE_UNDERCOVER ? mrWhiteCount : 0;
        this.hintForImpostor = hintForImpostor;
        this.timerMinutes = timerMinutes;
        this.categories = new ArrayList<>(categories);
    }

    public boolean isUndercoverMode() {
        return mode == MODE_UNDERCOVER;
    }

    /** Reparte roles y elige palabra. Devuelve false si no hay palabras disponibles. */
    public boolean newRound(WordRepository repo) {
        WordPick pick = isUndercoverMode()
                ? repo.pickPair(categories, random)
                : repo.pickRandom(categories, random);
        if (pick == null) return false;
        category = pick.category;
        word = pick.word;
        undercoverWord = pick.undercoverWord;

        int n = players.size();
        isImpostor = new boolean[n];
        isMrWhite = new boolean[n];
        alive = new boolean[n];
        for (int i = 0; i < n; i++) alive[i] = true;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        for (int k = 0; k < impostorCount + mrWhiteCount && !indices.isEmpty(); k++) {
            int idx = indices.remove(random.nextInt(indices.size()));
            isImpostor[idx] = true;
            isMrWhite[idx] = k >= impostorCount;
        }
        round = 1;
        starterIndex = randomStarter();
        return true;
    }

    public void nextRound() {
        round++;
        starterIndex = randomStarter();
    }

    /** Mr. White nunca empieza la ronda: sin escuchar ninguna pista no tendría nada que decir. */
    private int randomStarter() {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (alive[i] && !isMrWhite[i]) candidates.add(i);
        if (candidates.isEmpty()) return randomAlive();
        return candidates.get(random.nextInt(candidates.size()));
    }

    public int randomAlive() {
        List<Integer> aliveIdx = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (alive[i]) aliveIdx.add(i);
        return aliveIdx.get(random.nextInt(aliveIdx.size()));
    }

    /** Undercover: no es civil, pero sí tiene palabra (la parecida). */
    public boolean isUndercover(int i) {
        return isUndercoverMode() && isImpostor[i] && !isMrWhite[i];
    }

    /** Palabra que ve el jugador al revisar su rol, o null si no tiene (impostor / Mr. White). */
    public String wordFor(int i) {
        if (isUndercover(i)) return undercoverWord;
        return isImpostor[i] ? null : word;
    }

    public int impostorsAlive() {
        int c = 0;
        for (int i = 0; i < players.size(); i++) if (alive[i] && isImpostor[i]) c++;
        return c;
    }

    public int civiliansAlive() {
        int c = 0;
        for (int i = 0; i < players.size(); i++) if (alive[i] && !isImpostor[i]) c++;
        return c;
    }

    /** Impostores (modo Impostor) o undercovers (modo Undercover), sin contar a Mr. White. */
    public List<String> impostorNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (isImpostor[i] && !isMrWhite[i]) names.add(players.get(i));
        return names;
    }

    public List<String> mrWhiteNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (isMrWhite[i]) names.add(players.get(i));
        return names;
    }

    /** Ganan los jugadores (civiles) cuando no queda ningún impostor, undercover ni Mr. White. */
    public boolean civiliansWin() {
        return impostorsAlive() == 0;
    }

    /**
     * Modo Impostor: ganan los impostores cuando igualan o superan en número a los demás.
     * Modo Undercover: ganan los infiltrados si sobreviven hasta que solo queda un civil.
     */
    public boolean impostorsWin() {
        if (impostorsAlive() == 0) return false;
        if (isUndercoverMode()) return civiliansAlive() <= 1;
        return impostorsAlive() >= civiliansAlive();
    }
}
