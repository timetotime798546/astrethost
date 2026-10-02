package com.devtoolbox.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends Activity {

    // Tab buttons
    private TextView tabBase64;
    private TextView tabJson;
    private TextView tabHash;

    // View Containers
    private ScrollView viewTabBase64;
    private ScrollView viewTabJson;
    private ScrollView viewTabHash;

    // Base64 controls
    private EditText b64Input;
    private TextView b64Output;
    private Button btnB64Encode;
    private Button btnB64Decode;
    private Button btnB64Copy;

    // JSON Formatter controls
    private EditText jsonInput;
    private TextView jsonStatus;
    private TextView jsonOutput;
    private Button btnJsonCopy;

    // Hash Generator controls
    private EditText hashInput;
    private TextView hashMd5Output;
    private TextView hashSha256Output;
    private Button btnCopyMd5;
    private Button btnCopySha256;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI Components
        initializeViews();

        // Setup click navigation logic
        setupNavigation();

        // Implement Base64 features
        setupBase64Logic();

        // Implement Live JSON features
        setupJsonLogic();

        // Implement Live Hasher features
        setupHasherLogic();
    }

    private void initializeViews() {
        tabBase64 = (TextView) findViewById(R.id.tab_base64);
        tabJson = (TextView) findViewById(R.id.tab_json);
        tabHash = (TextView) findViewById(R.id.tab_hash);

        viewTabBase64 = (ScrollView) findViewById(R.id.view_tab_base64);
        viewTabJson = (ScrollView) findViewById(R.id.view_tab_json);
        viewTabHash = (ScrollView) findViewById(R.id.view_tab_hash);

        b64Input = (EditText) findViewById(R.id.b64_input);
        b64Output = (TextView) findViewById(R.id.b64_output);
        btnB64Encode = (Button) findViewById(R.id.btn_b64_encode);
        btnB64Decode = (Button) findViewById(R.id.btn_b64_decode);
        btnB64Copy = (Button) findViewById(R.id.btn_b64_copy);

        jsonInput = (EditText) findViewById(R.id.json_input);
        jsonStatus = (TextView) findViewById(R.id.json_status);
        jsonOutput = (TextView) findViewById(R.id.json_output);
        btnJsonCopy = (Button) findViewById(R.id.btn_json_copy);

        hashInput = (EditText) findViewById(R.id.hash_input);
        hashMd5Output = (TextView) findViewById(R.id.hash_md5_output);
        hashSha256Output = (TextView) findViewById(R.id.hash_sha256_output);
        btnCopyMd5 = (Button) findViewById(R.id.btn_copy_md5);
        btnCopySha256 = (Button) findViewById(R.id.btn_copy_sha256);
    }

    private void setupNavigation() {
        tabBase64.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        tabJson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        tabHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });
    }

    private void switchTab(int index) {
        // Reset tab styles to inactive (#334155 bg, #94A3B8 text)
        tabBase64.setBackgroundColor(0xFF334155);
        tabBase64.setTextColor(0xFF94A3B8);
        tabJson.setBackgroundColor(0xFF334155);
        tabJson.setTextColor(0xFF94A3B8);
        tabHash.setBackgroundColor(0xFF334155);
        tabHash.setTextColor(0xFF94A3B8);

        // Hide all frames
        viewTabBase64.setVisibility(View.GONE);
        viewTabJson.setVisibility(View.GONE);
        viewTabHash.setVisibility(View.GONE);

        // Active active states
        if (index == 0) {
            tabBase64.setBackgroundColor(0xFF6366F1);
            tabBase64.setTextColor(0xFFFFFFFF);
            viewTabBase64.setVisibility(View.VISIBLE);
        } else if (index == 1) {
            tabJson.setBackgroundColor(0xFF6366F1);
            tabJson.setTextColor(0xFFFFFFFF);
            viewTabJson.setVisibility(View.VISIBLE);
        } else if (index == 2) {
            tabHash.setBackgroundColor(0xFF6366F1);
            tabHash.setTextColor(0xFFFFFFFF);
            viewTabHash.setVisibility(View.VISIBLE);
        }
    }

    private void setupBase64Logic() {
        btnB64Encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = b64Input.getText().toString();
                if (input.trim().isEmpty()) {
                    b64Output.setText("");
                    Toast.makeText(MainActivity.this, "Input text is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    byte[] data = input.getBytes("UTF-8");
                    String encoded = Base64.encodeToString(data, Base64.NO_WRAP);
                    b64Output.setText(encoded);
                } catch (UnsupportedEncodingException e) {
                    b64Output.setText("Encoding Error: UTF-8 not supported.");
                }
            }
        });

        btnB64Decode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = b64Input.getText().toString();
                if (input.trim().isEmpty()) {
                    b64Output.setText("");
                    Toast.makeText(MainActivity.this, "Input base64 code is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    byte[] decoded = Base64.decode(input, Base64.DEFAULT);
                    String decodedString = new String(decoded, "UTF-8");
                    b64Output.setText(decodedString);
                } catch (IllegalArgumentException iae) {
                    b64Output.setText("Syntax Error: Invalid Base64 input character/sequence.");
                } catch (Exception e) {
                    b64Output.setText("Decode Error: " + e.getLocalizedMessage());
                }
            }
        });

        btnB64Copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String outText = b64Output.getText().toString();
                if (!outText.isEmpty()) {
                    copyToClipboard(outText);
                    Toast.makeText(MainActivity.this, "Copied Base64 result!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupJsonLogic() {
        jsonInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                runLiveJsonFormatter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnJsonCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String outText = jsonOutput.getText().toString();
                if (!outText.isEmpty()) {
                    copyToClipboard(outText);
                    Toast.makeText(MainActivity.this, "Copied JSON to clipboard!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Formatter output is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void runLiveJsonFormatter(String raw) {
        String clean = raw.trim();
        if (clean.isEmpty()) {
            jsonStatus.setText("Waiting for input...");
            jsonStatus.setTextColor(0xFF94A3B8); // Default gray slate
            jsonOutput.setText("");
            return;
        }

        try {
            if (clean.startsWith("{")) {
                JSONObject obj = new JSONObject(clean);
                String formatted = obj.toString(4);
                jsonOutput.setText(formatted);
                jsonStatus.setText("✓ Valid JSON Object");
                jsonStatus.setTextColor(0xFF10B981); // Teal green
            } else if (clean.startsWith("[")) {
                JSONArray arr = new JSONArray(clean);
                String formatted = arr.toString(4);
                jsonOutput.setText(formatted);
                jsonStatus.setText("✓ Valid JSON Array");
                jsonStatus.setTextColor(0xFF10B981); // Teal green
            } else {
                jsonStatus.setText("✗ Syntax Error: JSON must begin with '{' or '['");
                jsonStatus.setTextColor(0xFFEF4444); // Slate Red
            }
        } catch (Exception e) {
            jsonStatus.setText("✗ Invalid Syntax: " + e.getMessage());
            jsonStatus.setTextColor(0xFFEF4444); // Tailwind Red-500
        }
    }

    private void setupHasherLogic() {
        // Calculate empty state hashes instantly on creation
        calculateHashes("");

        hashInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateHashes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCopyMd5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(hashMd5Output.getText().toString());
                Toast.makeText(MainActivity.this, "Copied MD5 Hash!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopySha256.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(hashSha256Output.getText().toString());
                Toast.makeText(MainActivity.this, "Copied SHA-256 Hash!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateHashes(String inputText) {
        hashMd5Output.setText(getMD5Hex(inputText));
        hashSha256Output.setText(getSHA256Hex(inputText));
    }

    private String getMD5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return convertBytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "MD5 generation failure";
        } catch (UnsupportedEncodingException e) {
            return "Encoding error";
        }
    }

    private String getSHA256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return convertBytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "SHA-256 generation failure";
        } catch (UnsupportedEncodingException e) {
            return "Encoding error";
        }
    }

    private String convertBytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("DevToolbox Result", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }
}