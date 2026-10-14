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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Rondas de pistas, temporizador, votación y resultado final. */
public class GameActivity extends Activity {

    /** Quién ganó, para el color del resultado. */
    private static final int END_CIVILIANS = 0;
    private static final int END_IMPOSTORS = 1;
    private static final int END_MR_WHITE = 2;
    private static final int END_NONE = 3;

    private GameState game;

    private View gamePanel, endPanel, timerCard, endUndercoverCard;
    private LinearLayout endBanner, playersContainer, rolesContainer;
    private TextView txtRound, txtModeTag, txtStarter, txtTimer, txtVoteTitle, txtAlive;
    private TextView txtEndKicker, txtEndTitle, txtEndDetail, txtEndWordLabel, txtEndWord;
    private TextView txtEndUndercoverWord, txtEndCategory;
    private ImageButton btnTimerToggle;

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
        endUndercoverCard = findViewById(R.id.endUndercoverCard);
        endBanner = findViewById(R.id.endBanner);
        playersContainer = findViewById(R.id.playersContainer);
        rolesContainer = findViewById(R.id.rolesContainer);
        txtRound = findViewById(R.id.txtRound);
        txtModeTag = findViewById(R.id.txtModeTag);
        txtStarter = findViewById(R.id.txtStarter);
        txtTimer = findViewById(R.id.txtTimer);
        txtVoteTitle = findViewById(R.id.txtVoteTitle);
        txtAlive = findViewById(R.id.txtAlive);
        txtEndKicker = findViewById(R.id.txtEndKicker);
        txtEndTitle = findViewById(R.id.txtEndTitle);
        txtEndDetail = findViewById(R.id.txtEndDetail);
        txtEndWordLabel = findViewById(R.id.txtEndWordLabel);
        txtEndWord = findViewById(R.id.txtEndWord);
        txtEndUndercoverWord = findViewById(R.id.txtEndUndercoverWord);
        txtEndCategory = findViewById(R.id.txtEndCategory);
        btnTimerToggle = findViewById(R.id.btnTimerToggle);

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

        boolean undercover = game.isUndercoverMode();
        txtModeTag.setText(undercover ? R.string.mode_undercover : R.string.mode_impostor);
        txtModeTag.setBackgroundResource(undercover ? R.drawable.bg_tag_amber : R.drawable.bg_tag_red);
        txtModeTag.setTextColor(Ui.color(this, undercover ? R.color.amber : R.color.primary));
        txtVoteTitle.setText(undercover ? R.string.vote_title_undercover : R.string.vote_title_impostor);

        if (game.timerMinutes == 0) {
            timerCard.setVisibility(View.GONE);
        }
        Ui.applyFonts(findViewById(android.R.id.content));
        resetTimer();
        refreshRound();
    }

    // ------------------------------------------------------------------ ronda

    private void refreshRound() {
        txtRound.setText(getString(R.string.round) + " " + game.round);
        txtStarter.setText(game.players.get(game.starterIndex));
        int alive = 0;
        for (boolean a : game.alive) if (a) alive++;
        txtAlive.setText(alive + " en juego");

        playersContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout row = null;
        int gap = Ui.dp(this, 10);
        for (int i = 0; i < game.players.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) rowLp.topMargin = gap;
                playersContainer.addView(row, rowLp);
            }
            View tile = inflater.inflate(R.layout.item_player_tile, row, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tile.getLayoutParams();
            if (i % 2 == 1) lp.leftMargin = gap;
            bindTile(tile, i);
            row.addView(tile, lp);
        }
        // Fila impar: un hueco para que la última ficha no ocupe todo el ancho.
        if (game.players.size() % 2 == 1 && row != null) {
            View spacer = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1);
            lp.leftMargin = gap;
            row.addView(spacer, lp);
        }
        Ui.applyFonts(playersContainer);
    }

    private void bindTile(View tile, final int i) {
        String name = game.players.get(i);
        TextView avatar = tile.findViewById(R.id.txtAvatar);
        TextView txtName = tile.findViewById(R.id.txtName);
        TextView txtSub = tile.findViewById(R.id.txtSub);
        txtName.setText(name);
        avatar.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));
        if (game.alive[i]) {
            boolean starter = i == game.starterIndex;
            if (starter) {
                tile.setBackgroundResource(R.drawable.bg_tile_starter);
                avatar.setBackgroundResource(R.drawable.bg_avatar_bone);
                avatar.setTextColor(Ui.color(this, R.color.ink));
                txtName.setTypeface(txtName.getTypeface(), android.graphics.Typeface.BOLD);
                txtSub.setVisibility(View.VISIBLE);
                txtSub.setText(R.string.starts_lower);
            }
            tile.setContentDescription("Eliminar a " + name);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmEliminate(i);
                }
            });
        } else {
            tile.setBackgroundResource(R.drawable.bg_tile_dead);
            tile.setClickable(false);
            avatar.setVisibility(View.GONE);
            tile.findViewById(R.id.imgDead).setVisibility(View.VISIBLE);
            txtName.setTextColor(Ui.color(this, R.color.muted));
            txtName.setPaintFlags(txtName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            txtSub.setVisibility(View.VISIBLE);
            txtSub.setText(deadLabel(i));
            txtSub.setTextColor(Ui.color(this, roleColor(i)));
            tile.setContentDescription(name + ", " + deadLabel(i));
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

    private int roleColor(int i) {
        if (game.isMrWhite[i]) return R.color.text;
        if (game.isImpostor[i]) return game.isUndercoverMode() ? R.color.amber : R.color.primary;
        return game.isUndercoverMode() ? R.color.good : R.color.muted;
    }

    // ------------------------------------------------------------------ temporizador

    private void startTimer() {
        if (remainingMillis <= 0) resetTimer();
        timerRunning = true;
        setToggleIcon();
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
                txtTimer.setTextColor(Ui.color(GameActivity.this, R.color.primary));
                setToggleIcon();
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        timerRunning = false;
        setToggleIcon();
    }

    private void setToggleIcon() {
        btnTimerToggle.setImageResource(timerRunning ? R.drawable.ic_pause : R.drawable.ic_play);
        btnTimerToggle.setContentDescription(getString(timerRunning ? R.string.timer_pause : R.string.timer_start));
    }

    private void resetTimer() {
        pauseTimer();
        remainingMillis = game.timerMinutes * 60L * 1000L;
        txtTimer.setTextColor(Ui.color(this, R.color.amber));
        showTime();
    }

    private void showTime() {
        long totalSec = (remainingMillis + 999) / 1000;
        txtTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60));
    }

    // ------------------------------------------------------------------ votación

    private void confirmEliminate(final int idx) {
        String name = game.players.get(idx);
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar a " + name + "?")
                .setMessage(game.isUndercoverMode()
                        ? "Se revelará su rol, pero no su palabra."
                        : "Si era el impostor, tendrá una última oportunidad de adivinar la palabra.")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eliminate(idx);
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
        input.setBackgroundResource(R.drawable.bg_input);
        input.setTextColor(Ui.color(this, R.color.text));
        input.setMinHeight(Ui.dp(this, 52));
        input.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        Ui.applyFonts(input);
        int pad = Ui.dp(this, 20);
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
                                showEnd("Gana Mr. White", name + " adivinó la palabra de los civiles.", END_MR_WHITE);
                            } else {
                                showEnd("Ganan los impostores", name + " adivinó la palabra secreta.", END_IMPOSTORS);
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
            showEnd("Ganan los civiles", "Descubrieron a todos los infiltrados.", END_CIVILIANS);
        } else if (game.isUndercoverMode() && game.impostorsWin()) {
            showEnd("Ganan los infiltrados", "Sobrevivieron hasta que solo quedó un civil.", END_IMPOSTORS);
        } else if (game.civiliansWin()) {
            showEnd("Ganan los jugadores", "Descubrieron a todos los impostores.", END_CIVILIANS);
        } else if (game.impostorsWin()) {
            showEnd("Ganan los impostores", "Ya son tantos como el resto de jugadores.", END_IMPOSTORS);
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
                        showEnd("Partida terminada", "Nadie ganó esta vez.", END_NONE);
                    }
                })
                .setNegativeButton("Seguir jugando", null)
                .show();
    }

    private void showEnd(String title, String detail, int outcome) {
        pauseTimer();
        finished = true;
        gamePanel.setVisibility(View.GONE);
        endPanel.setVisibility(View.VISIBLE);

        int bannerBg, bannerText;
        switch (outcome) {
            case END_CIVILIANS:
                bannerBg = R.drawable.bg_win_good;
                bannerText = R.color.ink;
                break;
            case END_IMPOSTORS:
                bannerBg = game.isUndercoverMode() ? R.drawable.bg_win_white : R.drawable.bg_win_bad;
                bannerText = R.color.ink;
                break;
            case END_MR_WHITE:
                bannerBg = R.drawable.bg_win_white;
                bannerText = R.color.ink;
                break;
            default:
                bannerBg = R.drawable.bg_win_neutral;
                bannerText = R.color.text;
        }
        endBanner.setBackgroundResource(bannerBg);
        int c = Ui.color(this, bannerText);
        txtEndKicker.setText(getString(R.string.round) + " " + game.round + " · fin");
        txtEndKicker.setTextColor(c);
        txtEndTitle.setText(title);
        txtEndTitle.setTextColor(c);
        txtEndDetail.setText(detail);
        txtEndDetail.setTextColor(c);

        txtEndWord.setText(game.word);
        txtEndCategory.setText("Categoría: " + game.category);
        if (game.isUndercoverMode()) {
            txtEndWordLabel.setText(R.string.civilians);
            endUndercoverCard.setVisibility(View.VISIBLE);
            txtEndUndercoverWord.setText(game.undercoverWord);
        } else {
            txtEndWordLabel.setText(R.string.the_word_was);
            endUndercoverCard.setVisibility(View.GONE);
        }

        rolesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < game.players.size(); i++) {
            View row = inflater.inflate(R.layout.item_role_row, rolesContainer, false);
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundResource(R.color.surface_light);
                rolesContainer.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
            }
            TextView txtName = row.findViewById(R.id.txtName);
            TextView txtRole = row.findViewById(R.id.txtRole);
            txtName.setText(game.players.get(i));
            String role = roleName(i);
            txtRole.setText(role.substring(0, 1).toUpperCase(Locale.getDefault()) + role.substring(1));
            boolean special = game.isImpostor[i];
            Ui.applyFonts(row);
            if (special) {
                Ui.setBold(txtName, true);
                txtRole.setBackgroundResource(game.isMrWhite[i] ? R.drawable.bg_tag_bone
                        : game.isUndercoverMode() ? R.drawable.bg_tag_amber_solid : R.drawable.bg_tag_red);
                txtRole.setTextColor(Ui.color(this, game.isMrWhite[i] || game.isUndercoverMode()
                        ? R.color.ink : R.color.primary));
                txtRole.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));
            } else {
                txtRole.setTextColor(Ui.color(this, R.color.good));
            }
            rolesContainer.addView(row);
        }
        endPanel.scrollTo(0, 0);
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
