package com.aifilecompanion.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Tab Navigation Widgets
    private Button mBtnTabChat, mBtnTabFiles, mBtnTabSettings;
    private View mPanelChat, mPanelFiles, mPanelSettings;

    // AI Workspace Elements
    private LinearLayout mContainerChat;
    private ScrollView mScrollChat;
    private EditText mEditPrompt;
    private Button mBtnSendPrompt, mBtnAttachFileDialog;
    private LinearLayout mLayoutAttachmentIndicator;
    private TextView mTxtAttachedName;
    private Button mBtnDetachFile;

    // File Manager Elements
    private ListView mListStoredFiles;
    private Button mBtnCreateNewFile;
    private LinearLayout mLayoutFileEditor;
    private TextView mTxtEditorTitle;
    private EditText mEditFileTitle, mEditFileContent;
    private Button mBtnEditorCancel, mBtnEditorSave;

    // API Config Elements
    private EditText mEditApiKey;
    private RadioButton mRadioRealApi, mRadioMockApi;
    private Button mBtnSaveConfig;

    // Application Logic Variables
    private List<String> mFileList;
    private FileAdapter mFileAdapter;
    private String mAttachedFileName = ""; // empty means none attached
    private String mEditingTargetName = ""; // empty means creating new

    // Configuration keys
    private static final String PREFS_NAME = "AIStudioPrefs";
    private static final String KEY_API_KEY = "GeminiKey";
    private static final String KEY_USE_REAL_API = "UseRealAPI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts and establish state variables
        initViews();
        setupNavigation();
        setupFileActions();
        setupSettingsAndPrefs();
        setupChatWorkspace();

        // Welcome introduction bubble
        showIntroductionBubble();
    }

    private void initViews() {
        // Tabs
        mBtnTabChat = (Button) findViewById(R.id.btn_tab_chat);
        mBtnTabFiles = (Button) findViewById(R.id.btn_tab_files);
        mBtnTabSettings = (Button) findViewById(R.id.btn_tab_settings);

        // Panels
        mPanelChat = findViewById(R.id.panel_chat);
        mPanelFiles = findViewById(R.id.panel_files);
        mPanelSettings = findViewById(R.id.panel_settings);

        // Chat View components
        mContainerChat = (LinearLayout) findViewById(R.id.container_chat);
        mScrollChat = (ScrollView) findViewById(R.id.scroll_chat);
        mEditPrompt = (EditText) findViewById(R.id.edit_prompt);
        mBtnSendPrompt = (Button) findViewById(R.id.btn_send_prompt);
        mBtnAttachFileDialog = (Button) findViewById(R.id.btn_attach_file_dialog);
        mLayoutAttachmentIndicator = (LinearLayout) findViewById(R.id.layout_attachment_indicator);
        mTxtAttachedName = (TextView) findViewById(R.id.txt_attached_name);
        mBtnDetachFile = (Button) findViewById(R.id.btn_detach_file);

        // File manager components
        mListStoredFiles = (ListView) findViewById(R.id.list_stored_files);
        mBtnCreateNewFile = (Button) findViewById(R.id.btn_create_new_file);
        mLayoutFileEditor = (LinearLayout) findViewById(R.id.layout_file_editor);
        mTxtEditorTitle = (TextView) findViewById(R.id.txt_editor_title);
        mEditFileTitle = (EditText) findViewById(R.id.edit_file_title);
        mEditFileContent = (EditText) findViewById(R.id.edit_file_content);
        mBtnEditorCancel = (Button) findViewById(R.id.btn_editor_cancel);
        mBtnEditorSave = (Button) findViewById(R.id.btn_editor_save);

        // Settings components
        mEditApiKey = (EditText) findViewById(R.id.edit_api_key);
        mRadioRealApi = (RadioButton) findViewById(R.id.radio_real_api);
        mRadioMockApi = (RadioButton) findViewById(R.id.radio_mock_api);
        mBtnSaveConfig = (Button) findViewById(R.id.btn_save_config);

        // Initialize file model list & set ListView Adapter
        mFileList = new ArrayList<String>();
        mFileAdapter = new FileAdapter();
        mListStoredFiles.setAdapter(mFileAdapter);
        refreshFileList();
    }

    private void setupNavigation() {
        mBtnTabChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPanel(mPanelChat, mBtnTabChat);
            }
        });

        mBtnTabFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPanel(mPanelFiles, mBtnTabFiles);
                refreshFileList();
            }
        });

        mBtnTabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPanel(mPanelSettings, mBtnTabSettings);
            }
        });
    }

    private void switchPanel(View activePanel, Button activeTabButton) {
        // Clear all panel visibility settings
        mPanelChat.setVisibility(View.GONE);
        mPanelFiles.setVisibility(View.GONE);
        mPanelSettings.setVisibility(View.GONE);

        // Turn all tabs to dull colors
        mBtnTabChat.setBackgroundColor(Color.parseColor("#4B5563"));
        mBtnTabFiles.setBackgroundColor(Color.parseColor("#4B5563"));
        mBtnTabSettings.setBackgroundColor(Color.parseColor("#4B5563"));

        // Activate selected target
        activePanel.setVisibility(View.VISIBLE);
        activeTabButton.setBackgroundColor(Color.parseColor("#3B82F6"));
    }

    // ==========================================
    // PERSISTENCE & SETTINGS
    // ==========================================

    private void setupSettingsAndPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String apiKey = prefs.getString(KEY_API_KEY, "");
        boolean useReal = prefs.getBoolean(KEY_USE_REAL_API, false);

        mEditApiKey.setText(apiKey);
        if (useReal) {
            mRadioRealApi.setChecked(true);
            mRadioMockApi.setChecked(false);
        } else {
            mRadioRealApi.setChecked(false);
            mRadioMockApi.setChecked(true);
        }

        mBtnSaveConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputKey = mEditApiKey.getText().toString().trim();
                boolean selectedReal = mRadioRealApi.isChecked();

                if (selectedReal && inputKey.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please insert a valid Gemini Key or choose local simulation mode.", Toast.LENGTH_LONG).show();
                    return;
                }

                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(KEY_API_KEY, inputKey);
                editor.putBoolean(KEY_USE_REAL_API, selectedReal);
                editor.apply();

                Toast.makeText(MainActivity.this, "AI System configurations updated!", Toast.LENGTH_SHORT).show();
                switchPanel(mPanelChat, mBtnTabChat);
            }
        });
    }

    // ==========================================
    // FILE VAULT MANAGER LOGIC
    // ==========================================

    private void setupFileActions() {
        // Toggle new file editor screen
        mBtnCreateNewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditingTargetName = ""; // flag as a new creation
                mTxtEditorTitle.setText("Create New Document");
                mEditFileTitle.setEnabled(true);
                mEditFileTitle.setText("");
                mEditFileContent.setText("");
                mLayoutFileEditor.setVisibility(View.VISIBLE);
            }
        });

        mBtnEditorCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutFileEditor.setVisibility(View.GONE);
                mEditingTargetName = "";
            }
        });

        mBtnEditorSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDocumentFromEditor();
            }
        });
    }

    private void refreshFileList() {
        mFileList.clear();
        File folder = getFilesDir();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.getName().endsWith(".json")) {
                    mFileList.add(file.getName());
                }
            }
        }
        mFileAdapter.notifyDataSetChanged();
    }

    private void saveDocumentFromEditor() {
        String filename = mEditFileTitle.getText().toString().trim();
        String contents = mEditFileContent.getText().toString();

        if (filename.isEmpty()) {
            Toast.makeText(this, "Please declare a valid file name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Standardize file extensions to represent raw text/code
        if (!filename.contains(".")) {
            filename += ".txt";
        }

        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(contents);
            osw.flush();
            osw.close();
            fos.close();

            Toast.makeText(this, "Document successfully secured!", Toast.LENGTH_SHORT).show();
            mLayoutFileEditor.setVisibility(View.GONE);
            mEditingTargetName = "";
            refreshFileList();
        } catch (Exception e) {
            Toast.makeText(this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String readDocumentContent(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream fis = openFileInput(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            fis.close();
        } catch (Exception e) {
            return "[Error loading " + filename + "]";
        }
        return sb.toString();
    }

    // ==========================================
    // CHAT ENGINE / AI CORE WORKSPACE
    // ==========================================

    private void setupChatWorkspace() {
        // Attached file UI detaching mechanism
        mBtnDetachFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAttachedFileName = "";
                mLayoutAttachmentIndicator.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "File detached from current context", Toast.LENGTH_SHORT).show();
            }
        });

        // Prompt Attachment selection dialog handler
        mBtnAttachFileDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAttachmentChooserDialog();
            }
        });

        // Prompt Execution button
        mBtnSendPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processUserPromptSubmission();
            }
        });
    }

    private void showAttachmentChooserDialog() {
        // Refresh items list
        refreshFileList();
        if (mFileList.isEmpty()) {
            Toast.makeText(this, "No saved files available. Create one in the 'My Files' explorer!", Toast.LENGTH_LONG).show();
            return;
        }

        final String[] items = mFileList.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Local File to Attach");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAttachedFileName = items[which];
                mTxtAttachedName.setText("Attached: " + mAttachedFileName);
                mLayoutAttachmentIndicator.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, mAttachedFileName + " linked as prompt context!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void processUserPromptSubmission() {
        String query = mEditPrompt.getText().toString().trim();
        if (query.isEmpty() && mAttachedFileName.isEmpty()) {
            Toast.makeText(this, "Please construct a prompt or append a saved context file.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate visual elements for user prompt bubble
        appendChatBubble("User", query, mAttachedFileName);
        mEditPrompt.setText("");

        // Retrieve background variables
        final String userPrompt = query;
        final String attachmentName = mAttachedFileName;
        final String fileContent = attachmentName.isEmpty() ? "" : readDocumentContent(attachmentName);

        // Reset attachment state instantly on UI
        mAttachedFileName = "";
        mLayoutAttachmentIndicator.setVisibility(View.GONE);

        // Initialize progress placeholder
        final View statusBubble = appendChatBubble("System AI", "Processing analytical request...", "");

        // Fetch user engine mode configuration
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean useReal = prefs.getBoolean(KEY_USE_REAL_API, false);
        final String apiKey = prefs.getString(KEY_API_KEY, "");

        if (useReal && !apiKey.isEmpty()) {
            // Launch parallel runtime request thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String response = queryGeminiLiveEngine(apiKey, userPrompt, attachmentName, fileContent);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            updateChatBubble(statusBubble, response);
                        }
                    });
                }
            }).start();
        } else {
            // Simulated local intelligence fallback
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    String localAnalysis = getSimulatedAIResponse(userPrompt, attachmentName, fileContent);
                    updateChatBubble(statusBubble, localAnalysis);
                }
            }, 1200);
        }
    }

    private void showIntroductionBubble() {
        String welcome = "Welcome to your local AI Studio Companion Workspace!\n\n" +
                "You can write and manage scripts or document files under 'My Files', " +
                "then instantly attach them to your queries. Perfect for code exploration or data analysis.\n\n" +
                "Tip: Switch over to 'API Key' settings to toggle simulated responses or live Gemini servers!";
        appendChatBubble("System AI", welcome, "");
    }

    // ==========================================
    // CORE NETWORK REQUEST / LIVE GEMINI API
    // ==========================================

    private String queryGeminiLiveEngine(String apiKey, String prompt, String attachedName, String attachedText) {
        try {
            // Format precise structural model payload
            String targetUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

            // Combine the core prompt instructions with contextual details
            StringBuilder complexQuery = new StringBuilder();
            if (!attachedName.isEmpty()) {
                complexQuery.append("[Context Document: ").append(attachedName).append("]\n");
                complexQuery.append("'''\n").append(attachedText).append("\n'''\n\n");
            }
            complexQuery.append("User Request: ").append(prompt);

            // Generate clean target json payload
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textPart = new JSONObject();

            textPart.put("text", complexQuery.toString());
            partsArray.put(textPart);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            String requestPayload = jsonBody.toString();

            // Establish standard networking connection
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            // Stream body
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            writer.write(requestPayload);
            writer.flush();
            writer.close();

            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseString.append(line);
                }
                reader.close();

                // Decode standard API structure safely
                JSONObject responseJson = new JSONObject(responseString.toString());
                JSONArray candidates = responseJson.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        return parts.getJSONObject(0).getString("text");
                    }
                }
                return "AI responded, but structured payload structure was empty.";
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                StringBuilder errorString = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorString.append(line);
                }
                reader.close();
                return "Live API Error Code: " + statusCode + "\n" + errorString.toString();
            }

        } catch (Exception e) {
            return "Failed to establish network connection: " + e.getMessage();
        }
    }

    private String getSimulatedAIResponse(String prompt, String attachedFileName, String fileContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 [SIMULATED LOCAL DEVELOPMENT AGENT RESPONSE]\n\n");

        if (!attachedFileName.isEmpty()) {
            sb.append("✓ Successfully detected attached file: *").append(attachedFileName).append("*\n");
            sb.append("✓ Scanned document lines (").append(fileContent.length()).append(" characters parsed).\n\n");

            // Evaluate attached file types/characteristics
            if (attachedFileName.endsWith(".java") || attachedFileName.endsWith(".kt") || fileContent.contains("class ") || fileContent.contains("void ")) {
                sb.append("⚙️ Code Analysis findings:\n");
                sb.append("- Detected Java object structure. Structure compiles cleanly against SDK modules.\n");
                sb.append("- Suggestion: Ensure memory resources and IO readers are safely disposed inside standard try-with-resources blocks.\n\n");
            } else if (fileContent.length() < 10) {
                sb.append("⚠️ Content Warning: Linked file context contains very short or blank records.\n\n");
            } else {
                sb.append("📁 Abstract Document metrics:\n");
                sb.append("- Keywords observed: ")
                  .append(fileContent.length() > 30 ? fileContent.substring(0, 30).trim() : "general plain contents")
                  .append("...\n- Suggest adding precise comments to improve readability.\n\n");
            }
        }

        sb.append("Response to query \"").append(prompt.isEmpty() ? "Inspect Attached Code" : prompt).append("\":\n");
        sb.append("This is an elegant simulated response generated from your integrated AI File Companion engine. ");
        sb.append("You can integrate real Gemini operations by creating a key on Google AI Studio, navigating to 'API Key' on the top-right header, and adding your credential key.");

        return sb.toString();
    }

    // ==========================================
    // UI CHAT RENDERING BUBBLES
    // ==========================================

    private View appendChatBubble(String speaker, String text, String attachment) {
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.wrap_content
        );
        containerParams.setMargins(0, 4, 0, 10);

        LinearLayout bubbleWrapper = new LinearLayout(this);
        bubbleWrapper.setLayoutParams(containerParams);
        bubbleWrapper.setOrientation(LinearLayout.VERTICAL);
        bubbleWrapper.setPadding(12, 10, 12, 10);

        // Header / Identity Label
        TextView label = new TextView(this);
        label.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        label.setTextSize(11);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 0, 0, 4);

        // Message Body
        TextView body = new TextView(this);
        body.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.setTextSize(14);
        body.setTextColor(Color.parseColor("#111827"));

        if ("User".equals(speaker)) {
            bubbleWrapper.setBackgroundColor(Color.parseColor("#E0F2FE")); // soft blue tint
            label.setText("YOU (USER)");
            label.setTextColor(Color.parseColor("#0369A1"));

            if (!attachment.isEmpty()) {
                TextView tag = new TextView(this);
                tag.setText("Attached document: " + attachment);
                tag.setTextSize(11);
                tag.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                tag.setTextColor(Color.parseColor("#0F766E"));
                tag.setPadding(0, 4, 0, 4);
                bubbleWrapper.addView(tag);
            }
        } else {
            bubbleWrapper.setBackgroundColor(Color.parseColor("#F3F4F6")); // soft grey tint
            label.setText("AI COMPANION");
            label.setTextColor(Color.parseColor("#374151"));
        }

        body.setText(text);

        bubbleWrapper.addView(label);
        bubbleWrapper.addView(body);

        mContainerChat.addView(bubbleWrapper);

        // Instant scroll down
        mScrollChat.post(new Runnable() {
            @Override
            public void run() {
                mScrollChat.fullScroll(View.FOCUS_DOWN);
            }
        });

        return bubbleWrapper;
    }

    private void updateChatBubble(View bubbleView, String dynamicText) {
        if (bubbleView instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) bubbleView;
            if (container.getChildCount() > 0) {
                // Identify target text view (last child in basic configuration setup)
                View target = container.getChildAt(container.getChildCount() - 1);
                if (target instanceof TextView) {
                    ((TextView) target).setText(dynamicText);
                }
            }
        }
        // Repost scroll update
        mScrollChat.post(new Runnable() {
            @Override
            public void run() {
                mScrollChat.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private int spToPx(float spValue) {
        float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    // ==========================================
    // FILES LIST ADAPTER IMPLEMENTATION
    // ==========================================

    private class FileAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mFileList.size();
        }

        @Override
        public Object getItem(int position) {
            return mFileList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Dynamic Layout structure to skip absolute XML inflation dependencies safely
                LinearLayout root = new LinearLayout(MainActivity.this);
                root.setLayoutParams(new AbsListViewLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                root.setOrientation(LinearLayout.HORIZONTAL);
                root.setPadding(12, 16, 12, 16);
                root.setGravity(Gravity.CENTER_VERTICAL);

                // Document Metadata Text container
                LinearLayout textContainer = new LinearLayout(MainActivity.this);
                textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                textContainer.setOrientation(LinearLayout.VERTICAL);

                TextView titleView = new TextView(MainActivity.this);
                titleView.setId(101);
                titleView.setTextColor(Color.parseColor("#1F2937"));
                titleView.setTextSize(15);
                titleView.setTypeface(Typeface.DEFAULT_BOLD);

                TextView subtitleView = new TextView(MainActivity.this);
                subtitleView.setId(102);
                subtitleView.setTextColor(Color.parseColor("#6B7280"));
                subtitleView.setTextSize(12);

                textContainer.addView(titleView);
                textContainer.addView(subtitleView);

                // Option Action Button Bar
                LinearLayout actionContainer = new LinearLayout(MainActivity.this);
                actionContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                actionContainer.setOrientation(LinearLayout.HORIZONTAL);

                Button btnAttach = new Button(MainActivity.this);
                btnAttach.setId(201);
                btnAttach.setText("Link");
                btnAttach.setTextSize(11);
                btnAttach.setLayoutParams(new LinearLayout.LayoutParams(60 * dpToPx(), 36 * dpToPx()));
                btnAttach.setTextColor(Color.parseColor("#FFFFFF"));
                btnAttach.setBackgroundColor(Color.parseColor("#10B981"));

                Button btnOptions = new Button(MainActivity.this);
                btnOptions.setId(202);
                btnOptions.setText("⚙");
                btnOptions.setTextSize(14);
                LinearLayout.LayoutParams optParams = new LinearLayout.LayoutParams(40 * dpToPx(), 36 * dpToPx());
                optParams.setMargins(6 * dpToPx(), 0, 0, 0);
                btnOptions.setLayoutParams(optParams);
                btnOptions.setTextColor(Color.parseColor("#374151"));
                btnOptions.setBackgroundColor(Color.parseColor("#E5E7EB"));

                actionContainer.addView(btnAttach);
                actionContainer.addView(btnOptions);

                root.addView(textContainer);
                root.addView(actionContainer);
                convertView = root;
            }

            final String filename = mFileList.get(position);
            TextView nameTxt = (TextView) convertView.findViewById(101);
            TextView metaTxt = (TextView) convertView.findViewById(102);
            Button linkBtn = (Button) convertView.findViewById(201);
            Button actionsBtn = (Button) convertView.findViewById(202);

            nameTxt.setText(filename);

            // Read metrics data
            String preview = readDocumentContent(filename);
            int charsCount = preview.length();
            metaTxt.setText("Size: " + charsCount + " chars");

            // Attach action to link code context on chat panel
            linkBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAttachedFileName = filename;
                    mTxtAttachedName.setText("Attached: " + mAttachedFileName);
                    mLayoutAttachmentIndicator.setVisibility(View.VISIBLE);
                    switchPanel(mPanelChat, mBtnTabChat);
                    Toast.makeText(MainActivity.this, filename + " attached as active context!", Toast.LENGTH_SHORT).show();
                }
            });

            // Dialog for actions: View/Edit, Delete
            actionsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFileOperationsMenu(filename);
                }
            });

            return convertView;
        }

        private int dpToPx() {
            float density = getResources().getDisplayMetrics().density;
            return (int) (1.0f * density + 0.5f);
        }

        private int spToPx(float spValue) {
            float fontScale = getResources().getDisplayMetrics().scaledDensity;
            return (int) (spValue * fontScale + 0.5f);
        }
    }

    private void showFileOperationsMenu(final String filename) {
        String[] options = {"Read & Modify File", "Delete File permanently"};
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Manage Document: " + filename);
        b.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Read and Modify Document
                    mEditingTargetName = filename;
                    mTxtEditorTitle.setText("Modify File Content");
                    mEditFileTitle.setText(filename);
                    mEditFileTitle.setEnabled(false); // cannot rename directly to prevent stream loss
                    mEditFileContent.setText(readDocumentContent(filename));
                    mLayoutFileEditor.setVisibility(View.VISIBLE);
                } else if (which == 1) {
                    // Standard removal safety confirmation
                    AlertDialog.Builder warn = new AlertDialog.Builder(MainActivity.this);
                    warn.setTitle("Delete Document");
                    warn.setMessage("Are you sure you want to permanently delete " + filename + "? This cannot be undone.");
                    warn.setPositiveButton("Confirm Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFile(filename);
                            if (mAttachedFileName.equals(filename)) {
                                mAttachedFileName = "";
                                mLayoutAttachmentIndicator.setVisibility(View.GONE);
                            }
                            refreshFileList();
                            Toast.makeText(MainActivity.this, "File purged successfully.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    warn.setNegativeButton("Cancel", null);
                    warn.show();
                }
            }
        });
        b.setNegativeButton("Close", null);
        b.show();
    }

    // Static safety wrapper for platform layout params mapping
    private static class AbsListViewLayoutParams extends ViewGroup.LayoutParams {
        public AbsListViewLayoutParams(int width, int height) {
            super(width, height);
        }
    }
}