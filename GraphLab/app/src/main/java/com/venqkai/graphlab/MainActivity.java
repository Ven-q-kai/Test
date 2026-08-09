package com.venqkai.graphlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

public final class MainActivity extends Activity implements FunctionRowView.Listener, MathKeyboardView.Listener {
    private static final int BG = Color.rgb(11, 13, 18);
    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int PANEL2 = Color.rgb(24, 29, 40);
    private static final int TEXT = Color.rgb(239, 242, 248);
    private static final int MUTED = Color.rgb(153, 163, 184);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int ACCENT = Color.rgb(124, 157, 255);
    private static final int DANGER = Color.rgb(255, 112, 99);
    private static final int SUCCESS = Color.rgb(88, 214, 141);

    private final List<FunctionRowView> rows = new ArrayList<>();
    private final Handler autoHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoPlotTask = () -> plot(false);

    private LinearLayout root;
    private LinearLayout rowBox;
    private GraphView graph;
    private ScrollView scroll;
    private TextView subtitle;
    private TextView graphHint;
    private TextView status;
    private EditText minX;
    private EditText maxX;
    private EditText minY;
    private EditText maxY;
    private Button autoYButton;
    private Button fullscreenButton;
    private SharedPreferences prefs;
    private FunctionRowView activeRow;

    private int nextColor;
    private boolean autoY = true;
    private boolean fullscreen = false;
    private boolean loading = true;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        prefs = getSharedPreferences("graphlab", MODE_PRIVATE);
        setContentView(ui());
        load();
        loading = false;
        plot(false);
    }

    @Override protected void onPause() {
        super.onPause();
        save();
    }

    @Override public void onBackPressed() {
        if (fullscreen) {
            toggleFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    private View ui() {
        root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(10), dp(16), dp(14));

        LinearLayout head = row();
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text("GraphLab", 25, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));

        TextView ver = text("1.0.2", 11, ACCENT, true);
        ver.setPadding(dp(10), dp(5), dp(10), dp(5));
        ver.setBackground(round(Color.rgb(27, 35, 55), 20, Color.rgb(59, 76, 116), 1));
        head.addView(ver);

        fullscreenButton = secondary("⛶");
        fullscreenButton.setContentDescription("Alternar tela cheia do gráfico");
        fullscreenButton.setTextSize(18);
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(dp(44), dp(38));
        fp.leftMargin = dp(7);
        head.addView(fullscreenButton, fp);
        root.addView(head);

        subtitle = text("Edite e compare funções em tempo real.", 13, MUTED, false);
        subtitle.setPadding(dp(2), dp(3), 0, dp(12));
        root.addView(subtitle);

        graph = new GraphView(this);
        graph.setBackground(round(Color.rgb(14, 17, 24), 18, BORDER, 1));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(355));
        gp.bottomMargin = dp(7);
        root.addView(graph, gp);

        graphHint = text("Toque: medir • arraste a mira: inspecionar • arraste fora dela: mover • pinça: zoom", 11, MUTED, false);
        graphHint.setGravity(Gravity.CENTER);
        graphHint.setPadding(0, 0, 0, dp(10));
        root.addView(graphHint);

        scroll = new ScrollView(this);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout body = column();
        body.setPadding(0, dp(2), 0, dp(20));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        body.addView(section("FUNÇÕES"));
        rowBox = column();
        body.addView(rowBox);

        Button add = secondary("+ Adicionar função");
        add.setOnClickListener(v -> {
            addRow("", "", nextColor++, true);
            activeRow = rows.get(rows.size() - 1);
            activeRow.requestFormulaFocus();
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(44));
        ap.bottomMargin = dp(14);
        body.addView(add, ap);

        body.addView(section("TECLADO MATEMÁTICO"));
        MathKeyboardView keyboard = new MathKeyboardView(this, this);
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-1, -2);
        kp.bottomMargin = dp(18);
        body.addView(keyboard, kp);

        body.addView(section("INTERVALO DO EIXO X"));
        LinearLayout xRange = row();
        minX = number();
        maxX = number();
        LinearLayout.LayoutParams xa = new LinearLayout.LayoutParams(0, dp(58), 1);
        xa.rightMargin = dp(6);
        LinearLayout.LayoutParams xb = new LinearLayout.LayoutParams(0, dp(58), 1);
        xb.leftMargin = dp(6);
        xRange.addView(numberBox("Mínimo", minX), xa);
        xRange.addView(numberBox("Máximo", maxX), xb);
        body.addView(xRange);

        LinearLayout yHead = row();
        yHead.setGravity(Gravity.CENTER_VERTICAL);
        yHead.setPadding(0, dp(16), 0, dp(6));
        TextView yTitle = section("INTERVALO DO EIXO Y");
        yTitle.setPadding(dp(2), 0, 0, 0);
        yHead.addView(yTitle, new LinearLayout.LayoutParams(0, -2, 1));
        autoYButton = secondary("AUTO");
        autoYButton.setTextSize(11);
        autoYButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        autoYButton.setOnClickListener(v -> {
            autoY = !autoY;
            refreshYMode();
            plot(false);
        });
        yHead.addView(autoYButton, new LinearLayout.LayoutParams(dp(78), dp(36)));
        body.addView(yHead);

        LinearLayout yRange = row();
        minY = number();
        maxY = number();
        LinearLayout.LayoutParams ya = new LinearLayout.LayoutParams(0, dp(58), 1);
        ya.rightMargin = dp(6);
        LinearLayout.LayoutParams yb = new LinearLayout.LayoutParams(0, dp(58), 1);
        yb.leftMargin = dp(6);
        yRange.addView(numberBox("Mínimo Y", minY), ya);
        yRange.addView(numberBox("Máximo Y", maxY), yb);
        body.addView(yRange);

        TextView yHelp = text("AUTO ajusta o eixo Y às curvas. Desative para usar os limites acima.", 11, MUTED, false);
        yHelp.setPadding(dp(2), dp(7), 0, dp(10));
        body.addView(yHelp);

        TextView syntax = text("Aceita x, pi, e, tau, + − × ÷, ^, %, (), !, sin, cos, tan, sqrt, cbrt, abs, ln, log, exp, min, max, pow e root. Trigonometria usa radianos.", 12, MUTED, false);
        syntax.setPadding(dp(2), dp(5), 0, dp(14));
        body.addView(syntax);

        LinearLayout actions = row();
        Button go = primary("Plotar agora");
        go.setOnClickListener(v -> plot(true));
        Button reset = secondary("Reenquadrar");
        reset.setOnClickListener(v -> graph.resetViewport());
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dp(50), 1.35f);
        p1.rightMargin = dp(6);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(50), 1);
        p2.leftMargin = dp(6);
        actions.addView(go, p1);
        actions.addView(reset, p2);
        body.addView(actions);

        status = text("Pronto.", 12, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, 0);
        body.addView(status);

        watchRangeInputs(minX, maxX, minY, maxY);
        return root;
    }

    private void addRow(String name, String formula, int color, boolean visible) {
        FunctionRowView r = new FunctionRowView(this, name, formula, color, visible, this);
        rows.add(r);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        rowBox.addView(r, p);
        nextColor = Math.max(nextColor, color + 1);
        if (activeRow == null) activeRow = r;
    }

    @Override public void onChanged() {
        scheduleAutoPlot();
    }

    @Override public void onDelete(FunctionRowView r) {
        if (rows.size() == 1) {
            r.clearFields();
            activeRow = r;
            setStatus("Adicione uma função para plotar.", MUTED);
            return;
        }
        rows.remove(r);
        rowBox.removeView(r);
        if (activeRow == r) activeRow = rows.isEmpty() ? null : rows.get(0);
        scheduleAutoPlot();
    }

    @Override public void onDuplicate(FunctionRowView r) {
        addRow(r.getNameValue(), r.getFormula(), nextColor++, r.isVisibleFunction());
        activeRow = rows.get(rows.size() - 1);
        activeRow.requestFormulaFocus();
        setStatus("Função duplicada.", ACCENT);
        scheduleAutoPlot();
    }

    @Override public void onColorRequested(FunctionRowView r) {
        colorDialog(r);
    }

    @Override public void onFormulaFocused(FunctionRowView r) {
        activeRow = r;
    }

    @Override public void onInsert(String value, int cursorBack) {
        if (activeRow == null && !rows.isEmpty()) activeRow = rows.get(0);
        if (activeRow == null) return;
        activeRow.insertIntoFormula(value, cursorBack);
    }

    private void colorDialog(FunctionRowView target) {
        GridLayout g = new GridLayout(this);
        g.setColumnCount(4);
        g.setPadding(dp(14), dp(8), dp(14), dp(8));
        List<Button> buttons = new ArrayList<>();

        for (int i = 0; i < GraphView.paletteSize(); i++) {
            Button b = new Button(this);
            b.setTag(i);
            b.setMinWidth(0);
            b.setMinHeight(0);
            int stroke = i == target.getColorIndex() ? Color.WHITE : Color.argb(50, 255, 255, 255);
            b.setBackground(round(GraphView.colorFor(i), 100, stroke, i == target.getColorIndex() ? 2 : 1));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = dp(50);
            p.height = dp(50);
            p.setMargins(dp(6), dp(6), dp(6), dp(6));
            g.addView(b, p);
            buttons.add(b);
        }

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("Cor da função")
                .setView(g)
                .setNegativeButton("Cancelar", null)
                .create();

        for (Button b : buttons) {
            b.setOnClickListener(v -> {
                target.setColorIndex((Integer) v.getTag());
                d.dismiss();
                plot(false);
            });
        }
        d.show();
    }

    private void plot(boolean toast) {
        autoHandler.removeCallbacks(autoPlotTask);

        double lo;
        double hi;
        try {
            lo = num(minX.getText().toString());
            hi = num(maxX.getText().toString());
            if (!Double.isFinite(lo) || !Double.isFinite(hi) || lo >= hi || hi - lo > 1e12 || hi - lo < 1e-10) throw new Exception();
        } catch (Exception e) {
            setStatus("Intervalo X inválido. O mínimo precisa ser menor que o máximo.", DANGER);
            if (toast) Toast.makeText(this, "Revise o eixo X.", Toast.LENGTH_SHORT).show();
            return;
        }

        double yLo = -10;
        double yHi = 10;
        if (!autoY) {
            try {
                yLo = num(minY.getText().toString());
                yHi = num(maxY.getText().toString());
                if (!Double.isFinite(yLo) || !Double.isFinite(yHi) || yLo >= yHi || yHi - yLo > 1e12 || yHi - yLo < 1e-10) throw new Exception();
            } catch (Exception e) {
                setStatus("Intervalo Y inválido. Ative AUTO ou revise os limites.", DANGER);
                if (toast) Toast.makeText(this, "Revise o eixo Y.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        List<GraphView.FunctionSpec> ok = new ArrayList<>();
        boolean any = false;
        boolean errors = false;
        int visible = 0;

        for (FunctionRowView r : rows) {
            r.clearError();
            String f = r.getFormula();
            if (f.isEmpty()) continue;
            any = true;
            try {
                new Expression(f);
                ok.add(new GraphView.FunctionSpec(r.getNameValue(), f, GraphView.colorFor(r.getColorIndex()), r.isVisibleFunction()));
                if (r.isVisibleFunction()) visible++;
            } catch (RuntimeException e) {
                errors = true;
                r.showError(error(e.getMessage()));
            }
        }

        if (!any) {
            setStatus("Adicione pelo menos uma função.", MUTED);
            return;
        }
        if (ok.isEmpty()) {
            setStatus("Nenhuma função válida. Veja o erro na função.", DANGER);
            return;
        }

        graph.setFunctions(ok, lo, hi, autoY, yLo, yHi);
        save();

        if (errors) {
            setStatus("Algumas funções têm erro; as válidas continuam no gráfico.", DANGER);
        } else if (visible == 0) {
            setStatus("Todas as funções estão ocultas. Ative uma com ON.", MUTED);
        } else {
            setStatus(visible + (visible == 1 ? " curva visível" : " curvas visíveis") + " • atualização automática ativa", ACCENT);
        }
    }

    private void scheduleAutoPlot() {
        if (loading) return;
        autoHandler.removeCallbacks(autoPlotTask);
        autoHandler.postDelayed(autoPlotTask, 420);
    }

    private void watchRangeInputs(EditText... inputs) {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { scheduleAutoPlot(); }
        };
        for (EditText input : inputs) input.addTextChangedListener(watcher);
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        subtitle.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        graphHint.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        scroll.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        fullscreenButton.setText(fullscreen ? "↙" : "⛶");
        fullscreenButton.setContentDescription(fullscreen ? "Sair da tela cheia" : "Abrir gráfico em tela cheia");

        LinearLayout.LayoutParams gp = (LinearLayout.LayoutParams) graph.getLayoutParams();
        if (fullscreen) {
            gp.height = 0;
            gp.weight = 1f;
            gp.bottomMargin = 0;
            root.setPadding(dp(8), dp(6), dp(8), dp(8));
        } else {
            gp.height = dp(355);
            gp.weight = 0f;
            gp.bottomMargin = dp(7);
            root.setPadding(dp(16), dp(10), dp(16), dp(14));
        }
        graph.setLayoutParams(gp);
        graph.requestLayout();
    }

    private void refreshYMode() {
        if (autoYButton == null || minY == null || maxY == null) return;
        if (autoY) {
            autoYButton.setText("AUTO: ON");
            autoYButton.setTextColor(SUCCESS);
            autoYButton.setBackground(round(Color.rgb(22, 43, 38), 11, Color.rgb(50, 88, 72), 1));
        } else {
            autoYButton.setText("AUTO: OFF");
            autoYButton.setTextColor(TEXT);
            autoYButton.setBackground(round(PANEL2, 11, BORDER, 1));
        }
        minY.setEnabled(!autoY);
        maxY.setEnabled(!autoY);
        minY.setAlpha(autoY ? 0.45f : 1f);
        maxY.setAlpha(autoY ? 0.45f : 1f);
    }

    private String error(String m) {
        if (m == null || m.isEmpty()) return "Expressão inválida.";
        String s = m.replace("Esperado ')'", "Falta ')' para fechar a expressão")
                .replace("Expressão incompleta", "A expressão terminou antes do esperado");
        return s.endsWith(".") ? s : s + ".";
    }

    private void load() {
        minX.setText(prefs.getString("minX", "-10"));
        maxX.setText(prefs.getString("maxX", "10"));
        minY.setText(prefs.getString("minY", "-10"));
        maxY.setText(prefs.getString("maxY", "10"));
        autoY = prefs.getBoolean("autoY", true);
        refreshYMode();

        String saved = prefs.getString("functions_v101", "");
        if (saved != null && !saved.isEmpty()) {
            for (String line : saved.split("\\n")) {
                String[] p = line.split("\\|", -1);
                if (p.length == 4) {
                    try {
                        addRow(dec(p[0]), dec(p[1]), Integer.parseInt(p[2]), "1".equals(p[3]));
                    } catch (Exception ignored) {}
                }
            }
        }

        if (rows.isEmpty()) {
            String old = prefs.getString("equations", "");
            if (old != null && !old.trim().isEmpty()) {
                for (String f : old.split("\\n")) {
                    if (!f.trim().isEmpty()) addRow("", f, nextColor++, true);
                }
            }
        }

        if (rows.isEmpty()) addRow("", "", nextColor++, true);
        activeRow = rows.get(0);
    }

    private void save() {
        if (prefs == null || minX == null) return;
        StringBuilder s = new StringBuilder();
        for (FunctionRowView r : rows) {
            if (s.length() > 0) s.append('\n');
            s.append(enc(r.getNameValue())).append('|')
                    .append(enc(r.getFormula())).append('|')
                    .append(r.getColorIndex()).append('|')
                    .append(r.isVisibleFunction() ? '1' : '0');
        }

        prefs.edit()
                .putString("functions_v101", s.toString())
                .putString("minX", minX.getText().toString())
                .putString("maxX", maxX.getText().toString())
                .putString("minY", minY.getText().toString())
                .putString("maxY", maxY.getText().toString())
                .putBoolean("autoY", autoY)
                .apply();
    }

    private String enc(String s) {
        return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    private String dec(String s) {
        return new String(Base64.decode(s, Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8);
    }

    private LinearLayout numberBox(String label, EditText e) {
        LinearLayout b = column();
        b.setPadding(dp(12), dp(6), dp(10), dp(4));
        b.setBackground(round(PANEL, 13, BORDER, 1));
        b.addView(text(label, 10, MUTED, true));
        b.addView(e, new LinearLayout.LayoutParams(-1, dp(32)));
        return b;
    }

    private EditText number() {
        EditText e = new EditText(this);
        e.setSingleLine();
        e.setTextColor(TEXT);
        e.setTextSize(16);
        e.setPadding(0, 0, 0, 0);
        e.setBackgroundColor(Color.TRANSPARENT);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return e;
    }

    private TextView section(String s) {
        TextView t = text(s, 11, MUTED, true);
        t.setLetterSpacing(.12f);
        t.setPadding(dp(2), 0, 0, dp(7));
        return t;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button primary(String s) {
        Button b = button(s);
        b.setTextColor(Color.rgb(10, 13, 20));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(ACCENT, 14, 0, 0));
        return b;
    }

    private Button secondary(String s) {
        Button b = button(s);
        b.setTextColor(TEXT);
        b.setBackground(round(PANEL2, 14, BORDER, 1));
        return b;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private GradientDrawable round(int fill, int radius, int stroke, int width) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (width > 0) d.setStroke(dp(width), stroke);
        return d;
    }

    private void setStatus(String s, int c) {
        status.setText(s);
        status.setTextColor(c);
    }

    private double num(String s) {
        return Double.parseDouble(s.trim().replace(',', '.'));
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
