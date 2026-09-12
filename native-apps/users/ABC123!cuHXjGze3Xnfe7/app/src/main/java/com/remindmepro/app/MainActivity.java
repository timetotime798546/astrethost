package com.remindmepro.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private ListView listView;
    private View emptyView;
    private Button btnQuickAdd;
    private View loaderOverlay;
    private ReminderDatabaseHelper dbHelper;
    private ReminderAdapter adapter;
    private List<Reminder> reminderList;

    private Calendar selectedDateTime;

    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ReminderDatabaseHelper(this);
        listView = (ListView) findViewById(R.id.reminder_list_view);
        emptyView = findViewById(R.id.empty_view);
        btnQuickAdd = (Button) findViewById(R.id.btn_quick_add);
        loaderOverlay = findViewById(R.id.loader_overlay);

        btnQuickAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundClick();
                
                // Programmatic button click bounce animation
                btnQuickAdd.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(80)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                btnQuickAdd.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(80)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                showAddReminderDialog();
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, new IntentFilter("com.remindmepro.app.REFRESH_DATA"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(refreshReceiver, new IntentFilter("com.remindmepro.app.REFRESH_DATA"));
        }

        requestNotificationPermission();
        refreshData();

        // Play welcome startup chime sound
        playSoundStartup();

        // Animate and hide the loader screen overlay after a short delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loaderOverlay != null) {
                    loaderOverlay.animate()
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    loaderOverlay.setVisibility(View.GONE);
                                }
                            });
                }
            }
        }, 1800);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(refreshReceiver);
        } catch (Exception e) {
            // Ignored
        }
    }

    // Helper methods for generating premium Tone feedback
    private void playSoundStartup() {
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_SYSTEM, 85);
            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSoundSuccess() {
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_SYSTEM, 100);
            toneG.startTone(android.media.ToneGenerator.TONE_SUP_CONFIRM, 250);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSoundDelete() {
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_SYSTEM, 90);
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSoundClick() {
        try {
            android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_SYSTEM, 80);
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }
    }

    private void refreshData() {
        reminderList = dbHelper.getAllReminders();
        if (reminderList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            startEmptyClockAnimation();
        } else {
            emptyView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            stopEmptyClockAnimation();
        }
        
        adapter = new ReminderAdapter(this, reminderList);
        listView.setAdapter(adapter);
    }

    private void startEmptyClockAnimation() {
        View tvEmptyClock = findViewById(R.id.tv_empty_clock);
        if (tvEmptyClock != null) {
            tvEmptyClock.clearAnimation();
            ScaleAnimation pulse = new ScaleAnimation(
                    1.0f, 1.2f,
                    1.0f, 1.2f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulse.setDuration(1200);
            pulse.setRepeatCount(Animation.INFINITE);
            pulse.setRepeatMode(Animation.REVERSE);
            tvEmptyClock.startAnimation(pulse);
        }
    }

    private void stopEmptyClockAnimation() {
        View tvEmptyClock = findViewById(R.id.tv_empty_clock);
        if (tvEmptyClock != null) {
            tvEmptyClock.clearAnimation();
        }
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_reminder, null);
        builder.setView(dialogView);

        final EditText etTitle = (EditText) dialogView.findViewById(R.id.et_title);
        final EditText etDesc = (EditText) dialogView.findViewById(R.id.et_desc);
        final TextView tvDateLabel = (TextView) dialogView.findViewById(R.id.tv_date_label);
        final TextView tvTimeLabel = (TextView) dialogView.findViewById(R.id.tv_time_label);
        Button btnPickDate = (Button) dialogView.findViewById(R.id.btn_pick_date);
        Button btnPickTime = (Button) dialogView.findViewById(R.id.btn_pick_time);
        Button btnCancel = (Button) dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = (Button) dialogView.findViewById(R.id.btn_save);

        selectedDateTime = Calendar.getInstance();
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        final AlertDialog dialog = builder.create();

        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundClick();
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                selectedDateTime.set(Calendar.YEAR, year);
                                selectedDateTime.set(Calendar.MONTH, month);
                                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                                tvDateLabel.setText("Date: " + df.format(selectedDateTime.getTime()));
                                playSoundClick();
                            }
                        },
                        selectedDateTime.get(Calendar.YEAR),
                        selectedDateTime.get(Calendar.MONTH),
                        selectedDateTime.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });

        btnPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundClick();
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                                tvTimeLabel.setText("Time: " + tf.format(selectedDateTime.getTime()));
                                playSoundClick();
                            }
                        },
                        selectedDateTime.get(Calendar.HOUR_OF_DAY),
                        selectedDateTime.get(Calendar.MINUTE),
                        DateFormat.is24HourFormat(MainActivity.this));
                timePickerDialog.show();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSoundClick();
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString().trim();
                String desc = etDesc.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a title", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(MainActivity.this, "Reminder must be in the future!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Reminder reminder = new Reminder(title, desc, selectedDateTime.getTimeInMillis(), true);
                long id = dbHelper.addReminder(reminder);
                reminder.setId(id);

                scheduleAlarm(reminder);

                playSoundSuccess();
                Toast.makeText(MainActivity.this, "Reminder Created!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                refreshData();
            }
        });

        dialog.show();
    }

    private void scheduleAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("id", reminder.getId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("desc", reminder.getDescription());

        int pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) reminder.getId(), intent, pendingIntentFlag);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.getTimeMillis(), pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.getTimeMillis(), pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getTimeMillis(), pendingIntent);
            }
        } catch (Exception e) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getTimeMillis(), pendingIntent);
        }
    }

    private void cancelAlarm(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ReminderReceiver.class);
        int pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) reminder.getId(), intent, pendingIntentFlag);
        alarmManager.cancel(pendingIntent);
    }

    private class ReminderAdapter extends BaseAdapter {
        private Context context;
        private List<Reminder> list;
        private int lastPosition = -1;

        public ReminderAdapter(Context context, List<Reminder> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return list.get(position).getId();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.reminder_item, parent, false);
            }

            final Reminder reminder = list.get(position);

            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tv_item_desc);
            TextView tvDateTime = (TextView) convertView.findViewById(R.id.tv_item_datetime);
            TextView tvStatus = (TextView) convertView.findViewById(R.id.tv_item_status);
            Button btnDelete = (Button) convertView.findViewById(R.id.btn_item_delete);

            tvTitle.setText(reminder.getTitle());
            
            if (reminder.getDescription() == null || reminder.getDescription().isEmpty()) {
                tvDesc.setVisibility(View.GONE);
            } else {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(reminder.getDescription());
            }

            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            String formattedDate = formatter.format(new Date(reminder.getTimeMillis()));
            tvDateTime.setText("📅 " + formattedDate);

            if (reminder.getTimeMillis() <= System.currentTimeMillis() || !reminder.isActive()) {
                tvStatus.setText("EXPIRED");
                tvStatus.setTextColor(0xFF757575);
            } else {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(0xFF1E88E5);
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playSoundDelete();
                    cancelAlarm(reminder);
                    dbHelper.deleteReminder(reminder.getId());
                    Toast.makeText(context, "Reminder Deleted", Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            });

            // Clean cascading translation & alpha entry animation for list items
            if (position > lastPosition) {
                convertView.setTranslationY(120f);
                convertView.setAlpha(0f);
                convertView.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                lastPosition = position;
            }

            return convertView;
        }
    }
}