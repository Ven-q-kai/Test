package com.venqkai.graphlab;

import java.util.Locale;

public final class Expression {
    private final String source;
    private int pos;
    private double x;

    public Expression(String source) {
        if (source == null) throw new IllegalArgumentException("Equação vazia");
        String s = source.trim().toLowerCase(Locale.US)
                .replace("π", "pi")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-");
        if (s.startsWith("y=")) s = s.substring(2);
        if (s.startsWith("y =")) s = s.substring(3);
        if (s.startsWith("f(x)=")) s = s.substring(5);
        if (s.startsWith("f(x) =")) s = s.substring(6);
        this.source = s.replace(" ", "");
        if (this.source.isEmpty()) throw new IllegalArgumentException("Equação vazia");
        validate();
    }

    public double eval(double x) {
        this.x = x;
        this.pos = 0;
        double value = parseExpression();
        if (pos != source.length()) {
            throw error("Símbolo inesperado: '" + source.charAt(pos) + "'");
        }
        return value;
    }

    private void validate() {
        eval(0.731);
    }

    private double parseExpression() {
        double value = parseTerm();
        while (true) {
            if (match('+')) value += parseTerm();
            else if (match('-')) value -= parseTerm();
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseUnary();
        while (true) {
            if (match('*')) value *= parseUnary();
            else if (match('/')) value /= parseUnary();
            else if (match('%')) value %= parseUnary();
            else if (isImplicitMultiplicationAhead()) value *= parseUnary();
            else return value;
        }
    }

    private double parseUnary() {
        if (match('+')) return parseUnary();
        if (match('-')) return -parseUnary();
        return parsePower();
    }

    private double parsePower() {
        double base = parsePrimary();
        while (match('!')) base = factorial(base);
        if (match('^')) return Math.pow(base, parseUnary());
        return base;
    }

    private double parsePrimary() {
        if (match('(')) {
            double value = parseExpression();
            require(')');
            return value;
        }
        if (peekDigit() || peek('.')) return parseNumber();
        if (peekLetter()) {
            String name = parseIdentifier();
            if (name.equals("x")) return x;
            if (name.equals("pi")) return Math.PI;
            if (name.equals("e")) return Math.E;
            if (name.equals("tau")) return Math.PI * 2.0;
            if (!match('(')) throw error("Função '" + name + "' precisa de parênteses");
            double a = parseExpression();
            if (match(',')) {
                double b = parseExpression();
                require(')');
                return applyBinary(name, a, b);
            }
            require(')');
            return applyUnary(name, a);
        }
        if (pos >= source.length()) throw error("Expressão incompleta");
        throw error("Símbolo inesperado: '" + source.charAt(pos) + "'");
    }

    private double parseNumber() {
        int start = pos;
        boolean hasExponent = false;
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (Character.isDigit(c) || c == '.') {
                pos++;
            } else if ((c == 'e' || c == 'E') && !hasExponent && exponentStartsAt(pos)) {
                hasExponent = true;
                pos++;
                if (pos < source.length() && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) pos++;
            } else break;
        }
        try {
            return Double.parseDouble(source.substring(start, pos));
        } catch (NumberFormatException ex) {
            throw error("Número inválido");
        }
    }

    private boolean exponentStartsAt(int index) {
        int next = index + 1;
        if (next >= source.length()) return false;
        char c = source.charAt(next);
        if (c == '+' || c == '-') next++;
        return next < source.length() && Character.isDigit(source.charAt(next));
    }

    private String parseIdentifier() {
        int start = pos;
        while (pos < source.length() && Character.isLetter(source.charAt(pos))) pos++;
        return source.substring(start, pos);
    }

    private double applyUnary(String name, double a) {
        switch (name) {
            case "sin": return Math.sin(a);
            case "cos": return Math.cos(a);
            case "tan": return Math.tan(a);
            case "asin": return Math.asin(a);
            case "acos": return Math.acos(a);
            case "atan": return Math.atan(a);
            case "sinh": return Math.sinh(a);
            case "cosh": return Math.cosh(a);
            case "tanh": return Math.tanh(a);
            case "sqrt": return Math.sqrt(a);
            case "cbrt": return Math.cbrt(a);
            case "abs": return Math.abs(a);
            case "ln": return Math.log(a);
            case "log": return Math.log10(a);
            case "exp": return Math.exp(a);
            case "floor": return Math.floor(a);
            case "ceil": return Math.ceil(a);
            case "round": return Math.rint(a);
            case "sign": return Math.signum(a);
            case "deg": return Math.toDegrees(a);
            case "rad": return Math.toRadians(a);
            default: throw error("Função desconhecida: " + name);
        }
    }

    private double applyBinary(String name, double a, double b) {
        switch (name) {
            case "pow": return Math.pow(a, b);
            case "min": return Math.min(a, b);
            case "max": return Math.max(a, b);
            case "root": return Math.pow(b, 1.0 / a);
            case "atan2": return Math.atan2(a, b);
            default: throw error("Função de dois argumentos desconhecida: " + name);
        }
    }

    private double factorial(double value) {
        if (!Double.isFinite(value) || value < 0 || Math.rint(value) != value || value > 170) return Double.NaN;
        double result = 1.0;
        for (int i = 2; i <= (int) value; i++) result *= i;
        return result;
    }

    private boolean isImplicitMultiplicationAhead() {
        if (pos >= source.length()) return false;
        char c = source.charAt(pos);
        return c == '(' || Character.isLetter(c);
    }

    private boolean match(char c) {
        if (peek(c)) { pos++; return true; }
        return false;
    }

    private void require(char c) {
        if (!match(c)) throw error("Esperado '" + c + "'");
    }

    private boolean peek(char c) {
        return pos < source.length() && source.charAt(pos) == c;
    }

    private boolean peekDigit() {
        return pos < source.length() && Character.isDigit(source.charAt(pos));
    }

    private boolean peekLetter() {
        return pos < source.length() && Character.isLetter(source.charAt(pos));
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " na posição " + (pos + 1));
    }
}
