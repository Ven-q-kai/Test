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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public final class TablePanelView extends LinearLayout {
    public interface Provider {
        List<AnalysisEngine.FunctionInput> getVisibleFunctions();
        double getMinX();
        double getMaxX();
    }

    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int PANEL2 = Color.rgb(24, 29, 40);
    private static final int TEXT = Color.rgb(239, 242, 248);
    private static final int MUTED = Color.rgb(153, 163, 184);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int ACCENT = Color.rgb(124, 157, 255);

    private final Provider provider;
    private final EditText rowsInput;
    private final TextView output;

    public TablePanelView(Context context, Provider provider) {
        super(context);
        this.provider = provider;
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(2), dp(2), dp(24));

        TextView title = text("Tabela de valores", 19, TEXT, true);
        addView(title);
        TextView help = text("Gera valores igualmente espaçados no intervalo X atual. Até quatro funções aparecem lado a lado.", 12, MUTED, false);
        help.setPadding(0, dp(3), 0, dp(12));
        addView(help);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setBackground(round(PANEL, 14, BORDER, 1));
        controls.setPadding(dp(12), dp(8), dp(8), dp(8));

        LinearLayout labelBox = new LinearLayout(context);
        labelBox.setOrientation(VERTICAL);
        labelBox.addView(text("LINHAS", 10, MUTED, true));
        rowsInput = new EditText(context);
        rowsInput.setSingleLine(true);
        rowsInput.setText("11");
        rowsInput.setTextColor(TEXT);
        rowsInput.setTextSize(16);
        rowsInput.setBackgroundColor(Color.TRANSPARENT);
        rowsInput.setPadding(0, 0, 0, 0);
        rowsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        labelBox.addView(rowsInput, new LinearLayout.LayoutParams(dp(70), dp(30)));
        controls.addView(labelBox, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button generate = new Button(context);
        generate.setText("Gerar tabela");
        generate.setAllCaps(false);
        generate.setTextColor(Color.rgb(10, 13, 20));
        generate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        generate.setMinWidth(0);
        generate.setMinHeight(0);
        generate.setBackground(round(ACCENT, 12, 0, 0));
        generate.setOnClickListener(v -> refresh());
        controls.addView(generate, new LinearLayout.LayoutParams(dp(130), dp(42)));
        addView(controls, new LayoutParams(-1, -2));

        HorizontalScrollView horizontal = new HorizontalScrollView(context);
        horizontal.setFillViewport(false);
        LayoutParams hp = new LayoutParams(-1, -2);
        hp.topMargin = dp(10);
        addView(horizontal, hp);

        output = text("Abra esta aba com funções válidas e toque em Gerar tabela.", 13, TEXT, false);
        output.setTypeface(Typeface.MONOSPACE);
        output.setTextIsSelectable(true);
        output.setPadding(dp(12), dp(12), dp(12), dp(12));
        output.setBackground(round(PANEL2, 14, BORDER, 1));
        horizontal.addView(output, new HorizontalScrollView.LayoutParams(-2, -2));
    }

    public void refresh() {
        List<AnalysisEngine.FunctionInput> functions = provider.getVisibleFunctions();
        if (functions.isEmpty()) {
            output.setText("Nenhuma função visível e válida para tabelar.");
            return;
        }

        int rows;
        try { rows = Integer.parseInt(rowsInput.getText().toString().trim()); }
        catch (Exception ex) { rows = 11; }
        rows = Math.max(3, Math.min(101, rows));
        rowsInput.setText(String.valueOf(rows));

        double minX = provider.getMinX();
        double maxX = provider.getMaxX();
        int columns = Math.min(4, functions.size());
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(java.util.Locale.US, "%-12s", "x"));
        for (int i = 0; i < columns; i++) {
            String name = shortName(functions.get(i).name);
            sb.append(String.format(java.util.Locale.US, "%-16s", name));
        }
        sb.append('\n');
        sb.append(repeat('─', 12 + columns * 16)).append('\n');

        Expression[] expressions = new Expression[columns];
        for (int i = 0; i < columns; i++) expressions[i] = new Expression(functions.get(i).formula);

        for (int r = 0; r < rows; r++) {
            double x = minX + (maxX - minX) * r / (rows - 1.0);
            sb.append(String.format(java.util.Locale.US, "%-12s", AnalysisEngine.format(x)));
            for (Expression expression : expressions) {
                double y;
                try { y = expression.eval(x); }
                catch (RuntimeException ex) { y = Double.NaN; }
                sb.append(String.format(java.util.Locale.US, "%-16s", AnalysisEngine.format(y)));
            }
            sb.append('\n');
        }

        if (functions.size() > columns) {
            sb.append("\n+").append(functions.size() - columns).append(" função(ões) não exibida(s) para manter a tabela legível.");
        }
        output.setText(sb.toString());
    }

    private String shortName(String value) {
        String s = value == null || value.trim().isEmpty() ? "f(x)" : value.trim();
        return s.length() > 13 ? s.substring(0, 12) + "…" : s;
    }

    private String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radius, int stroke, int width) {
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
