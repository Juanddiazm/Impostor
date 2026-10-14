package com.impostor.juego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Gestión de palabras: agregar, eliminar, crear categorías y activarlas para el juego. */
public class WordsActivity extends Activity {

    private WordRepository repo;
    private LinearLayout categoryTabs;
    private FlowLayout wordChips;
    private Switch switchEnabled;
    private Button btnDeleteCategory, btnRestore;
    private EditText editWord;
    private TextView txtCount, txtEmpty;

    private List<String> categories = new ArrayList<>();
    private String currentCategory;
    private boolean updatingSwitch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);
        repo = new WordRepository(this);

        categoryTabs = findViewById(R.id.categoryTabs);
        wordChips = findViewById(R.id.wordChips);
        switchEnabled = findViewById(R.id.switchEnabled);
        btnDeleteCategory = findViewById(R.id.btnDeleteCategory);
        btnRestore = findViewById(R.id.btnRestore);
        editWord = findViewById(R.id.editWord);
        txtCount = findViewById(R.id.txtCount);
        txtEmpty = findViewById(R.id.txtEmpty);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
        findViewById(R.id.rowEnabled).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchEnabled.toggle();
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

        Ui.applyFonts(findViewById(android.R.id.content));
        refreshCategories(null);
    }

    private void refreshCategories(String select) {
        categories = repo.getCategories();
        if (select != null && categories.contains(select)) {
            currentCategory = select;
        } else if (currentCategory == null || !categories.contains(currentCategory)) {
            currentCategory = categories.get(0);
        }
        refreshWords();
    }

    private void refreshTabs() {
        categoryTabs.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View selectedTab = null;
        for (final String c : categories) {
            TextView tab = (TextView) inflater.inflate(R.layout.item_chip, categoryTabs, false);
            boolean selected = c.equals(currentCategory);
            tab.setText(c + " · " + repo.getWords(c).size());
            tab.setBackgroundResource(selected ? R.drawable.bg_chip_on : R.drawable.bg_chip);
            tab.setTextColor(Ui.color(this, selected ? R.color.ink : R.color.text));
            tab.setSelected(selected);
            Ui.applyFonts(tab);
            Ui.setBold(tab, selected);
            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentCategory = c;
                    refreshWords();
                }
            });
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            if (categoryTabs.getChildCount() > 0) lp.leftMargin = Ui.dp(this, 8);
            categoryTabs.addView(tab, lp);
            if (selected) selectedTab = tab;
        }
        if (selectedTab != null) {
            final View target = selectedTab;
            final HorizontalScrollView scroller = (HorizontalScrollView) categoryTabs.getParent();
            scroller.post(new Runnable() {
                @Override
                public void run() {
                    int left = target.getLeft() - Ui.dp(WordsActivity.this, 20);
                    int right = target.getRight() + Ui.dp(WordsActivity.this, 20) - scroller.getWidth();
                    if (scroller.getScrollX() > left) scroller.smoothScrollTo(left, 0);
                    else if (scroller.getScrollX() < right) scroller.smoothScrollTo(right, 0);
                }
            });
        }
    }

    private void refreshWords() {
        if (currentCategory == null) return;
        refreshTabs();
        updatingSwitch = true;
        switchEnabled.setChecked(repo.isCategoryEnabled(currentCategory));
        updatingSwitch = false;
        boolean builtIn = repo.isBuiltInCategory(currentCategory);
        btnDeleteCategory.setVisibility(builtIn ? View.GONE : View.VISIBLE);
        int hidden = repo.countHidden(currentCategory);
        btnRestore.setVisibility(builtIn && hidden > 0 ? View.VISIBLE : View.GONE);
        btnRestore.setText(hidden == 1 ? "Restaurar 1 palabra oculta" : "Restaurar " + hidden + " palabras ocultas");

        List<String> words = repo.getWords(currentCategory);
        String count = words.size() + (words.size() == 1 ? " palabra" : " palabras");
        if (builtIn) {
            count += " · " + repo.countPairs(currentCategory) + " parejas Undercover";
        } else {
            count += words.size() >= 2 ? " · parejas al azar en Undercover" : " · categoría tuya";
        }
        txtCount.setText(count);
        txtEmpty.setVisibility(words.isEmpty() ? View.VISIBLE : View.GONE);

        wordChips.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (final String word : words) {
            View chip = inflater.inflate(R.layout.item_word_chip, wordChips, false);
            boolean custom = !repo.isBuiltInWord(currentCategory, word);
            TextView txt = chip.findViewById(R.id.txtWord);
            txt.setText(word);
            ImageButton del = chip.findViewById(R.id.btnDelete);
            del.setContentDescription((custom ? "Eliminar " : "Ocultar ") + word);
            if (custom) {
                chip.setBackgroundResource(R.drawable.bg_chip_custom);
                txt.setTextColor(Ui.color(this, R.color.amber));
                txt.setTypeface(txt.getTypeface(), android.graphics.Typeface.BOLD);
                del.setImageResource(R.drawable.ic_close_amber);
            }
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteWord(word);
                }
            });
            wordChips.addView(chip);
        }
        Ui.applyFonts(wordChips);
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
    }

    private void newCategoryDialog() {
        final EditText input = new EditText(this);
        input.setHint("Nombre de la categoría");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
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
}
