package com.impostor.juego;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

    /** Extra opcional con el modo elegido en el inicio ({@link GameState#MODE_IMPOSTOR} o UNDERCOVER). */
    public static final String EXTRA_MODE = "mode";

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

    private TextView txtPlayers, txtImpostors, txtMrWhite, txtTime, txtCategories, txtRoleSummary;
    private TextView txtModeDesc, txtImpostorsSection, txtImpostorsLabel, txtHintTitle;
    private TextView btnModeImpostor, btnModeUndercover;
    private View rowMrWhite, txtInfiltratorsNote, dotImpostor, dotMrWhite;
    private LinearLayout namesContainer, roleBar;
    private FlowLayout categoryChips;
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
        txtTime = findViewById(R.id.txtTime);
        txtCategories = findViewById(R.id.txtCategories);
        txtRoleSummary = findViewById(R.id.txtRoleSummary);
        txtModeDesc = findViewById(R.id.txtModeDesc);
        txtImpostorsSection = findViewById(R.id.txtImpostorsSection);
        txtImpostorsLabel = findViewById(R.id.txtImpostorsLabel);
        txtHintTitle = findViewById(R.id.txtHintTitle);
        btnModeImpostor = findViewById(R.id.btnModeImpostor);
        btnModeUndercover = findViewById(R.id.btnModeUndercover);
        rowMrWhite = findViewById(R.id.rowMrWhite);
        txtInfiltratorsNote = findViewById(R.id.txtInfiltratorsNote);
        dotImpostor = findViewById(R.id.dotImpostor);
        dotMrWhite = findViewById(R.id.dotMrWhite);
        namesContainer = findViewById(R.id.namesContainer);
        roleBar = findViewById(R.id.roleBar);
        categoryChips = findViewById(R.id.categoryChips);
        switchHint = findViewById(R.id.switchHint);

        // Restaurar la última configuración usada; el modo elegido en el inicio tiene prioridad.
        mode = getIntent().getIntExtra(EXTRA_MODE, prefs.getInt("mode", GameState.MODE_IMPOSTOR));
        playerCount = prefs.getInt("players", 5);
        impostorCount = prefs.getInt("impostors", 1);
        mrWhiteCount = prefs.getInt("mrwhite", 0);
        timerMinutes = prefs.getInt("minutes", 0);
        switchHint.setChecked(prefs.getBoolean("hint", true));
        for (int i = 0; i < MAX_PLAYERS; i++) {
            names.add(prefs.getString("name" + i, ""));
        }

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
        findViewById(R.id.rowHint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchHint.toggle();
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
        findViewById(R.id.btnAllCategories).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (String c : repo.getCategories()) repo.setCategoryEnabled(c, true);
                refreshCategories();
            }
        });
        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });

        Ui.applyFonts(findViewById(android.R.id.content));
        buildNameFields();
        clampCounts();
        refreshMode();
        refreshCounters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        repo = new WordRepository(this);
        refreshCategories();
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

    private boolean undercover() {
        return mode == GameState.MODE_UNDERCOVER;
    }

    /** Color de los infiltrados en el modo actual: bermellón (impostor) o ámbar (undercover). */
    private int accent() {
        return Ui.color(this, undercover() ? R.color.amber : R.color.primary);
    }

    private void refreshMode() {
        boolean u = undercover();
        styleSegment(btnModeImpostor, !u, R.drawable.bg_segment_impostor);
        styleSegment(btnModeUndercover, u, R.drawable.bg_segment_undercover);
        txtModeDesc.setText(u ? R.string.mode_undercover_desc : R.string.mode_impostor_desc);
        txtImpostorsSection.setText(u ? R.string.infiltrators : R.string.impostors);
        txtImpostorsLabel.setText(u ? R.string.undercover_count : R.string.impostor_count);
        rowMrWhite.setVisibility(u ? View.VISIBLE : View.GONE);
        txtInfiltratorsNote.setVisibility(u ? View.VISIBLE : View.GONE);
        txtHintTitle.setText(u ? R.string.hint_for_mr_white : R.string.hint_for_impostor);
        switchHint.setContentDescription(txtHintTitle.getText());
        txtImpostors.setTextColor(accent());
        dotImpostor.setBackground(dot(accent()));
        dotMrWhite.setBackground(dot(Ui.color(this, R.color.text)));
    }

    private void styleSegment(TextView segment, boolean selected, int selectedBg) {
        if (selected) {
            segment.setBackgroundResource(selectedBg);
        } else {
            segment.setBackground(null);
        }
        segment.setTextColor(Ui.color(this, selected ? R.color.ink : R.color.muted));
        segment.setSelected(selected);
    }

    private GradientDrawable dot(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private GradientDrawable bar(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(Ui.dp(this, 5));
        d.setColor(color);
        return d;
    }

    private void refreshCounters() {
        txtPlayers.setText(String.valueOf(playerCount));
        txtImpostors.setText(String.valueOf(impostorCount));
        txtMrWhite.setText(String.valueOf(mrWhiteCount));
        txtTime.setText(timerMinutes == 0 ? getString(R.string.no_limit)
                : String.format(Locale.getDefault(), "%d min", timerMinutes));

        int whites = mrWhites();
        int civilians = playerCount - impostorCount - whites;
        roleBar.removeAllViews();
        addBarSegment(civilians, Ui.color(this, R.color.good));
        addBarSegment(impostorCount, accent());
        addBarSegment(whites, Ui.color(this, R.color.text));

        StringBuilder sb = new StringBuilder();
        sb.append(civilians).append(civilians == 1 ? " civil · " : " civiles · ");
        if (undercover()) {
            sb.append(impostorCount).append(" undercover");
            if (whites > 0) sb.append(" · ").append(whites).append(" Mr. White");
        } else {
            sb.append(impostorCount).append(impostorCount == 1 ? " impostor" : " impostores");
        }
        txtRoleSummary.setText(sb.toString());
    }

    private void addBarSegment(int weight, int color) {
        if (weight <= 0) return;
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        if (roleBar.getChildCount() > 0) lp.leftMargin = Ui.dp(this, 4);
        v.setLayoutParams(lp);
        v.setBackground(bar(color));
        roleBar.addView(v);
    }

    private void refreshCategories() {
        categoryChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final String c : repo.getCategories()) {
            final int count = repo.getWords(c).size();
            TextView chip = (TextView) inflater.inflate(R.layout.item_chip, categoryChips, false);
            chip.setText(c);
            boolean on = repo.isCategoryEnabled(c) && count > 0;
            chip.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
            chip.setTextColor(Ui.color(this, on ? R.color.ink : R.color.muted));
            chip.setSelected(on);
            Ui.applyFonts(chip);
            Ui.setBold(chip, on);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (count == 0) {
                        Toast.makeText(SetupActivity.this, "\"" + c + "\" no tiene palabras todavía",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.setCategoryEnabled(c, !repo.isCategoryEnabled(c));
                    refreshCategories();
                }
            });
            categoryChips.addView(chip);
        }
        List<String> enabled = repo.getEnabledCategories();
        int total = repo.getCategories().size();
        txtCategories.setText(enabled.isEmpty() ? "Elige al menos una categoría"
                : enabled.size() + " de " + total + " categorías activas");
    }

    private void buildNameFields() {
        namesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < playerCount; i++) {
            View row = inflater.inflate(R.layout.item_name, namesContainer, false);
            ((TextView) row.findViewById(R.id.txtNumber)).setText(String.valueOf(i + 1));
            EditText edit = row.findViewById(R.id.editName);
            edit.setHint("Jugador " + (i + 1));
            edit.setText(names.get(i));
            edit.setContentDescription("Nombre del jugador " + (i + 1));
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundResource(R.color.surface_light);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1));
                lp.leftMargin = Ui.dp(this, 10);
                lp.rightMargin = Ui.dp(this, 10);
                namesContainer.addView(divider, lp);
            }
            namesContainer.addView(row);
        }
        Ui.applyFonts(namesContainer);
    }

    private void collectNames() {
        int player = 0;
        for (int i = 0; i < namesContainer.getChildCount(); i++) {
            EditText edit = namesContainer.getChildAt(i).findViewById(R.id.editName);
            if (edit == null) continue;
            names.set(player++, edit.getText().toString().trim());
        }
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
            Toast.makeText(this, undercover()
                    ? "No hay parejas de palabras en las categorías elegidas"
                    : "No hay palabras disponibles en las categorías elegidas", Toast.LENGTH_LONG).show();
            return;
        }
        GameState.current = game;
        startActivity(new Intent(this, RevealActivity.class));
    }
}
