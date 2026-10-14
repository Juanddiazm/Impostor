package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Pantalla "pasa el teléfono": cada jugador ve su rol en privado. El rol solo se muestra
 * mientras se mantiene pulsado el botón; al soltar se oculta.
 */
public class RevealActivity extends Activity {

    private GameState game;
    private int index = 0;
    private boolean seen = false;
    private boolean showing = false;

    private View passPanel, rolePanel, roleCard;
    private LinearLayout btnHold, progressBar;
    private ImageView imgRole, imgHold;
    private TextView txtProgress, txtPlayerName, txtRoleTitle, txtWord, txtCategory, txtRoleDesc, txtHold;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        game = GameState.current;
        if (game == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_reveal);

        passPanel = findViewById(R.id.passPanel);
        rolePanel = findViewById(R.id.rolePanel);
        roleCard = findViewById(R.id.roleCard);
        btnHold = findViewById(R.id.btnHold);
        progressBar = findViewById(R.id.progressBar);
        imgRole = findViewById(R.id.imgRole);
        imgHold = findViewById(R.id.imgHold);
        txtProgress = findViewById(R.id.txtProgress);
        txtPlayerName = findViewById(R.id.txtPlayerName);
        txtRoleTitle = findViewById(R.id.txtRoleTitle);
        txtWord = findViewById(R.id.txtWord);
        txtCategory = findViewById(R.id.txtCategory);
        txtRoleDesc = findViewById(R.id.txtRoleDesc);
        txtHold = findViewById(R.id.txtHold);
        btnNext = findViewById(R.id.btnNext);

        btnHold.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setShowing(true);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        setShowing(false);
                        return true;
                    default:
                        return true;
                }
            }
        });
        // Con TalkBack no hay "mantener pulsado": un toque alterna mostrar/ocultar.
        btnHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShowing(!showing);
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShowing(false);
                index++;
                if (index >= game.players.size()) {
                    startActivity(new Intent(RevealActivity.this, GameActivity.class));
                    finish();
                } else {
                    showPass();
                }
            }
        });

        Ui.applyFonts(findViewById(android.R.id.content));
        showPass();
    }

    private int accent() {
        return Ui.color(this, game.isUndercoverMode() ? R.color.amber : R.color.primary);
    }

    private void showPass() {
        seen = false;
        int n = game.players.size();
        txtProgress.setText((index + 1) + " de " + n);
        txtPlayerName.setText(game.players.get(index));
        btnNext.setText(index == n - 1 ? R.string.start_round : R.string.next_player);
        refreshNext();
        refreshProgress();
        setShowing(false);
    }

    private void refreshNext() {
        btnNext.setEnabled(seen);
        btnNext.setAlpha(seen ? 1f : 0.45f);
        btnNext.setTextColor(Ui.color(this, seen ? R.color.text : R.color.muted));
    }

    private void refreshProgress() {
        progressBar.removeAllViews();
        int n = game.players.size();
        for (int i = 0; i < n; i++) {
            View seg = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            if (i > 0) lp.leftMargin = Ui.dp(this, n > 12 ? 3 : 6);
            GradientDrawable d = new GradientDrawable();
            d.setCornerRadius(Ui.dp(this, 3));
            d.setColor(i < index ? Ui.color(this, R.color.text)
                    : i == index ? accent() : Ui.color(this, R.color.surface_light));
            seg.setBackground(d);
            progressBar.addView(seg, lp);
        }
    }

    private void setShowing(boolean show) {
        showing = show;
        boolean red = !game.isUndercoverMode();
        passPanel.setVisibility(show ? View.GONE : View.VISIBLE);
        rolePanel.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            fillRole();
            if (!seen) {
                seen = true;
                refreshNext();
            }
            btnHold.setBackgroundResource(red ? R.drawable.bg_hold_active_red : R.drawable.bg_hold_active);
            imgHold.setImageResource(red ? R.drawable.ic_eye_red : R.drawable.ic_eye);
            txtHold.setText(R.string.release_to_hide);
            txtHold.setTextColor(accent());
        } else {
            btnHold.setBackgroundResource(red ? R.drawable.bg_hold_red : R.drawable.bg_hold);
            imgHold.setImageResource(R.drawable.ic_hand);
            txtHold.setText(R.string.hold_short);
            txtHold.setTextColor(Ui.color(this, R.color.ink));
        }
    }

    private void fillRole() {
        String word = game.wordFor(index);
        if (word == null) {
            // Impostor o Mr. White: sin palabra.
            boolean mrWhite = game.isMrWhite[index];
            int color = Ui.color(this, mrWhite ? R.color.amber : R.color.primary);
            roleCard.setBackgroundResource(mrWhite ? R.drawable.bg_card_mrwhite : R.drawable.bg_card_impostor);
            imgRole.setVisibility(View.VISIBLE);
            imgRole.setImageResource(mrWhite ? R.drawable.ic_mask_amber : R.drawable.ic_mask_red);
            txtRoleTitle.setText(R.string.you_are);
            txtRoleTitle.setTextColor(Ui.color(this, R.color.muted));
            txtWord.setText(mrWhite ? R.string.role_mr_white : R.string.role_impostor);
            txtWord.setTextColor(color);
            txtRoleDesc.setText(mrWhite ? R.string.mr_white_desc : R.string.impostor_desc);
            txtRoleDesc.setTextColor(Ui.color(this, R.color.muted));
            txtCategory.setBackgroundResource(R.drawable.bg_pill);
            txtCategory.setTextColor(Ui.color(this, R.color.text));
            if (game.hintForImpostor) {
                txtCategory.setVisibility(View.VISIBLE);
                txtCategory.setText("Pista: " + game.category);
            } else {
                txtCategory.setVisibility(View.GONE);
            }
        } else {
            // En modo Undercover civiles y undercovers ven la misma pantalla: nadie sabe qué es.
            int ink = Ui.color(this, R.color.ink);
            int inkMuted = Ui.color(this, R.color.ink_muted);
            roleCard.setBackgroundResource(R.drawable.bg_card_word);
            imgRole.setVisibility(View.GONE);
            txtRoleTitle.setText(game.isUndercoverMode() ? R.string.your_word : R.string.secret_word);
            txtRoleTitle.setTextColor(inkMuted);
            txtWord.setText(word);
            txtWord.setTextColor(ink);
            txtRoleDesc.setText(game.isUndercoverMode() ? R.string.undercover_word_desc : R.string.civil_desc);
            txtRoleDesc.setTextColor(inkMuted);
            txtCategory.setVisibility(View.VISIBLE);
            txtCategory.setBackgroundResource(R.drawable.bg_pill_word);
            txtCategory.setTextColor(ink);
            txtCategory.setText(game.category);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("¿Cancelar la partida?")
                .setMessage("Se perderá el reparto de roles actual.")
                .setPositiveButton("Sí, cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GameState.current = null;
                        finish();
                    }
                })
                .setNegativeButton("Seguir", null)
                .show();
    }
}
