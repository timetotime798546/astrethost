package com.schoolcompanion.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // SharedPreferences identifiers
    private static final String PREFS_NAME = "SchoolCompanionPrefs";
    private static final String KEY_CLASSES = "school_classes";
    private static final String KEY_TASKS = "school_tasks";
    private static final String KEY_COURSES = "school_courses";
    private static final String KEY_NOTES = "school_notes";

    // Tab buttons
    private Button btnTabSchedule, btnTabTasks, btnTabGpa, btnTabNotes;
    
    // Tab containers
    private View containerSchedule, containerTasks, containerGpa, containerNotes;

    // Schedule View references
    private EditText etClassName, etClassTime, etClassRoom;
    private Spinner spinnerClassDay;
    private Button btnAddClass;
    private LinearLayout layoutScheduleList;

    // Tasks View references
    private EditText etTaskName, etTaskSubject, etTaskDue;
    private Button btnAddTask;
    private LinearLayout layoutTasksList;

    // GPA View references
    private TextView tvGpaScore;
    private EditText etCourseName;
    private Spinner spinnerGpaGrade, spinnerGpaCredits;
    private Button btnAddCourse;
    private LinearLayout layoutCourseList;

    // Notes View references
    private EditText etNoteTitle, etNoteBody;
    private Button btnAddNote;
    private LinearLayout layoutNotesList;

    // Data lists parsed from storage
    private JSONArray classesArray;
    private JSONArray tasksArray;
    private JSONArray coursesArray;
    private JSONArray notesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load persisted data
        loadSavedData();

        // Bind Views
        initNavigation();
        initScheduleTab();
        initTasksTab();
        initGpaTab();
        initNotesTab();

        // Set initial screen state
        switchTab(0);
    }

    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        try {
            classesArray = new JSONArray(prefs.getString(KEY_CLASSES, "[]"));
            tasksArray = new JSONArray(prefs.getString(KEY_TASKS, "[]"));
            coursesArray = new JSONArray(prefs.getString(KEY_COURSES, "[]"));
            notesArray = new JSONArray(prefs.getString(KEY_NOTES, "[]"));
        } catch (JSONException e) {
            classesArray = new JSONArray();
            tasksArray = new JSONArray();
            coursesArray = new JSONArray();
            notesArray = new JSONArray();
        }
    }

    private void saveData(String key, JSONArray array) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, array.toString());
        editor.apply();
    }

    private void initNavigation() {
        btnTabSchedule = (Button) findViewById(R.id.btn_tab_schedule);
        btnTabTasks = (Button) findViewById(R.id.btn_tab_tasks);
        btnTabGpa = (Button) findViewById(R.id.btn_tab_gpa);
        btnTabNotes = (Button) findViewById(R.id.btn_tab_notes);

        containerSchedule = findViewById(R.id.container_schedule);
        containerTasks = findViewById(R.id.container_tasks);
        containerGpa = findViewById(R.id.container_gpa);
        containerNotes = findViewById(R.id.container_notes);

        btnTabSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        btnTabTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        btnTabGpa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        btnTabNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });
    }

    private void switchTab(int index) {
        // Clear styles
        btnTabSchedule.setTextColor(0xFF7F8C8D);
        btnTabSchedule.setTypeface(null, android.graphics.Typeface.NORMAL);
        btnTabTasks.setTextColor(0xFF7F8C8D);
        btnTabTasks.setTypeface(null, android.graphics.Typeface.NORMAL);
        btnTabGpa.setTextColor(0xFF7F8C8D);
        btnTabGpa.setTypeface(null, android.graphics.Typeface.NORMAL);
        btnTabNotes.setTextColor(0xFF7F8C8D);
        btnTabNotes.setTypeface(null, android.graphics.Typeface.NORMAL);

        containerSchedule.setVisibility(View.GONE);
        containerTasks.setVisibility(View.GONE);
        containerGpa.setVisibility(View.GONE);
        containerNotes.setVisibility(View.GONE);

        // Set active selected state
        switch (index) {
            case 0:
                btnTabSchedule.setTextColor(0xFF2E3A59);
                btnTabSchedule.setTypeface(null, android.graphics.Typeface.BOLD);
                containerSchedule.setVisibility(View.VISIBLE);
                break;
            case 1:
                btnTabTasks.setTextColor(0xFF2E3A59);
                btnTabTasks.setTypeface(null, android.graphics.Typeface.BOLD);
                containerTasks.setVisibility(View.VISIBLE);
                break;
            case 2:
                btnTabGpa.setTextColor(0xFF2E3A59);
                btnTabGpa.setTypeface(null, android.graphics.Typeface.BOLD);
                containerGpa.setVisibility(View.VISIBLE);
                break;
            case 3:
                btnTabNotes.setTextColor(0xFF2E3A59);
                btnTabNotes.setTypeface(null, android.graphics.Typeface.BOLD);
                containerNotes.setVisibility(View.VISIBLE);
                break;
        }
    }

    // ==================== SCHEDULE LOGIC ====================
    private void initScheduleTab() {
        etClassName = (EditText) findViewById(R.id.et_class_name);
        etClassTime = (EditText) findViewById(R.id.et_class_time);
        etClassRoom = (EditText) findViewById(R.id.et_class_room);
        spinnerClassDay = (Spinner) findViewById(R.id.spinner_class_day);
        btnAddClass = (Button) findViewById(R.id.btn_add_class);
        layoutScheduleList = (LinearLayout) findViewById(R.id.layout_schedule_list);

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, days);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClassDay.setAdapter(adapter);

        btnAddClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etClassName.getText().toString().trim();
                String time = etClassTime.getText().toString().trim();
                String room = etClassRoom.getText().toString().trim();
                String day = spinnerClassDay.getSelectedItem().toString();

                if (name.isEmpty() || time.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please specify Class Name and Time", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", name);
                    obj.put("day", day);
                    obj.put("time", time);
                    obj.put("room", room.isEmpty() ? "N/A" : room);

                    classesArray.put(obj);
                    saveData(KEY_CLASSES, classesArray);
                    renderSchedule();

                    etClassName.setText("");
                    etClassTime.setText("");
                    etClassRoom.setText("");
                    Toast.makeText(MainActivity.this, "Class Added Successfully!", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        renderSchedule();
    }

    private void renderSchedule() {
        layoutScheduleList.removeAllViews();

        if (classesArray.length() == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No classes added to schedule yet.");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF7F8C8D);
            tvEmpty.setPadding(0, 20, 0, 20);
            layoutScheduleList.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < classesArray.length(); i++) {
            try {
                final int index = i;
                final JSONObject obj = classesArray.getJSONObject(i);

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setBackgroundColor(0xFFFFFFFF);
                itemLayout.setPadding(12, 12, 12, 12);
                itemLayout.setGravity(Gravity.CENTER_VERTICAL);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                itemLayout.setLayoutParams(lp);

                LinearLayout infoLayout = new LinearLayout(this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvName = new TextView(this);
                tvName.setText(obj.getString("name"));
                tvName.setTextSize(16spToPx(6));
                tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvName.setTextColor(0xFF2E3A59);

                TextView tvSub = new TextView(this);
                tvSub.setText(obj.getString("day") + " • " + obj.getString("time") + " | " + obj.getString("room"));
                tvSub.setTextSize(12spToPx(5));
                tvSub.setTextColor(0xFF7F8C8D);

                infoLayout.addView(tvName);
                infoLayout.addView(tvSub);

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(10);
                btnDelete.setBackgroundColor(0xFFE74C3C);
                btnDelete.setTextColor(0xFFFFFFFF);
                btnDelete.setPadding(6, 4, 6, 4);
                btnDelete.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80));
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeClass(index);
                    }
                });

                itemLayout.addView(infoLayout);
                itemLayout.addView(btnDelete);

                layoutScheduleList.addView(itemLayout);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeClass(int index) {
        JSONArray newList = new JSONArray();
        for (int i = 0; i < classesArray.length(); i++) {
            if (i != index) {
                try {
                    newList.put(classesArray.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        classesArray = newList;
        saveData(KEY_CLASSES, classesArray);
        renderSchedule();
    }


    // ==================== TASKS LOGIC ====================
    private void initTasksTab() {
        etTaskName = (EditText) findViewById(R.id.et_task_name);
        etTaskSubject = (EditText) findViewById(R.id.et_task_subject);
        etTaskDue = (EditText) findViewById(R.id.et_task_due);
        btnAddTask = (Button) findViewById(R.id.btn_add_task);
        layoutTasksList = (LinearLayout) findViewById(R.id.layout_tasks_list);

        btnAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTaskName.getText().toString().trim();
                String subject = etTaskSubject.getText().toString().trim();
                String due = etTaskDue.getText().toString().trim();

                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter an assignment title", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("title", title);
                    obj.put("subject", subject.isEmpty() ? "General" : subject);
                    obj.put("due", due.isEmpty() ? "No Date Set" : due);

                    tasksArray.put(obj);
                    saveData(KEY_TASKS, tasksArray);
                    renderTasks();

                    etTaskName.setText("");
                    etTaskSubject.setText("");
                    etTaskDue.setText("");
                    Toast.makeText(MainActivity.this, "Task Tracked!", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        renderTasks();
    }

    private void renderTasks() {
        layoutTasksList.removeAllViews();

        if (tasksArray.length() == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("All assignments completed! Great job.");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF7F8C8D);
            tvEmpty.setPadding(0, 20, 0, 20);
            layoutTasksList.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < tasksArray.length(); i++) {
            try {
                final int index = i;
                JSONObject obj = tasksArray.getJSONObject(i);

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setBackgroundColor(0xFFFFFFFF);
                itemLayout.setPadding(12, 12, 12, 12);
                itemLayout.setGravity(Gravity.CENTER_VERTICAL);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                itemLayout.setLayoutParams(lp);

                LinearLayout infoLayout = new LinearLayout(this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvTitle = new TextView(this);
                tvTitle.setText(obj.getString("title"));
                tvTitle.setTextSize(16);
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvTitle.setTextColor(0xFF2C3E50);

                TextView tvDetails = new TextView(this);
                tvDetails.setText("Subject: " + obj.getString("subject") + " | Due: " + obj.getString("due"));
                tvDetails.setTextSize(12);
                tvDetails.setTextColor(0xFF7F8C8D);

                infoLayout.addView(tvTitle);
                infoLayout.addView(tvDetails);

                Button btnDone = new Button(this);
                btnDone.setText("Complete");
                btnDone.setTextSize(10);
                btnDone.setBackgroundColor(0xFF2ECC71);
                btnDone.setTextColor(0xFFFFFFFF);
                btnDone.setPadding(6, 4, 6, 4);
                btnDone.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80));
                btnDone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeTask(index);
                        Toast.makeText(MainActivity.this, "Completed task dismissed!", Toast.LENGTH_SHORT).show();
                    }
                });

                itemLayout.addView(infoLayout);
                itemLayout.addView(btnDone);

                layoutTasksList.addView(itemLayout);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeTask(int index) {
        JSONArray newList = new JSONArray();
        for (int i = 0; i < tasksArray.length(); i++) {
            if (i != index) {
                try {
                    newList.put(tasksArray.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        tasksArray = newList;
        saveData(KEY_TASKS, tasksArray);
        renderTasks();
    }


    // ==================== GPA LOGIC ====================
    private void initGpaTab() {
        tvGpaScore = (TextView) findViewById(R.id.tv_gpa_score);
        etCourseName = (EditText) findViewById(R.id.et_course_name);
        spinnerGpaGrade = (Spinner) findViewById(R.id.spinner_gpa_grade);
        spinnerGpaCredits = (Spinner) findViewById(R.id.spinner_gpa_credits);
        btnAddCourse = (Button) findViewById(R.id.btn_add_course);
        layoutCourseList = (LinearLayout) findViewById(R.id.layout_course_list);

        String[] grades = {"A", "B", "C", "D", "F"};
        ArrayAdapter<String> gradeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, grades);
        gradeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGpaGrade.setAdapter(gradeAdapter);

        String[] credits = {"1", "2", "3", "4", "5"};
        ArrayAdapter<String> creditAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, credits);
        creditAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGpaCredits.setAdapter(creditAdapter);

        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String course = etCourseName.getText().toString().trim();
                String grade = spinnerGpaGrade.getSelectedItem().toString();
                int credits = Integer.parseInt(spinnerGpaCredits.getSelectedItem().toString());

                if (course.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter the course name", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("course", course);
                    obj.put("grade", grade);
                    obj.put("credits", credits);

                    coursesArray.put(obj);
                    saveData(KEY_COURSES, coursesArray);
                    renderCoursesAndGpa();

                    etCourseName.setText("");
                    Toast.makeText(MainActivity.this, "Course Added!", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        renderCoursesAndGpa();
    }

    private void renderCoursesAndGpa() {
        layoutCourseList.removeAllViews();

        if (coursesArray.length() == 0) {
            tvGpaScore.setText("0.00");
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No courses added yet.");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF7F8C8D);
            tvEmpty.setPadding(0, 20, 0, 20);
            layoutCourseList.addView(tvEmpty);
            return;
        }

        double totalGradePoints = 0.0;
        int totalCredits = 0;

        for (int i = 0; i < coursesArray.length(); i++) {
            try {
                final int index = i;
                JSONObject obj = coursesArray.getJSONObject(i);

                String courseName = obj.getString("course");
                String grade = obj.getString("grade");
                int credits = obj.getInt("credits");

                double points = getGpaPoints(grade);
                totalGradePoints += (points * credits);
                totalCredits += credits;

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setBackgroundColor(0xFFFFFFFF);
                itemLayout.setPadding(12, 12, 12, 12);
                itemLayout.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                itemLayout.setLayoutParams(lp);

                LinearLayout infoLayout = new LinearLayout(this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                infoLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvCourse = new TextView(this);
                tvCourse.setText(courseName);
                tvCourse.setTextSize(16);
                tvCourse.setTypeface(null, android.graphics.Typeface.BOLD);
                tvCourse.setTextColor(0xFF2C3E50);

                TextView tvGradeCredits = new TextView(this);
                tvGradeCredits.setText("Grade: " + grade + " | Credits: " + credits);
                tvGradeCredits.setTextSize(12);
                tvGradeCredits.setTextColor(0xFF7F8C8D);

                infoLayout.addView(tvCourse);
                infoLayout.addView(tvGradeCredits);

                Button btnDelete = new Button(this);
                btnDelete.setText("Remove");
                btnDelete.setTextSize(10);
                btnDelete.setBackgroundColor(0xFFE74C3C);
                btnDelete.setTextColor(0xFFFFFFFF);
                btnDelete.setPadding(6, 4, 6, 4);
                btnDelete.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80));
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeCourse(index);
                    }
                });

                itemLayout.addView(infoLayout);
                itemLayout.addView(btnDelete);

                layoutCourseList.addView(itemLayout);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (totalCredits > 0) {
            double finalGpa = totalGradePoints / totalCredits;
            tvGpaScore.setText(String.format("%.2f", finalGpa));
        } else {
            tvGpaScore.setText("0.00");
        }
    }

    private double getGpaPoints(String grade) {
        if ("A".equals(grade)) return 4.0;
        if ("B".equals(grade)) return 3.0;
        if ("C".equals(grade)) return 2.0;
        if ("D".equals(grade)) return 1.0;
        return 0.0;
    }

    private void removeCourse(int index) {
        JSONArray newList = new JSONArray();
        for (int i = 0; i < coursesArray.length(); i++) {
            if (i != index) {
                try {
                    newList.put(coursesArray.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        coursesArray = newList;
        saveData(KEY_COURSES, coursesArray);
        renderCoursesAndGpa();
    }


    // ==================== NOTES LOGIC ====================
    private void initNotesTab() {
        etNoteTitle = (EditText) findViewById(R.id.et_note_title);
        etNoteBody = (EditText) findViewById(R.id.et_note_body);
        btnAddNote = (Button) findViewById(R.id.btn_add_note);
        layoutNotesList = (LinearLayout) findViewById(R.id.layout_notes_list);

        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etNoteTitle.getText().toString().trim();
                String body = etNoteBody.getText().toString().trim();

                if (title.isEmpty() || body.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter both Title and Content", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("title", title);
                    obj.put("body", body);

                    notesArray.put(obj);
                    saveData(KEY_NOTES, notesArray);
                    renderNotes();

                    etNoteTitle.setText("");
                    etNoteBody.setText("");
                    Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        renderNotes();
    }

    private void renderNotes() {
        layoutNotesList.removeAllViews();

        if (notesArray.length() == 0) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No saved notes.");
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setTextColor(0xFF7F8C8D);
            tvEmpty.setPadding(0, 20, 0, 20);
            layoutNotesList.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < notesArray.length(); i++) {
            try {
                final int index = i;
                final JSONObject obj = notesArray.getJSONObject(i);

                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setBackgroundColor(0xFFFFFFFF);
                itemLayout.setPadding(16, 16, 16, 16);
                
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 14);
                itemLayout.setLayoutParams(lp);

                TextView tvTitle = new TextView(this);
                tvTitle.setText(obj.getString("title"));
                tvTitle.setTextSize(16);
                tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvTitle.setTextColor(0xFF2E3A59);
                tvTitle.setPadding(0, 0, 0, 6);

                TextView tvBody = new TextView(this);
                tvBody.setText(obj.getString("body"));
                tvBody.setTextSize(14);
                tvBody.setTextColor(0xFF555555);
                tvBody.setPadding(0, 0, 0, 10);

                LinearLayout actionLayout = new LinearLayout(this);
                actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionLayout.setGravity(Gravity.RIGHT);

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setTextSize(10);
                btnDelete.setBackgroundColor(0xFFE74C3C);
                btnDelete.setTextColor(0xFFFFFFFF);
                btnDelete.setPadding(10, 4, 10, 4);
                btnDelete.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, 70));
                btnDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeNote(index);
                    }
                });

                actionLayout.addView(btnDelete);

                itemLayout.addView(tvTitle);
                itemLayout.addView(tvBody);
                itemLayout.addView(actionLayout);

                layoutNotesList.addView(itemLayout);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeNote(int index) {
        JSONArray newList = new JSONArray();
        for (int i = 0; i < notesArray.length(); i++) {
            if (i != index) {
                try {
                    newList.put(notesArray.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        notesArray = newList;
        saveData(KEY_NOTES, notesArray);
        renderNotes();
    }

    // Convert sp to px helper for precise programmatic text sizes
    private int spToPx(float spVal) {
        return (int) (spVal * getResources().getDisplayMetrics().scaledDensity);
    }
}