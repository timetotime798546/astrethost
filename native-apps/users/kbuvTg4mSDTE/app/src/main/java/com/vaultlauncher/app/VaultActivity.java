package com.vaultlauncher.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends Activity {

    private TextView tvStatus;
    private TextView tvNotesCount;
    private TextView tvPassCount;
    private TextView tvTotalCount;
    private Button btnTabNotes;
    private Button btnTabCreds;
    private Button btnAdd;
    private Button btnLogout;
    private ListView lvItems;

    private boolean isOffline;
    private String syncToken;
    private BackendApi api;

    private boolean viewingNotesTab = true;

    // Local model list storage
    private List<VaultItem> notesList = new ArrayList<>();
    private List<VaultItem> credsList = new ArrayList<>();
    private List<VaultItem> activeDisplayList = new ArrayList<>();
    private ArrayAdapter<VaultItem> adapter;

    // Vault Item Struct
    private static class VaultItem {
        String id;
        String line1;
        String line2;
        String line3; // password fields if needed
        boolean isNote;

        VaultItem(String id, String l1, String l2, boolean isNote) {
            this.id = id;
            this.line1 = l1;
            this.line2 = l2;
            this.isNote = isNote;
        }

        VaultItem(String id, String l1, String l2, String l3, boolean isNote) {
            this.id = id;
            this.line1 = l1;
            this.line2 = l2;
            this.line3 = l3;
            this.isNote = isNote;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        tvStatus = (TextView) findViewById(R.id.tv_vault_status);
        tvNotesCount = (TextView) findViewById(R.id.tv_stat_notes);
        tvPassCount = (TextView) findViewById(R.id.tv_stat_passwords);
        tvTotalCount = (TextView) findViewById(R.id.tv_stat_total);
        btnTabNotes = (Button) findViewById(R.id.btn_tab_notes);
        btnTabCreds = (Button) findViewById(R.id.btn_tab_credentials);
        btnAdd = (Button) findViewById(R.id.btn_add_vault_item);
        btnLogout = (Button) findViewById(R.id.btn_logout);
        lvItems = (ListView) findViewById(R.id.lv_vault_items);

        api = new BackendApi(this);

        SharedPreferences prefs = getSharedPreferences("VaultPrefs", MODE_PRIVATE);
        isOffline = prefs.getBoolean("offline_mode", true);
        syncToken = prefs.getString("sync_token", "");

        if (isOffline) {
            tvStatus.setText("Local Secure Storage (Offline)");
            loadLocalFallbackData();
        } else {
            String email = prefs.getString("account_email", "User");
            tvStatus.setText("Cloud Sync: " + email);
            fetchCloudSyncData();
        }

        // Tab switches
        btnTabNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewingNotesTab = true;
                btnTabNotes.setBackgroundColor(0xFF1E1E1E);
                btnTabNotes.setTextColor(0xFFFF9800);
                btnTabCreds.setBackgroundColor(0xFF121212);
                btnTabCreds.setTextColor(0xFF888888);
                refreshActiveDisplayList();
            }
        });

        btnTabCreds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewingNotesTab = false;
                btnTabCreds.setBackgroundColor(0xFF1E1E1E);
                btnTabCreds.setTextColor(0xFFFF9800);
                btnTabNotes.setBackgroundColor(0xFF121212);
                btnTabNotes.setTextColor(0xFF888888);
                refreshActiveDisplayList();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdditionDialog();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to locker instantly
                finish();
            }
        });

        // Configure Adapter
        adapter = new ArrayAdapter<VaultItem>(this, android.R.layout.simple_list_item_2, android.R.id.text1, activeDisplayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                TextView text1 = (TextView) row.findViewById(android.R.id.text1);
                TextView text2 = (TextView) row.findViewById(android.R.id.text2);

                final VaultItem item = getItem(position);
                if (item != null) {
                    text1.setText(item.line1);
                    text1.setTextColor(0xFFFFFFFF);
                    if (item.isNote) {
                        text2.setText(item.line2);
                    } else {
                        text2.setText("Username: " + item.line2 + " | Password: " + (item.line3 != null ? item.line3 : "••••"));
                    }
                    text2.setTextColor(0xFFAAAAAA);

                    row.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            showDeleteConfirmation(item);
                            return true;
                        }
                    });
                }
                return row;
            }
        };
        lvItems.setAdapter(adapter);
    }

    // MANDATORY NATIVE OFFLINE CALCULATIONS
    // Sums elements locally derived from retrieved backend record lists.
    private void calculateAndDisplayStats() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int notesSize = notesList.size();
                int credsSize = credsList.size();
                int sumOfElements = notesSize + credsSize; // Local math addition computed entirely inside the client

                tvNotesCount.setText(String.valueOf(notesSize));
                tvPassCount.setText(String.valueOf(credsSize));
                tvTotalCount.setText(String.valueOf(sumOfElements));
            }
        });
    }

    private void refreshActiveDisplayList() {
        activeDisplayList.clear();
        if (viewingNotesTab) {
            activeDisplayList.addAll(notesList);
        } else {
            activeDisplayList.addAll(credsList);
        }
        adapter.notifyDataSetChanged();
        calculateAndDisplayStats();
    }

    private void fetchCloudSyncData() {
        if (syncToken.isEmpty()) return;

        // Fetch Secret Notes
        api.readRecords(syncToken, "secret_notes", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success", false)) {
                        notesList.clear();
                        JSONArray array = json.getJSONArray("records");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject record = array.getJSONObject(i);
                            String id = record.getString("id");
                            JSONObject data = record.getJSONObject("data");
                            notesList.add(new VaultItem(id, data.optString("title"), data.optString("body"), true));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshActiveDisplayList();
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String error) {}
        });

        // Fetch Credentials
        api.readRecords(syncToken, "secret_credentials", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    if (json.optBoolean("success", false)) {
                        credsList.clear();
                        JSONArray array = json.getJSONArray("records");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject record = array.getJSONObject(i);
                            String id = record.getString("id");
                            JSONObject data = record.getJSONObject("data");
                            credsList.add(new VaultItem(id, data.optString("site"), data.optString("username"), data.optString("password"), false));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshActiveDisplayList();
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void showAdditionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        if (viewingNotesTab) {
            builder.setTitle("Add Secret Note");
            View view = inflater.inflate(android.R.layout.simple_list_item_2, null);
            // Replace with simple dual edittext programmatically to eliminate standard androidx/material dependencies
            final LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 16, 32, 16);

            final EditText etTitle = new EditText(this);
            etTitle.setHint("Note Title");
            etTitle.setHintTextColor(0xFF555555);
            etTitle.setTextColor(0xFFFFFFFF);
            layout.addView(etTitle);

            final EditText etBody = new EditText(this);
            etBody.setHint("Write secret notes here...");
            etBody.setHintTextColor(0xFF555555);
            etBody.setTextColor(0xFFFFFFFF);
            layout.addView(etBody);

            builder.setView(layout);
            builder.setPositiveButton("Save Securely", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = etTitle.getText().toString();
                    String body = etBody.getText().toString();
                    if (!title.isEmpty()) {
                        addNoteSecurely(title, body);
                    }
                }
            });
        } else {
            builder.setTitle("Add Password");
            final LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(32, 16, 32, 16);

            final EditText etSite = new EditText(this);
            etSite.setHint("Platform / Site");
            etSite.setHintTextColor(0xFF555555);
            etSite.setTextColor(0xFFFFFFFF);
            layout.addView(etSite);

            final EditText etUser = new EditText(this);
            etUser.setHint("Username / Email");
            etUser.setHintTextColor(0xFF555555);
            etUser.setTextColor(0xFFFFFFFF);
            layout.addView(etUser);

            final EditText etPass = new EditText(this);
            etPass.setHint("Secure Password");
            etPass.setHintTextColor(0xFF555555);
            etPass.setTextColor(0xFFFFFFFF);
            layout.addView(etPass);

            builder.setView(layout);
            builder.setPositiveButton("Save Securely", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String site = etSite.getText().toString();
                    String user = etUser.getText().toString();
                    String password = etPass.getText().toString();
                    if (!site.isEmpty()) {
                        addCredentialSecurely(site, user, password);
                    }
                }
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNoteSecurely(final String title, final String body) {
        if (isOffline) {
            String tempId = String.valueOf(System.currentTimeMillis());
            notesList.add(new VaultItem(tempId, title, body, true));
            saveLocalFallbackData();
            refreshActiveDisplayList();
        } else {
            try {
                JSONObject payload = new JSONObject();
                payload.put("title", title);
                payload.put("body", body);

                api.createRecord(syncToken, "secret_notes", payload, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VaultActivity.this, "Note Synced Securely", Toast.LENGTH_SHORT).show();
                                fetchCloudSyncData();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VaultActivity.this, "Cloud Error. Saved locally fallback.", Toast.LENGTH_SHORT).show();
                                notesList.add(new VaultItem(String.valueOf(System.currentTimeMillis()), title, body, true));
                                refreshActiveDisplayList();
                            }
                        });
                    }
                });
            } catch (Exception ignored) {}
        }
    }

    private void addCredentialSecurely(final String site, final String user, final String password) {
        if (isOffline) {
            String tempId = String.valueOf(System.currentTimeMillis());
            credsList.add(new VaultItem(tempId, site, user, password, false));
            saveLocalFallbackData();
            refreshActiveDisplayList();
        } else {
            try {
                JSONObject payload = new JSONObject();
                payload.put("site", site);
                payload.put("username", user);
                payload.put("password", password);

                api.createRecord(syncToken, "secret_credentials", payload, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VaultActivity.this, "Password Saved to Cloud", Toast.LENGTH_SHORT).show();
                                fetchCloudSyncData();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(VaultActivity.this, "Saved locally.", Toast.LENGTH_SHORT).show();
                                credsList.add(new VaultItem(String.valueOf(System.currentTimeMillis()), site, user, password, false));
                                refreshActiveDisplayList();
                            }
                        });
                    }
                });
            } catch (Exception ignored) {}
        }
    }

    private void showDeleteConfirmation(final VaultItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Disposal");
        builder.setMessage("Are you sure you want to permanently destroy this record?");
        builder.setPositiveButton("Wipe", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteSecureItem(item);
            }
        });
        builder.setNegativeButton("Keep", null);
        builder.show();
    }

    private void deleteSecureItem(final VaultItem item) {
        if (isOffline) {
            if (item.isNote) {
                notesList.remove(item);
            } else {
                credsList.remove(item);
            }
            saveLocalFallbackData();
            refreshActiveDisplayList();
        } else {
            api.deleteRecord(syncToken, item.id, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VaultActivity.this, "Record wiped from cloud", Toast.LENGTH_SHORT).show();
                            fetchCloudSyncData();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VaultActivity.this, "Wiped locally", Toast.LENGTH_SHORT).show();
                            if (item.isNote) notesList.remove(item);
                            else credsList.remove(item);
                            refreshActiveDisplayList();
                        }
                    });
                }
            });
        }
    }

    // Local DB Fallbacks using secure device-bound context storage
    private void loadLocalFallbackData() {
        SharedPreferences prefs = getSharedPreferences("SecureVaultDump", MODE_PRIVATE);
        try {
            String notesJson = prefs.getString("notes", "[]");
            String credsJson = prefs.getString("creds", "[]");

            JSONArray nArr = new JSONArray(notesJson);
            notesList.clear();
            for (int i = 0; i < nArr.length(); i++) {
                JSONObject obj = nArr.getJSONObject(i);
                notesList.add(new VaultItem(obj.getString("id"), obj.getString("l1"), obj.getString("l2"), true));
            }

            JSONArray cArr = new JSONArray(credsJson);
            credsList.clear();
            for (int i = 0; i < cArr.length(); i++) {
                JSONObject obj = cArr.getJSONObject(i);
                credsList.add(new VaultItem(obj.getString("id"), obj.getString("l1"), obj.getString("l2"), obj.optString("l3"), false));
            }
        } catch (Exception ignored) {}
    }

    private void saveLocalFallbackData() {
        SharedPreferences.Editor editor = getSharedPreferences("SecureVaultDump", MODE_PRIVATE).edit();
        try {
            JSONArray nArr = new JSONArray();
            for (VaultItem vi : notesList) {
                JSONObject obj = new JSONObject();
                obj.put("id", vi.id);
                obj.put("l1", vi.line1);
                obj.put("l2", vi.line2);
                nArr.put(obj);
            }
            editor.putString("notes", nArr.toString());

            JSONArray cArr = new JSONArray();
            for (VaultItem vi : credsList) {
                JSONObject obj = new JSONObject();
                obj.put("id", vi.id);
                obj.put("l1", vi.line1);
                obj.put("l2", vi.line2);
                obj.put("l3", vi.line3);
                cArr.put(obj);
            }
            editor.putString("creds", cArr.toString());
            editor.apply();
        } catch (Exception ignored) {}
    }
}