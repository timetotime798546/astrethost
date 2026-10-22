package com.luminacalc.app;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CalculatorEngine {

    private boolean isDegreeMode = true;

    public void setDegreeMode(boolean degreeMode) {
        this.isDegreeMode = degreeMode;
    }

    public boolean isDegreeMode() {
        return isDegreeMode;
    }

    public String evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "0";
        }

        String cleaned = expression.replace("×", "*")
                                   .replace("÷", "/")
                                   .replace("−", "-")
                                   .replace(" ", "");

        try {
            double result = evaluateInfix(cleaned);
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return "Cannot divide by zero";
            }
            return formatDoubleResult(result);
        } catch (ArithmeticException ae) {
            return "Error";
        } catch (Exception e) {
            return "Invalid expression";
        }
    }

    public String formatDoubleResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Error";
        }
        try {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.round(new MathContext(12, RoundingMode.HALF_UP));
            bd = bd.stripTrailingZeros();
            return bd.toPlainString();
        } catch (Exception e) {
            if (value == (long) value) {
                return String.format("%d", (long) value);
            } else {
                return String.format("%.8f", value).replaceAll("0*$", "").replaceAll("\\.$", "");
            }
        }
    }

    private double evaluateInfix(String exp) throws Exception {
        List<String> tokens = tokenize(exp);
        List<String> rpn = infixToRPN(tokens);
        return evaluateRPN(rpn);
    }

    private List<String> tokenize(String expr) throws Exception {
        List<String> tokens = new ArrayList<String>();
        int i = 0;
        int len = expr.length();

        while (i < len) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                while (i < len && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    num.append(expr.charAt(i));
                    i++;
                }
                tokens.add(num.toString());
                continue;
            }

            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^' || c == '(' || c == ')') {
                // Determine if '-' is unary negation
                if (c == '-') {
                    boolean isUnary = (i == 0);
                    if (!isUnary) {
                        String prev = tokens.get(tokens.size() - 1);
                        if (prev.equals("+") || prev.equals("-") || prev.equals("*") || prev.equals("/") 
                                || prev.equals("(") || prev.equals("^")) {
                            isUnary = true;
                        }
                    }
                    if (isUnary) {
                        // Check if followed by digit or expression
                        if (i + 1 < len && (Character.isDigit(expr.charAt(i + 1)) || expr.charAt(i + 1) == '.')) {
                            StringBuilder num = new StringBuilder("-");
                            i++;
                            while (i < len && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                                num.append(expr.charAt(i));
                                i++;
                            }
                            tokens.add(num.toString());
                            continue;
                        } else {
                            tokens.add("0"); // convert -X into 0 - X
                        }
                    }
                }
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }

            // Word tokens like sin, cos, tan, ln, log, sqrt
            if (Character.isLetter(c)) {
                StringBuilder word = new StringBuilder();
                while (i < len && Character.isLetter(expr.charAt(i))) {
                    word.append(expr.charAt(i));
                    i++;
                }
                tokens.add(word.toString());
                continue;
            }

            i++; // skip unknown char
        }

        return tokens;
    }

    private int getPrecedence(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/") || op.equals("%")) return 2;
        if (op.equals("^")) return 3;
        if (isFunction(op)) return 4;
        return 0;
    }

    private boolean isFunction(String token) {
        return token.equals("sin") || token.equals("cos") || token.equals("tan")
                || token.equals("ln") || token.equals("log") || token.equals("sqrt")
                || token.equals("fact");
    }

    private List<String> infixToRPN(List<String> tokens) throws Exception {
        List<String> output = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (isNumber(token)) {
                output.add(token);
            } else if (isFunction(token)) {
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (!stack.isEmpty() && stack.peek().equals("(")) {
                    stack.pop(); // remove '('
                }
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(stack.pop());
                }
            } else {
                // Operator
                while (!stack.isEmpty() && getPrecedence(stack.peek()) >= getPrecedence(token) && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (!op.equals("(") && !op.equals(")")) {
                output.add(op);
            }
        }

        return output;
    }

    private double evaluateRPN(List<String> rpn) throws Exception {
        Stack<Double> stack = new Stack<Double>();

        for (int i = 0; i < rpn.size(); i++) {
            String token = rpn.get(i);

            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isFunction(token)) {
                if (stack.isEmpty()) throw new Exception("Invalid function stack");
                double arg = stack.pop();
                stack.push(executeFunction(token, arg));
            } else {
                if (stack.size() < 2) throw new Exception("Invalid operator stack");
                double b = stack.pop();
                double a = stack.pop();

                if (token.equals("+")) stack.push(a + b);
                else if (token.equals("-")) stack.push(a - b);
                else if (token.equals("*")) stack.push(a * b);
                else if (token.equals("/")) {
                    if (b == 0) throw new ArithmeticException("Division by zero");
                    stack.push(a / b);
                } else if (token.equals("%")) {
                    stack.push(a * (b / 100.0));
                } else if (token.equals("^")) {
                    stack.push(Math.pow(a, b));
                }
            }
        }

        if (stack.isEmpty()) return 0;
        return stack.pop();
    }

    private double executeFunction(String func, double arg) {
        if (func.equals("sin")) {
            double radians = isDegreeMode ? Math.toRadians(arg) : arg;
            return Math.sin(radians);
        } else if (func.equals("cos")) {
            double radians = isDegreeMode ? Math.toRadians(arg) : arg;
            return Math.cos(radians);
        } else if (func.equals("tan")) {
            double radians = isDegreeMode ? Math.toRadians(arg) : arg;
            return Math.tan(radians);
        } else if (func.equals("ln")) {
            return Math.log(arg);
        } else if (func.equals("log")) {
            return Math.log10(arg);
        } else if (func.equals("sqrt")) {
            return Math.sqrt(arg);
        } else if (func.equals("fact")) {
            return factorial((long) arg);
        }
        return arg;
    }

    private double factorial(long n) {
        if (n < 0) return 0;
        if (n == 0 || n == 1) return 1;
        double fact = 1;
        for (long i = 2; i <= n && i <= 30; i++) {
            fact *= i;
        }
        return fact;
    }

    private boolean isNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}