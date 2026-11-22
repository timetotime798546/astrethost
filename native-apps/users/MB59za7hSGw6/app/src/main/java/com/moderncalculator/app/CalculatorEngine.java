package com.moderncalculator.app;

import java.util.ArrayList;
import java.util.List;

public class CalculatorEngine {

    private boolean isDegreeMode = true;

    public void setDegreeMode(boolean degreeMode) {
        this.isDegreeMode = degreeMode;
    }

    public boolean isDegreeMode() {
        return isDegreeMode;
    }

    public double evaluate(String expression) throws Exception {
        String sanitized = sanitize(expression);
        return parseExpression(sanitized);
    }

    private String sanitize(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return "0";
        }
        return expr.replace("×", "*")
                .replace("÷", "/")
                .replace("π", String.valueOf(Math.PI))
                .replace("e", String.valueOf(Math.E))
                .trim();
    }

    private double parseExpression(final String str) throws Exception {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() throws Exception {
                nextChar();
                double x = parseTerm();
                while (true) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else break;
                }
                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected token: " + (char) ch);
                }
                return x;
            }

            double parseTerm() throws Exception {
                double x = parseFactor();
                while (true) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        if (divisor == 0) throw new ArithmeticException("Divide by zero");
                        x /= divisor;
                    } else if (eat('%')) {
                        x %= parseFactor();
                    } else break;
                }
                return x;
            }

            double parseFactor() throws Exception {
                if (eat('+')) return parseFactor(); // Unary plus
                if (eat('-')) return -parseFactor(); // Unary minus

                double x;
                int startPos = this.pos;

                if (eat('(')) {
                    x = parse();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parse();
                        eat(')');
                    } else {
                        x = parseFactor();
                    }

                    if (func.equals("sin")) {
                        x = isDegreeMode ? Math.sin(Math.toRadians(x)) : Math.sin(x);
                    } else if (func.equals("cos")) {
                        x = isDegreeMode ? Math.cos(Math.toRadians(x)) : Math.cos(x);
                    } else if (func.equals("tan")) {
                        x = isDegreeMode ? Math.tan(Math.toRadians(x)) : Math.tan(x);
                    } else if (func.equals("sqrt")) {
                        if (x < 0) throw new ArithmeticException("Invalid input for sqrt");
                        x = Math.sqrt(x);
                    } else if (func.equals("log")) {
                        if (x <= 0) throw new ArithmeticException("Invalid input for log");
                        x = Math.log10(x);
                    } else if (func.equals("ln")) {
                        if (x <= 0) throw new ArithmeticException("Invalid input for ln");
                        x = Math.log(x);
                    } else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected character: " + (char) ch);
                }

                if (eat('^')) {
                    x = Math.pow(x, parseFactor());
                }

                if (eat('!')) {
                    x = factorial((int) x);
                }

                return x;
            }

            private double factorial(int n) {
                if (n < 0) return Double.NaN;
                double res = 1;
                for (int i = 2; i <= n; i++) {
                    res *= i;
                }
                return res;
            }

        }.parse();
    }
}