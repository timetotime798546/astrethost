package com.taskflow.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;
    private int loggedUserId;

    // Top containers for views switching
    private ScrollViewHelper layoutHome;
    private LinearLayout layoutExplore;
    private LinearLayout layoutFavorites;
    private LinearLayout layoutHistory;
    private ScrollViewHelper layoutProfile;

    // Layouts references directly matching main XML
    private View tabHome, tabExplore, tabFavorites, tabHistory, tabProfile;

    // Bottom Tabs Navigation elements
    private LinearLayout navHome, navExplore, navFavorites, navHistory, navProfile;

    // List views
    private ListView lvHomeTasks, lvExploreTasks, lvFavoritesTasks, lvHistoryLogs;
    private LinearLayout llHomeEmpty, llExploreEmpty, llFavoritesEmpty, llHistoryEmpty;

    // Stats indicators
    private TextView tvStatsPending, tvStatsCompleted, tvStatsFavorites;
    private TextView tvGreeting;
    private EditText etSearchInput;

    private TaskAdapter homeAdapter, exploreAdapter, favoritesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        SharedPreferences prefs = getSharedPreferences("taskflow_session", MODE_PRIVATE);
        loggedUserId = prefs.getInt("logged_user_id", -1);

        if (loggedUserId == -1) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        initViewElements();
        setupNavigation();
        reloadAllData();
    }

    private void initViewElements() {
        tabHome = findViewById(R.id.tab_home);
        tabExplore = findViewById(R.id.tab_explore);
        tabFavorites = findViewById(R.id.tab_favorites);
        tabHistory = findViewById(R.id.tab_history);
        tabProfile = findViewById(R.id.tab_profile);

        navHome = (LinearLayout) findViewById(R.id.nav_home);
        navExplore = (LinearLayout) findViewById(R.id.nav_explore);
        navFavorites = (LinearLayout) findViewById(R.id.nav_favorites);
        navHistory = (LinearLayout) findViewById(R.id.nav_history);
        navProfile = (LinearLayout) findViewById(R.id.nav_profile);

        lvHomeTasks = (ListView) findViewById(R.id.lvHomeTasks);
        lvExploreTasks = (ListView) findViewById(R.id.lvExploreTasks);
        lvFavoritesTasks = (ListView) findViewById(R.id.lvFavoritesTasks);
        lvHistoryLogs = (ListView) findViewById(R.id.lvHistoryLogs);

        llHomeEmpty = (LinearLayout) findViewById(R.id.llHomeEmptyState);
        llExploreEmpty = (LinearLayout) findViewById(R.id.llExploreEmptyState);
        llFavoritesEmpty = (LinearLayout) findViewById(R.id.llFavoritesEmptyState);
        llHistoryEmpty = (LinearLayout) findViewById(R.id.llHistoryEmptyState);

        tvStatsPending = (TextView) findViewById(R.id.tvStatsPending);
        tvStatsCompleted = (TextView) findViewById(R.id.tvStatsCompleted);
        tvStatsFavorites = (TextView) findViewById(R.id.tvStatsFavorites);
        tvGreeting = (TextView) findViewById(R.id.tvHomeGreeting);

        etSearchInput = (EditText) findViewById(R.id.etExploreSearch);
        Button btnClearSearch = (Button) findViewById(R.id.btnExploreClearSearch);

        DatabaseHelper.User userObj = dbHelper.getUserProfile(loggedUserId);
        if (userObj != null) {
            tvGreeting.setText("Hello, " + userObj.username + "!");
            TextView tvProfUser = (TextView) findViewById(R.id.tvProfileUsername);
            TextView tvProfEmail = (TextView) findViewById(R.id.tvProfileEmail);
            TextView tvProfPhone = (TextView) findViewById(R.id.tvProfilePhone);
            if (tvProfUser != null) tvProfUser.setText("@" + userObj.username);
            if (tvProfEmail != null) tvProfEmail.setText(userObj.email);
            if (tvProfPhone != null) tvProfPhone.setText(userObj.phone);
        }

        // Set Shortcut click to access preferences settings directly
        findViewById(R.id.btnHomeSettingsShortcut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });

        findViewById(R.id.btnProfileManagePrefs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
            }
        });

        // Add quick add click event handler
        findViewById(R.id.btnHomeAddTask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent detail = new Intent(MainActivity.this, TaskDetailActivity.class);
                startActivity(detail);
            }
        });

        // Search change handler trigger
        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (exploreAdapter != null) {
                    exploreAdapter.performSearch(s.toString());
                    if (exploreAdapter.getCount() == 0) {
                        llExploreEmpty.setVisibility(View.VISIBLE);
                    } else {
                        llExploreEmpty.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchInput.setText("");
            }
        });

        // Clear local history helper triggers
        findViewById(R.id.btnHistoryClearAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirm Action")
                    .setMessage("Do you want to permanently clear your task history trace log?")
                    .setPositiveButton("Clear History", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dbHelper.clearHistory(loggedUserId);
                            Toast.makeText(MainActivity.this, "Log clean!", Toast.LENGTH_SHORT).show();
                            reloadAllData();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            }
        });

        // Set Profile Sign out event trigger
        findViewById(R.id.btnProfileLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = getSharedPreferences("taskflow_session", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(MainActivity.this, "Signed out successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Register item item editing redirection
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
                intent.putExtra("task_id", (int) id);
                startActivity(intent);
            }
        };

        lvHomeTasks.setOnItemClickListener(listener);
        lvExploreTasks.setOnItemClickListener(listener);
        lvFavoritesTasks.setOnItemClickListener(listener);
    }

    private void setupNavigation() {
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTabsColors();
                tabHome.setVisibility(View.GONE);
                tabExplore.setVisibility(View.GONE);
                tabFavorites.setVisibility(View.GONE);
                tabHistory.setVisibility(View.GONE);
                tabProfile.setVisibility(View.GONE);

                int id = v.getId();
                if (id == R.id.nav_home) {
                    tabHome.setVisibility(View.VISIBLE);
                    setTabActiveText(navHome);
                } else if (id == R.id.nav_explore) {
                    tabExplore.setVisibility(View.VISIBLE);
                    setTabActiveText(navExplore);
                } else if (id == R.id.nav_favorites) {
                    tabFavorites.setVisibility(View.VISIBLE);
                    setTabActiveText(navFavorites);
                } else if (id == R.id.nav_history) {
                    tabHistory.setVisibility(View.VISIBLE);
                    setTabActiveText(navHistory);
                } else if (id == R.id.nav_profile) {
                    tabProfile.setVisibility(View.VISIBLE);
                    setTabActiveText(navProfile);
                }
                reloadAllData();
            }
        };

        navHome.setOnClickListener(click);
        navExplore.setOnClickListener(click);
        navFavorites.setOnClickListener(click);
        navHistory.setOnClickListener(click);
        navProfile.setOnClickListener(click);
    }

    private void setTabActiveText(LinearLayout navLay) {
        TextView label = (TextView) navLay.getChildAt(1);
        label.setTextColor(Color.parseColor("#2196F3"));
    }

    private void resetTabsColors() {
        LinearLayout[] tabs = {navHome, navExplore, navFavorites, navHistory, navProfile};
        for (LinearLayout tab : tabs) {
            TextView label = (TextView) tab.getChildAt(1);
            label.setTextColor(Color.parseColor("#6D7A8A"));
        }
    }

    private void reloadAllData() {
        List<DatabaseHelper.TaskItem> allTasks = new ArrayList<>();
        List<DatabaseHelper.TaskItem> favoritedTasks = new ArrayList<>();
        int countCompleted = 0;
        int countPending = 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ITEMS, null, DatabaseHelper.ITEM_COL_USER_ID + "=?",
                new String[]{String.valueOf(loggedUserId)}, null, null, DatabaseHelper.ITEM_COL_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                DatabaseHelper.TaskItem item = new DatabaseHelper.TaskItem();
                item.id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_ID));
                item.userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_USER_ID));
                item.title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_TITLE));
                item.description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DESC));
                item.category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_CATEGORY));
                item.dueDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_DUE_DATE));
                item.isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_COMPLETED)) == 1;
                item.isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.ITEM_COL_FAVORITE)) == 1;

                allTasks.add(item);
                if (item.isFavorite) {
                    favoritedTasks.add(item);
                }
                if (item.isCompleted) {
                    countCompleted++;
                } else {
                    countPending++;
                }
            }
            cursor.close();
        }

        // Update static dashboard statistics counts
        tvStatsPending.setText(String.valueOf(countPending));
        tvStatsCompleted.setText(String.valueOf(countCompleted));
        tvStatsFavorites.setText(String.valueOf(favoritedTasks.size()));

        Runnable refresh = new Runnable() {
            @Override
            public void run() {
                reloadAllData();
            }
        };

        // Set Custom adapter views
        homeAdapter = new TaskAdapter(this, allTasks, loggedUserId, refresh);
        lvHomeTasks.setAdapter(homeAdapter);
        if (allTasks.isEmpty()) {
            llHomeEmpty.setVisibility(View.VISIBLE);
            lvHomeTasks.setVisibility(View.GONE);
        } else {
            llHomeEmpty.setVisibility(View.GONE);
            lvHomeTasks.setVisibility(View.VISIBLE);
        }

        exploreAdapter = new TaskAdapter(this, allTasks, loggedUserId, refresh);
        lvExploreTasks.setAdapter(exploreAdapter);
        exploreAdapter.performSearch(etSearchInput.getText().toString());

        favoritesAdapter = new TaskAdapter(this, favoritedTasks, loggedUserId, refresh);
        lvFavoritesTasks.setAdapter(favoritesAdapter);
        if (favoritedTasks.isEmpty()) {
            llFavoritesEmpty.setVisibility(View.VISIBLE);
            lvFavoritesTasks.setVisibility(View.GONE);
        } else {
            llFavoritesEmpty.setVisibility(View.GONE);
            lvFavoritesTasks.setVisibility(View.VISIBLE);
        }

        // History elements load
        List<DatabaseHelper.HistoryLog> histList = dbHelper.fetchHistory(loggedUserId);
        HistoryAdapter histAdapter = new HistoryAdapter(this, histList);
        lvHistoryLogs.setAdapter(histAdapter);
        if (histList.isEmpty()) {
            llHistoryEmpty.setVisibility(View.VISIBLE);
            lvHistoryLogs.setVisibility(View.GONE);
        } else {
            llHistoryEmpty.setVisibility(View.GONE);
            lvHistoryLogs.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAllData();
    }

    // Standard internal class nested inside activity mapping user history activities
    private static class HistoryAdapter extends BaseAdapter {
        private Context context;
        private List<DatabaseHelper.HistoryLog> logs;

        public HistoryAdapter(Context ctx, List<DatabaseHelper.HistoryLog> logs) {
            this.context = ctx;
            this.logs = logs;
        }

        @Override
        public int getCount() {
            return logs.size();
        }

        @Override
        public Object getItem(int pos) {
            return logs.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return logs.get(pos).id;
        }

        @Override
        public View getView(int pos, View convert, ViewGroup parent) {
            if (convert == null) {
                convert = LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
            }
            DatabaseHelper.HistoryLog log = logs.get(pos);
            TextView action = (TextView) convert.findViewById(R.id.tvHistoryAction);
            TextView detail = (TextView) convert.findViewById(R.id.tvHistoryDetail);
            TextView dateStr = (TextView) convert.findViewById(R.id.tvHistoryTimestamp);

            action.setText(log.action);
            detail.setText(log.detail);
            dateStr.setText(log.timestamp);

            return convert;
        }
    }
}