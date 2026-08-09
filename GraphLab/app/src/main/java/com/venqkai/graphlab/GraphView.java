package com.venqkai.graphlab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GraphView extends View {
    private static final int BG = Color.rgb(14, 17, 24);
    private static final int GRID = Color.rgb(39, 46, 63);
    private static final int AXIS = Color.rgb(124, 137, 164);
    private static final int TEXT = Color.rgb(180, 188, 207);
    private static final int INSPECT_BG = Color.rgb(27, 32, 44);
    private static final int INSPECT_LINE = Color.rgb(199, 206, 222);

    private static final int[] COLORS = {
            Color.rgb(124, 157, 255), Color.rgb(255, 110, 168), Color.rgb(255, 190, 92),
            Color.rgb(88, 214, 141), Color.rgb(180, 128, 255), Color.rgb(255, 112, 99),
            Color.rgb(87, 205, 223), Color.rgb(236, 132, 79)
    };

    public static final class FunctionSpec {
        public final String name;
        public final String formula;
        public final int color;
        public final boolean visible;

        public FunctionSpec(String name, String formula, int color, boolean visible) {
            this.name = name == null ? "" : name.trim();
            this.formula = formula == null ? "" : formula.trim();
            this.color = color;
            this.visible = visible;
        }

        public String displayName() {
            return name.isEmpty() ? formula : name;
        }
    }

    private static final class Curve {
        final FunctionSpec spec;
        final Expression expression;

        Curve(FunctionSpec spec) {
            this.spec = spec;
            this.expression = new Expression(spec.formula);
        }
    }

    private static final class InspectValue {
        final Curve curve;
        final double y;

        InspectValue(Curve curve, double y) {
            this.curve = curve;
            this.y = y;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DecimalFormat numberFormat = new DecimalFormat("0.###");
    private final List<Curve> curves = new ArrayList<>();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private double centerX = 0.0;
    private double centerY = 0.0;
    private double spanX = 20.0;
    private double spanY = 20.0;
    private double initialCenterX = 0.0;
    private double initialCenterY = 0.0;
    private double initialSpanX = 20.0;
    private double initialSpanY = 20.0;

    private boolean inspectorVisible = false;
    private boolean draggingInspector = false;
    private double inspectorX = 0.0;
    private float inspectorTouchY = 90f;

    public GraphView(Context context) {
        super(context);
        setBackgroundColor(BG);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        textPaint.setColor(TEXT);
        textPaint.setTextSize(dp(11));

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                inspectorVisible = false;
                draggingInspector = false;
                return true;
            }

            @Override public boolean onScale(ScaleGestureDetector detector) {
                double factor = detector.getScaleFactor();
                if (!Double.isFinite(factor) || factor <= 0) return false;
                factor = Math.max(0.65, Math.min(1.5, factor));
                spanX = clamp(spanX / factor, 1e-8, 1e12);
                spanY = clamp(spanY / factor, 1e-8, 1e12);
                invalidate();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (scaleDetector.isInProgress() || getWidth() == 0 || getHeight() == 0 || draggingInspector) return false;
                inspectorVisible = false;
                centerX += distanceX / getWidth() * spanX;
                centerY -= distanceY / getHeight() * spanY;
                invalidate();
                return true;
            }

            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (getWidth() <= 0) return false;
                inspectorX = pxToX(e.getX(), getWidth());
                inspectorTouchY = e.getY();
                inspectorVisible = true;
                invalidate();
                return true;
            }

            @Override public boolean onDoubleTap(MotionEvent e) {
                resetViewport();
                return true;
            }
        });
    }

    public void setFunctions(List<FunctionSpec> functions, double minX, double maxX) {
        setFunctions(functions, minX, maxX, true, -10, 10);
    }

    public void setFunctions(List<FunctionSpec> functions, double minX, double maxX,
                             boolean autoY, double minY, double maxY) {
        curves.clear();
        for (FunctionSpec spec : functions) curves.add(new Curve(spec));

        inspectorVisible = false;
        draggingInspector = false;
        centerX = (minX + maxX) * 0.5;
        spanX = Math.max(1e-8, maxX - minX);

        if (autoY) {
            autoFitY(minX, maxX);
        } else {
            centerY = (minY + maxY) * 0.5;
            spanY = Math.max(1e-8, maxY - minY);
        }

        rememberViewport();
        invalidate();
    }

    public void resetViewport() {
        inspectorVisible = false;
        draggingInspector = false;
        centerX = initialCenterX;
        centerY = initialCenterY;
        spanX = initialSpanX;
        spanY = initialSpanY;
        invalidate();
    }

    public static int colorFor(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }

    public static int paletteSize() {
        return COLORS.length;
    }

    private void rememberViewport() {
        initialCenterX = centerX;
        initialCenterY = centerY;
        initialSpanX = spanX;
        initialSpanY = spanY;
    }

    private void autoFitY(double minX, double maxX) {
        List<Double> values = new ArrayList<>();
        int samples = 700;

        for (Curve curve : curves) {
            if (!curve.spec.visible) continue;
            for (int i = 0; i < samples; i++) {
                double x = minX + (maxX - minX) * i / (samples - 1.0);
                try {
                    double y = curve.expression.eval(x);
                    if (Double.isFinite(y) && Math.abs(y) < 1e12) values.add(y);
                } catch (RuntimeException ignored) {}
            }
        }

        if (values.isEmpty()) {
            centerY = 0;
            spanY = 20;
            return;
        }

        Collections.sort(values);
        int lowIndex = (int) Math.floor((values.size() - 1) * 0.02);
        int highIndex = (int) Math.ceil((values.size() - 1) * 0.98);
        double low = values.get(Math.max(0, lowIndex));
        double high = values.get(Math.min(values.size() - 1, highIndex));

        if (!Double.isFinite(low) || !Double.isFinite(high)) {
            low = -10;
            high = 10;
        }

        if (Math.abs(high - low) < 1e-12) {
            double margin = Math.max(1.0, Math.abs(low) * 0.25);
            low -= margin;
            high += margin;
        }

        double margin = (high - low) * 0.12;
        centerY = (low + high) * 0.5;
        spanY = Math.max(1e-8, (high - low) + 2 * margin);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        drawGrid(canvas, w, h);
        drawFunctions(canvas, w, h);
        drawLegend(canvas, w);
        if (inspectorVisible) drawInspector(canvas, w, h);
    }

    private void drawGrid(Canvas canvas, int w, int h) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(GRID);

        double xStep = niceStep(spanX / 7.0);
        double yStep = niceStep(spanY / 7.0);
        double minX = centerX - spanX / 2.0;
        double maxX = centerX + spanX / 2.0;
        double minY = centerY - spanY / 2.0;
        double maxY = centerY + spanY / 2.0;

        double firstX = Math.ceil(minX / xStep) * xStep;
        for (double x = firstX; x <= maxX + xStep * 0.001; x += xStep) {
            float px = xToPx(x, w);
            boolean axis = Math.abs(x) < xStep * 1e-7;
            paint.setColor(axis ? AXIS : GRID);
            paint.setStrokeWidth(axis ? dp(1.6f) : dp(1));
            canvas.drawLine(px, 0, px, h, paint);
            if (px > dp(28) && px < w - dp(16)) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(TEXT);
                canvas.drawText(format(x), px, h - dp(8), textPaint);
            }
        }

        double firstY = Math.ceil(minY / yStep) * yStep;
        for (double y = firstY; y <= maxY + yStep * 0.001; y += yStep) {
            float py = yToPx(y, h);
            boolean axis = Math.abs(y) < yStep * 1e-7;
            paint.setColor(axis ? AXIS : GRID);
            paint.setStrokeWidth(axis ? dp(1.6f) : dp(1));
            canvas.drawLine(0, py, w, py, paint);
            if (py > dp(18) && py < h - dp(24)) {
                textPaint.setTextAlign(Paint.Align.LEFT);
                textPaint.setColor(TEXT);
                canvas.drawText(format(y), dp(7), py - dp(4), textPaint);
            }
        }
    }

    private void drawFunctions(Canvas canvas, int w, int h) {
        if (curves.isEmpty()) return;
        double minX = centerX - spanX / 2.0;
        double maxX = centerX + spanX / 2.0;
        int samples = Math.max(600, Math.min(1800, w * 2));

        for (Curve curve : curves) {
            if (!curve.spec.visible) continue;
            paint.setColor(curve.spec.color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.3f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            Path path = new Path();
            boolean started = false;
            float previousY = 0;

            for (int i = 0; i < samples; i++) {
                double x = minX + (maxX - minX) * i / (samples - 1.0);
                double y;
                try { y = curve.expression.eval(x); }
                catch (RuntimeException ex) { y = Double.NaN; }

                if (!Double.isFinite(y) || Math.abs(y - centerY) > spanY * 25) {
                    started = false;
                    continue;
                }

                float px = (float) (i * (w - 1.0) / (samples - 1.0));
                float py = yToPx(y, h);
                if (!started || Math.abs(py - previousY) > h * 0.72f) {
                    path.moveTo(px, py);
                    started = true;
                } else {
                    path.lineTo(px, py);
                }
                previousY = py;
            }
            canvas.drawPath(path, paint);
        }
    }

    private void drawLegend(Canvas canvas, int w) {
        float x = dp(10), y = dp(13);
        textPaint.setTextSize(dp(11));

        for (Curve curve : curves) {
            if (!curve.spec.visible) continue;
            String label = curve.spec.displayName();
            if (label.length() > 22) label = label.substring(0, 21) + "…";
            float textWidth = textPaint.measureText(label);
            float itemWidth = dp(15) + textWidth + dp(12);

            if (x + itemWidth > w - dp(6)) {
                x = dp(10);
                y += dp(22);
            }

            paint.setColor(curve.spec.color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(x, y - dp(7), x + dp(8), y + dp(1)), dp(3), dp(3), paint);
            textPaint.setColor(TEXT);
            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(label, x + dp(13), y, textPaint);
            x += itemWidth;
            if (y > dp(58)) break;
        }
    }

    private void drawInspector(Canvas canvas, int w, int h) {
        float px = xToPx(inspectorX, w);
        if (px < 0 || px > w) return;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(INSPECT_LINE);
        canvas.drawLine(px, 0, px, h, paint);

        List<InspectValue> values = new ArrayList<>();
        for (Curve curve : curves) {
            if (!curve.spec.visible) continue;
            try {
                double y = curve.expression.eval(inspectorX);
                if (Double.isFinite(y) && Math.abs(y - centerY) <= spanY * 25) {
                    values.add(new InspectValue(curve, y));
                    float py = yToPx(y, h);
                    if (py >= 0 && py <= h) {
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(curve.spec.color);
                        canvas.drawCircle(px, py, dp(4.2f), paint);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(dp(1.5f));
                        paint.setColor(BG);
                        canvas.drawCircle(px, py, dp(4.2f), paint);
                    }
                }
            } catch (RuntimeException ignored) {}
        }

        int maxShown = h > dp(420) ? 5 : 4;
        int shown = Math.min(maxShown, values.size());
        int lines = 1 + shown + (values.size() > shown ? 1 : 0);
        float bubbleWidth = dp(190);
        float bubbleHeight = dp(14 + lines * 19);
        float bubbleX = px + dp(10);
        if (bubbleX + bubbleWidth > w - dp(6)) bubbleX = px - bubbleWidth - dp(10);
        bubbleX = Math.max(dp(6), Math.min(w - bubbleWidth - dp(6), bubbleX));

        float desiredY = inspectorTouchY - bubbleHeight * 0.5f;
        float bubbleY = Math.max(dp(66), Math.min(h - bubbleHeight - dp(8), desiredY));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INSPECT_BG);
        canvas.drawRoundRect(new RectF(bubbleX, bubbleY, bubbleX + bubbleWidth, bubbleY + bubbleHeight), dp(10), dp(10), paint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(dp(11));
        textPaint.setColor(Color.rgb(229, 233, 242));
        float textX = bubbleX + dp(10);
        float textY = bubbleY + dp(18);
        canvas.drawText("x = " + format(inspectorX), textX, textY, textPaint);

        for (int i = 0; i < shown; i++) {
            InspectValue item = values.get(i);
            String name = item.curve.spec.displayName();
            if (name.length() > 14) name = name.substring(0, 13) + "…";
            textY += dp(19);
            paint.setColor(item.curve.spec.color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(textX + dp(3), textY - dp(4), dp(3), paint);
            textPaint.setColor(Color.rgb(229, 233, 242));
            canvas.drawText(name + ": y = " + format(item.y), textX + dp(11), textY, textPaint);
        }

        if (values.size() > shown) {
            textY += dp(19);
            textPaint.setColor(TEXT);
            canvas.drawText("+ " + (values.size() - shown) + " curva(s)", textX, textY, textPaint);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1 && getWidth() > 0) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && inspectorVisible) {
                float inspectorPx = xToPx(inspectorX, getWidth());
                if (Math.abs(event.getX() - inspectorPx) <= dp(30)) {
                    draggingInspector = true;
                    inspectorX = pxToX(clampFloat(event.getX(), 0, getWidth()), getWidth());
                    inspectorTouchY = event.getY();
                    invalidate();
                    return true;
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && draggingInspector) {
                inspectorX = pxToX(clampFloat(event.getX(), 0, getWidth()), getWidth());
                inspectorTouchY = event.getY();
                invalidate();
                return true;
            } else if ((event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) && draggingInspector) {
                draggingInspector = false;
                invalidate();
                return true;
            }
        }

        boolean a = scaleDetector.onTouchEvent(event);
        boolean b = gestureDetector.onTouchEvent(event);
        return a || b || super.onTouchEvent(event);
    }

    private float xToPx(double x, int width) {
        return (float) ((x - (centerX - spanX / 2.0)) / spanX * width);
    }

    private double pxToX(float px, int width) {
        return (centerX - spanX / 2.0) + (px / width) * spanX;
    }

    private float yToPx(double y, int height) {
        return (float) (height - (y - (centerY - spanY / 2.0)) / spanY * height);
    }

    private double niceStep(double rough) {
        if (!Double.isFinite(rough) || rough <= 0) return 1.0;
        double power = Math.pow(10, Math.floor(Math.log10(rough)));
        double fraction = rough / power;
        double nice;
        if (fraction < 1.5) nice = 1;
        else if (fraction < 3) nice = 2;
        else if (fraction < 7) nice = 5;
        else nice = 10;
        return nice * power;
    }

    private String format(double value) {
        if (Math.abs(value) < 1e-11) value = 0;
        double abs = Math.abs(value);
        if ((abs > 0 && abs < 0.001) || abs >= 100000) {
            return String.format(java.util.Locale.US, "%.1e", value);
        }
        return numberFormat.format(value);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
