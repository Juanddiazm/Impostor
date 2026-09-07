package com.impostor.juego;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Estado de la partida en curso. Se comparte entre pantallas mediante {@link #current}. */
public class GameState {

    public static GameState current;

    public final List<String> players;
    public final int impostorCount;
    public final boolean hintForImpostor;
    public final int timerMinutes;
    public final List<String> categories;

    public boolean[] isImpostor;
    public boolean[] alive;
    public String word;
    public String category;
    public int round;
    public int starterIndex;

    private final Random random = new Random();

    public GameState(List<String> players, int impostorCount, boolean hintForImpostor,
                     int timerMinutes, List<String> categories) {
        this.players = new ArrayList<>(players);
        this.impostorCount = impostorCount;
        this.hintForImpostor = hintForImpostor;
        this.timerMinutes = timerMinutes;
        this.categories = new ArrayList<>(categories);
    }

    /** Reparte roles y elige palabra. Devuelve false si no hay palabras disponibles. */
    public boolean newRound(WordRepository repo) {
        WordPick pick = repo.pickRandom(categories, random);
        if (pick == null) return false;
        category = pick.category;
        word = pick.word;

        int n = players.size();
        isImpostor = new boolean[n];
        alive = new boolean[n];
        for (int i = 0; i < n; i++) alive[i] = true;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        for (int k = 0; k < impostorCount && !indices.isEmpty(); k++) {
            int idx = indices.remove(random.nextInt(indices.size()));
            isImpostor[idx] = true;
        }
        round = 1;
        starterIndex = random.nextInt(n);
        return true;
    }

    public void nextRound() {
        round++;
        starterIndex = randomAlive();
    }

    public int randomAlive() {
        List<Integer> aliveIdx = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (alive[i]) aliveIdx.add(i);
        return aliveIdx.get(random.nextInt(aliveIdx.size()));
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

    public List<String> impostorNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) if (isImpostor[i]) names.add(players.get(i));
        return names;
    }

    /** Ganan los jugadores cuando no queda ningún impostor. */
    public boolean civiliansWin() {
        return impostorsAlive() == 0;
    }

    /** Ganan los impostores cuando igualan o superan en número a los demás. */
    public boolean impostorsWin() {
        return impostorsAlive() > 0 && impostorsAlive() >= civiliansAlive();
    }
}
