package com.venqkai.graphlab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public final class MathKeyboardView extends LinearLayout {
    public interface Listener {
        void onInsert(String text, int cursorBack);
    }

    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int KEY = Color.rgb(27, 33, 45);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int TEXT = Color.rgb(239, 242, 248);

    public MathKeyboardView(Context context, Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(8), dp(8), dp(8));
        setBackground(rounded(PANEL, 13, BORDER, 1));

        addView(keyRow(listener, new Key[] {
                new Key("x", "x", 0),
                new Key("x²", "x^2", 0),
                new Key("^", "^", 0),
                new Key("√", "sqrt()", 1),
                new Key("π", "pi", 0),
                new Key("e", "e", 0),
                new Key("(", "(", 0),
                new Key(")", ")", 0)
        }));

        addView(keyRow(listener, new Key[] {
                new Key("sin", "sin()", 1),
                new Key("cos", "cos()", 1),
                new Key("tan", "tan()", 1),
                new Key("log", "log()", 1),
                new Key("ln", "ln()", 1),
                new Key("abs", "abs()", 1),
                new Key("+", "+", 0),
                new Key("−", "-", 0),
                new Key("×", "*", 0),
                new Key("÷", "/", 0)
        }));
    }

    private HorizontalScrollView keyRow(Listener listener, Key[] keys) {
        HorizontalScrollView scroll = new HorizontalScrollView(getContext());
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        for (Key key : keys) {
            Button b = new Button(getContext());
            b.setText(key.label);
            b.setTextColor(TEXT);
            b.setTextSize(13);
            b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            b.setAllCaps(false);
            b.setMinWidth(0);
            b.setMinHeight(0);
            b.setPadding(dp(10), 0, dp(10), 0);
            b.setBackground(rounded(KEY, 10, BORDER, 1));
            b.setOnClickListener(v -> listener.onInsert(key.text, key.cursorBack));

            LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT, dp(38));
            p.rightMargin = dp(6);
            p.bottomMargin = dp(5);
            row.addView(b, p);
        }

        scroll.addView(row, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private static final class Key {
        final String label;
        final String text;
        final int cursorBack;

        Key(String label, String text, int cursorBack) {
            this.label = label;
            this.text = text;
            this.cursorBack = cursorBack;
        }
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (width > 0) d.setStroke(dp(width), stroke);
        return d;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
