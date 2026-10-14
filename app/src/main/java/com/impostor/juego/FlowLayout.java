package com.impostor.juego;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Coloca a sus hijos en filas y pasa a la siguiente cuando no caben (chips de categorías y palabras). */
public class FlowLayout extends ViewGroup {

    private final int gap;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = Ui.dp(context, 8);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = child.getMeasuredWidth();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            x += w + gap;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
        }
        int height = y + rowHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxWidth = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int w = child.getMeasuredWidth(), h = child.getMeasuredHeight();
            if (x > 0 && x + w > maxWidth) {
                x = 0;
                y += rowHeight + gap;
                rowHeight = 0;
            }
            child.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + gap;
            rowHeight = Math.max(rowHeight, h);
        }
    }
}
