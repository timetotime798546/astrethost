package com.sqlmanager.app;

import java.util.ArrayList;
import java.util.List;

public class SqlParser {
    
    /**
     * Splits multi-line SQL input by semicolon while properly preserving semicolons
     * embedded inside single or double quotes, and stripping comments.
     */
    public static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return statements;
        }
        
        // Remove simple single-line SQL comments starting with --
        String[] lines = sql.split("\n");
        StringBuilder sbNoComments = new StringBuilder();
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.startsWith("--")) {
                continue;
            }
            // Strip comments that are mid-line
            int commentIdx = line.indexOf("--");
            if (commentIdx != -1) {
                // Check if comment indicator is inside a literal quote (basic validation)
                int singleQuotesBefore = 0;
                for (int i = 0; i < commentIdx; i++) {
                    if (line.charAt(i) == '\'') singleQuotesBefore++;
                }
                if (singleQuotesBefore % 2 == 0) {
                    line = line.substring(0, commentIdx);
                }
            }
            sbNoComments.append(line).append("\n");
        }
        
        String cleanSql = sbNoComments.toString().trim();
        StringBuilder sb = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inBracket = false;
        
        int length = cleanSql.length();
        for (int i = 0; i < length; i++) {
            char c = cleanSql.charAt(i);
            
            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
            } else if (c == '[' && !inSingleQuotes && !inDoubleQuotes) {
                inBracket = true;
            } else if (c == ']' && !inSingleQuotes && !inDoubleQuotes) {
                inBracket = false;
            }
            
            if (c == ';' && !inSingleQuotes && !inDoubleQuotes && !inBracket) {
                String statement = sb.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        
        String remaining = sb.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }
        
        return statements;
    }
    
    /**
     * Identifies the query command prefix type.
     */
    public static String getCommandType(String statement) {
        if (statement == null) return "UNKNOWN";
        String clean = statement.trim().toUpperCase();
        
        if (clean.startsWith("SELECT")) return "SELECT";
        if (clean.startsWith("INSERT")) return "INSERT";
        if (clean.startsWith("UPDATE")) return "UPDATE";
        if (clean.startsWith("DELETE")) return "DELETE";
        if (clean.startsWith("CREATE TABLE")) return "CREATE TABLE";
        if (clean.startsWith("DROP TABLE")) return "DROP TABLE";
        if (clean.startsWith("ALTER TABLE")) return "ALTER TABLE";
        if (clean.startsWith("CREATE INDEX")) return "CREATE INDEX";
        if (clean.startsWith("DROP INDEX")) return "DROP INDEX";
        if (clean.startsWith("PRAGMA")) return "PRAGMA";
        if (clean.startsWith("BEGIN")) return "BEGIN";
        if (clean.startsWith("COMMIT")) return "COMMIT";
        if (clean.startsWith("ROLLBACK")) return "ROLLBACK";
        
        // Match general statements
        String[] words = clean.split("\\s+");
        if (words.length > 0) {
            return words[0];
        }
        return "UNKNOWN";
    }
}