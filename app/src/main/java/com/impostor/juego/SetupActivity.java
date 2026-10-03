package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SetupActivity extends Activity {

    private static final String PREFS = "impostor_setup";
    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 20;
    private static final int MAX_MINUTES = 10;

    private WordRepository repo;
    private SharedPreferences prefs;

    private int mode = GameState.MODE_IMPOSTOR;
    private int playerCount = 5;
    private int impostorCount = 1;
    private int mrWhiteCount = 0;
    private int timerMinutes = 0;
    private final List<String> names = new ArrayList<>();

    private TextView txtPlayers, txtImpostors, txtMrWhite, txtTime, txtCategories;
    private TextView txtModeDesc, txtImpostorsSection, txtImpostorsLabel;
    private View rowMrWhite, txtInfiltratorsNote;
    private Button btnModeImpostor, btnModeUndercover;
    private LinearLayout namesContainer;
    private Switch switchHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        repo = new WordRepository(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        txtPlayers = findViewById(R.id.txtPlayers);
        txtImpostors = findViewById(R.id.txtImpostors);
        txtMrWhite = findViewById(R.id.txtMrWhite);
        txtModeDesc = findViewById(R.id.txtModeDesc);
        txtImpostorsSection = findViewById(R.id.txtImpostorsSection);
        txtImpostorsLabel = findViewById(R.id.txtImpostorsLabel);
        rowMrWhite = findViewById(R.id.rowMrWhite);
        txtInfiltratorsNote = findViewById(R.id.txtInfiltratorsNote);
        btnModeImpostor = findViewById(R.id.btnModeImpostor);
        btnModeUndercover = findViewById(R.id.btnModeUndercover);
        txtTime = findViewById(R.id.txtTime);
        txtCategories = findViewById(R.id.txtCategories);
        namesContainer = findViewById(R.id.namesContainer);
        switchHint = findViewById(R.id.switchHint);

        // Restaurar la última configuración usada.
        mode = prefs.getInt("mode", GameState.MODE_IMPOSTOR);
        playerCount = prefs.getInt("players", 5);
        impostorCount = prefs.getInt("impostors", 1);
        mrWhiteCount = prefs.getInt("mrwhite", 0);
        timerMinutes = prefs.getInt("minutes", 0);
        switchHint.setChecked(prefs.getBoolean("hint", true));
        for (int i = 0; i < MAX_PLAYERS; i++) {
            names.add(prefs.getString("name" + i, ""));
        }

        btnModeImpostor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(GameState.MODE_IMPOSTOR);
            }
        });
        btnModeUndercover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(GameState.MODE_UNDERCOVER);
            }
        });
        findViewById(R.id.btnPlayersMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePlayers(-1);
            }
        });
        findViewById(R.id.btnPlayersPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePlayers(1);
            }
        });
        findViewById(R.id.btnImpostorsMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                impostorCount = Math.max(1, impostorCount - 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnImpostorsPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                impostorCount = Math.min(maxImpostors() - mrWhites(), impostorCount + 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnMrWhiteMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mrWhiteCount = Math.max(0, mrWhiteCount - 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnMrWhitePlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mrWhiteCount = Math.min(maxImpostors() - impostorCount, mrWhiteCount + 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnTimeMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerMinutes = Math.max(0, timerMinutes - 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnTimePlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerMinutes = Math.min(MAX_MINUTES, timerMinutes + 1);
                refreshCounters();
            }
        });
        findViewById(R.id.btnCategories).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseCategories();
            }
        });
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        buildNameFields();
        clampCounts();
        refreshMode();
        refreshCounters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        repo = new WordRepository(this);
        refreshCategoriesLabel();
    }

    /** Máximo de jugadores no civiles (impostores, o undercovers + Mr. White). */
    private int maxImpostors() {
        return Math.max(1, (playerCount - 1) / 2);
    }

    /** Mr. White solo existe en modo Undercover. */
    private int mrWhites() {
        return mode == GameState.MODE_UNDERCOVER ? mrWhiteCount : 0;
    }

    /** Ajusta impostores y Mr. White al máximo permitido para la cantidad de jugadores. */
    private void clampCounts() {
        impostorCount = Math.max(1, Math.min(impostorCount, maxImpostors()));
        mrWhiteCount = Math.max(0, Math.min(mrWhiteCount, maxImpostors() - impostorCount));
    }

    private void changePlayers(int delta) {
        collectNames();
        playerCount = Math.max(MIN_PLAYERS, Math.min(MAX_PLAYERS, playerCount + delta));
        clampCounts();
        buildNameFields();
        refreshCounters();
    }

    private void setMode(int newMode) {
        mode = newMode;
        clampCounts();
        refreshMode();
        refreshCounters();
    }

    private void refreshMode() {
        boolean undercover = mode == GameState.MODE_UNDERCOVER;
        styleModeButton(btnModeImpostor, !undercover);
        styleModeButton(btnModeUndercover, undercover);
        txtModeDesc.setText(undercover ? R.string.mode_undercover_desc : R.string.mode_impostor_desc);
        txtImpostorsSection.setText(undercover ? R.string.infiltrators : R.string.impostors);
        txtImpostorsLabel.setText(undercover ? R.string.undercover_count : R.string.impostor_count);
        rowMrWhite.setVisibility(undercover ? View.VISIBLE : View.GONE);
        txtInfiltratorsNote.setVisibility(undercover ? View.VISIBLE : View.GONE);
        switchHint.setText(undercover ? R.string.hint_for_mr_white : R.string.hint_for_impostor);
    }

    private void styleModeButton(Button button, boolean selected) {
        button.setBackgroundResource(selected ? R.drawable.bg_button_primary : R.drawable.bg_button_secondary);
        button.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void refreshCounters() {
        txtPlayers.setText(String.valueOf(playerCount));
        txtImpostors.setText(String.valueOf(impostorCount));
        txtMrWhite.setText(String.valueOf(mrWhiteCount));
        txtTime.setText(timerMinutes == 0 ? getString(R.string.no_limit)
                : String.format(Locale.getDefault(), "%d min", timerMinutes));
    }

    private void refreshCategoriesLabel() {
        List<String> enabled = repo.getEnabledCategories();
        int total = repo.getCategories().size();
        if (enabled.isEmpty()) {
            txtCategories.setText("Ninguna categoría seleccionada");
        } else if (enabled.size() == total) {
            txtCategories.setText("Todas las categorías (" + total + ")");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < enabled.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(enabled.get(i));
            }
            txtCategories.setText(enabled.size() + " de " + total + ": " + sb);
        }
    }

    private void buildNameFields() {
        namesContainer.removeAllViews();
        int padding = (int) (10 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < playerCount; i++) {
            EditText edit = new EditText(this);
            edit.setHint("Jugador " + (i + 1));
            edit.setText(names.get(i));
            edit.setSingleLine(true);
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            edit.setBackgroundResource(R.drawable.bg_input);
            edit.setPadding(padding, padding, padding, padding);
            edit.setTextColor(getResources().getColor(R.color.text));
            edit.setHintTextColor(getResources().getColor(R.color.muted));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = padding / 2;
            edit.setLayoutParams(lp);
            namesContainer.addView(edit);
        }
    }

    private void collectNames() {
        for (int i = 0; i < namesContainer.getChildCount(); i++) {
            EditText edit = (EditText) namesContainer.getChildAt(i);
            names.set(i, edit.getText().toString().trim());
        }
    }

    private void chooseCategories() {
        final List<String> all = repo.getCategories();
        final String[] labels = new String[all.size()];
        final boolean[] checked = new boolean[all.size()];
        for (int i = 0; i < all.size(); i++) {
            String c = all.get(i);
            labels[i] = c + " (" + repo.getWords(c).size() + ")";
            checked[i] = repo.isCategoryEnabled(c);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_categories)
                .setMultiChoiceItems(labels, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setPositiveButton("Listo", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < all.size(); i++) {
                            repo.setCategoryEnabled(all.get(i), checked[i]);
                        }
                        refreshCategoriesLabel();
                    }
                })
                .setNeutralButton("Todas", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (String c : all) repo.setCategoryEnabled(c, true);
                        refreshCategoriesLabel();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void startGame() {
        collectNames();
        List<String> categories = repo.getEnabledCategories();
        if (categories.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una categoría con palabras", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            String n = names.get(i);
            if (n.isEmpty()) n = "Jugador " + (i + 1);
            // Evitar nombres repetidos.
            String candidate = n;
            int suffix = 2;
            while (players.contains(candidate)) candidate = n + " " + suffix++;
            players.add(candidate);
        }

        SharedPreferences.Editor editor = prefs.edit()
                .putInt("mode", mode)
                .putInt("players", playerCount)
                .putInt("impostors", impostorCount)
                .putInt("mrwhite", mrWhiteCount)
                .putInt("minutes", timerMinutes)
                .putBoolean("hint", switchHint.isChecked());
        for (int i = 0; i < MAX_PLAYERS; i++) editor.putString("name" + i, names.get(i));
        editor.apply();

        GameState game = new GameState(players, mode, impostorCount, mrWhites(),
                switchHint.isChecked(), timerMinutes, categories);
        if (!game.newRound(repo)) {
            Toast.makeText(this, mode == GameState.MODE_UNDERCOVER
                    ? "No hay parejas de palabras en las categorías elegidas"
                    : "No hay palabras disponibles en las categorías elegidas", Toast.LENGTH_LONG).show();
            return;
        }
        GameState.current = game;
        startActivity(new Intent(this, RevealActivity.class));
    }
}
