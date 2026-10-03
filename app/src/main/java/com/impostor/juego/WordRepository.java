package com.impostor.juego;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Fuente de palabras del juego. Combina las categorías incluidas en la app con las
 * palabras y categorías que el usuario agrega, guardadas en SharedPreferences como JSON.
 */
public class WordRepository {

    private static final String PREFS = "impostor_words";
    private static final String KEY_CUSTOM = "custom_words";          // {categoria: [palabras]}
    private static final String KEY_HIDDEN = "hidden_words";          // {categoria: [palabras originales ocultas]}
    private static final String KEY_DISABLED = "disabled_categories"; // [categorias desactivadas]

    private static final LinkedHashMap<String, String[]> BUILT_IN = new LinkedHashMap<>();

    static {
        BUILT_IN.put("Animales", new String[]{
                "Perro", "Gato", "Elefante", "Jirafa", "León", "Tigre", "Delfín", "Tiburón", "Águila",
                "Pingüino", "Canguro", "Mono", "Serpiente", "Tortuga", "Caballo", "Vaca", "Cerdo", "Oveja",
                "Lobo", "Zorro", "Oso", "Murciélago", "Cocodrilo", "Rana", "Abeja", "Pulpo", "Ballena", "Búho"});
        BUILT_IN.put("Comida", new String[]{
                "Pizza", "Hamburguesa", "Sushi", "Tacos", "Paella", "Empanada", "Arepa", "Ceviche", "Lasaña",
                "Ensalada", "Helado", "Chocolate", "Pan", "Queso", "Arroz", "Sopa", "Pollo asado", "Huevo frito",
                "Tortilla", "Churros", "Galletas", "Pastel", "Café", "Cereal", "Palomitas", "Sandía", "Aguacate", "Papas fritas"});
        BUILT_IN.put("Lugares", new String[]{
                "Playa", "Hospital", "Escuela", "Aeropuerto", "Cine", "Biblioteca", "Supermercado", "Gimnasio",
                "Museo", "Estadio", "Restaurante", "Parque", "Iglesia", "Banco", "Cárcel", "Hotel", "Zoológico",
                "Montaña", "Desierto", "Selva", "Oficina", "Peluquería", "Farmacia", "Estación de tren", "Circo",
                "Discoteca", "Piscina", "Casino"});
        BUILT_IN.put("Profesiones", new String[]{
                "Médico", "Bombero", "Policía", "Profesor", "Chef", "Piloto", "Abogado", "Ingeniero", "Actor",
                "Cantante", "Futbolista", "Astronauta", "Carpintero", "Mecánico", "Enfermero", "Dentista",
                "Arquitecto", "Periodista", "Fotógrafo", "Agricultor", "Taxista", "Científico", "Payaso", "Mago",
                "Panadero", "Veterinario", "Youtuber", "Pintor"});
        BUILT_IN.put("Deportes", new String[]{
                "Fútbol", "Baloncesto", "Tenis", "Natación", "Ciclismo", "Boxeo", "Golf", "Béisbol", "Voleibol",
                "Atletismo", "Ajedrez", "Surf", "Esquí", "Patinaje", "Karate", "Yoga", "Rugby", "Hockey",
                "Escalada", "Gimnasia", "Fórmula 1", "Ping pong", "Bolos", "Esgrima"});
        BUILT_IN.put("Objetos", new String[]{
                "Teléfono", "Paraguas", "Reloj", "Lámpara", "Espejo", "Cuchara", "Tijeras", "Almohada", "Mochila",
                "Llave", "Martillo", "Cámara", "Libro", "Televisor", "Silla", "Ventilador", "Cepillo de dientes",
                "Gafas", "Botella", "Maleta", "Escalera", "Vela", "Globo", "Peluche", "Lápiz", "Moneda", "Anillo"});
        BUILT_IN.put("Transporte", new String[]{
                "Avión", "Tren", "Bicicleta", "Barco", "Autobús", "Motocicleta", "Helicóptero", "Submarino",
                "Cohete", "Patineta", "Taxi", "Camión", "Tranvía", "Globo aerostático", "Canoa", "Ambulancia",
                "Tractor", "Metro", "Velero", "Crucero", "Teleférico", "Patines"});
        BUILT_IN.put("Países y ciudades", new String[]{
                "España", "México", "Colombia", "Argentina", "Brasil", "Japón", "Egipto", "Italia", "Francia",
                "Estados Unidos", "China", "Australia", "Canadá", "Alemania", "India", "París", "Nueva York",
                "Tokio", "Roma", "Londres", "Buenos Aires", "Bogotá", "Ciudad de México", "Madrid", "Río de Janeiro",
                "Lima", "Santiago", "Las Vegas"});
        BUILT_IN.put("Personajes", new String[]{
                "Superman", "Batman", "Spider-Man", "Harry Potter", "Mario Bros", "Pikachu", "Shrek", "Bob Esponja",
                "Mickey Mouse", "Homero Simpson", "Darth Vader", "Elsa de Frozen", "Goku", "Sherlock Holmes",
                "Drácula", "Papá Noel", "Peter Pan", "Cenicienta", "Iron Man", "Barbie", "Don Quijote", "Mafalda",
                "El Chavo", "Hulk"});
        BUILT_IN.put("En casa", new String[]{
                "Cocina", "Baño", "Sofá", "Cama", "Nevera", "Lavadora", "Microondas", "Ducha", "Escoba", "Plancha",
                "Horno", "Cortina", "Alfombra", "Armario", "Inodoro", "Balcón", "Garaje", "Jardín", "Chimenea",
                "Mesa", "Licuadora", "Aspiradora", "Tostadora"});
        BUILT_IN.put("Naturaleza", new String[]{
                "Sol", "Luna", "Lluvia", "Nieve", "Volcán", "Río", "Océano", "Arcoíris", "Tormenta", "Terremoto",
                "Bosque", "Cascada", "Isla", "Cueva", "Estrella", "Relámpago", "Viento", "Niebla", "Huracán",
                "Glaciar", "Cactus", "Girasol", "Lago"});
        BUILT_IN.put("Ropa", new String[]{
                "Camiseta", "Pantalón", "Vestido", "Zapatos", "Sombrero", "Bufanda", "Guantes", "Chaqueta",
                "Calcetines", "Corbata", "Pijama", "Bikini", "Botas", "Gorra", "Cinturón", "Falda", "Abrigo",
                "Sandalias", "Uniforme", "Disfraz", "Traje de baño", "Sudadera"});
        BUILT_IN.put("Música y arte", new String[]{
                "Guitarra", "Piano", "Violín", "Batería", "Trompeta", "Flauta", "Micrófono", "Ópera", "Reguetón",
                "Rock", "Salsa", "Ballet", "Pintura", "Escultura", "Teatro", "Grafiti", "Tambor", "Coro", "Karaoke",
                "Orquesta", "Cumbia", "Tango", "Rap"});
        BUILT_IN.put("Tecnología", new String[]{
                "Computadora", "Internet", "Wifi", "Robot", "Videojuego", "Dron", "Auriculares", "Teclado",
                "Cargador", "Impresora", "Satélite", "Inteligencia artificial", "TikTok", "YouTube", "WhatsApp",
                "Instagram", "Netflix", "Google", "Emoji", "Selfie", "Bluetooth", "Pantalla táctil", "Contraseña"});
    }


    /**
     * Parejas de palabras parecidas para el modo Undercover: los civiles reciben una y el
     * undercover la otra (se sortea cuál). Cada pareja escrita como "Palabra A/Palabra B".
     */
    private static final LinkedHashMap<String, String[]> BUILT_IN_PAIRS = new LinkedHashMap<>();

    static {
        BUILT_IN_PAIRS.put("Animales", new String[]{
                "Perro/Gato", "León/Tigre", "Delfín/Ballena", "Águila/Halcón", "Búho/Murciélago",
                "Lobo/Zorro", "Caballo/Burro", "Oveja/Cabra", "Rana/Sapo", "Abeja/Avispa", "Pingüino/Foca",
                "Mono/Gorila", "Cocodrilo/Lagarto", "Vaca/Toro", "Mariposa/Polilla", "Ratón/Hámster",
                "Tortuga/Caracol", "Pulpo/Calamar"});
        BUILT_IN_PAIRS.put("Comida", new String[]{
                "Hamburguesa/Perro caliente", "Sushi/Ceviche", "Tacos/Burritos", "Empanada/Arepa",
                "Helado/Paleta", "Chocolate/Caramelo", "Pan/Tostada", "Queso/Mantequilla", "Arroz/Pasta",
                "Café/Té", "Churros/Buñuelos", "Pastel/Flan", "Palomitas/Papas fritas", "Sandía/Melón",
                "Limón/Naranja", "Ketchup/Mostaza", "Pizza/Lasaña", "Pollo asado/Pavo"});
        BUILT_IN_PAIRS.put("Lugares", new String[]{
                "Playa/Piscina", "Hospital/Farmacia", "Escuela/Universidad", "Aeropuerto/Estación de tren",
                "Cine/Teatro", "Biblioteca/Librería", "Supermercado/Tienda", "Museo/Galería de arte",
                "Restaurante/Cafetería", "Parque/Plaza", "Iglesia/Catedral", "Hotel/Hostal",
                "Zoológico/Acuario", "Selva/Bosque", "Discoteca/Bar", "Casino/Bingo", "Cárcel/Comisaría",
                "Gimnasio/Spa"});
        BUILT_IN_PAIRS.put("Profesiones", new String[]{
                "Médico/Enfermero", "Bombero/Policía", "Chef/Panadero", "Piloto/Azafata", "Abogado/Juez",
                "Ingeniero/Arquitecto", "Actor/Cantante", "Futbolista/Árbitro", "Astronauta/Científico",
                "Carpintero/Albañil", "Mecánico/Electricista", "Dentista/Médico", "Periodista/Fotógrafo",
                "Agricultor/Jardinero", "Taxista/Conductor de autobús", "Payaso/Mago", "Youtuber/Streamer",
                "Pintor/Escultor", "Profesor/Director de escuela"});
        BUILT_IN_PAIRS.put("Deportes", new String[]{
                "Fútbol/Rugby", "Baloncesto/Voleibol", "Tenis/Bádminton", "Natación/Waterpolo",
                "Ciclismo/Motociclismo", "Boxeo/Karate", "Golf/Minigolf", "Béisbol/Críquet",
                "Esquí/Snowboard", "Surf/Windsurf", "Ping pong/Tenis", "Bolos/Billar", "Ajedrez/Damas",
                "Yoga/Pilates", "Fórmula 1/Rally", "Escalada/Senderismo", "Hockey/Curling",
                "Esgrima/Kendo"});
        BUILT_IN_PAIRS.put("Objetos", new String[]{
                "Paraguas/Impermeable", "Reloj/Despertador", "Lámpara/Linterna", "Espejo/Ventana",
                "Cuchara/Tenedor", "Tijeras/Cuchillo", "Almohada/Cojín", "Mochila/Maleta", "Llave/Candado",
                "Martillo/Destornillador", "Lápiz/Bolígrafo", "Libro/Revista", "Silla/Taburete",
                "Vela/Fósforo", "Globo/Burbuja", "Peluche/Muñeca", "Moneda/Billete", "Anillo/Collar",
                "Gafas/Lentes de contacto", "Botella/Vaso", "Teléfono/Tablet"});
        BUILT_IN_PAIRS.put("Transporte", new String[]{
                "Avión/Helicóptero", "Tren/Metro", "Bicicleta/Motocicleta", "Barco/Lancha",
                "Autobús/Tranvía", "Cohete/Nave espacial", "Patineta/Patines", "Taxi/Uber",
                "Camión/Tractor", "Globo aerostático/Zepelín", "Canoa/Kayak",
                "Ambulancia/Patrulla de policía", "Crucero/Ferry", "Teleférico/Ascensor",
                "Submarino/Barco pirata"});
        BUILT_IN_PAIRS.put("Países y ciudades", new String[]{
                "España/Portugal", "Argentina/Uruguay", "Japón/China", "Egipto/Marruecos", "Italia/Grecia",
                "Estados Unidos/Canadá", "Australia/Nueva Zelanda", "Alemania/Austria", "Brasil/Colombia",
                "México/Perú", "París/Londres", "Nueva York/Los Ángeles", "Tokio/Seúl", "Roma/Atenas",
                "Buenos Aires/Montevideo", "Bogotá/Medellín", "Madrid/Barcelona",
                "Río de Janeiro/São Paulo", "Lima/Cusco", "Las Vegas/Mónaco"});
        BUILT_IN_PAIRS.put("Personajes", new String[]{
                "Superman/Capitán América", "Batman/Iron Man", "Spider-Man/Ant-Man", "Harry Potter/Merlín",
                "Mario Bros/Luigi", "Pikachu/Sonic", "Shrek/Hulk", "Bob Esponja/Patricio",
                "Mickey Mouse/Bugs Bunny", "Homero Simpson/Pedro Picapiedra", "Darth Vader/Voldemort",
                "Elsa de Frozen/Cenicienta", "Goku/Naruto", "Sherlock Holmes/Detective Conan",
                "Drácula/Frankenstein", "Papá Noel/Reyes Magos", "Peter Pan/Pinocho", "Barbie/Ken"});
        BUILT_IN_PAIRS.put("En casa", new String[]{
                "Cocina/Comedor", "Ducha/Bañera", "Sofá/Sillón", "Cama/Cuna", "Nevera/Congelador",
                "Lavadora/Secadora", "Microondas/Horno", "Escoba/Trapeador", "Cortina/Persiana",
                "Armario/Cajonera", "Inodoro/Lavamanos", "Balcón/Terraza", "Garaje/Sótano", "Jardín/Patio",
                "Chimenea/Estufa", "Mesa/Escritorio", "Licuadora/Batidora", "Aspiradora/Escoba",
                "Tostadora/Sandwichera", "Plancha/Secador de pelo"});
        BUILT_IN_PAIRS.put("Naturaleza", new String[]{
                "Sol/Luna", "Lluvia/Granizo", "Nieve/Hielo", "Volcán/Montaña", "Río/Lago", "Océano/Mar",
                "Arcoíris/Aurora boreal", "Tormenta/Huracán", "Terremoto/Tsunami", "Isla/Península",
                "Cueva/Túnel", "Estrella/Planeta", "Relámpago/Trueno", "Niebla/Nube", "Girasol/Margarita",
                "Rosa/Tulipán", "Glaciar/Iceberg", "Cascada/Fuente", "Cactus/Palmera"});
        BUILT_IN_PAIRS.put("Ropa", new String[]{
                "Camiseta/Camisa", "Pantalón/Short", "Vestido/Falda", "Zapatos/Zapatillas",
                "Sombrero/Gorra", "Bufanda/Corbata", "Guantes/Calcetines", "Chaqueta/Abrigo",
                "Pijama/Bata", "Bikini/Traje de baño", "Botas/Sandalias", "Cinturón/Tirantes",
                "Uniforme/Disfraz", "Sudadera/Suéter"});
        BUILT_IN_PAIRS.put("Música y arte", new String[]{
                "Guitarra/Bajo", "Piano/Órgano", "Violín/Violonchelo", "Batería/Tambor",
                "Trompeta/Saxofón", "Flauta/Clarinete", "Micrófono/Altavoz", "Ópera/Musical",
                "Reguetón/Trap", "Rock/Metal", "Salsa/Bachata", "Pintura/Dibujo", "Escultura/Cerámica",
                "Teatro/Cine", "Grafiti/Mural", "Coro/Orquesta", "Karaoke/Concierto", "Cumbia/Vallenato",
                "Rap/Reggae", "Ballet/Tango"});
        BUILT_IN_PAIRS.put("Tecnología", new String[]{
                "Computadora/Tablet", "Internet/Wifi", "Robot/Dron", "Auriculares/Parlante",
                "Teclado/Ratón", "Impresora/Escáner", "Satélite/Antena", "TikTok/Instagram",
                "YouTube/Netflix", "WhatsApp/Telegram", "Google/Wikipedia", "Emoji/Sticker", "Selfie/Foto",
                "Contraseña/Huella digital", "Videojuego/Consola", "Bluetooth/Wifi", "Cargador/Batería",
                "Facebook/Twitter"});
    }

    private final SharedPreferences prefs;
    private JSONObject custom;
    private JSONObject hidden;
    private JSONArray disabled;

    public WordRepository(Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    public WordRepository(SharedPreferences prefs) {
        this.prefs = prefs;
        custom = loadObject(KEY_CUSTOM);
        hidden = loadObject(KEY_HIDDEN);
        try {
            disabled = new JSONArray(prefs.getString(KEY_DISABLED, "[]"));
        } catch (JSONException e) {
            disabled = new JSONArray();
        }
    }

    private JSONObject loadObject(String key) {
        try {
            return new JSONObject(prefs.getString(key, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private void save() {
        prefs.edit()
                .putString(KEY_CUSTOM, custom.toString())
                .putString(KEY_HIDDEN, hidden.toString())
                .putString(KEY_DISABLED, disabled.toString())
                .apply();
    }

    // ---------------------------------------------------------------- categorías

    /** Todas las categorías: primero las originales, luego las creadas por el usuario. */
    public List<String> getCategories() {
        LinkedHashSet<String> result = new LinkedHashSet<>(BUILT_IN.keySet());
        Iterator<String> it = custom.keys();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return new ArrayList<>(result);
    }

    public boolean isBuiltInCategory(String category) {
        return BUILT_IN.containsKey(category);
    }

    public boolean isCategoryEnabled(String category) {
        return indexInArray(disabled, category) < 0;
    }

    public void setCategoryEnabled(String category, boolean enabled) {
        int idx = indexInArray(disabled, category);
        if (enabled && idx >= 0) {
            disabled.remove(idx);
        } else if (!enabled && idx < 0) {
            disabled.put(category);
        }
        save();
    }

    public List<String> getEnabledCategories() {
        List<String> result = new ArrayList<>();
        for (String c : getCategories()) {
            if (isCategoryEnabled(c) && !getWords(c).isEmpty()) {
                result.add(c);
            }
        }
        return result;
    }

    /** Crea una categoría nueva. Devuelve false si ya existe. */
    public boolean addCategory(String name) {
        String clean = name.trim();
        if (clean.isEmpty()) return false;
        for (String existing : getCategories()) {
            if (normalize(existing).equals(normalize(clean))) return false;
        }
        try {
            custom.put(clean, new JSONArray());
        } catch (JSONException ignored) {
        }
        save();
        return true;
    }

    /** Elimina una categoría creada por el usuario (las originales no se pueden borrar). */
    public boolean removeCategory(String category) {
        if (isBuiltInCategory(category) || !custom.has(category)) return false;
        custom.remove(category);
        int idx = indexInArray(disabled, category);
        if (idx >= 0) disabled.remove(idx);
        save();
        return true;
    }

    // ---------------------------------------------------------------- palabras

    /** Palabras visibles de una categoría: originales (no ocultas) + personalizadas. */
    public List<String> getWords(String category) {
        List<String> result = new ArrayList<>();
        String[] builtIn = BUILT_IN.get(category);
        JSONArray hiddenArr = hidden.optJSONArray(category);
        if (builtIn != null) {
            for (String w : builtIn) {
                if (hiddenArr == null || indexInArray(hiddenArr, w) < 0) {
                    result.add(w);
                }
            }
        }
        JSONArray customArr = custom.optJSONArray(category);
        if (customArr != null) {
            for (int i = 0; i < customArr.length(); i++) {
                result.add(customArr.optString(i));
            }
        }
        return result;
    }

    public boolean isBuiltInWord(String category, String word) {
        String[] builtIn = BUILT_IN.get(category);
        if (builtIn == null) return false;
        for (String w : builtIn) {
            if (w.equals(word)) return true;
        }
        return false;
    }

    /** Agrega una palabra. Devuelve false si está vacía o ya existe en la categoría. */
    public boolean addWord(String category, String word) {
        String clean = word.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) return false;
        clean = clean.substring(0, 1).toUpperCase(Locale.getDefault()) + clean.substring(1);
        String key = normalize(clean);
        for (String existing : getWords(category)) {
            if (normalize(existing).equals(key)) return false;
        }
        // Si era una palabra original oculta, simplemente se vuelve a mostrar.
        JSONArray hiddenArr = hidden.optJSONArray(category);
        if (hiddenArr != null) {
            for (int i = 0; i < hiddenArr.length(); i++) {
                if (normalize(hiddenArr.optString(i)).equals(key)) {
                    hiddenArr.remove(i);
                    save();
                    return true;
                }
            }
        }
        JSONArray customArr = custom.optJSONArray(category);
        if (customArr == null) {
            customArr = new JSONArray();
            try {
                custom.put(category, customArr);
            } catch (JSONException ignored) {
            }
        }
        customArr.put(clean);
        save();
        return true;
    }

    /** Agrega varias palabras separadas por comas o saltos de línea. Devuelve cuántas se agregaron. */
    public int addWords(String category, String text) {
        int added = 0;
        for (String part : text.split("[,;\\n]")) {
            if (addWord(category, part)) added++;
        }
        return added;
    }

    /** Quita una palabra: las personalizadas se borran, las originales se ocultan. */
    public void removeWord(String category, String word) {
        JSONArray customArr = custom.optJSONArray(category);
        if (customArr != null) {
            int idx = indexInArray(customArr, word);
            if (idx >= 0) {
                customArr.remove(idx);
                save();
                return;
            }
        }
        if (isBuiltInWord(category, word)) {
            JSONArray hiddenArr = hidden.optJSONArray(category);
            if (hiddenArr == null) {
                hiddenArr = new JSONArray();
                try {
                    hidden.put(category, hiddenArr);
                } catch (JSONException ignored) {
                }
            }
            if (indexInArray(hiddenArr, word) < 0) hiddenArr.put(word);
            save();
        }
    }

    /** Vuelve a mostrar todas las palabras originales de la categoría. */
    public void restoreBuiltIn(String category) {
        hidden.remove(category);
        save();
    }

    public int countHidden(String category) {
        JSONArray arr = hidden.optJSONArray(category);
        return arr == null ? 0 : arr.length();
    }

    public int countCustomWords() {
        int total = 0;
        Iterator<String> it = custom.keys();
        while (it.hasNext()) {
            JSONArray arr = custom.optJSONArray(it.next());
            if (arr != null) total += arr.length();
        }
        return total;
    }

    public int countAllWords() {
        int total = 0;
        for (String c : getCategories()) total += getWords(c).size();
        return total;
    }

    /** Elige una palabra al azar entre las categorías dadas. Devuelve (categoría, palabra) o null. */
    public WordPick pickRandom(List<String> categories, Random random) {
        List<WordPick> pool = new ArrayList<>();
        for (String c : categories) {
            for (String w : getWords(c)) pool.add(new WordPick(c, w));
        }
        if (pool.isEmpty()) return null;
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Elige una pareja de palabras parecidas para el modo Undercover entre las categorías dadas.
     * Las categorías originales usan sus parejas incluidas (sin las que tengan una palabra oculta);
     * las categorías creadas por el usuario forman parejas con dos de sus palabras al azar.
     * Devuelve null si no hay ninguna pareja posible.
     */
    public WordPick pickPair(List<String> categories, Random random) {
        List<WordPick> pool = new ArrayList<>();
        for (String c : categories) {
            String[] pairs = BUILT_IN_PAIRS.get(c);
            if (pairs != null) {
                JSONArray hiddenArr = hidden.optJSONArray(c);
                for (String pair : pairs) {
                    String[] parts = pair.split("/");
                    if (isHidden(hiddenArr, parts[0]) || isHidden(hiddenArr, parts[1])) continue;
                    pool.add(new WordPick(c, parts[0], parts[1]));
                }
            } else if (!isBuiltInCategory(c)) {
                List<String> words = getWords(c);
                if (words.size() < 2) continue;
                for (int i = 0; i < words.size(); i++) {
                    int other = random.nextInt(words.size() - 1);
                    if (other >= i) other++;
                    pool.add(new WordPick(c, words.get(i), words.get(other)));
                }
            }
        }
        if (pool.isEmpty()) return null;
        WordPick pick = pool.get(random.nextInt(pool.size()));
        // Se sortea cuál de las dos palabras es la de los civiles.
        return random.nextBoolean() ? pick : new WordPick(pick.category, pick.undercoverWord, pick.word);
    }

    /** Número de parejas incluidas para el modo Undercover. */
    public static int countBuiltInPairs() {
        int total = 0;
        for (String[] pairs : BUILT_IN_PAIRS.values()) total += pairs.length;
        return total;
    }

    private static boolean isHidden(JSONArray hiddenArr, String word) {
        return hiddenArr != null && indexInArray(hiddenArr, word) >= 0;
    }

    // ---------------------------------------------------------------- utilidades

    /** Minúsculas, sin tildes ni espacios sobrantes: sirve para comparar palabras. */
    public static String normalize(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.getDefault()), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
    }

    private static int indexInArray(JSONArray arr, String value) {
        for (int i = 0; i < arr.length(); i++) {
            if (value.equals(arr.optString(i))) return i;
        }
        return -1;
    }
}
