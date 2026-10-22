package com.apexcalc.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure Java recursive descent expression parser and offline mathematical calculation engine.
 * Supports basic arithmetic (+, -, *, /), modulo %, powers (^), square root, constants (pi), and brackets.
 */
public class MathEvaluator {

    public static class EvaluationException extends Exception {
        public EvaluationException(String message) {
            super(message);
        }
    }

    public static double evaluate(String expression) throws EvaluationException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new EvaluationException("Empty expression");
        }

        // Normalize special symbols
        String cleanExpr = expression.replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", String.valueOf(Math.PI))
                .replace(" ", "");

        Parser parser = new Parser(cleanExpr);
        return parser.parse();
    }

    public static String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }

        // If the number is an exact integer, render without .0
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e14) {
            return String.format(java.util.Locale.US, "%,d", (long) value);
        }

        try {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
            return bd.toPlainString();
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static class Parser {
        private final String str;
        private int pos = -1;
        private int ch;

        Parser(String str) {
            this.str = str;
        }

        private void nextChar() {
            ch = (++pos < str.length()) ? str.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parse() throws EvaluationException {
            nextChar();
            double x = parseExpression();
            if (pos < str.length()) {
                throw new EvaluationException("Unexpected: " + (char) ch);
            }
            return x;
        }

        // Grammar:
        // expression = term | expression `+` term | expression `-` term
        // term = factor | term `*` factor | term `/` factor | term `%` factor
        // factor = `+` factor | `-` factor | `(` expression `)` | number | functionName factor | factor `^` factor

        private double parseExpression() throws EvaluationException {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) {
                    x += parseTerm();
                } else if (eat('-')) {
                    x -= parseTerm();
                } else {
                    return x;
                }
            }
        }

        private double parseTerm() throws EvaluationException {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) {
                    x *= parseFactor();
                } else if (eat('/')) {
                    double divisor = parseFactor();
                    if (divisor == 0.0) {
                        throw new EvaluationException("Divide by zero");
                    }
                    x /= divisor;
                } else if (eat('%')) {
                    double divisor = parseFactor();
                    if (divisor == 0.0) {
                        throw new EvaluationException("Divide by zero");
                    }
                    x %= divisor;
                } else {
                    return x;
                }
            }
        }

        private double parseFactor() throws EvaluationException {
            if (eat('+')) return parseFactor(); // unary plus
            if (eat('-')) return -parseFactor(); // unary minus

            double x;
            int startPos = this.pos;

            if (eat('(')) { // parentheses
                x = parseExpression();
                if (!eat(')')) {
                    throw new EvaluationException("Missing closing parenthesis");
                }
            } else if (eat('√')) { // square root
                double val = parseFactor();
                if (val < 0) {
                    throw new EvaluationException("Negative square root");
                }
                x = Math.sqrt(val);
            } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                try {
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } catch (NumberFormatException e) {
                    throw new EvaluationException("Invalid number format");
                }
            } else if (ch >= 'a' && ch <= 'z') { // functions e.g. sqrt
                while (ch >= 'a' && ch <= 'z') nextChar();
                String func = str.substring(startPos, this.pos);
                double arg = parseFactor();
                if (func.equals("sqrt")) {
                    if (arg < 0) throw new EvaluationException("Negative square root");
                    x = Math.sqrt(arg);
                } else {
                    throw new EvaluationException("Unknown function: " + func);
                }
            } else {
                throw new EvaluationException("Unexpected symbol: " + (ch == -1 ? "End of input" : String.valueOf((char) ch)));
            }

            if (eat('^')) {
                x = Math.pow(x, parseFactor()); // power exponent
            }

            return x;
        }
    }
}