package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** Pantalla "pasa el teléfono": cada jugador ve su rol en privado. */
public class RevealActivity extends Activity {

    private GameState game;
    private int index = 0;

    private View passPanel, rolePanel, roleCard;
    private TextView txtProgress, txtPlayerName, txtRoleTitle, txtWord, txtCategory, txtRoleDesc;

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
        txtProgress = findViewById(R.id.txtProgress);
        txtPlayerName = findViewById(R.id.txtPlayerName);
        txtRoleTitle = findViewById(R.id.txtRoleTitle);
        txtWord = findViewById(R.id.txtWord);
        txtCategory = findViewById(R.id.txtCategory);
        txtRoleDesc = findViewById(R.id.txtRoleDesc);

        Button btnReveal = findViewById(R.id.btnReveal);
        btnReveal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRole();
            }
        });
        findViewById(R.id.btnHide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                index++;
                if (index >= game.players.size()) {
                    startActivity(new Intent(RevealActivity.this, GameActivity.class));
                    finish();
                } else {
                    showPass();
                }
            }
        });

        showPass();
    }

    private void showPass() {
        passPanel.setVisibility(View.VISIBLE);
        rolePanel.setVisibility(View.GONE);
        txtProgress.setText("JUGADOR " + (index + 1) + " DE " + game.players.size());
        txtPlayerName.setText(game.players.get(index));
        ((Button) findViewById(R.id.btnReveal)).setText("Soy " + game.players.get(index) + ", ver mi rol");
    }

    private void showRole() {
        passPanel.setVisibility(View.GONE);
        rolePanel.setVisibility(View.VISIBLE);
        boolean impostor = game.isImpostor[index];
        if (impostor) {
            roleCard.setBackgroundResource(R.drawable.bg_card_impostor);
            txtRoleTitle.setText(game.players.get(index) + ", tu rol es…");
            txtWord.setText(R.string.you_are_impostor);
            txtWord.setTextColor(getResources().getColor(R.color.primary));
            txtRoleDesc.setText(R.string.impostor_desc);
            if (game.hintForImpostor) {
                txtCategory.setVisibility(View.VISIBLE);
                txtCategory.setText("Pista: " + game.category);
            } else {
                txtCategory.setVisibility(View.GONE);
            }
        } else {
            roleCard.setBackgroundResource(R.drawable.bg_card_civil);
            txtRoleTitle.setText(R.string.secret_word);
            txtWord.setText(game.word);
            txtWord.setTextColor(getResources().getColor(R.color.text));
            txtRoleDesc.setText(R.string.civil_desc);
            txtCategory.setVisibility(View.VISIBLE);
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
