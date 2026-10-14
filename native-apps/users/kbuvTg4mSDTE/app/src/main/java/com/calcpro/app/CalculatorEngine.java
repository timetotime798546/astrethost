package com.calcpro.app;

import java.util.ArrayList;
import java.util.List;

public class CalculatorEngine {

    public static String evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "0";
        }

        try {
            String cleaned = expression.replace('×', '*').replace('÷', '/');
            double result = evaluateExpression(cleaned);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "Error";
            }
            return formatResult(result);
        } catch (Exception e) {
            return "Error";
        }
    }

    private static String formatResult(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            String res = String.format("%.8f", value);
            while (res.endsWith("0") && res.contains(".")) {
                res = res.substring(0, res.length() - 1);
            }
            if (res.endsWith(".")) {
                res = res.substring(0, res.length() - 1);
            }
            return res;
        }
    }

    private static double evaluateExpression(String expr) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = expr.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else {
                        x = parseFactor();
                    }
                    if (func.equals("sqrt") || func.equals("√")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("log")) x = Math.log10(x);
                    else if (func.equals("ln")) x = Math.log(x);
                    else throw new RuntimeException("Unknown function: " + func);
                } else if (ch == 'π' || ch == 'pi') {
                    nextChar();
                    x = Math.PI;
                } else if (ch == 'e') {
                    nextChar();
                    x = Math.E;
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
    }
}