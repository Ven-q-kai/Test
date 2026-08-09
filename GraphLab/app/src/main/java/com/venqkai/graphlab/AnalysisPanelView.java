package com.venqkai.graphlab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public final class AnalysisPanelView extends LinearLayout {
    public interface Provider {
        List<AnalysisEngine.FunctionInput> getVisibleFunctions();
        double getMinX();
        double getMaxX();
    }

    private static final int PANEL = Color.rgb(18, 22, 31);
    private static final int TEXT = Color.rgb(239, 242, 248);
    private static final int MUTED = Color.rgb(153, 163, 184);
    private static final int BORDER = Color.rgb(45, 53, 70);
    private static final int ACCENT = Color.rgb(124, 157, 255);
    private final Provider provider;
    private final LinearLayout results;

    public AnalysisPanelView(Context context, Provider provider) {
        super(context);
        this.provider = provider;
        setOrientation(VERTICAL);
        setPadding(dp(2), dp(2), dp(2), dp(24));

        addView(text("Análise numérica", 19, TEXT, true));
        TextView help = text("Procura raízes, máximos, mínimos e interseções no intervalo X atual. Também estima derivada no centro e integral definida no intervalo.", 12, MUTED, false);
        help.setPadding(0, dp(3), 0, dp(12));
        addView(help);

        Button analyze = new Button(context);
        analyze.setText("Analisar funções");
        analyze.setAllCaps(false);
        analyze.setTextColor(Color.rgb(10, 13, 20));
        analyze.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        analyze.setMinHeight(0);
        analyze.setMinWidth(0);
        analyze.setBackground(round(ACCENT, 13, 0, 0));
        analyze.setOnClickListener(v -> refresh());
        addView(analyze, new LayoutParams(-1, dp(48)));

        TextView note = text("Resultados são aproximações numéricas. Funções descontínuas ou muito oscilatórias podem exigir um intervalo menor.", 11, MUTED, false);
        note.setPadding(dp(2), dp(8), dp(2), dp(10));
        addView(note);

        results = new LinearLayout(context);
        results.setOrientation(VERTICAL);
        addView(results, new LayoutParams(-1, -2));
        showEmpty();
    }

    public void refresh() {
        results.removeAllViews();
        List<AnalysisEngine.FunctionInput> functions = provider.getVisibleFunctions();
        if (functions.isEmpty()) {
            showMessage("Nenhuma função visível e válida para analisar.");
            return;
        }

        double minX = provider.getMinX();
        double maxX = provider.getMaxX();
        AnalysisEngine.Report report;
        try {
            report = AnalysisEngine.analyze(functions, minX, maxX);
        } catch (RuntimeException ex) {
            showMessage("Não foi possível concluir a análise: " + ex.getMessage());
            return;
        }

        for (AnalysisEngine.FunctionReport fr : report.functions) {
            LinearLayout card = card();
            TextView name = text(fr.input.name, 16, fr.input.color, true);
            card.addView(name);
            TextView formula = text(fr.input.formula, 12, MUTED, false);
            formula.setPadding(0, dp(1), 0, dp(8));
            card.addView(formula);

            card.addView(metric("Derivada em x = " + AnalysisEngine.format(fr.centerX), AnalysisEngine.format(fr.derivativeAtCenter)));
            card.addView(metric("Integral de " + AnalysisEngine.format(minX) + " até " + AnalysisEngine.format(maxX), AnalysisEngine.format(fr.integral)));
            card.addView(detail("Raízes", listNumbers(fr.roots)));
            card.addView(detail("Mínimos", listPoints(fr.minima)));
            card.addView(detail("Máximos", listPoints(fr.maxima)));
            results.addView(card, cardParams());
        }

        if (!report.intersections.isEmpty()) {
            LinearLayout card = card();
            card.addView(text("Interseções", 16, TEXT, true));
            StringBuilder sb = new StringBuilder();
            for (AnalysisEngine.Intersection intersection : report.intersections) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(intersection.a).append(" × ").append(intersection.b)
                        .append(": (").append(AnalysisEngine.format(intersection.point.x))
                        .append(", ").append(AnalysisEngine.format(intersection.point.y)).append(')');
            }
            TextView value = text(sb.toString(), 12, MUTED, false);
            value.setPadding(0, dp(7), 0, 0);
            value.setTextIsSelectable(true);
            card.addView(value);
            results.addView(card, cardParams());
        }
    }

    private TextView metric(String label, String value) {
        TextView t = text(label + "  •  " + value, 13, TEXT, false);
        t.setPadding(0, dp(4), 0, dp(4));
        t.setTextIsSelectable(true);
        return t;
    }

    private LinearLayout detail(String label, String value) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(VERTICAL);
        box.setPadding(0, dp(7), 0, 0);
        box.addView(text(label.toUpperCase(), 10, MUTED, true));
        TextView body = text(value, 12, TEXT, false);
        body.setPadding(0, dp(2), 0, 0);
        body.setTextIsSelectable(true);
        box.addView(body);
        return box;
    }

    private String listNumbers(List<Double> values) {
        if (values.isEmpty()) return "nenhuma encontrada";
        StringBuilder sb = new StringBuilder();
        for (double v : values) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(AnalysisEngine.format(v));
        }
        return sb.toString();
    }

    private String listPoints(List<AnalysisEngine.Point> values) {
        if (values.isEmpty()) return "nenhum encontrado";
        StringBuilder sb = new StringBuilder();
        for (AnalysisEngine.Point p : values) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append('(').append(AnalysisEngine.format(p.x)).append(", ")
                    .append(AnalysisEngine.format(p.y)).append(')');
        }
        return sb.toString();
    }

    private void showEmpty() {
        showMessage("Toque em Analisar funções para gerar um relatório do intervalo atual.");
    }

    private void showMessage(String message) {
        results.removeAllViews();
        LinearLayout card = card();
        TextView t = text(message, 13, MUTED, false);
        t.setPadding(0, dp(2), 0, dp(2));
        card.addView(t);
        results.addView(card, cardParams());
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(round(PANEL, 14, BORDER, 1));
        return card;
    }

    private LayoutParams cardParams() {
        LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(8);
        return p;
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
