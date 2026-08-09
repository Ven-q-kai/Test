package com.venqkai.graphlab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FunctionRowView extends LinearLayout {
    public interface Listener {
        void onChanged();
        void onDelete(FunctionRowView row);
        void onColorRequested(FunctionRowView row);
    }

    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int PANEL_2 = Color.rgb(24, 29, 40);
    private static final int TEXT = Color.rgb(239, 242, 248);
    private static final int MUTED = Color.rgb(153, 163, 184);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int DANGER = Color.rgb(255, 112, 99);
    private static final int SUCCESS = Color.rgb(88, 214, 141);

    private final EditText nameInput;
    private final EditText formulaInput;
    private final Button colorButton;
    private final Button visibilityButton;
    private final TextView errorText;
    private int colorIndex;
    private boolean visible;

    public FunctionRowView(Context context, String name, String formula, int colorIndex, boolean visible, Listener listener) {
        super(context);
        this.colorIndex = Math.floorMod(colorIndex, GraphView.paletteSize());
        this.visible = visible;
        setOrientation(VERTICAL);
        setPadding(dp(10), dp(9), dp(10), dp(9));
        setBackground(rounded(PANEL, 15, BORDER, 1));

        nameInput = input(name, "Nome (opcional)", 14, false);
        formulaInput = input(formula, "ex.: x^2 + sin(x)", 16, true);
        colorButton = new Button(context);
        visibilityButton = new Button(context);
        Button deleteButton = new Button(context);
        errorText = new TextView(context);

        colorButton.setMinWidth(0); colorButton.setMinHeight(0); colorButton.setPadding(0,0,0,0);
        visibilityButton.setAllCaps(false); visibilityButton.setTextSize(11); visibilityButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        visibilityButton.setMinWidth(0); visibilityButton.setMinHeight(0); visibilityButton.setPadding(0,0,0,0);
        deleteButton.setText("×"); deleteButton.setTextColor(MUTED); deleteButton.setTextSize(21); deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setMinWidth(0); deleteButton.setMinHeight(0); deleteButton.setPadding(0,0,0,dp(2));
        deleteButton.setBackground(rounded(PANEL_2, 10, 0, 0));

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams cp = new LayoutParams(dp(36), dp(36)); cp.rightMargin = dp(8); top.addView(colorButton, cp);
        top.addView(nameInput, new LayoutParams(0, dp(40), 1f));
        LayoutParams vp = new LayoutParams(dp(58), dp(36)); vp.leftMargin = dp(6); top.addView(visibilityButton, vp);
        LayoutParams dp = new LayoutParams(this.dp(38), this.dp(36)); dp.leftMargin = this.dp(6); top.addView(deleteButton, dp);
        addView(top);

        LayoutParams fp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)); fp.topMargin = dp(7);
        formulaInput.setBackground(rounded(PANEL_2, 11, Color.rgb(38,45,61), 1));
        addView(formulaInput, fp);
        errorText.setTextColor(DANGER); errorText.setTextSize(11); errorText.setPadding(dp(4),dp(6),dp(4),0); errorText.setVisibility(GONE);
        addView(errorText);

        refreshColor(); refreshVisibility();
        colorButton.setOnClickListener(v -> listener.onColorRequested(this));
        visibilityButton.setOnClickListener(v -> { this.visible = !this.visible; refreshVisibility(); listener.onChanged(); });
        deleteButton.setOnClickListener(v -> listener.onDelete(this));
    }

    public String getNameValue() { return nameInput.getText().toString().trim(); }
    public String getFormula() { return formulaInput.getText().toString().trim(); }
    public int getColorIndex() { return colorIndex; }
    public boolean isVisibleFunction() { return visible; }
    public void clearFields() { nameInput.setText(""); formulaInput.setText(""); clearError(); }
    public void setColorIndex(int index) { colorIndex = Math.floorMod(index, GraphView.paletteSize()); refreshColor(); }
    public void showError(String message) { errorText.setText(message); errorText.setVisibility(VISIBLE); }
    public void clearError() { errorText.setText(""); errorText.setVisibility(GONE); }

    private EditText input(String value, String hint, int size, boolean boxed) {
        EditText e = new EditText(getContext());
        e.setSingleLine(true); e.setText(value); e.setHint(hint); e.setTextColor(TEXT); e.setHintTextColor(Color.rgb(99,109,130));
        e.setTextSize(size); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setPadding(boxed ? dp(12) : dp(8), 0, boxed ? dp(12) : dp(8), 0);
        if (!boxed) e.setBackgroundColor(Color.TRANSPARENT);
        return e;
    }

    private void refreshColor() { colorButton.setBackground(rounded(GraphView.colorFor(colorIndex), 100, Color.argb(80,255,255,255), 1)); }
    private void refreshVisibility() {
        if (visible) {
            visibilityButton.setText("ON"); visibilityButton.setTextColor(SUCCESS);
            visibilityButton.setBackground(rounded(Color.rgb(22,43,38), 10, Color.rgb(50,88,72), 1));
        } else {
            visibilityButton.setText("OFF"); visibilityButton.setTextColor(MUTED);
            visibilityButton.setBackground(rounded(PANEL_2, 10, BORDER, 1));
        }
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius));
        if (width > 0) d.setStroke(dp(width), stroke); return d;
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
