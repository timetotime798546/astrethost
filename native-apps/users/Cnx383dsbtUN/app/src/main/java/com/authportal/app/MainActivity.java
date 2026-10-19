package com.authportal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    public static class RecordItem {
        public String id;
        public String title;
        public String content;
        public String category;
        public int priority;
        public boolean completed;

        public RecordItem(String id, String title, String content, String category, int priority, boolean completed) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.category = category;
            this.priority = priority;
            this.completed = completed;
        }
    }

    private BackendApi backendApi;
    private final List<RecordItem> recordList = new ArrayList<RecordItem>();

    private TextView tvAdminBanner;
    private TextView tvUserWelcome;
    private TextView tvTotalRecords;
    private TextView tvCompletionPercent;
    private TextView tvHighPriorityRatio;
    private TextView tvSummaryBreakdown;
    private LinearLayout llRecordsContainer;
    private ProgressBar pbLoadingRecords;
    private TextView tvEmptyState;
    private Button btnAddNewRecord;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backendApi = new BackendApi(this);

        if (!backendApi.isLoggedIn()) {
            redirectToAuth();
            return;
        }

        setContentView(R.layout.activity_main);

        tvAdminBanner = (TextView) findViewById(R.id.tvAdminBanner);
        tvUserWelcome = (TextView) findViewById(R.id.tvUserWelcome);
        tvTotalRecords = (TextView) findViewById(R.id.tvTotalRecords);
        tvCompletionPercent = (TextView) findViewById(R.id.tvCompletionPercent);
        tvHighPriorityRatio = (TextView) findViewById(R.id.tvHighPriorityRatio);
        tvSummaryBreakdown = (TextView) findViewById(R.id.tvSummaryBreakdown);
        llRecordsContainer = (LinearLayout) findViewById(R.id.llRecordsContainer);
        pbLoadingRecords = (ProgressBar) findViewById(R.id.pbLoadingRecords);
        tvEmptyState = (TextView) findViewById(R.id.tvEmptyState);
        btnAddNewRecord = (Button) findViewById(R.id.btnAddNewRecord);
        btnLogout = (Button) findViewById(R.id.btnLogout);

        tvUserWelcome.setText("Logged in as: " + backendApi.getLoggedEmail());

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        btnAddNewRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRecordDialog();
            }
        });

        loadAdminBannerSettings();
        fetchUserRecords();
    }

    private void redirectToAuth() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    private void performLogout() {
        backendApi.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                redirectToAuth();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Session closed", Toast.LENGTH_SHORT).show();
                redirectToAuth();
            }
        });
    }

    private void loadAdminBannerSettings() {
        backendApi.fetchAdminConfig(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONObject settings = response.optJSONObject("settings");
                if (settings != null) {
                    String banner = settings.optString("portal_banner_text", "Welcome to AuthPortal Secure Workspace");
                    tvAdminBanner.setText(banner);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Silently fallback to default layout text
            }
        });
    }

    private void fetchUserRecords() {
        pbLoadingRecords.setVisibility(View.VISIBLE);
        backendApi.readRecords("user_records", new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                pbLoadingRecords.setVisibility(View.GONE);
                recordList.clear();

                // Contract: READ returns root "success": true and "records": [ ... ]
                JSONArray recordsArray = response.optJSONArray("records");
                if (recordsArray != null) {
                    for (int i = 0; i < recordsArray.length(); i++) {
                        JSONObject recordObj = recordsArray.optJSONObject(i);
                        if (recordObj != null) {
                            String id = recordObj.optString("id", "");
                            JSONObject data = recordObj.optJSONObject("data");
                            if (data != null) {
                                String title = data.optString("title", "Untitled");
                                String content = data.optString("content", "");
                                String category = data.optString("category", "General");
                                int priority = data.optInt("priority_level", 1);
                                boolean completed = data.optBoolean("completed", false);

                                recordList.add(new RecordItem(id, title, content, category, priority, completed));
                            }
                        }
                    }
                }

                renderRecords();
                calculateAndDisplayMetrics();
            }

            @Override
            public void onError(String errorMessage) {
                pbLoadingRecords.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Sync error: " + errorMessage, Toast.LENGTH_SHORT).show();
                calculateAndDisplayMetrics();
            }
        });
    }

    // MANDATORY LOCAL OFFLINE CALCULATIONS
    // Formula 1: completion_rate = (completed_count * 100) / total_count
    // Formula 2: high_priority_ratio = (high_priority_count * 100) / total_count
    private void calculateAndDisplayMetrics() {
        int total = recordList.size();
        int completedCount = 0;
        int highPriorityCount = 0;

        for (int i = 0; i < total; i++) {
            RecordItem item = recordList.get(i);
            if (item.completed) {
                completedCount++;
            }
            if (item.priority >= 3) {
                highPriorityCount++;
            }
        }

        int pendingCount = total - completedCount;
        int completionPercentage = 0;
        int highPriorityPercentage = 0;

        if (total > 0) {
            completionPercentage = (completedCount * 100) / total;
            highPriorityPercentage = (highPriorityCount * 100) / total;
        }

        tvTotalRecords.setText(String.valueOf(total));
        tvCompletionPercent.setText(completionPercentage + "%");
        tvHighPriorityRatio.setText(highPriorityPercentage + "%");

        String breakdown = "Pending: " + pendingCount + " | Completed: " + completedCount + " | High Priority: " + highPriorityCount;
        tvSummaryBreakdown.setText(breakdown);
    }

    private void renderRecords() {
        llRecordsContainer.removeAllViews();

        if (recordList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvEmptyState.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < recordList.size(); i++) {
            final RecordItem item = recordList.get(i);
            View itemView = inflater.inflate(R.layout.item_note_card, llRecordsContainer, false);

            TextView tvCardTitle = (TextView) itemView.findViewById(R.id.tvCardTitle);
            TextView tvCardBadge = (TextView) itemView.findViewById(R.id.tvCardBadge);
            TextView tvCardCategory = (TextView) itemView.findViewById(R.id.tvCardCategory);
            TextView tvCardContent = (TextView) itemView.findViewById(R.id.tvCardContent);
            TextView tvCardStatus = (TextView) itemView.findViewById(R.id.tvCardStatus);
            Button btnToggleComplete = (Button) itemView.findViewById(R.id.btnToggleComplete);
            Button btnDeleteCard = (Button) itemView.findViewById(R.id.btnDeleteCard);

            tvCardTitle.setText(item.title);
            tvCardBadge.setText("Priority " + item.priority);
            tvCardCategory.setText("Category: " + item.category);
            tvCardContent.setText(item.content);

            if (item.completed) {
                tvCardStatus.setText("Status: Completed");
                tvCardStatus.setTextColor(getResources().getColor(R.color.success));
                btnToggleComplete.setText("Mark Pending");
            } else {
                tvCardStatus.setText("Status: Pending");
                tvCardStatus.setTextColor(getResources().getColor(R.color.warning));
                btnToggleComplete.setText("Mark Done");
            }

            btnToggleComplete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleRecordStatus(item);
                }
            });

            btnDeleteCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteRecord(item);
                }
            });

            llRecordsContainer.addView(itemView);
        }
    }

    private void toggleRecordStatus(final RecordItem item) {
        try {
            JSONObject data = new JSONObject();
            data.put("title", item.title);
            data.put("content", item.content);
            data.put("category", item.category);
            data.put("priority_level", item.priority);
            data.put("completed", !item.completed);

            backendApi.updateRecord(item.id, data, new BackendApi.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    item.completed = !item.completed;
                    renderRecords();
                    calculateAndDisplayMetrics();
                    Toast.makeText(MainActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(MainActivity.this, "Update failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecord(final RecordItem item) {
        backendApi.deleteRecord(item.id, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                recordList.remove(item);
                renderRecords();
                calculateAndDisplayMetrics();
                Toast.makeText(MainActivity.this, "Record deleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Delete failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddRecordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_item, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        final EditText etItemTitle = (EditText) dialogView.findViewById(R.id.etItemTitle);
        final EditText etItemCategory = (EditText) dialogView.findViewById(R.id.etItemCategory);
        final EditText etItemContent = (EditText) dialogView.findViewById(R.id.etItemContent);
        final EditText etItemPriority = (EditText) dialogView.findViewById(R.id.etItemPriority);
        final CheckBox cbItemCompleted = (CheckBox) dialogView.findViewById(R.id.cbItemCompleted);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btnCancelDialog);
        Button btnSave = (Button) dialogView.findViewById(R.id.btnSaveRecord);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etItemTitle.getText().toString().trim();
                String category = etItemCategory.getText().toString().trim();
                String content = etItemContent.getText().toString().trim();
                String priorityStr = etItemPriority.getText().toString().trim();
                boolean completed = cbItemCompleted.isChecked();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Title is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (category.isEmpty()) {
                    category = "General";
                }

                int priority = 1;
                try {
                    priority = Integer.parseInt(priorityStr);
                } catch (Exception ignored) {
                }

                try {
                    JSONObject data = new JSONObject();
                    data.put("title", title);
                    data.put("category", category);
                    data.put("content", content);
                    data.put("priority_level", priority);
                    data.put("completed", completed);

                    final String finalTitle = title;
                    final String finalCategory = category;
                    final String finalContent = content;
                    final int finalPriority = priority;
                    final boolean finalCompleted = completed;

                    backendApi.createRecord("user_records", data, new BackendApi.ApiCallback() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            // Contract: CREATE response has root "success": true and "record" object
                            JSONObject recordObj = response.optJSONObject("record");
                            String newId = (recordObj != null) ? recordObj.optString("id", String.valueOf(System.currentTimeMillis())) : String.valueOf(System.currentTimeMillis());

                            RecordItem newItem = new RecordItem(newId, finalTitle, finalContent, finalCategory, finalPriority, finalCompleted);
                            recordList.add(0, newItem);

                            renderRecords();
                            calculateAndDisplayMetrics();
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Record created successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(MainActivity.this, "Failed to save record: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }
}