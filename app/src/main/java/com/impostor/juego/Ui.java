package com.impostor.juego;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Tipografías y utilidades de vista compartidas por todas las pantallas. */
public final class Ui {

    private static Typeface display, regular, bold;

    private Ui() {
    }

    private static void load(Context context) {
        if (display != null) return;
        AssetManager assets = context.getApplicationContext().getAssets();
        display = Typeface.createFromAsset(assets, "fonts/unbounded_extrabold.ttf");
        regular = Typeface.createFromAsset(assets, "fonts/dmsans_regular.ttf");
        bold = Typeface.createFromAsset(assets, "fonts/dmsans_bold.ttf");
    }

    /**
     * Aplica las fuentes de la app a una vista y sus hijas: Unbounded a los textos con tag
     * "display" (estilo Display) y DM Sans al resto, en negrita si el texto ya era negrita.
     */
    public static void applyFonts(View view) {
        load(view.getContext());
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if ("display".equals(tv.getTag())) {
                tv.setTypeface(display);
            } else {
                Typeface current = tv.getTypeface();
                if (current == bold || current == regular) return; // ya aplicada
                tv.setTypeface(current != null && current.isBold() ? bold : regular);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyFonts(group.getChildAt(i));
        }
    }

    public static void setBold(TextView tv, boolean isBold) {
        load(tv.getContext());
        tv.setTypeface(isBold ? bold : regular);
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static int color(Context context, int resId) {
        return context.getResources().getColor(resId);
    }
}
