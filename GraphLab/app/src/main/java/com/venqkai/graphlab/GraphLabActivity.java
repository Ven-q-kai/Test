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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GraphLabActivity extends Activity implements
        FunctionRowView.Listener,
        MathKeyboardView.Listener,
        TablePanelView.Provider,
        AnalysisPanelView.Provider {

    private static final int BG = Color.rgb(10, 12, 17);
    private static final int SURFACE = Color.rgb(17, 20, 28);
    private static final int SURFACE2 = Color.rgb(23, 27, 37);
    private static final int TEXT = Color.rgb(240, 243, 249);
    private static final int MUTED = Color.rgb(147, 157, 177);
    private static final int BORDER = Color.rgb(39, 46, 61);
    private static final int ACCENT = Color.rgb(124, 157, 255);
    private static final int DANGER = Color.rgb(255, 112, 99);
    private static final int SUCCESS = Color.rgb(88, 214, 141);

    private final List<FunctionRowView> rows = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoPlot = () -> plot(false);

    private LinearLayout root;
    private GraphView graph;
    private LinearLayout rowBox;
    private FrameLayout panelHost;
    private ScrollView functionsPanel;
    private ScrollView tablePanelScroll;
    private ScrollView analysisPanelScroll;
    private TablePanelView tablePanel;
    private AnalysisPanelView analysisPanel;
    private TextView subtitle;
    private TextView graphHint;
    private TextView status;
    private Button tabFunctions;
    private Button tabTable;
    private Button tabAnalysis;
    private Button fullscreenButton;
    private Button autoYButton;
    private Button keyboardButton;
    private MathKeyboardView keyboard;
    private EditText minX;
    private EditText maxX;
    private EditText minY;
    private EditText maxY;
    private SharedPreferences prefs;
    private FunctionRowView activeRow;

    private boolean autoY = true;
    private boolean loading = true;
    private boolean fullscreen = false;
    private boolean keyboardVisible = false;
    private int nextColor = 0;
    private int selectedTab = 0;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        prefs = getSharedPreferences("graphlab", MODE_PRIVATE);
        setContentView(buildUi());
        load();
        loading = false;
        plot(false);
        switchTab(0);
    }

    @Override protected void onPause() {
        super.onPause();
        save();
    }

    @Override public void onBackPressed() {
        if (fullscreen) toggleFullscreen();
        else super.onBackPressed();
    }

    private View buildUi() {
        root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(8), dp(14), dp(10));

        LinearLayout appBar = row();
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("GraphLab", 24, TEXT, true);
        appBar.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1f));

        TextView version = text("1.1.0", 10, ACCENT, true);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(10), dp(4), dp(10), dp(4));
        version.setBackground(round(Color.rgb(25, 31, 48), 18, Color.rgb(52, 67, 103), 1));
        appBar.addView(version, new LinearLayout.LayoutParams(dp(58), dp(30)));

        fullscreenButton = iconButton("⛶");
        fullscreenButton.setContentDescription("Abrir gráfico em tela cheia");
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        LinearLayout.LayoutParams fbp = new LinearLayout.LayoutParams(dp(40), dp(36));
        fbp.leftMargin = dp(7);
        appBar.addView(fullscreenButton, fbp);
        root.addView(appBar);

        subtitle = text("Funções, tabela e análise no mesmo espaço.", 12, MUTED, false);
        subtitle.setPadding(dp(1), 0, 0, dp(9));
        root.addView(subtitle);

        graph = new GraphView(this);
        graph.setBackground(round(Color.rgb(13, 16, 22), 16, BORDER, 1));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(300));
        gp.bottomMargin = dp(5);
        root.addView(graph, gp);

        graphHint = text("Toque para medir • arraste a mira • pinça para zoom • dois toques para reenquadrar", 10, MUTED, false);
        graphHint.setGravity(Gravity.CENTER);
        graphHint.setPadding(0, 0, 0, dp(8));
        root.addView(graphHint);

        LinearLayout tabs = row();
        tabs.setBackground(round(SURFACE, 14, BORDER, 1));
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabFunctions = tabButton("ƒ  Funções");
        tabTable = tabButton("▦  Tabela");
        tabAnalysis = tabButton("⌁  Análise");
        tabFunctions.setOnClickListener(v -> switchTab(0));
        tabTable.setOnClickListener(v -> switchTab(1));
        tabAnalysis.setOnClickListener(v -> switchTab(2));
        tabs.addView(tabFunctions, new LinearLayout.LayoutParams(0, dp(42), 1f));
        tabs.addView(tabTable, new LinearLayout.LayoutParams(0, dp(42), 1f));
        tabs.addView(tabAnalysis, new LinearLayout.LayoutParams(0, dp(42), 1f));
        root.addView(tabs);

        status = text("Pronto.", 11, MUTED, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(4), dp(6), dp(4), dp(6));
        root.addView(status);

        panelHost = new FrameLayout(this);
        root.addView(panelHost, new LinearLayout.LayoutParams(-1, 0, 1f));

        functionsPanel = buildFunctionsPanel();
        panelHost.addView(functionsPanel, new FrameLayout.LayoutParams(-1, -1));

        tablePanelScroll = new ScrollView(this);
        tablePanelScroll.setFillViewport(false);
        tablePanel = new TablePanelView(this, this);
        tablePanelScroll.addView(tablePanel, new ScrollView.LayoutParams(-1, -2));
        panelHost.addView(tablePanelScroll, new FrameLayout.LayoutParams(-1, -1));

        analysisPanelScroll = new ScrollView(this);
        analysisPanelScroll.setFillViewport(false);
        analysisPanel = new AnalysisPanelView(this, this);
        analysisPanelScroll.addView(analysisPanel, new ScrollView.LayoutParams(-1, -2));
        panelHost.addView(analysisPanelScroll, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private ScrollView buildFunctionsPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        LinearLayout body = column();
        body.setPadding(0, dp(8), 0, dp(22));
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(section("FUNÇÕES"), new LinearLayout.LayoutParams(0, dp(30), 1f));
        Button add = smallButton("+ Nova");
        add.setOnClickListener(v -> {
            addRow("", "", nextColor++, true);
            activeRow = rows.get(rows.size() - 1);
            activeRow.requestFormulaFocus();
        });
        header.addView(add, new LinearLayout.LayoutParams(dp(82), dp(34)));
        body.addView(header);

        rowBox = column();
        body.addView(rowBox);

        keyboardButton = compactAction("⌨  Teclado matemático");
        keyboardButton.setOnClickListener(v -> {
            keyboardVisible = !keyboardVisible;
            keyboard.setVisibility(keyboardVisible ? View.VISIBLE : View.GONE);
            keyboardButton.setText(keyboardVisible ? "⌨  Ocultar teclado" : "⌨  Teclado matemático");
        });
        LinearLayout.LayoutParams kbp = new LinearLayout.LayoutParams(-1, dp(42));
        kbp.topMargin = dp(2);
        body.addView(keyboardButton, kbp);

        keyboard = new MathKeyboardView(this, this);
        keyboard.setVisibility(View.GONE);
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-1, -2);
        kp.topMargin = dp(7);
        kp.bottomMargin = dp(12);
        body.addView(keyboard, kp);

        LinearLayout axisCard = column();
        axisCard.setPadding(dp(12), dp(10), dp(12), dp(12));
        axisCard.setBackground(round(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams acp = new LinearLayout.LayoutParams(-1, -2);
        acp.topMargin = dp(10);
        body.addView(axisCard, acp);

        axisCard.addView(section("JANELA DO GRÁFICO"));
        LinearLayout xRow = row();
        minX = numberInput();
        maxX = numberInput();
        LinearLayout.LayoutParams x1 = new LinearLayout.LayoutParams(0, dp(54), 1f); x1.rightMargin = dp(5);
        LinearLayout.LayoutParams x2 = new LinearLayout.LayoutParams(0, dp(54), 1f); x2.leftMargin = dp(5);
        xRow.addView(numberBox("X mínimo", minX), x1);
        xRow.addView(numberBox("X máximo", maxX), x2);
        axisCard.addView(xRow);

        LinearLayout yHeader = row();
        yHeader.setGravity(Gravity.CENTER_VERTICAL);
        yHeader.setPadding(0, dp(9), 0, dp(5));
        yHeader.addView(text("Eixo Y", 12, MUTED, true), new LinearLayout.LayoutParams(0, dp(34), 1f));
        autoYButton = smallButton("AUTO");
        autoYButton.setOnClickListener(v -> { autoY = !autoY; refreshYMode(); plot(false); });
        yHeader.addView(autoYButton, new LinearLayout.LayoutParams(dp(92), dp(34)));
        axisCard.addView(yHeader);

        LinearLayout yRow = row();
        minY = numberInput();
        maxY = numberInput();
        LinearLayout.LayoutParams y1 = new LinearLayout.LayoutParams(0, dp(54), 1f); y1.rightMargin = dp(5);
        LinearLayout.LayoutParams y2 = new LinearLayout.LayoutParams(0, dp(54), 1f); y2.leftMargin = dp(5);
        yRow.addView(numberBox("Y mínimo", minY), y1);
        yRow.addView(numberBox("Y máximo", maxY), y2);
        axisCard.addView(yRow);

        LinearLayout actions = row();
        Button plotNow = primary("Plotar agora");
        plotNow.setOnClickListener(v -> plot(true));
        Button reset = secondary("Reenquadrar");
        reset.setOnClickListener(v -> graph.resetViewport());
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(46), 1.2f); a.rightMargin = dp(5); a.topMargin = dp(10);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(46), 1f); b.leftMargin = dp(5); b.topMargin = dp(10);
        actions.addView(plotNow, a);
        actions.addView(reset, b);
        axisCard.addView(actions);

        TextView syntax = text("Mais funções: asin, acos, atan, sinh, cosh, tanh, floor, ceil, round, sign, deg, rad e atan2, além das já existentes.", 11, MUTED, false);
        syntax.setPadding(dp(2), dp(10), dp(2), 0);
        body.addView(syntax);

        watch(minX, maxX, minY, maxY);
        return scroll;
    }

    private void switchTab(int tab) {
        selectedTab = tab;
        functionsPanel.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        tablePanelScroll.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        analysisPanelScroll.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        styleTab(tabFunctions, tab == 0);
        styleTab(tabTable, tab == 1);
        styleTab(tabAnalysis, tab == 2);
        if (tab == 1) tablePanel.refresh();
    }

    private void addRow(String name, String formula, int color, boolean visible) {
        FunctionRowView row = new FunctionRowView(this, name, formula, color, visible, this);
        rows.add(row);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(7);
        rowBox.addView(row, p);
        nextColor = Math.max(nextColor, color + 1);
        if (activeRow == null) activeRow = row;
    }

    @Override public void onChanged() { schedulePlot(); }

    @Override public void onDelete(FunctionRowView row) {
        if (rows.size() == 1) {
            row.clearFields(); activeRow = row; setStatus("Adicione uma função para plotar.", MUTED); return;
        }
        rows.remove(row); rowBox.removeView(row);
        if (activeRow == row) activeRow = rows.get(0);
        schedulePlot();
    }

    @Override public void onDuplicate(FunctionRowView row) {
        addRow(row.getNameValue(), row.getFormula(), nextColor++, row.isVisibleFunction());
        activeRow = rows.get(rows.size() - 1);
        setStatus("Função duplicada.", ACCENT);
        schedulePlot();
    }

    @Override public void onColorRequested(FunctionRowView row) { colorDialog(row); }
    @Override public void onFormulaFocused(FunctionRowView row) { activeRow = row; }

    @Override public void onInsert(String value, int cursorBack) {
        if (activeRow == null && !rows.isEmpty()) activeRow = rows.get(0);
        if (activeRow != null) activeRow.insertIntoFormula(value, cursorBack);
    }

    private void colorDialog(FunctionRowView target) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(14), dp(8), dp(14), dp(8));
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < GraphView.paletteSize(); i++) {
            Button button = new Button(this);
            button.setTag(i);
            button.setMinWidth(0); button.setMinHeight(0); button.setPadding(0,0,0,0);
            int stroke = i == target.getColorIndex() ? Color.WHITE : Color.argb(50,255,255,255);
            button.setBackground(round(GraphView.colorFor(i), 100, stroke, i == target.getColorIndex() ? 2 : 1));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = dp(48); p.height = dp(48); p.setMargins(dp(5),dp(5),dp(5),dp(5));
            grid.addView(button, p); buttons.add(button);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Cor da função").setView(grid).setNegativeButton("Cancelar", null).create();
        for (Button button : buttons) button.setOnClickListener(v -> {
            target.setColorIndex((Integer) v.getTag()); dialog.dismiss(); plot(false);
        });
        dialog.show();
    }

    private void plot(boolean toast) {
        handler.removeCallbacks(autoPlot);
        double lo, hi, yLo = -10, yHi = 10;
        try {
            lo = num(minX.getText().toString()); hi = num(maxX.getText().toString());
            if (!Double.isFinite(lo) || !Double.isFinite(hi) || lo >= hi || hi-lo < 1e-10 || hi-lo > 1e12) throw new Exception();
        } catch (Exception ex) {
            setStatus("Intervalo X inválido.", DANGER); if (toast) Toast.makeText(this,"Revise o eixo X.",Toast.LENGTH_SHORT).show(); return;
        }
        if (!autoY) {
            try {
                yLo = num(minY.getText().toString()); yHi = num(maxY.getText().toString());
                if (!Double.isFinite(yLo) || !Double.isFinite(yHi) || yLo >= yHi || yHi-yLo < 1e-10 || yHi-yLo > 1e12) throw new Exception();
            } catch (Exception ex) {
                setStatus("Intervalo Y inválido.", DANGER); if (toast) Toast.makeText(this,"Revise o eixo Y.",Toast.LENGTH_SHORT).show(); return;
            }
        }

        List<GraphView.FunctionSpec> valid = new ArrayList<>();
        boolean any = false, errors = false;
        int visible = 0;
        for (FunctionRowView row : rows) {
            row.clearError();
            String formula = row.getFormula();
            if (formula.isEmpty()) continue;
            any = true;
            try {
                new Expression(formula);
                valid.add(new GraphView.FunctionSpec(row.getNameValue(), formula, GraphView.colorFor(row.getColorIndex()), row.isVisibleFunction()));
                if (row.isVisibleFunction()) visible++;
            } catch (RuntimeException ex) {
                errors = true; row.showError(humanError(ex.getMessage()));
            }
        }
        if (!any) { setStatus("Adicione pelo menos uma função.", MUTED); return; }
        if (valid.isEmpty()) { setStatus("Nenhuma função válida.", DANGER); return; }

        graph.setFunctions(valid, lo, hi, autoY, yLo, yHi);
        save();
        if (errors) setStatus("Há função com erro; as válidas continuam no gráfico.", DANGER);
        else if (visible == 0) setStatus("Todas as funções estão ocultas.", MUTED);
        else setStatus(visible + (visible == 1 ? " curva" : " curvas") + " • atualização automática", ACCENT);
    }

    @Override public List<AnalysisEngine.FunctionInput> getVisibleFunctions() {
        List<AnalysisEngine.FunctionInput> out = new ArrayList<>();
        for (FunctionRowView row : rows) {
            if (!row.isVisibleFunction() || row.getFormula().isEmpty()) continue;
            try {
                new Expression(row.getFormula());
                String name = row.getNameValue().isEmpty() ? row.getFormula() : row.getNameValue();
                out.add(new AnalysisEngine.FunctionInput(name, row.getFormula(), GraphView.colorFor(row.getColorIndex())));
            } catch (RuntimeException ignored) {}
        }
        return out;
    }

    @Override public double getMinX() {
        try { return num(minX.getText().toString()); } catch (Exception ex) { return -10; }
    }

    @Override public double getMaxX() {
        try { return num(maxX.getText().toString()); } catch (Exception ex) { return 10; }
    }

    private void schedulePlot() {
        if (loading) return;
        handler.removeCallbacks(autoPlot);
        handler.postDelayed(autoPlot, 420);
    }

    private void watch(EditText... fields) {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { schedulePlot(); }
        };
        for (EditText field : fields) field.addTextChangedListener(watcher);
    }

    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        subtitle.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        graphHint.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        status.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        panelHost.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        View tabs = (View) tabFunctions.getParent();
        tabs.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        fullscreenButton.setText(fullscreen ? "↙" : "⛶");
        LinearLayout.LayoutParams gp = (LinearLayout.LayoutParams) graph.getLayoutParams();
        if (fullscreen) {
            gp.height = 0; gp.weight = 1f; gp.bottomMargin = 0; root.setPadding(dp(7),dp(5),dp(7),dp(7));
        } else {
            gp.height = dp(300); gp.weight = 0; gp.bottomMargin = dp(5); root.setPadding(dp(14),dp(8),dp(14),dp(10));
        }
        graph.setLayoutParams(gp);
        graph.requestLayout();
    }

    private void refreshYMode() {
        if (autoYButton == null) return;
        autoYButton.setText(autoY ? "AUTO: ON" : "AUTO: OFF");
        autoYButton.setTextColor(autoY ? SUCCESS : TEXT);
        autoYButton.setBackground(round(autoY ? Color.rgb(20,42,36) : SURFACE2, 10, autoY ? Color.rgb(46,84,68) : BORDER, 1));
        minY.setEnabled(!autoY); maxY.setEnabled(!autoY);
        minY.setAlpha(autoY ? .42f : 1f); maxY.setAlpha(autoY ? .42f : 1f);
    }

    private void load() {
        minX.setText(prefs.getString("minX", "-10")); maxX.setText(prefs.getString("maxX", "10"));
        minY.setText(prefs.getString("minY", "-10")); maxY.setText(prefs.getString("maxY", "10"));
        autoY = prefs.getBoolean("autoY", true); refreshYMode();
        String saved = prefs.getString("functions_v101", "");
        if (saved != null && !saved.isEmpty()) {
            for (String line : saved.split("\\n")) {
                String[] p = line.split("\\|", -1);
                if (p.length == 4) try { addRow(dec(p[0]), dec(p[1]), Integer.parseInt(p[2]), "1".equals(p[3])); } catch (Exception ignored) {}
            }
        }
        if (rows.isEmpty()) {
            String old = prefs.getString("equations", "");
            if (old != null) for (String f : old.split("\\n")) if (!f.trim().isEmpty()) addRow("", f, nextColor++, true);
        }
        if (rows.isEmpty()) addRow("", "", nextColor++, true);
        activeRow = rows.get(0);
    }

    private void save() {
        if (prefs == null || minX == null) return;
        StringBuilder sb = new StringBuilder();
        for (FunctionRowView row : rows) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(enc(row.getNameValue())).append('|').append(enc(row.getFormula())).append('|')
                    .append(row.getColorIndex()).append('|').append(row.isVisibleFunction() ? '1' : '0');
        }
        prefs.edit().putString("functions_v101", sb.toString())
                .putString("minX", minX.getText().toString()).putString("maxX", maxX.getText().toString())
                .putString("minY", minY.getText().toString()).putString("maxY", maxY.getText().toString())
                .putBoolean("autoY", autoY).apply();
    }

    private String humanError(String m) {
        if (m == null || m.isEmpty()) return "Expressão inválida.";
        String s = m.replace("Esperado ')'", "Falta ')' para fechar a expressão").replace("Expressão incompleta", "A expressão terminou antes do esperado");
        return s.endsWith(".") ? s : s + ".";
    }

    private String enc(String s) { return Base64.encodeToString(s.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP); }
    private String dec(String s) { return new String(Base64.decode(s, Base64.URL_SAFE | Base64.NO_WRAP), StandardCharsets.UTF_8); }
    private double num(String s) { return Double.parseDouble(s.trim().replace(',', '.')); }

    private LinearLayout numberBox(String label, EditText input) {
        LinearLayout box = column(); box.setPadding(dp(10),dp(5),dp(8),dp(3)); box.setBackground(round(SURFACE2,11,BORDER,1));
        box.addView(text(label,9,MUTED,true)); box.addView(input,new LinearLayout.LayoutParams(-1,dp(30))); return box;
    }

    private EditText numberInput() {
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextColor(TEXT); e.setTextSize(15); e.setPadding(0,0,0,0); e.setBackgroundColor(Color.TRANSPARENT);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED); return e;
    }

    private TextView section(String s) { TextView t=text(s,10,MUTED,true); t.setLetterSpacing(.12f); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private TextView text(String s,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }

    private Button tabButton(String s){ Button b=button(s); b.setTextSize(11); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setPadding(dp(4),0,dp(4),0); return b; }
    private void styleTab(Button b, boolean selected){ b.setTextColor(selected ? TEXT : MUTED); b.setBackground(round(selected ? SURFACE2 : Color.TRANSPARENT,10,selected ? BORDER : Color.TRANSPARENT,selected ? 1 : 0)); }
    private Button iconButton(String s){ Button b=button(s); b.setTextColor(TEXT); b.setTextSize(18); b.setBackground(round(SURFACE2,11,BORDER,1)); return b; }
    private Button smallButton(String s){ Button b=button(s); b.setTextColor(TEXT); b.setTextSize(11); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(round(SURFACE2,10,BORDER,1)); return b; }
    private Button compactAction(String s){ Button b=button(s); b.setTextColor(TEXT); b.setTextSize(12); b.setBackground(round(SURFACE,12,BORDER,1)); return b; }
    private Button primary(String s){ Button b=button(s); b.setTextColor(Color.rgb(10,13,20)); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(round(ACCENT,13,0,0)); return b; }
    private Button secondary(String s){ Button b=button(s); b.setTextColor(TEXT); b.setBackground(round(SURFACE2,13,BORDER,1)); return b; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setMinWidth(0); b.setMinHeight(0); return b; }
    private GradientDrawable round(int fill,int radius,int stroke,int width){ GradientDrawable d=new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius)); if(width>0)d.setStroke(dp(width),stroke); return d; }
    private void setStatus(String s,int color){ status.setText(s); status.setTextColor(color); }
    private int dp(float v){ return Math.round(v * getResources().getDisplayMetrics().density); }
}
