import android.content.SharedPreferences;
import com.impostor.juego.*;
import java.util.*;

public class LogicTest {
    /** SharedPreferences en memoria para probar sin Android. */
    static class FakePrefs implements SharedPreferences {
        final Map<String, Object> map = new HashMap<>();
        public Map<String, ?> getAll() { return map; }
        public String getString(String k, String d) { return map.containsKey(k) ? (String) map.get(k) : d; }
        public Set<String> getStringSet(String k, Set<String> d) { return d; }
        public int getInt(String k, int d) { return map.containsKey(k) ? (Integer) map.get(k) : d; }
        public long getLong(String k, long d) { return d; }
        public float getFloat(String k, float d) { return d; }
        public boolean getBoolean(String k, boolean d) { return map.containsKey(k) ? (Boolean) map.get(k) : d; }
        public boolean contains(String k) { return map.containsKey(k); }
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
        public Editor edit() {
            return new Editor() {
                final Map<String, Object> pending = new HashMap<>();
                public Editor putString(String k, String v) { pending.put(k, v); return this; }
                public Editor putStringSet(String k, Set<String> v) { return this; }
                public Editor putInt(String k, int v) { pending.put(k, v); return this; }
                public Editor putLong(String k, long v) { return this; }
                public Editor putFloat(String k, float v) { return this; }
                public Editor putBoolean(String k, boolean v) { pending.put(k, v); return this; }
                public Editor remove(String k) { pending.put(k, null); return this; }
                public Editor clear() { map.clear(); return this; }
                public boolean commit() { apply(); return true; }
                public void apply() { for (Map.Entry<String, Object> e : pending.entrySet()) { if (e.getValue() == null) map.remove(e.getKey()); else map.put(e.getKey(), e.getValue()); } }
            };
        }
    }

    static int checks = 0;
    static void check(boolean cond, String msg) { checks++; if (!cond) throw new AssertionError("FALLO: " + msg); }

    public static void main(String[] args) {
        FakePrefs prefs = new FakePrefs();
        WordRepository repo = new WordRepository(prefs);

        // Palabras incluidas
        List<String> cats = repo.getCategories();
        check(cats.size() == 14, "14 categorías, hay " + cats.size());
        check(repo.countAllWords() > 300, "más de 300 palabras: " + repo.countAllWords());
        check(repo.getWords("Animales").contains("Perro"), "Perro en Animales");
        for (String c : cats) {
            Set<String> seen = new HashSet<>();
            for (String w : repo.getWords(c)) check(seen.add(WordRepository.normalize(w)), "duplicado '" + w + "' en " + c);
        }

        // Agregar palabras
        check(repo.addWord("Animales", "  ornitorrinco "), "agregar ornitorrinco");
        check(repo.getWords("Animales").contains("Ornitorrinco"), "capitalizada");
        check(!repo.addWord("Animales", "ORNITORRINCO"), "duplicado ignorado (mayúsculas)");
        check(!repo.addWord("Animales", "perro"), "duplicado ignorado (original)");
        check(!repo.addWord("Animales", "   "), "vacía ignorada");
        check(repo.addWords("Comida", "Ajiaco, bandeja paisa\nsancocho, pizza") == 3, "3 de 4 agregadas");
        check(repo.countCustomWords() == 4, "4 personalizadas: " + repo.countCustomWords());
        check(!repo.isBuiltInWord("Comida", "Ajiaco") && repo.isBuiltInWord("Comida", "Pizza"), "isBuiltInWord");

        // Persistencia: nueva instancia sobre las mismas prefs
        WordRepository repo2 = new WordRepository(prefs);
        check(repo2.getWords("Comida").contains("Ajiaco"), "persistido");

        // Ocultar original / eliminar personalizada / restaurar
        int before = repo2.getWords("Animales").size();
        repo2.removeWord("Animales", "Perro");
        check(!repo2.getWords("Animales").contains("Perro"), "Perro oculto");
        check(repo2.countHidden("Animales") == 1, "1 oculta");
        repo2.removeWord("Animales", "Ornitorrinco");
        check(repo2.getWords("Animales").size() == before - 2, "dos menos");
        check(repo2.addWord("Animales", "perro"), "volver a agregar una oculta la restaura");
        check(repo2.countHidden("Animales") == 0 && repo2.countCustomWords() == 3, "restaurada sin duplicar");
        repo2.removeWord("Animales", "Gato");
        repo2.restoreBuiltIn("Animales");
        check(repo2.getWords("Animales").contains("Gato"), "restaurar originales");

        // Categorías personalizadas
        check(repo2.addCategory("Chistes internos"), "crear categoría");
        check(!repo2.addCategory("chistes internos"), "categoría duplicada");
        check(!repo2.addCategory("Animales"), "categoría existente");
        check(repo2.getCategories().size() == 15, "15 categorías");
        check(!repo2.isBuiltInCategory("Chistes internos"), "no es original");
        check(repo2.getEnabledCategories().size() == 14, "categoría vacía no cuenta como activa");
        repo2.addWord("Chistes internos", "La vez del asado");
        check(repo2.getEnabledCategories().size() == 15, "con palabra ya está activa");
        repo2.setCategoryEnabled("Chistes internos", false);
        check(!repo2.isCategoryEnabled("Chistes internos"), "desactivada");
        check(repo2.getEnabledCategories().size() == 14, "desactivada no cuenta");
        repo2.setCategoryEnabled("Chistes internos", true);
        check(!repo2.removeCategory("Animales"), "no se borran originales");
        check(repo2.removeCategory("Chistes internos"), "borrar personalizada");
        check(repo2.getCategories().size() == 14, "vuelve a 14");

        // normalize
        check(WordRepository.normalize("  Camión   GRANDE ").equals("camion grande"), "normalize");

        // pickRandom
        Random rnd = new Random(7);
        WordPick pick = repo2.pickRandom(Arrays.asList("Deportes"), rnd);
        check(pick != null && pick.category.equals("Deportes") && repo2.getWords("Deportes").contains(pick.word), "pick");
        check(repo2.pickRandom(new ArrayList<String>(), rnd) == null, "sin categorías -> null");

        // GameState
        List<String> players = Arrays.asList("Ana", "Beto", "Caro", "Dani", "Eli");
        GameState g = new GameState(players, 2, true, 0, Arrays.asList("Animales", "Comida"));
        check(g.newRound(repo2), "nueva ronda");
        int imps = 0; for (boolean b : g.isImpostor) if (b) imps++;
        check(imps == 2, "2 impostores");
        check(g.impostorsAlive() == 2 && g.civiliansAlive() == 3, "vivos");
        check(!g.civiliansWin() && !g.impostorsWin(), "sin ganador al inicio");
        check(g.word != null && g.category != null && g.round == 1, "palabra y ronda");
        check(g.impostorNames().size() == 2, "nombres impostores");
        // Eliminar un civil -> 2 vs 2 -> ganan impostores
        for (int i = 0; i < 5; i++) if (!g.isImpostor[i]) { g.alive[i] = false; break; }
        check(g.impostorsWin(), "impostores ganan al igualar");
        // Nueva partida: eliminar ambos impostores -> ganan civiles
        g.newRound(repo2);
        for (int i = 0; i < 5; i++) if (g.isImpostor[i]) g.alive[i] = false;
        check(g.civiliansWin() && !g.impostorsWin(), "civiles ganan");
        g.newRound(repo2);
        g.nextRound();
        check(g.round == 2 && g.alive[g.starterIndex], "ronda 2 empieza un vivo");
        // Reparto aleatorio cubre a todos los jugadores a lo largo de muchas partidas
        boolean[] everImpostor = new boolean[5];
        for (int k = 0; k < 300; k++) { g.newRound(repo2); for (int i = 0; i < 5; i++) if (g.isImpostor[i]) everImpostor[i] = true; }
        for (int i = 0; i < 5; i++) check(everImpostor[i], "jugador " + i + " nunca fue impostor");

        for (int i = 0; i < 5; i++) check(!g.isMrWhite[i], "sin Mr. White en modo Impostor");

        // ---------------------------------------------------------- modo Undercover
        WordRepository repo3 = new WordRepository(new FakePrefs());
        check(WordRepository.countBuiltInPairs() > 200, "más de 200 parejas: " + WordRepository.countBuiltInPairs());
        Set<String> civilSeen = new HashSet<>(), undercoverSeen = new HashSet<>();
        for (int k = 0; k < 2000; k++) {
            WordPick p = repo3.pickPair(repo3.getCategories(), rnd);
            check(p != null && p.word != null && p.undercoverWord != null, "pareja completa");
            check(!WordRepository.normalize(p.word).equals(WordRepository.normalize(p.undercoverWord)),
                    "palabras distintas: " + p.word);
            check(repo3.isBuiltInCategory(p.category), "categoría válida");
            civilSeen.add(p.word);
            undercoverSeen.add(p.undercoverWord);
        }
        check(civilSeen.contains("Perro") || undercoverSeen.contains("Perro"), "Perro/Gato sale");
        // Se sortea qué palabra es la de los civiles: ambas palabras de una pareja aparecen en los dos lados.
        Set<String> both = new HashSet<>(civilSeen);
        both.retainAll(undercoverSeen);
        check(both.size() > 50, "las palabras cambian de lado: " + both.size());
        // Solo categorías elegidas
        for (int k = 0; k < 200; k++) {
            check(repo3.pickPair(Arrays.asList("Ropa"), rnd).category.equals("Ropa"), "solo Ropa");
        }
        // Las parejas con una palabra oculta no se usan
        repo3.removeWord("Animales", "Perro");
        for (int k = 0; k < 1000; k++) {
            WordPick p = repo3.pickPair(Arrays.asList("Animales"), rnd);
            check(!p.word.equals("Perro") && !p.undercoverWord.equals("Perro"), "Perro oculto no sale");
        }
        // Categorías del usuario: parejas al azar con sus palabras (mínimo 2)
        repo3.addCategory("Amigos");
        repo3.addWord("Amigos", "Juan");
        check(repo3.pickPair(Arrays.asList("Amigos"), rnd) == null, "1 palabra -> sin pareja");
        repo3.addWords("Amigos", "Pedro, Luisa");
        Set<String> amigos = new HashSet<>(Arrays.asList("Juan", "Pedro", "Luisa"));
        for (int k = 0; k < 300; k++) {
            WordPick p = repo3.pickPair(Arrays.asList("Amigos"), rnd);
            check(amigos.contains(p.word) && amigos.contains(p.undercoverWord) && !p.word.equals(p.undercoverWord),
                    "pareja de categoría propia");
        }

        List<String> six = Arrays.asList("Ana", "Beto", "Caro", "Dani", "Eli", "Fede");
        GameState u = new GameState(six, GameState.MODE_UNDERCOVER, 1, 1, true, 0, Arrays.asList("Animales", "Comida"));
        check(u.isUndercoverMode(), "modo undercover");
        check(u.newRound(repo3), "nueva partida undercover");
        int und = 0, mw = 0, civ = 0;
        for (int i = 0; i < 6; i++) {
            if (u.isMrWhite[i]) { mw++; check(u.isImpostor[i] && u.wordFor(i) == null, "Mr. White sin palabra"); }
            else if (u.isUndercover(i)) { und++; check(u.undercoverWord.equals(u.wordFor(i)), "undercover ve su palabra"); }
            else { civ++; check(u.word.equals(u.wordFor(i)), "civil ve la palabra"); }
        }
        check(und == 1 && mw == 1 && civ == 4, "reparto 4/1/1");
        check(u.impostorsAlive() == 2 && u.civiliansAlive() == 4, "vivos undercover");
        check(u.impostorNames().size() == 1 && u.mrWhiteNames().size() == 1, "nombres por rol");
        check(!u.civiliansWin() && !u.impostorsWin(), "sin ganador al inicio (undercover)");
        // Eliminar civiles: con 2 civiles y 2 infiltrados la partida sigue; con 1 civil ganan los infiltrados.
        int killed = 0;
        for (int i = 0; i < 6 && killed < 2; i++) if (!u.isImpostor[i]) { u.alive[i] = false; killed++; }
        check(!u.impostorsWin() && !u.civiliansWin(), "2 civiles vs 2 infiltrados: sigue");
        for (int i = 0; i < 6; i++) if (!u.isImpostor[i] && u.alive[i]) { u.alive[i] = false; break; }
        check(u.impostorsWin(), "un solo civil: ganan los infiltrados");
        // Eliminar a los dos infiltrados: ganan los civiles.
        u.newRound(repo3);
        for (int i = 0; i < 6; i++) if (u.isImpostor[i]) u.alive[i] = false;
        check(u.civiliansWin() && !u.impostorsWin(), "civiles ganan (undercover)");
        // Mr. White nunca empieza la ronda.
        for (int k = 0; k < 300; k++) {
            u.newRound(repo3);
            check(!u.isMrWhite[u.starterIndex], "Mr. White no empieza");
            u.nextRound();
            check(!u.isMrWhite[u.starterIndex] && u.alive[u.starterIndex], "Mr. White no empieza ronda 2");
        }
        // Sin categorías con parejas -> no se puede empezar
        GameState empty = new GameState(six, GameState.MODE_UNDERCOVER, 1, 0, true, 0, new ArrayList<String>());
        check(!empty.newRound(repo3), "sin parejas -> false");
        // Sin Mr. White: todos los infiltrados son undercover
        GameState u2 = new GameState(six, GameState.MODE_UNDERCOVER, 2, 0, false, 0, Arrays.asList("Deportes"));
        u2.newRound(repo3);
        int u2c = 0; for (int i = 0; i < 6; i++) { check(!u2.isMrWhite[i], "sin Mr. White"); if (u2.isUndercover(i)) u2c++; }
        check(u2c == 2, "2 undercovers");

        System.out.println("OK: " + checks + " comprobaciones pasaron");
    }
}
