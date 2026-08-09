package com.venqkai.graphlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
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
    private static final int SUCCESS = Color.rgb(88, 214, 141);

    private GraphView graphView;
    private LinearLayout equationsContainer;
    private EditText minXInput;
    private EditText maxXInput;
    private TextView statusText;
    private SharedPreferences preferences;
    private final List<EquationRow> rows = new ArrayList<>();
    private int functionCounter = 0;

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

    @Override protected void onPause() {
        super.onPause();
        saveStateRaw();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(10), dp(16), dp(14));

        LinearLayout headerLine = new LinearLayout(this);
        headerLine.setOrientation(LinearLayout.HORIZONTAL);
        headerLine.setGravity(Gravity.CENTER_VERTICAL);
        headerLine.setPadding(dp(2), dp(4), dp(2), 0);

        TextView title = text("GraphLab", 25, TEXT, true);
        headerLine.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView version = text("1.0.1", 11, ACCENT, true);
        version.setGravity(Gravity.CENTER);
        version.setBackground(rounded(Color.rgb(27, 35, 55), 20, Color.rgb(59, 76, 116), 1));
        version.setPadding(dp(10), dp(5), dp(10), dp(5));
        headerLine.addView(version);
        root.addView(headerLine);

        TextView subtitle = text("Compare funções, personalize as curvas e inspecione coordenadas.", 13, MUTED, false);
        subtitle.setPadding(dp(2), dp(2), dp(2), dp(12));
        root.addView(subtitle);

        graphView = new GraphView(this);
        graphView.setBackground(rounded(Color.rgb(14, 17, 24), 18, BORDER, 1));
        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(355));
        graphParams.bottomMargin = dp(7);
        root.addView(graphView, graphParams);

        TextView graphHint = text("Toque para medir x/y • arraste para mover • pinça para zoom • dois toques para reenquadrar", 11, MUTED, false);
        graphHint.setGravity(Gravity.CENTER_HORIZONTAL);
        graphHint.setPadding(dp(4), 0, dp(4), dp(10));
        root.addView(graphHint);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

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
        addFunction.setOnClickListener(v -> addEquationRow("", "", functionCounter++, true));
        LinearLayout.LayoutParams addParams = fullWidthParams(dp(44));
        addParams.topMargin = dp(4);
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

        TextView hint = text("Sintaxe: x, + − × ÷, ^, %, (), !, pi, e, tau, sin, cos, tan, sqrt, cbrt, abs, ln, log, exp, min, max, pow e root. Trigonometria usa radianos.", 12, MUTED, false);
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

        statusText = text("Pronto.", 12, MUTED, false);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        statusText.setPadding(dp(4), dp(12), dp(4), dp(2));
        content.addView(statusText);

        return root;
    }

    private void addEquationRow(String name, String formula, int colorIndex, boolean visible) {
        EquationRow row = new EquationRow(name, formula, colorIndex, visible);
        rows.add(row);
        equationsContainer.addView(row.root, row.params());
        functionCounter = Math.max(functionCounter, colorIndex + 1);
    }

    private final class EquationRow {
        final LinearLayout root;
        final EditText nameInput;
        final EditText formulaInput;
        final Button colorButton;
        final Button visibilityButton;
        final TextView errorText;
        int colorIndex;
        boolean visible;

        EquationRow(String name, String formula, int colorIndex, boolean visible) {
            this.colorIndex = Math.floorMod(colorIndex, GraphView.paletteSize());
            this.visible = visible;

            root = new LinearLayout(MainActivity.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackground(rounded(PANEL, 15, BORDER, 1));
            root.setPadding(dp(10), dp(9), dp(10), dp(9));

            LinearLayout top = new LinearLayout(MainActivity.this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            colorButton = new Button(MainActivity.this);
            colorButton.setText("");
            colorButton.setMinWidth(0);
            colorButton.setMinHeight(0);
            colorButton.setPadding(0, 0, 0, 0);
            colorButton.setOnClickListener(v -> showColorPicker(this));
            refreshColor();
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            colorParams.rightMargin = dp(8);
            top.addView(colorButton, colorParams);

            nameInput = new EditText(MainActivity.this);
            nameInput.setSingleLine(true);
            nameInput.setText(name);
            nameInput.setHint("Nome (opcional)");
            nameInput.setHintTextColor(Color.rgb(99, 109, 130));
            nameInput.setTextColor(TEXT);
            nameInput.setTextSize(14);
            nameInput.setPadding(dp(8), 0, dp(8), 0);
            nameInput.setBackgroundColor(Color.TRANSPARENT);
            nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            top.addView(nameInput, new LinearLayout.LayoutParams(0, dp(40), 1f));

            visibilityButton = new Button(MainActivity.this);
            visibilityButton.setAllCaps(false);
            visibilityButton.setTextSize(11);
            visibilityButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            visibilityButton.setMinWidth(0);
            visibilityButton.setMinHeight(0);
            visibilityButton.setPadding(0, 0, 0, 0);
            visibilityButton.setOnClickListener(v -> {
                this.visible = !this.visible;
                refreshVisibility();
                plot(false);
            });
            refreshVisibility();
            LinearLayout.LayoutParams visibilityParams = new LinearLayout.LayoutParams(dp(58), dp(36));
            visibilityParams.leftMargin = dp(6);
            top.addView(visibilityButton, visibilityParams);

            Button delete = new Button(MainActivity.this);
            delete.setText("×");
            delete.setTextColor(MUTED);
            delete.setTextSize(21);
            delete.setGravity(Gravity.CENTER);
            delete.setPadding(0, 0, 0, dp(2));
            delete.setMinWidth(0);
            delete.setMinHeight(0);
            delete.setBackground(rounded(PANEL_2, 10, 0, 0));
            delete.setOnClickListener(v -> {
                if (rows.size() > 1) {
                    rows.remove(this);
                    equationsContainer.removeView(root);
                    plot(false);
                } else {
                    nameInput.setText("");
                    formulaInput.setText("");
                    clearError();
                }
            });
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(38), dp(36));
            deleteParams.leftMargin = dp(6);
            top.addView(delete, deleteParams);
            root.addView(top);

            formulaInput = new EditText(MainActivity.this);
            formulaInput.setSingleLine(true);
            formulaInput.setText(formula);
            formulaInput.setHint("ex.: x^2 + sin(x)");
            formulaInput.setHintTextColor(Color.rgb(99, 109, 130));
            formulaInput.setTextColor(TEXT);
            formulaInput.setTextSize(16);
            formulaInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            formulaInput.setPadding(dp(12), 0, dp(12), 0);
            formulaInput.setBackground(rounded(PANEL_2, 11, Color.rgb(38, 45, 61), 1));
            LinearLayout.LayoutParams formulaParams = fullWidthParams(dp(46));
            formulaParams.topMargin = dp(7);
            root.addView(formulaInput, formulaParams);

            errorText = text("", 11, DANGER, false);
            errorText.setPadding(dp(4), dp(6), dp(4), 0);
            errorText.setVisibility(View.GONE);
            root.addView(errorText);
        }

        LinearLayout.LayoutParams params() {
            LinearLayout.LayoutParams params = fullWidthParams(ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(8);
            return params;
        }

        void refreshColor() {
            colorButton.setBackground(rounded(GraphView.colorFor(colorIndex), 100, Color.argb(80, 255, 255, 255), 1));
        }

        void refreshVisibility() {
            if (visible) {
                visibilityButton.setText("ON");
                visibilityButton.setTextColor(SUCCESS);
                visibilityButton.setBackground(rounded(Color.rgb(22, 43, 38), 10, Color.rgb(50, 88, 72), 1));
            } else {
                visibilityButton.setText("OFF");
                visibilityButton.setTextColor(MUTED);
                visibilityButton.setBackground(rounded(PANEL_2, 10, BORDER, 1));
            }
        }

        void setColorIndex(int index) {
            colorIndex = Math.floorMod(index, GraphView.paletteSize());
            refreshColor();
        }

        void showError(String message) {
            errorText.setText(message);
            errorText.setVisibility(View.VISIBLE);
        }

        void clearError() {
            errorText.setText("");
            errorText.setVisibility(View.GONE);
        }
    }

    private void showColorPicker(EquationRow row) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(14), dp(8), dp(14), dp(8));
        List<Button> choices = new ArrayList<>();

        for (int i = 0; i < GraphView.paletteSize(); i++) {
            Button choice = new Button(this);
            choice.setText("");
            choice.setMinWidth(0);
            choice.setMinHeight(0);
            choice.setPadding(0, 0, 0, 0);
            int stroke = i == row.colorIndex ? Color.WHITE : Color.argb(50, 255, 255, 255);
            choice.setBackground(rounded(GraphView.colorFor(i), 100, stroke, i == row.colorIndex ? 2 : 1));
            choice.setTag(i);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = dp(50);
            p.height = dp(50);
            p.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(choice, p);
            choices.add(choice);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Cor da função")
                .setView(grid)
                .setNegativeButton("Cancelar", null)
                .create();

        for (Button choice : choices) {
            choice.setOnClickListener(v -> {
                row.setColorIndex((Integer) v.getTag());
                dialog.dismiss();
                plot(false);
            });
        }
        dialog.show();
    }

    private void plot(boolean showMessages) {
        double minX;
        double maxX;
        try {
            minX = parseDouble(minXInput.getText().toString());
            maxX = parseDouble(maxXInput.getText().toString());
            if (!Double.isFinite(minX) || !Double.isFinite(maxX) || minX >= maxX) {
                throw new IllegalArgumentException();
            }
            if (Math.abs(maxX - minX) < 1e-10 || Math.abs(maxX - minX) > 1e12) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            status("Intervalo X inválido. O mínimo precisa ser menor que o máximo.", DANGER);
            if (showMessages) Toast.makeText(this, "Revise o intervalo do eixo X.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<GraphView.FunctionSpec> valid = new ArrayList<>();
        boolean anyFormula = false;
        boolean hadError = false;
        int visibleCount = 0;

        for (EquationRow row : rows) {
            row.clearError();
            String formula = row.formulaInput.getText().toString().trim();
            if (formula.isEmpty()) continue;
            anyFormula = true;

            try {
                new Expression(formula);
                String name = row.nameInput.getText().toString().trim();
                GraphView.FunctionSpec spec = new GraphView.FunctionSpec(
                        name, formula, GraphView.colorFor(row.colorIndex), row.visible);
                valid.add(spec);
                if (row.visible) visibleCount++;
            } catch (RuntimeException ex) {
                hadError = true;
                row.showError(humanizeError(ex.getMessage()));
            }
        }

        if (!anyFormula) {
            status("Adicione pelo menos uma função.", DANGER);
            if (showMessages) Toast.makeText(this, "Digite uma função para plotar.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (valid.isEmpty()) {
            status("Nenhuma função válida. Veja o erro indicado abaixo da equação.", DANGER);
            return;
        }

        try {
            graphView.setFunctions(valid, minX, maxX);
            saveStateRaw();
            if (hadError) {
                status("Algumas funções têm erro. As válidas continuam no gráfico.", DANGER);
            } else if (visibleCount == 0) {
                status("Todas as funções estão ocultas. Ative pelo menos uma com ON.", MUTED);
            } else {
                status(visibleCount + (visibleCount == 1 ? " curva visível" : " curvas visíveis") + " • toque no gráfico para medir", ACCENT);
            }
        } catch (RuntimeException ex) {
            status("Não consegui desenhar uma das expressões.", DANGER);
            if (showMessages) Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String humanizeError(String message) {
        if (message == null || message.trim().isEmpty()) return "Expressão inválida.";
        String result = message;
        result = result.replace("Esperado ')'", "Falta ')' para fechar a expressão");
        result = result.replace("Esperado '('", "Falta '('");
        result = result.replace("Expressão incompleta", "A expressão terminou antes do esperado");
        if (!result.endsWith(".")) result += ".";
        return result;
    }

    private void loadState() {
        String min = preferences.getString("minX", "-10");
        String max = preferences.getString("maxX", "10");
        minXInput.setText(min == null ? "-10" : min);
        maxXInput.setText(max == null ? "10" : max);

        String storedV101 = preferences.getString("functions_v101", "");
        if (storedV101 != null && !storedV101.trim().isEmpty()) {
            String[] lines = storedV101.split("\\n");
            for (String line : lines) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) continue;
                try {
                    String name = decode(parts[0]);
                    String formula = decode(parts[1]);
                    int colorIndex = Integer.parseInt(parts[2]);
                    boolean visible = "1".equals(parts[3]);
                    addEquationRow(name, formula, colorIndex, visible);
                } catch (RuntimeException ignored) {}
            }
        }

        if (!rows.isEmpty()) return;

        String oldStored = preferences.getString("equations", "");
        if (oldStored != null && !oldStored.trim().isEmpty()) {
            String[] parts = oldStored.split("\\n", -1);
            for (String formula : parts) {
                if (!formula.trim().isEmpty()) addEquationRow("", formula, functionCounter++, true);
            }
        }

        if (rows.isEmpty()) addEquationRow("", "", functionCounter++, true);
    }

    private void saveStateRaw() {
        if (preferences == null || minXInput == null || maxXInput == null) return;
        StringBuilder sb = new StringBuilder();
        for (EquationRow row : rows) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(encode(row.nameInput.getText().toString())).append('|')
                    .append(encode(row.formulaInput.getText().toString())).append('|')
                    .append(row.colorIndex).append('|')
                    .append(row.visible ? '1' : '0');
        }

        preferences.edit()
                .putString("functions_v101", sb.toString())
                .putString("minX", minXInput.getText().toString().trim())
                .putString("maxX", maxXInput.getText().toString().trim())
                .apply();
    }

    private String encode(String value) {
        return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    private String decode(String value) {
        return new String(Base64.decode(value, Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8);
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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
