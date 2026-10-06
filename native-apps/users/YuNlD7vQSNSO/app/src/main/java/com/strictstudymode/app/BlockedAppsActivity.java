package com.strictstudymode.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockedAppsActivity extends Activity {

    private EditText edtSearch;
    private ListView lstApps;
    private TextView txtAppsLockedMessage;
    private AppAdapter adapter;
    private List<AppModel> allAppsList;
    private List<AppModel> filteredList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_apps);

        prefs = getSharedPreferences("StrictStudyPrefs", Context.MODE_PRIVATE);
        edtSearch = (EditText) findViewById(R.id.edtSearch);
        lstApps = (ListView) findViewById(R.id.lstApps);
        txtAppsLockedMessage = (TextView) findViewById(R.id.txtAppsLockedMessage);

        allAppsList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Prevent manipulation during strict sessions
        boolean active = isStudyModeCurrentlyActive();
        boolean strict = prefs.getBoolean("strict_mode", false);
        if (active && strict) {
            txtAppsLockedMessage.setVisibility(View.VISIBLE);
            edtSearch.setEnabled(false);
            lstApps.setEnabled(false);
        } else {
            txtAppsLockedMessage.setVisibility(View.GONE);
        }

        loadInstalledLauncherApps();

        adapter = new AppAdapter();
        lstApps.setAdapter(adapter);

        lstApps.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isStudyModeCurrentlyActive() && prefs.getBoolean("strict_mode", false)) {
                    Toast.makeText(BlockedAppsActivity.this, "Locked! Cannot edit settings during study session.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AppModel app = filteredList.get(position);
                app.isBlocked = !app.isBlocked;
                saveAppBlockState(app.packageName, app.isBlocked);
                adapter.notifyDataSetChanged();
            }
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean isStudyModeCurrentlyActive() {
        long end = prefs.getLong("manual_session_end", 0);
        boolean manualActive = System.currentTimeMillis() < end;

        // Check schedules
        boolean schedActive = false;
        String list = prefs.getString("schedules_list", "");
        if (!list.isEmpty()) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentDay = cal.get(java.util.Calendar.DAY_OF_WEEK);
            int currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);

            String[] ids = list.split(",");
            for (String id : ids) {
                if (id.trim().isEmpty()) continue;
                String info = prefs.getString("schedule_info_" + id, "");
                if (info.isEmpty()) continue;

                String[] parts = info.split("\\|");
                if (parts.length >= 5) {
                    int schedDay = Integer.parseInt(parts[0]);
                    int startHour = Integer.parseInt(parts[1]);
                    int startMin = Integer.parseInt(parts[2]);
                    int endHour = Integer.parseInt(parts[3]);
                    int endMin = Integer.parseInt(parts[4]);

                    int startMinutes = startHour * 60 + startMin;
                    int endMinutes = endHour * 60 + endMin;

                    if (schedDay == currentDay && currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                        schedActive = true;
                        break;
                    }
                }
            }
        }
        return manualActive || schedActive;
    }

    private void loadInstalledLauncherApps() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Set<String> blockedPackages = getBlockedPackages(this);

        for (ResolveInfo ri : resolveInfos) {
            if (ri.activityInfo != null) {
                String pkg = ri.activityInfo.packageName;
                // Skip our own app so they can't lock themselves out of the management interface completely
                if (pkg.equals(getPackageName())) {
                    continue;
                }

                String label = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                boolean isBlocked = blockedPackages.contains(pkg);

                // Filter out duplicate entries if any
                boolean exists = false;
                for (AppModel m : allAppsList) {
                    if (m.packageName.equals(pkg)) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    allAppsList.add(new AppModel(pkg, label, icon, isBlocked));
                }
            }
        }

        filteredList.addAll(allAppsList);
    }

    private void filterApps(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allAppsList);
        } else {
            String lower = query.toLowerCase();
            for (AppModel app : allAppsList) {
                if (app.appName.toLowerCase().contains(lower) || app.packageName.toLowerCase().contains(lower)) {
                    filteredList.add(app);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    public static Set<String> getBlockedPackages(Context context) {
        SharedPreferences shared = context.getSharedPreferences("StrictStudyPrefs", Context.MODE_PRIVATE);
        String saved = shared.getString("blocked_packages_list", "");
        Set<String> set = new HashSet<>();
        if (!saved.isEmpty()) {
            String[] arr = saved.split(",");
            for (String s : arr) {
                if (!s.trim().isEmpty()) {
                    set.add(s.trim());
                }
            }
        }
        return set;
    }

    private void saveAppBlockState(String packageName, boolean isBlocked) {
        Set<String> blocked = getBlockedPackages(this);
        if (isBlocked) {
            blocked.add(packageName);
        } else {
            blocked.remove(packageName);
        }

        // Serialize
        StringBuilder sb = new StringBuilder();
        for (String pkg : blocked) {
            sb.append(pkg).append(",");
        }
        prefs.edit().putString("blocked_packages_list", sb.toString()).apply();
    }

    private static class AppModel {
        String packageName;
        String appName;
        Drawable icon;
        boolean isBlocked;

        AppModel(String packageName, String appName, Drawable icon, boolean isBlocked) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
            this.isBlocked = isBlocked;
        }
    }

    private class AppAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(BlockedAppsActivity.this).inflate(R.layout.item_app, parent, false);
            }

            ImageView imgIcon = (ImageView) convertView.findViewById(R.id.imgAppIcon);
            TextView txtName = (TextView) convertView.findViewById(R.id.txtAppName);
            TextView txtPackage = (TextView) convertView.findViewById(R.id.txtAppPackage);
            CheckBox chk = (CheckBox) convertView.findViewById(R.id.chkBlocked);

            AppModel app = filteredList.get(position);
            imgIcon.setImageDrawable(app.icon);
            txtName.setText(app.appName);
            txtPackage.setText(app.packageName);
            chk.setChecked(app.isBlocked);

            return convertView;
        }
    }
}