# Impostor 🕵️

Juego de fiesta para Android: todos los jugadores reciben la misma palabra secreta… menos el
impostor, que tiene que fingir que la conoce. Incluye también el modo **Undercover**. Se juega con
un solo teléfono que se va pasando.

**APK listo para instalar:** [`release/impostor-v1.1.apk`](release/impostor-v1.1.apk)
(Android 5.0 o superior, ~100 KB, sin permisos ni internet).

## Cómo se juega

1. Configura la partida: modo de juego (Impostor o Undercover), número de jugadores (3 a 20),
   sus nombres, cuántos impostores hay,
   qué categorías de palabras usar, si el impostor recibe la categoría como pista y un
   temporizador opcional por ronda.
2. Cada jugador toma el teléfono, pulsa **Ver mi rol**, lo lee en privado y pulsa
   **Ocultar y pasar**.
3. Por turnos (la app indica quién empieza) cada uno dice una pista relacionada con la palabra.
4. Al final de la ronda se vota y se elimina a un sospechoso:
   - Si era el impostor, tiene una última oportunidad de adivinar la palabra. Si acierta, ganan
     los impostores.
   - Si no lo era, la partida sigue con otra ronda de pistas.
5. Ganan los jugadores cuando descubren a todos los impostores. Ganan los impostores cuando
   llegan a ser tantos como el resto.

## Modo Undercover

Se elige en **Configurar partida > Modo de juego**. Nadie sabe con certeza de qué equipo es:

- **Civiles:** reciben una palabra (por ejemplo, *Perro*).
- **Undercover:** recibe una palabra parecida (por ejemplo, *Gato*) y ve exactamente la misma
  pantalla que los civiles, así que no sabe que es undercover hasta que lo deduce por las pistas.
- **Mr. White** (opcional, 0 o más): no recibe palabra, sabe que es Mr. White y nunca empieza
  la ronda. Puede recibir la categoría como pista.

Al eliminar a alguien se revela su rol, pero no su palabra. Si eliminan a Mr. White, tiene una
última oportunidad de adivinar la palabra de los civiles: si acierta, gana él. Ganan los civiles
al eliminar a todos los undercovers y Mr. White; ganan los infiltrados si sobreviven hasta que
solo queda un civil.

La app incluye más de 250 parejas de palabras parecidas repartidas en las 14 categorías (se sortea
cuál es la de los civiles). Respeta las categorías elegidas y no usa parejas con palabras que
hayas ocultado. En las categorías creadas por ti se forman parejas al azar con sus palabras (hace
falta que tengan al menos dos).

## Agregar tus propias palabras

En el menú principal entra en **Palabras**:

- Elige una categoría y escribe la palabra nueva (puedes escribir varias separadas por comas).
- Crea categorías nuevas con **+ Categoría** (por ejemplo, chistes internos de tu grupo).
- Oculta palabras originales que no te gusten o elimina las tuyas; las originales se pueden
  restaurar en cualquier momento.
- Activa o desactiva cada categoría para el juego.

Todo se guarda en el teléfono; la app incluye más de 340 palabras en 14 categorías en español.

## Estructura del proyecto

```
app/src/main/AndroidManifest.xml
app/src/main/java/com/impostor/juego/
    MainActivity.java     Menú principal y reglas
    SetupActivity.java    Configuración de la partida
    RevealActivity.java   Pantalla "pasa el teléfono" para ver el rol
    GameActivity.java     Rondas, temporizador, votación y resultado
    WordsActivity.java    Gestión de palabras y categorías
    WordRepository.java   Palabras y parejas Undercover incluidas + palabras del usuario
    GameState.java        Estado de la partida (modos Impostor y Undercover)
app/src/main/res/         Layouts, estilos, colores e iconos
scripts/                  Build sin Android Studio (ver abajo)
keystore/impostor.jks     Clave de firma (alias `impostor`, contraseña `impostor123`)
release/                  APK firmado
```

La app usa solo el framework de Android (sin AndroidX ni otras dependencias), por eso el APK
es tan pequeño. Es un proyecto Gradle estándar y también se puede abrir en Android Studio.

## Compilar

### Con Android Studio / Gradle

Abre la carpeta del proyecto en Android Studio y ejecuta `Build > Build APK(s)`, o desde la
terminal (con el SDK de Android instalado):

```
gradle assembleRelease
```

### Sin el SDK de Android (`scripts/build-apk.sh`)

El APK publicado se generó con este script, que solo necesita un JDK, Python 3 y cuatro
herramientas sueltas en la carpeta `tools/`:

| Archivo             | Origen                                                                    |
|---------------------|---------------------------------------------------------------------------|
| `aapt2`             | Binario incluido en Apktool (`brut.apktool/apktool-lib/src/main/resources/prebuilt/linux/aapt2`) |
| `android-34.jar`    | `android.jar` de la plataforma 34 (por ejemplo del repo `Sable/android-platforms`) |
| `r8lib.jar`         | `https://storage.googleapis.com/r8-releases/raw/8.3.37/r8lib.jar`          |
| `apksig-2.3.0.jar`  | Maven Central, `com.android.tools.build:apksig:2.3.0`                     |
| `json.jar`          | Solo para `scripts/run-tests.sh`: Maven Central, `org.json:json:20240303`  |

```
scripts/build-apk.sh release/impostor-v1.1.apk
```

Para firmar se usa `keystore/impostor.jks`. El paso de firma v1 de apksig 2.3.0 necesita un JDK
17 o anterior; si tu `java` por defecto es más nuevo, indica otro con `SIGN_JAVA=/ruta/a/java`.

### Pruebas

`scripts/run-tests.sh` compila la lógica del juego contra `android.jar` y ejecuta
`scripts/LogicTest.java` en la JVM (palabras personalizadas, ocultar/restaurar, categorías,
parejas del modo Undercover, reparto de roles y condiciones de victoria de ambos modos).
