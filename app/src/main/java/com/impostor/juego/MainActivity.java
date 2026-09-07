package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SetupActivity.class));
            }
        });
        findViewById(R.id.btnWords).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WordsActivity.class));
            }
        });
        findViewById(R.id.btnHowTo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToPlay();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WordRepository repo = new WordRepository(this);
        TextView count = findViewById(R.id.txtWordCount);
        count.setText(repo.countAllWords() + " palabras en " + repo.getCategories().size()
                + " categorías · " + repo.countCustomWords() + " agregadas por ti");
    }

    private void showHowToPlay() {
        String rules = "1. Se reúnen 3 o más jugadores alrededor de un solo teléfono.\n\n"
                + "2. La app elige una palabra secreta y la muestra a todos… excepto al impostor, "
                + "que solo sabe que es el impostor (y, si lo activas, la categoría como pista).\n\n"
                + "3. Cada jugador ve su rol en privado y pasa el teléfono al siguiente.\n\n"
                + "4. Por turnos, cada uno dice UNA palabra o frase corta relacionada con la palabra secreta. "
                + "Sé sutil: si eres demasiado obvio, el impostor adivinará la palabra; "
                + "si eres demasiado vago, sospecharán de ti.\n\n"
                + "5. Al terminar la ronda, todos discuten y votan para eliminar a un sospechoso.\n\n"
                + "6. Si eliminan al impostor, este tiene una última oportunidad: si adivina la palabra, gana.\n\n"
                + "7. Ganan los jugadores si descubren a todos los impostores. "
                + "Ganan los impostores si llegan a ser tantos como el resto de jugadores.";
        new AlertDialog.Builder(this)
                .setTitle("Cómo jugar")
                .setMessage(rules)
                .setPositiveButton("Entendido", null)
                .show();
    }
}
