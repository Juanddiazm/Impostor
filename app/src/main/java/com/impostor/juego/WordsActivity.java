package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Gestión de palabras: agregar, eliminar, crear categorías y activarlas para el juego. */
public class WordsActivity extends Activity {

    private WordRepository repo;
    private Spinner spinner;
    private Switch switchEnabled;
    private Button btnDeleteCategory, btnRestore;
    private EditText editWord;
    private TextView txtCount;
    private ListView listWords;

    private List<String> categories = new ArrayList<>();
    private String currentCategory;
    private WordAdapter adapter;
    private boolean updatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);
        repo = new WordRepository(this);

        spinner = findViewById(R.id.spinnerCategory);
        switchEnabled = findViewById(R.id.switchEnabled);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        btnRestore = findViewById(R.id.btnRestore);
        editWord = findViewById(R.id.editWord);
        txtCount = findViewById(R.id.txtCount);
        listWords = findViewById(R.id.listWords);

        adapter = new WordAdapter(this);
        listWords.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = categories.get(position);
                refreshWords();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!updatingSwitch && currentCategory != null) {
                    repo.setCategoryEnabled(currentCategory, isChecked);
                }
            }
        });

        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addWords();
            }
        });
        editWord.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addWords();
                    return true;
                }
                return false;
            }
        });
        findViewById(R.id.btnNewCategory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newCategoryDialog();
            }
        });
        btnDeleteCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCategory();
            }
        });
        btnRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repo.restoreBuiltIn(currentCategory);
                refreshWords();
                Toast.makeText(WordsActivity.this, "Palabras originales restauradas", Toast.LENGTH_SHORT).show();
            }
        });

        refreshCategories(null);
    }

    private void refreshCategories(String select) {
        categories = repo.getCategories();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        int pos = select == null ? -1 : categories.indexOf(select);
        if (pos < 0 && currentCategory != null) pos = categories.indexOf(currentCategory);
        if (pos < 0) pos = 0;
        spinner.setSelection(pos);
        currentCategory = categories.get(pos);
        refreshWords();
    }

    private void refreshWords() {
        if (currentCategory == null) return;
        updatingSwitch = true;
        switchEnabled.setChecked(repo.isCategoryEnabled(currentCategory));
        updatingSwitch = false;
        boolean builtIn = repo.isBuiltInCategory(currentCategory);
        btnDeleteCategory.setVisibility(builtIn ? View.GONE : View.VISIBLE);
        btnRestore.setVisibility(builtIn && repo.countHidden(currentCategory) > 0 ? View.VISIBLE : View.GONE);

        List<String> words = repo.getWords(currentCategory);
        adapter.clear();
        adapter.addAll(words);
        adapter.notifyDataSetChanged();
        txtCount.setText(words.size() + (words.size() == 1 ? " palabra" : " palabras")
                + (builtIn ? " · las originales se pueden ocultar" : " · categoría creada por ti"));
    }

    private void addWords() {
        String text = editWord.getText().toString();
        if (text.trim().isEmpty()) return;
        int added = repo.addWords(currentCategory, text);
        if (added == 0) {
            Toast.makeText(this, "Esa palabra ya existe en " + currentCategory, Toast.LENGTH_SHORT).show();
        } else {
            editWord.setText("");
            Toast.makeText(this, added == 1 ? "Palabra agregada" : added + " palabras agregadas",
                    Toast.LENGTH_SHORT).show();
        }
        refreshWords();
        listWords.smoothScrollToPosition(adapter.getCount() - 1);
    }

    private void newCategoryDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nombre de la categoría");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(pad, pad / 2, pad, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_category)
                .setView(wrapper)
                .setPositiveButton("Crear", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (repo.addCategory(name)) {
                            refreshCategories(name);
                            Toast.makeText(WordsActivity.this, "Ahora agrega palabras a " + name,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WordsActivity.this, "Nombre vacío o ya existe", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteCategory() {
        new AlertDialog.Builder(this)
                .setTitle("¿Eliminar \"" + currentCategory + "\"?")
                .setMessage("Se borrarán todas sus palabras.")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repo.removeCategory(currentCategory);
                        currentCategory = null;
                        refreshCategories(null);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeleteWord(final String word) {
        boolean builtIn = repo.isBuiltInWord(currentCategory, word);
        new AlertDialog.Builder(this)
                .setTitle((builtIn ? "¿Ocultar \"" : "¿Eliminar \"") + word + "\"?")
                .setMessage(builtIn ? "Es una palabra original: no saldrá en el juego, pero podrás restaurarla."
                        : "Esta palabra la agregaste tú y se borrará definitivamente.")
                .setPositiveButton(builtIn ? "Ocultar" : "Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repo.removeWord(currentCategory, word);
                        refreshWords();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private class WordAdapter extends ArrayAdapter<String> {
        WordAdapter(Context context) {
            super(context, R.layout.item_word, new ArrayList<String>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView != null ? convertView
                    : LayoutInflater.from(getContext()).inflate(R.layout.item_word, parent, false);
            final String word = getItem(position);
            ((TextView) row.findViewById(R.id.txtWord)).setText(word);
            TextView tag = row.findViewById(R.id.txtTag);
            tag.setText(repo.isBuiltInWord(currentCategory, word) ? "" : "tuya");
            row.findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteWord(word);
                }
            });
            return row;
        }
    }
}
