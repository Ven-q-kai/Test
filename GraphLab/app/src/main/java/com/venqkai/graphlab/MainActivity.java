package com.venqkai.graphlab;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 13, 18);
    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int PANEL_2 = Color.rgb(24, 29, 40);
    private static final int TEXT = Color.rgb(239, 242, 248);
    private static final int MUTED = Color.rgb(153, 163, 184);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int ACCENT = Color.rgb(124, 157, 255);
    private static final int DANGER = Color.rgb(255, 112, 99);

    private GraphView graphView;
    private LinearLayout equationsContainer;
    private EditText minXInput;
    private EditText maxXInput;
    private TextView statusText;
    private int functionCounter = 0;
    private SharedPreferences preferences;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);

        preferences = getSharedPreferences("graphlab", MODE_PRIVATE);
        setContentView(buildUi());
        loadState();
        plot(false);
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(10), dp(16), dp(14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(2), dp(4), dp(2), dp(12));

        TextView title = text("GraphLab", 25, TEXT, true);
        TextView subtitle = text("Digite suas funções e compare várias curvas no mesmo plano.", 13, MUTED, false);
        subtitle.setPadding(0, dp(2), 0, 0);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        graphView = new GraphView(this);
        graphView.setBackground(rounded(Color.rgb(14, 17, 24), 18, BORDER, 1));
        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(355));
        graphParams.bottomMargin = dp(12);
        root.addView(graphView, graphParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scrollParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(20));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(sectionLabel("FUNÇÕES"));

        equationsContainer = new LinearLayout(this);
        equationsContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(equationsContainer);

        Button addFunction = secondaryButton("+ Adicionar função");
        addFunction.setOnClickListener(v -> addEquationRow(""));
        LinearLayout.LayoutParams addParams = fullWidthParams(dp(44));
        addParams.topMargin = dp(6);
        addParams.bottomMargin = dp(18);
        content.addView(addFunction, addParams);

        content.addView(sectionLabel("INTERVALO DO EIXO X"));
        LinearLayout rangeRow = new LinearLayout(this);
        rangeRow.setOrientation(LinearLayout.HORIZONTAL);
        rangeRow.setWeightSum(2f);

        minXInput = numberInput("-10");
        maxXInput = numberInput("10");
        LinearLayout.LayoutParams halfA = new LinearLayout.LayoutParams(0, dp(52), 1f);
        halfA.rightMargin = dp(6);
        LinearLayout.LayoutParams halfB = new LinearLayout.LayoutParams(0, dp(52), 1f);
        halfB.leftMargin = dp(6);
        rangeRow.addView(labeledBox("Mínimo", minXInput), halfA);
        rangeRow.addView(labeledBox("Máximo", maxXInput), halfB);
        content.addView(rangeRow);

        TextView hint = text("Sintaxe: x, + − × ÷, ^, %, (), !, pi, e, sin, cos, tan, sqrt, cbrt, abs, ln, log, exp, min, max, pow e root. Trigonometria usa radianos.", 12, MUTED, false);
        hint.setPadding(dp(2), dp(10), dp(2), dp(14));
        content.addView(hint);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button plotButton = primaryButton("Plotar");
        plotButton.setOnClickListener(v -> plot(true));
        Button resetButton = secondaryButton("Reenquadrar");
        resetButton.setOnClickListener(v -> graphView.resetViewport());

        LinearLayout.LayoutParams buttonA = new LinearLayout.LayoutParams(0, dp(50), 1.35f);
        buttonA.rightMargin = dp(6);
        LinearLayout.LayoutParams buttonB = new LinearLayout.LayoutParams(0, dp(50), 1f);
        buttonB.leftMargin = dp(6);
        buttons.addView(plotButton, buttonA);
        buttons.addView(resetButton, buttonB);
        content.addView(buttons);

        statusText = text("Arraste para mover • pinça para zoom • dois toques para reenquadrar", 12, MUTED, false);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(dp(4), dp(12), dp(4), dp(2));
        content.addView(statusText);

        return root;
    }

    private void addEquationRow(String initial) {
        final int colorIndex = functionCounter++;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(rounded(PANEL, 14, BORDER, 1));
        row.setPadding(dp(10), dp(7), dp(8), dp(7));
        row.setTag("equation-row");

        View colorDot = new View(this);
        colorDot.setBackground(rounded(GraphView.colorFor(colorIndex), 100, 0, 0));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.rightMargin = dp(10);
        row.addView(colorDot, dotParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(initial);
        input.setHint("ex.: x^2 + sin(x)");
        input.setHintTextColor(Color.rgb(99, 109, 130));
        input.setTextColor(TEXT);
        input.setTextSize(16);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setPadding(dp(10), 0, dp(10), 0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setTag("equation-input");
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, dp(46), 1f);
        row.addView(input, inputParams);

        Button delete = new Button(this);
        delete.setText("×");
        delete.setTextColor(MUTED);
        delete.setTextSize(22);
        delete.setGravity(Gravity.CENTER);
        delete.setPadding(0, 0, 0, dp(2));
        delete.setMinWidth(0);
        delete.setMinHeight(0);
        delete.setBackground(rounded(PANEL_2, 11, 0, 0));
        delete.setOnClickListener(v -> {
            if (equationsContainer.getChildCount() > 1) equationsContainer.removeView(row);
            else input.setText("");
        });
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        deleteParams.leftMargin = dp(6);
        row.addView(delete, deleteParams);

        LinearLayout.LayoutParams rowParams = fullWidthParams(ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(8);
        equationsContainer.addView(row, rowParams);
    }

    private void plot(boolean showMessages) {
        List<String> formulas = collectFormulas();
        if (formulas.isEmpty()) {
            status("Adicione pelo menos uma função.", DANGER);
            if (showMessages) Toast.makeText(this, "Digite uma função para plotar.", Toast.LENGTH_SHORT).show();
            return;
        }

        double minX;
        double maxX;
        try {
            minX = parseDouble(minXInput.getText().toString());
            maxX = parseDouble(maxXInput.getText().toString());
            if (!Double.isFinite(minX) || !Double.isFinite(maxX) || minX >= maxX) {
                throw new IllegalArgumentException("O mínimo precisa ser menor que o máximo.");
            }
            if (Math.abs(maxX - minX) < 1e-10 || Math.abs(maxX - minX) > 1e12) {
                throw new IllegalArgumentException("Intervalo fora de uma faixa útil.");
            }
        } catch (RuntimeException ex) {
            status("Intervalo inválido.", DANGER);
            if (showMessages) Toast.makeText(this, "Intervalo X inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> valid = new ArrayList<>();
        String firstError = null;
        for (String formula : formulas) {
            try {
                new Expression(formula);
                valid.add(formula);
            } catch (RuntimeException ex) {
                if (firstError == null) firstError = formula + ": " + ex.getMessage();
            }
        }

        if (valid.isEmpty()) {
            status("Nenhuma função válida.", DANGER);
            if (showMessages && firstError != null) Toast.makeText(this, firstError, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            graphView.setFunctions(valid, minX, maxX);
            saveState(minX, maxX);
            if (firstError == null) {
                status(valid.size() + (valid.size() == 1 ? " função plotada" : " funções plotadas"), ACCENT);
            } else {
                status("Algumas funções foram ignoradas por erro de sintaxe.", DANGER);
                if (showMessages) Toast.makeText(this, firstError, Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException ex) {
            status("Não consegui desenhar essa expressão.", DANGER);
            if (showMessages) Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadState() {
        String stored = preferences.getString("equations", "");
        String min = preferences.getString("minX", "-10");
        String max = preferences.getString("maxX", "10");
        minXInput.setText(min);
        maxXInput.setText(max);

        if (stored == null || stored.trim().isEmpty()) {
            addEquationRow("");
            return;
        }
        String[] parts = stored.split("\\n", -1);
        for (String part : parts) {
            if (!part.trim().isEmpty()) addEquationRow(part);
        }
        if (equationsContainer.getChildCount() == 0) addEquationRow("");
    }

    private void saveState(double minX, double maxX) {
        StringBuilder sb = new StringBuilder();
        for (String formula : collectFormulas()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(formula);
        }
        preferences.edit()
                .putString("equations", sb.toString())
                .putString("minX", trimDouble(minX))
                .putString("maxX", trimDouble(maxX))
                .apply();
    }

    private List<String> collectFormulas() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < equationsContainer.getChildCount(); i++) {
            View child = equationsContainer.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            for (int j = 0; j < row.getChildCount(); j++) {
                View item = row.getChildAt(j);
                if (item instanceof EditText && "equation-input".equals(item.getTag())) {
                    String value = ((EditText) item).getText().toString().trim();
                    if (!value.isEmpty()) result.add(value);
                }
            }
        }
        return result;
    }

    private LinearLayout labeledBox(String label, EditText input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(PANEL, 13, BORDER, 1));
        box.setPadding(dp(12), dp(6), dp(10), dp(4));
        TextView labelView = text(label, 10, MUTED, true);
        box.addView(labelView);
        box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
        return box;
    }

    private EditText numberInput(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(99, 109, 130));
        input.setTextColor(TEXT);
        input.setTextSize(16);
        input.setPadding(0, 0, 0, 0);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return input;
    }

    private TextView sectionLabel(String value) {
        TextView label = text(value, 11, MUTED, true);
        label.setLetterSpacing(0.12f);
        label.setPadding(dp(2), 0, 0, dp(7));
        return label;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(10, 13, 20));
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setBackground(rounded(ACCENT, 14, 0, 0));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setBackground(rounded(PANEL_2, 14, BORDER, 1));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams fullWidthParams(int height) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private void status(String value, int color) {
        statusText.setText(value);
        statusText.setTextColor(color);
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.trim().replace(',', '.'));
    }

    private String trimDouble(double value) {
        if (Math.rint(value) == value) return Long.toString((long) value);
        return Double.toString(value);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
