package com.venqkai.graphlab;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalysisEngine {
    public static final class FunctionInput {
        public final String name;
        public final String formula;
        public final int color;

        public FunctionInput(String name, String formula, int color) {
            this.name = name == null || name.trim().isEmpty() ? formula : name.trim();
            this.formula = formula;
            this.color = color;
        }
    }

    public static final class FunctionReport {
        public final FunctionInput input;
        public final List<Double> roots = new ArrayList<>();
        public final List<Point> minima = new ArrayList<>();
        public final List<Point> maxima = new ArrayList<>();
        public double integral;
        public double derivativeAtCenter;
        public double centerX;

        FunctionReport(FunctionInput input) { this.input = input; }
    }

    public static final class Intersection {
        public final String a;
        public final String b;
        public final Point point;

        Intersection(String a, String b, Point point) {
            this.a = a; this.b = b; this.point = point;
        }
    }

    public static final class Point {
        public final double x;
        public final double y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    public static final class Report {
        public final List<FunctionReport> functions = new ArrayList<>();
        public final List<Intersection> intersections = new ArrayList<>();
    }

    private static final int SAMPLES = 1600;
    private static final double EPS = 1e-8;

    private AnalysisEngine() {}

    public static Report analyze(List<FunctionInput> inputs, double minX, double maxX) {
        Report report = new Report();
        List<Expression> expressions = new ArrayList<>();
        for (FunctionInput input : inputs) {
            Expression e = new Expression(input.formula);
            expressions.add(e);
            FunctionReport fr = new FunctionReport(input);
            fr.centerX = (minX + maxX) * 0.5;
            fr.derivativeAtCenter = derivative(e, fr.centerX, maxX - minX);
            fr.integral = simpson(e, minX, maxX, 1200);
            findRootsAndExtrema(e, minX, maxX, fr);
            report.functions.add(fr);
        }

        for (int i = 0; i < expressions.size(); i++) {
            for (int j = i + 1; j < expressions.size(); j++) {
                findIntersections(expressions.get(i), expressions.get(j), inputs.get(i), inputs.get(j), minX, maxX, report.intersections);
            }
        }
        return report;
    }

    public static List<Point> table(Expression expression, double minX, double maxX, int rows) {
        List<Point> result = new ArrayList<>();
        int count = Math.max(2, rows);
        for (int i = 0; i < count; i++) {
            double x = minX + (maxX - minX) * i / (count - 1.0);
            double y = safeEval(expression, x);
            result.add(new Point(x, y));
        }
        return result;
    }

    private static void findRootsAndExtrema(Expression e, double minX, double maxX, FunctionReport report) {
        double step = (maxX - minX) / SAMPLES;
        double prevX = minX;
        double prevY = safeEval(e, prevX);
        double prevSlope = Double.NaN;

        for (int i = 1; i <= SAMPLES; i++) {
            double x = minX + step * i;
            double y = safeEval(e, x);

            if (finite(prevY) && finite(y)) {
                if (Math.abs(prevY) < 1e-7) addUnique(report.roots, prevX, step * 0.6);
                if (prevY * y < 0) {
                    double root = bisectZero(e, prevX, x, prevY, y);
                    if (finite(root)) addUnique(report.roots, root, step * 0.6);
                }

                double slope = (y - prevY) / step;
                if (finite(prevSlope) && prevSlope * slope < 0) {
                    double cx = x - step;
                    double cy = safeEval(e, cx);
                    if (finite(cy)) {
                        Point p = refineExtremum(e, Math.max(minX, cx - step), Math.min(maxX, cx + step));
                        if (p != null) {
                            if (prevSlope < 0 && slope > 0) addUniquePoint(report.minima, p, step);
                            if (prevSlope > 0 && slope < 0) addUniquePoint(report.maxima, p, step);
                        }
                    }
                }
                prevSlope = slope;
            } else {
                prevSlope = Double.NaN;
            }
            prevX = x;
            prevY = y;
        }

        Collections.sort(report.roots);
        trim(report.roots, 10);
        trimPoints(report.minima, 8);
        trimPoints(report.maxima, 8);
    }

    private static void findIntersections(Expression a, Expression b, FunctionInput ia, FunctionInput ib,
                                          double minX, double maxX, List<Intersection> out) {
        double step = (maxX - minX) / SAMPLES;
        double px = minX;
        double py = diff(a, b, px);
        for (int i = 1; i <= SAMPLES; i++) {
            double x = minX + step * i;
            double y = diff(a, b, x);
            if (finite(py) && finite(y)) {
                if (Math.abs(py) < 1e-7 || py * y < 0) {
                    double root = Math.abs(py) < 1e-7 ? px : bisectDifference(a, b, px, x, py, y);
                    if (finite(root)) {
                        double value = safeEval(a, root);
                        if (finite(value) && !hasIntersectionNear(out, ia.name, ib.name, root, step)) {
                            out.add(new Intersection(ia.name, ib.name, new Point(root, value)));
                            if (out.size() >= 16) return;
                        }
                    }
                }
            }
            px = x;
            py = y;
        }
    }

    private static Point refineExtremum(Expression e, double left, double right) {
        if (!(left < right)) return null;
        for (int i = 0; i < 32; i++) {
            double m1 = left + (right - left) / 3.0;
            double m2 = right - (right - left) / 3.0;
            double y1 = safeEval(e, m1);
            double y2 = safeEval(e, m2);
            if (!finite(y1) || !finite(y2)) return null;
            if (y1 < y2) right = m2; else left = m1;
        }
        double x = (left + right) * 0.5;
        double y = safeEval(e, x);
        return finite(y) ? new Point(x, y) : null;
    }

    private static double derivative(Expression e, double x, double span) {
        double h = Math.max(1e-6, Math.abs(span) * 1e-5);
        double a = safeEval(e, x - h);
        double b = safeEval(e, x + h);
        return finite(a) && finite(b) ? (b - a) / (2.0 * h) : Double.NaN;
    }

    private static double simpson(Expression e, double a, double b, int n) {
        if ((n & 1) == 1) n++;
        double h = (b - a) / n;
        double sum = safeEval(e, a) + safeEval(e, b);
        if (!finite(sum)) return Double.NaN;
        for (int i = 1; i < n; i++) {
            double y = safeEval(e, a + i * h);
            if (!finite(y)) return Double.NaN;
            sum += (i % 2 == 0 ? 2 : 4) * y;
        }
        return sum * h / 3.0;
    }

    private static double bisectZero(Expression e, double left, double right, double fl, double fr) {
        for (int i = 0; i < 48; i++) {
            double mid = (left + right) * 0.5;
            double fm = safeEval(e, mid);
            if (!finite(fm)) return Double.NaN;
            if (Math.abs(fm) < EPS) return mid;
            if (fl * fm <= 0) { right = mid; fr = fm; }
            else { left = mid; fl = fm; }
        }
        return (left + right) * 0.5;
    }

    private static double bisectDifference(Expression a, Expression b, double left, double right, double fl, double fr) {
        for (int i = 0; i < 48; i++) {
            double mid = (left + right) * 0.5;
            double fm = diff(a, b, mid);
            if (!finite(fm)) return Double.NaN;
            if (Math.abs(fm) < EPS) return mid;
            if (fl * fm <= 0) { right = mid; fr = fm; }
            else { left = mid; fl = fm; }
        }
        return (left + right) * 0.5;
    }

    private static double diff(Expression a, Expression b, double x) {
        double av = safeEval(a, x), bv = safeEval(b, x);
        return finite(av) && finite(bv) ? av - bv : Double.NaN;
    }

    private static double safeEval(Expression e, double x) {
        try {
            double y = e.eval(x);
            return Math.abs(y) < 1e14 ? y : Double.NaN;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static boolean finite(double v) { return Double.isFinite(v); }

    private static void addUnique(List<Double> list, double value, double tolerance) {
        for (double v : list) if (Math.abs(v - value) <= tolerance) return;
        list.add(value);
    }

    private static void addUniquePoint(List<Point> list, Point p, double tolerance) {
        for (Point q : list) if (Math.abs(q.x - p.x) <= tolerance) return;
        list.add(p);
    }

    private static boolean hasIntersectionNear(List<Intersection> list, String a, String b, double x, double tol) {
        for (Intersection i : list) {
            boolean same = (i.a.equals(a) && i.b.equals(b)) || (i.a.equals(b) && i.b.equals(a));
            if (same && Math.abs(i.point.x - x) <= tol) return true;
        }
        return false;
    }

    private static void trim(List<Double> list, int max) {
        while (list.size() > max) list.remove(list.size() - 1);
    }

    private static void trimPoints(List<Point> list, int max) {
        while (list.size() > max) list.remove(list.size() - 1);
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) return "indefinido";
        if (Math.abs(value) < 1e-11) value = 0;
        double a = Math.abs(value);
        if ((a > 0 && a < 0.001) || a >= 100000) return String.format(java.util.Locale.US, "%.3e", value);
        return new DecimalFormat("0.####").format(value);
    }
}