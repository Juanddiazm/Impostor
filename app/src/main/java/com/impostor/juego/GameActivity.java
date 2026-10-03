package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rondas de pistas, temporizador, votación y resultado final. */
public class GameActivity extends Activity {

    private GameState game;

    private View gamePanel, endPanel, timerCard;
    private TextView txtRound, txtStarter, txtTimer;
    private TextView txtEndTitle, txtEndWordLabel, txtEndWord, txtEndImpostors, txtEndDetail;
    private Button btnTimerToggle;
    private LinearLayout playersContainer;

    private CountDownTimer timer;
    private long remainingMillis;
    private boolean timerRunning = false;
    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = GameState.current;
        if (game == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_game);

        gamePanel = findViewById(R.id.gamePanel);
        endPanel = findViewById(R.id.endPanel);
        timerCard = findViewById(R.id.timerCard);
        txtRound = findViewById(R.id.txtRound);
        txtStarter = findViewById(R.id.txtStarter);
        txtTimer = findViewById(R.id.txtTimer);
        btnTimerToggle = findViewById(R.id.btnTimerToggle);
        playersContainer = findViewById(R.id.playersContainer);
        txtEndTitle = findViewById(R.id.txtEndTitle);
        txtEndWordLabel = findViewById(R.id.txtEndWordLabel);
        txtEndWord = findViewById(R.id.txtEndWord);
        txtEndImpostors = findViewById(R.id.txtEndImpostors);
        txtEndDetail = findViewById(R.id.txtEndDetail);

        btnTimerToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerRunning) pauseTimer(); else startTimer();
            }
        });
        findViewById(R.id.btnTimerReset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();
            }
        });
        findViewById(R.id.btnVote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVoteDialog();
            }
        });
        findViewById(R.id.btnEnd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmEnd();
            }
        });
        findViewById(R.id.btnPlayAgain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playAgain();
            }
        });
        findViewById(R.id.btnMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GameState.current = null;
                Intent i = new Intent(GameActivity.this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            }
        });

        if (game.timerMinutes == 0) {
            timerCard.setVisibility(View.GONE);
        }
        resetTimer();
        refreshRound();
    }

    // ------------------------------------------------------------------ ronda

    private void refreshRound() {
        txtRound.setText(getString(R.string.round) + " " + game.round);
        txtStarter.setText(game.players.get(game.starterIndex));
        playersContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < game.players.size(); i++) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_player, playersContainer, false);
            String name = game.players.get(i);
            if (game.alive[i]) {
                chip.setText(i == game.starterIndex ? "▶ " + name : name);
            } else {
                chip.setText(name + "  · " + deadLabel(i));
                chip.setBackgroundResource(R.drawable.bg_chip_dead);
                chip.setTextColor(getResources().getColor(R.color.muted));
                chip.setPaintFlags(chip.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            playersContainer.addView(chip);
        }
    }

    /** Lo que se muestra de un jugador eliminado. En modo Undercover se revela su rol. */
    private String deadLabel(int i) {
        if (!game.isUndercoverMode()) return game.isImpostor[i] ? "era impostor" : "eliminado";
        return "era " + roleName(i);
    }

    private String roleName(int i) {
        if (game.isMrWhite[i]) return "Mr. White";
        if (game.isImpostor[i]) return game.isUndercoverMode() ? "undercover" : "impostor";
        return "civil";
    }

    // ------------------------------------------------------------------ temporizador

    private void startTimer() {
        if (remainingMillis <= 0) resetTimer();
        timerRunning = true;
        btnTimerToggle.setText(R.string.timer_pause);
        timer = new CountDownTimer(remainingMillis, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                showTime();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                timerRunning = false;
                txtTimer.setText("¡Tiempo!");
                txtTimer.setTextColor(getResources().getColor(R.color.primary));
                btnTimerToggle.setText(R.string.timer_start);
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        timerRunning = false;
        btnTimerToggle.setText(R.string.timer_start);
    }

    private void resetTimer() {
        pauseTimer();
        remainingMillis = game.timerMinutes * 60L * 1000L;
        txtTimer.setTextColor(getResources().getColor(R.color.secondary));
        showTime();
    }

    private void showTime() {
        long totalSec = (remainingMillis + 999) / 1000;
        txtTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60));
    }

    // ------------------------------------------------------------------ votación

    private void showVoteDialog() {
        final List<Integer> aliveIdx = new ArrayList<>();
        for (int i = 0; i < game.players.size(); i++) if (game.alive[i]) aliveIdx.add(i);
        final String[] labels = new String[aliveIdx.size()];
        for (int i = 0; i < aliveIdx.size(); i++) labels[i] = game.players.get(aliveIdx.get(i));
        final int[] selected = {-1};

        new AlertDialog.Builder(this)
                .setTitle(game.isUndercoverMode() ? "¿A quién eliminan?" : "¿Quién es el impostor?")
                .setSingleChoiceItems(labels, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selected[0] = which;
                    }
                })
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (selected[0] >= 0) eliminate(aliveIdx.get(selected[0]));
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminate(final int idx) {
        pauseTimer();
        game.alive[idx] = false;
        String name = game.players.get(idx);
        if (game.isImpostor[idx] && (!game.isUndercoverMode() || game.isMrWhite[idx])) {
            askImpostorGuess(idx);
            return;
        }
        String title, message;
        if (game.isUndercoverMode()) {
            title = game.isImpostor[idx] ? "¡" + name + " era undercover!" : name + " era civil";
            if (game.civiliansWin()) {
                message = "No queda ningún infiltrado.";
            } else if (game.impostorsWin()) {
                message = "Solo queda un civil…";
            } else {
                message = "Todavía quedan infiltrados entre ustedes. Siguiente ronda de pistas.";
            }
        } else {
            title = name + " no era impostor";
            message = game.impostorsWin()
                    ? "Los impostores ya son tantos como el resto…"
                    : "El impostor sigue entre ustedes. Siguiente ronda de pistas.";
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        afterElimination();
                    }
                })
                .show();
    }

    /**
     * El impostor (o Mr. White) atrapado tiene una última oportunidad de adivinar la palabra
     * de los civiles. Si acierta, ganan los impostores (o Mr. White).
     */
    private void askImpostorGuess(int idx) {
        final String name = game.players.get(idx);
        final boolean mrWhite = game.isMrWhite[idx];
        final EditText input = new EditText(this);
        input.setHint("Escribe tu respuesta");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("¡" + name + (mrWhite ? " era Mr. White!" : " era el impostor!"))
                .setMessage(mrWhite
                        ? "Última oportunidad: si adivina la palabra de los civiles, gana Mr. White."
                        : "Última oportunidad: si adivina la palabra secreta, los impostores ganan.")
                .setView(wrapper)
                .setCancelable(false)
                .setPositiveButton("Comprobar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String guess = input.getText().toString();
                        if (!guess.trim().isEmpty()
                                && WordRepository.normalize(guess).equals(WordRepository.normalize(game.word))) {
                            if (mrWhite) {
                                showEnd("¡Gana Mr. White!",
                                        name + " adivinó la palabra de los civiles.", true);
                            } else {
                                showEnd("¡Ganan los impostores!",
                                        name + " adivinó la palabra secreta.", true);
                            }
                        } else {
                            afterElimination();
                        }
                    }
                })
                .setNegativeButton("No adivina", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        afterElimination();
                    }
                })
                .show();
    }

    private void afterElimination() {
        if (game.isUndercoverMode() && game.civiliansWin()) {
            showEnd("¡Ganan los civiles!", "Descubrieron a todos los infiltrados.", false);
        } else if (game.isUndercoverMode() && game.impostorsWin()) {
            showEnd("¡Ganan los infiltrados!", "Sobrevivieron hasta que solo quedó un civil.", true);
        } else if (game.civiliansWin()) {
            showEnd("¡Ganan los jugadores!", "Descubrieron a todos los impostores.", false);
        } else if (game.impostorsWin()) {
            showEnd("¡Ganan los impostores!", "Ya son tantos como el resto de jugadores.", true);
        } else {
            game.nextRound();
            resetTimer();
            refreshRound();
        }
    }

    // ------------------------------------------------------------------ fin

    private void confirmEnd() {
        new AlertDialog.Builder(this)
                .setTitle("¿Terminar la partida?")
                .setMessage(game.isUndercoverMode()
                        ? "Se revelarán las palabras y el rol de cada jugador."
                        : "Se revelará la palabra y quiénes eran los impostores.")
                .setPositiveButton("Terminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showEnd("Partida terminada", "", game.impostorsAlive() > 0);
                    }
                })
                .setNegativeButton("Seguir jugando", null)
                .show();
    }

    private void showEnd(String title, String detail, boolean impostorsWon) {
        pauseTimer();
        finished = true;
        gamePanel.setVisibility(View.GONE);
        endPanel.setVisibility(View.VISIBLE);
        txtEndTitle.setText(title);
        txtEndTitle.setTextColor(getResources().getColor(impostorsWon ? R.color.primary : R.color.good));
        txtEndWord.setText(game.word);
        String info = "Categoría: " + game.category;
        if (game.isUndercoverMode()) {
            txtEndWordLabel.setText("La palabra de los civiles era");
            String roles = joinNames("Undercover: ", "Undercovers: ", game.impostorNames());
            if (!game.mrWhiteNames().isEmpty()) {
                roles += "\n" + joinNames("Mr. White: ", "Mr. White: ", game.mrWhiteNames());
            }
            txtEndImpostors.setText(roles);
            info = "Palabra del undercover: " + game.undercoverWord + "\n" + info;
        } else {
            txtEndWordLabel.setText("La palabra era");
            txtEndImpostors.setText(joinNames("Impostor: ", "Impostores: ", game.impostorNames()));
        }
        txtEndDetail.setText(detail.isEmpty() ? info : detail + "\n" + info);
    }

    private static String joinNames(String singular, String plural, List<String> names) {
        StringBuilder sb = new StringBuilder(names.size() == 1 ? singular : plural);
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private void playAgain() {
        WordRepository repo = new WordRepository(this);
        if (!game.newRound(repo)) {
            finish();
            return;
        }
        startActivity(new Intent(this, RevealActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (finished) {
            findViewById(R.id.btnMenu).performClick();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("¿Salir de la partida?")
                .setMessage("Se perderá el progreso de esta partida.")
                .setPositiveButton("Salir", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameState.current = null;
                        finish();
                    }
                })
                .setNegativeButton("Seguir", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
