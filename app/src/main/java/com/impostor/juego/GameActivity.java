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
    private TextView txtEndTitle, txtEndWord, txtEndImpostors, txtEndDetail;
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
                chip.setText(name + (game.isImpostor[i] ? "  · era impostor" : "  · eliminado"));
                chip.setBackgroundResource(R.drawable.bg_chip_dead);
                chip.setTextColor(getResources().getColor(R.color.muted));
                chip.setPaintFlags(chip.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            playersContainer.addView(chip);
        }
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
                .setTitle("¿Quién es el impostor?")
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
        if (game.isImpostor[idx]) {
            askImpostorGuess(name);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(name + " no era impostor")
                    .setMessage(game.impostorsWin()
                            ? "Los impostores ya son tantos como el resto…"
                            : "El impostor sigue entre ustedes. Siguiente ronda de pistas.")
                    .setCancelable(false)
                    .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            afterElimination();
                        }
                    })
                    .show();
        }
    }

    /** El impostor atrapado tiene una última oportunidad de adivinar la palabra. */
    private void askImpostorGuess(final String name) {
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
                .setTitle("¡" + name + " era el impostor!")
                .setMessage("Última oportunidad: si adivina la palabra secreta, los impostores ganan.")
                .setView(wrapper)
                .setCancelable(false)
                .setPositiveButton("Comprobar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String guess = input.getText().toString();
                        if (!guess.trim().isEmpty()
                                && WordRepository.normalize(guess).equals(WordRepository.normalize(game.word))) {
                            showEnd("¡Ganan los impostores!",
                                    name + " adivinó la palabra secreta.", true);
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
        if (game.civiliansWin()) {
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
                .setMessage("Se revelará la palabra y quiénes eran los impostores.")
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
        List<String> impostors = game.impostorNames();
        StringBuilder sb = new StringBuilder(impostors.size() == 1 ? "Impostor: " : "Impostores: ");
        for (int i = 0; i < impostors.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(impostors.get(i));
        }
        txtEndImpostors.setText(sb.toString());
        txtEndDetail.setText(detail.isEmpty() ? "Categoría: " + game.category
                : detail + "\nCategoría: " + game.category);
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
