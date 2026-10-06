package com.strictstudymode.app;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends Activity {

    private Button btnStartTime;
    private Button btnEndTime;
    private CheckBox chkMon, chkTue, chkWed, chkThu, chkFri, chkSat, chkSun;
    private Button btnAddSchedule;
    private ListView lstSchedules;
    private TextView txtScheduleLockedMessage;

    private int startHour = 9, startMin = 0;
    private int endHour = 17, endMin = 0;

    private SharedPreferences prefs;
    private List<ScheduleItem> scheduleList;
    private ScheduleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        prefs = getSharedPreferences("StrictStudyPrefs", Context.MODE_PRIVATE);

        btnStartTime = (Button) findViewById(R.id.btnStartTime);
        btnEndTime = (Button) findViewById(R.id.btnEndTime);
        chkMon = (CheckBox) findViewById(R.id.chkMon);
        chkTue = (CheckBox) findViewById(R.id.chkTue);
        chkWed = (CheckBox) findViewById(R.id.chkWed);
        chkThu = (CheckBox) findViewById(R.id.chkThu);
        chkFri = (CheckBox) findViewById(R.id.chkFri);
        chkSat = (CheckBox) findViewById(R.id.chkSat);
        chkSun = (CheckBox) findViewById(R.id.chkSun);
        btnAddSchedule = (Button) findViewById(R.id.btnAddSchedule);
        lstSchedules = (ListView) findViewById(R.id.lstSchedules);
        txtScheduleLockedMessage = (TextView) findViewById(R.id.txtScheduleLockedMessage);

        scheduleList = new ArrayList<>();
        adapter = new ScheduleAdapter();
        lstSchedules.setAdapter(adapter);

        // Security Guard: Prevent additions/deletions when active strict mode
        boolean active = isStudyModeCurrentlyActive();
        boolean strict = prefs.getBoolean("strict_mode", false);
        if (active && strict) {
            txtScheduleLockedMessage.setVisibility(View.VISIBLE);
            btnAddSchedule.setEnabled(false);
            btnStartTime.setEnabled(false);
            btnEndTime.setEnabled(false);
        } else {
            txtScheduleLockedMessage.setVisibility(View.GONE);
        }

        btnStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(ScheduleActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        startHour = hourOfDay;
                        startMin = minute;
                        btnStartTime.setText(String.format("%02d:%02d", startHour, startMin));
                    }
                }, startHour, startMin, true).show();
            }
        });

        btnEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(ScheduleActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        endHour = hourOfDay;
                        endMin = minute;
                        btnEndTime.setText(String.format("%02d:%02d", endHour, endMin));
                    }
                }, endHour, endMin, true).show();
            }
        });

        btnAddSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewSchedule();
            }
        });

        loadSchedules();
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
                    int sHour = Integer.parseInt(parts[1]);
                    int sMin = Integer.parseInt(parts[2]);
                    int eHour = Integer.parseInt(parts[3]);
                    int eMin = Integer.parseInt(parts[4]);

                    int startMinutes = sHour * 60 + sMin;
                    int endMinutes = eHour * 60 + eMin;

                    if (schedDay == currentDay && currentMinutes >= startMinutes && currentMinutes <= endMinutes) {
                        schedActive = true;
                        break;
                    }
                }
            }
        }
        return manualActive || schedActive;
    }

    private void saveNewSchedule() {
        List<Integer> selectedDays = new ArrayList<>();
        if (chkSun.isChecked()) selectedDays.add(1);
        if (chkMon.isChecked()) selectedDays.add(2);
        if (chkTue.isChecked()) selectedDays.add(3);
        if (chkWed.isChecked()) selectedDays.add(4);
        if (chkThu.isChecked()) selectedDays.add(5);
        if (chkFri.isChecked()) selectedDays.add(6);
        if (chkSat.isChecked()) selectedDays.add(7);

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one repeat day!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time
        if ((startHour * 60 + startMin) >= (endHour * 60 + endMin)) {
            Toast.makeText(this, "End Time must be chronologically after Start Time!", Toast.LENGTH_LONG).show();
            return;
        }

        String existingList = prefs.getString("schedules_list", "");
        String[] ids = existingList.split(",");
        int maxId = 0;
        for (String id : ids) {
            if (!id.trim().isEmpty()) {
                try {
                    int val = Integer.parseInt(id.trim());
                    if (val > maxId) maxId = val;
                } catch (Exception e) {}
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder idListBuilder = new StringBuilder(existingList);

        for (int day : selectedDays) {
            maxId++;
            // Format: DAY|START_HOUR|START_MINUTE|END_HOUR|END_MINUTE
            String infoString = day + "|" + startHour + "|" + startMin + "|" + endHour + "|" + endMin;
            editor.putString("schedule_info_" + maxId, infoString);
            if (idListBuilder.length() > 0 && idListBuilder.charAt(idListBuilder.length() - 1) != ',') {
                idListBuilder.append(",");
            }
            idListBuilder.append(maxId);
        }

        editor.putString("schedules_list", idListBuilder.toString());
        editor.apply();

        Toast.makeText(this, "Focus hours added successfully!", Toast.LENGTH_SHORT).show();

        // Reset check boxes
        chkMon.setChecked(false);
        chkTue.setChecked(false);
        chkWed.setChecked(false);
        chkThu.setChecked(false);
        chkFri.setChecked(false);
        chkSat.setChecked(false);
        chkSun.setChecked(false);

        loadSchedules();
    }

    private void loadSchedules() {
        scheduleList.clear();
        String list = prefs.getString("schedules_list", "");
        if (list.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        String[] ids = list.split(",");
        for (String id : ids) {
            if (id.trim().isEmpty()) continue;
            String info = prefs.getString("schedule_info_" + id, "");
            if (info.isEmpty()) continue;

            String[] parts = info.split("\\|");
            if (parts.length >= 5) {
                int day = Integer.parseInt(parts[0]);
                int sHour = Integer.parseInt(parts[1]);
                int sMin = Integer.parseInt(parts[2]);
                int eHour = Integer.parseInt(parts[3]);
                int eMin = Integer.parseInt(parts[4]);

                scheduleList.add(new ScheduleItem(Integer.parseInt(id), day, sHour, sMin, eHour, eMin));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteSchedule(int id) {
        if (isStudyModeCurrentlyActive() && prefs.getBoolean("strict_mode", false)) {
            Toast.makeText(this, "Locked! Cannot delete schedules during a strict session.", Toast.LENGTH_SHORT).show();
            return;
        }

        String list = prefs.getString("schedules_list", "");
        String[] ids = list.split(",");
        StringBuilder sb = new StringBuilder();
        for (String currentId : ids) {
            if (!currentId.trim().isEmpty() && Integer.parseInt(currentId.trim()) != id) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(currentId.trim());
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("schedules_list", sb.toString());
        editor.remove("schedule_info_" + id);
        editor.apply();

        Toast.makeText(this, "Timetable removed.", Toast.LENGTH_SHORT).show();
        loadSchedules();
    }

    private static class ScheduleItem {
        int id;
        int day;
        int startHour;
        int startMin;
        int endHour;
        int endMin;

        ScheduleItem(int id, int day, int startHour, int startMin, int endHour, int endMin) {
            this.id = id;
            this.day = day;
            this.startHour = startHour;
            this.startMin = startMin;
            this.endHour = endHour;
            this.endMin = endMin;
        }

        String getDayName() {
            switch (day) {
                case 1: return "Sunday";
                case 2: return "Monday";
                case 3: return "Tuesday";
                case 4: return "Wednesday";
                case 5: return "Thursday";
                case 6: return "Friday";
                case 7: return "Saturday";
                default: return "Unknown";
            }
        }
    }

    private class ScheduleAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return scheduleList.size();
        }

        @Override
        public Object getItem(int position) {
            return scheduleList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ScheduleActivity.this).inflate(R.layout.item_schedule, parent, false);
            }

            TextView txtTimeRange = (TextView) convertView.findViewById(R.id.txtTimeRange);
            TextView txtDays = (TextView) convertView.findViewById(R.id.txtDays);
            Button btnDelete = (Button) convertView.findViewById(R.id.btnDelete);

            final ScheduleItem item = scheduleList.get(position);
            txtTimeRange.setText(String.format("%02d:%02d - %02d:%02d", item.startHour, item.startMin, item.endHour, item.endMin));
            txtDays.setText("Every " + item.getDayName());

            boolean isStrictStudyActive = isStudyModeCurrentlyActive() && prefs.getBoolean("strict_mode", false);
            if (isStrictStudyActive) {
                btnDelete.setEnabled(false);
                btnDelete.setAlpha(0.5f);
            } else {
                btnDelete.setEnabled(true);
                btnDelete.setAlpha(1.0f);
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteSchedule(item.id);
                }
            });

            return convertView;
        }
    }
}