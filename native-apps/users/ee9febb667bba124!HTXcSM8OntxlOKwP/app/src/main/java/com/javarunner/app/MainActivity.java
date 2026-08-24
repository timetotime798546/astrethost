package com.javarunner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText etCodeEditor;
    private TextView tvLineNumbers;
    private TextView tvConsoleOutput;
    private ScrollView consoleScroll;
    private TextView tvActiveTab;
    private LinearLayout fileListContainer;
    private Button btnCompile, btnRun, btnStop;

    private Map<String, String> projectFiles = new HashMap<>();
    private String activeFileName = "Main.java";
    private JavaEngine currentEngine = null;
    private Thread executionThread = null;
    private boolean isCompiling = false;

    // Regex compiler patterns for editor syntax highlighting
    private final Pattern keyWordPattern = Pattern.compile("\\b(class|public|static|void|int|double|boolean|String|if|else|for|while|return|new|import|private|protected)\\b");
    private final Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
    private final Pattern stringPattern = Pattern.compile("\"([^\"\\\\\\\\]|\\\\\\\\.)*\"");
    private final Pattern commentPattern = Pattern.compile("//.*|/\\*.*?\\*/", Pattern.DOTALL);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCodeEditor = findViewById(R.id.et_code_editor);
        tvLineNumbers = findViewById(R.id.tv_line_numbers);
        tvConsoleOutput = findViewById(R.id.tv_console_output);
        consoleScroll = findViewById(R.id.console_scroll);
        tvActiveTab = findViewById(R.id.tv_active_tab);
        fileListContainer = findViewById(R.id.file_list_container);
        btnCompile = findViewById(R.id.btn_compile);
        btnRun = findViewById(R.id.btn_run);
        btnStop = findViewById(R.id.btn_stop);

        setupDefaultWorkspace();
        updateFileListUI();
        loadActiveFileToEditor();

        // TextWatcher to handle real-time syntax styling and line counts
        etCodeEditor.addTextChangedListener(new TextWatcher() {
            private boolean isRestyling = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Update local storage representation
                projectFiles.put(activeFileName, s.toString());
                saveFileToDisk(activeFileName, s.toString());

                // Perform line counter sync
                updateLineNumbers();

                if (isRestyling) return;
                isRestyling = true;

                // Highlighting implementation
                clearSpans(s);
                highlightSyntax(s);

                isRestyling = false;
            }
        });

        findViewById(R.id.btn_new_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateFileDialog();
            }
        });

        findViewById(R.id.tv_clear_console).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvConsoleOutput.setText("");
            }
        });

        btnCompile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performCompilation();
            }
        });

        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performExecution();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopExecution();
            }
        });
    }

    private void setupDefaultWorkspace() {
        File dir = getFilesDir();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            // Create entry sample code template
            String mainTemplate = "public class Main {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Welcome to local Java Runner!\");\n" +
                    "        int iterations = 5;\n" +
                    "        System.out.println(\"Starting iterative loop with bounds...\");\n" +
                    "        for (int i = 0; i < iterations; i++) {\n" +
                    "            System.out.println(\"Running item index \" + i);\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            String utilsTemplate = "public class Utils {\n" +
                    "    // Helper methods can be included here\n" +
                    "}";
            saveFileToDisk("Main.java", mainTemplate);
            saveFileToDisk("Utils.java", utilsTemplate);
        }

        // Read internal files
        File[] updatedFiles = dir.listFiles();
        if (updatedFiles != null) {
            for (File file : updatedFiles) {
                if (file.isFile() && file.getName().endsWith(".java")) {
                    projectFiles.put(file.getName(), readFileFromDisk(file));
                }
            }
        }
    }

    private void updateFileListUI() {
        fileListContainer.removeAllViews();
        for (final String fileName : projectFiles.keySet()) {
            TextView tv = new TextView(this);
            tv.setText(fileName);
            tv.setTextSize(12);
            tv.setPadding(8, 12, 8, 12);
            tv.setSingleLine(true);

            if (fileName.equals(activeFileName)) {
                tv.setTextColor(Color.parseColor("#03DAC6"));
                tv.setBackgroundColor(Color.parseColor("#2c2c2c"));
            } else {
                tv.setTextColor(Color.parseColor("#e0e0e0"));
            }

            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activeFileName = fileName;
                    updateFileListUI();
                    loadActiveFileToEditor();
                }
            });

            tv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showDeleteFileDialog(fileName);
                    return true;
                }
            });

            fileListContainer.addView(tv);
        }
    }

    private void loadActiveFileToEditor() {
        tvActiveTab.setText(activeFileName);
        String content = projectFiles.get(activeFileName);
        if (content == null) content = "";
        etCodeEditor.setText(content);
        updateLineNumbers();
    }

    private void updateLineNumbers() {
        int linesCount = etCodeEditor.getLineCount();
        if (linesCount == 0) linesCount = 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= linesCount; i++) {
            sb.append(i).append("\n");
        }
        tvLineNumbers.setText(sb.toString());
    }

    private void highlightSyntax(Editable s) {
        // Apply KeyWord syntax formatting
        Matcher m = keyWordPattern.matcher(s);
        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(Color.parseColor("#C678DD")), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Apply numbers
        m = numberPattern.matcher(s);
        while (m.find()) { 
            s.setSpan(new ForegroundColorSpan(Color.parseColor("#D19A66")), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Apply String matching blocks
        m = stringPattern.matcher(s);
        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(Color.parseColor("#98C379")), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // Apply annotations / Comments code statements
        m = commentPattern.matcher(s);
        while (m.find()) {
            s.setSpan(new ForegroundColorSpan(Color.parseColor("#5C6370")), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void clearSpans(Editable s) {
        ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            s.removeSpan(span);
        }
    }

    private void showCreateFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("Create Java File");

        final EditText input = new EditText(this);
        input.setHint("Calculator.java");
        input.setTextColor(Color.WHITE);
        builder.setView(input);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Filename cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!name.endsWith(".java")) {
                    name += ".java";
                }
                if (projectFiles.containsKey(name)) {
                    Toast.makeText(MainActivity.this, "File already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String nameWithoutExtension = name.substring(0, name.lastIndexOf(".java"));
                String template = "public class " + nameWithoutExtension + " {\n\n}";
                projectFiles.put(name, template);
                saveFileToDisk(name, template);
                activeFileName = name;
                updateFileListUI();
                loadActiveFileToEditor();
            } 
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteFileDialog(final String fileName) { 
        if (projectFiles.size() <= 1) {
            Toast.makeText(this, "You must keep at least one source file in the workspace.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK); 
        builder.setTitle("Delete File");
        builder.setMessage("Are you sure you want to delete " + fileName + "?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                projectFiles.remove(fileName);
                File file = new File(getFilesDir(), fileName);
                if (file.exists()) file.delete();
                if (activeFileName.equals(fileName)) {
                    activeFileName = projectFiles.keySet().iterator().next();
                }
                updateFileListUI();
                loadActiveFileToEditor();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void performCompilation() {
        if (isCompiling) return;
        isCompiling = true;
        btnCompile.setEnabled(false);

        appendConsoleLog("Checking system environment configuration...", false);
        
        JavaEngine testEngine = new JavaEngine(projectFiles, new JavaEngine.LogCallback() {
            @Override
            public void onLog(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendConsoleLog(text, false);
                    }
                });
            }

            @Override
            public void onError(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendConsoleLog(text, true);
                    }
                });
            }
        });

        boolean compilationResult = testEngine.compile();
        isCompiling = false;
        btnCompile.setEnabled(true);
        
        if (compilationResult) {
            Toast.makeText(this, "Compilation succeeded.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Compilation failed! Review console errors.", Toast.LENGTH_LONG).show();
        }
    }

    private void performExecution() {
        if (currentEngine != null) {
            Toast.makeText(this, "An instance is already executing.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRun.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        btnCompile.setEnabled(false);

        currentEngine = new JavaEngine(projectFiles, new JavaEngine.LogCallback() {
            @Override
            public void onLog(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendConsoleLog(text, false);
                    }
                });
            }

            @Override
            public void onError(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendConsoleLog(text, true);
                    }
                });
            }
        });

        executionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (currentEngine.compile()) {
                    currentEngine.run();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resetExecutionControls();
                    }
                });
            }
        });
        executionThread.start();
    }

    private void stopExecution() {
        if (currentEngine != null) {
            currentEngine.stop();
            if (executionThread != null && executionThread.isAlive()) {
                executionThread.interrupt();
            }
            appendConsoleLog("Process forcefully terminated by stop request.", true);
            resetExecutionControls();
        }
    }

    private void resetExecutionControls() {
        currentEngine = null;
        executionThread = null;
        btnRun.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.GONE);
        btnCompile.setEnabled(true);
    }

    private void appendConsoleLog(String message, boolean isError) {
        SpannableString spannable = new SpannableString(message + "\n");
        int color = isError ? Color.parseColor("#CF6679") : Color.parseColor("#98c379");
        spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvConsoleOutput.append(spannable);
        
        // Auto scroll to latest runtime statement
        consoleScroll.post(new Runnable() {
            @Override
            public void run() {
                consoleScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    // IO File utilities helper procedures
    private void saveFileToDisk(String name, String data) {
        File file = new File(getFilesDir(), name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }
    }

    private String readFileFromDisk(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
        }
    }
}