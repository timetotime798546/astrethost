package com.studentadminpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // Screen Layout Containers
    private LinearLayout layoutLogin, layoutDashboard, layoutAddStudent, layoutViewStudents, layoutAttendance, layoutFees, layoutMarks;

    // Databases helper
    private DatabaseHelper dbHelper;

    // Login Views
    private EditText etUsername, etPassword;
    private Button btnLogin;

    // Dashboard Status Views
    private TextView tvStatTotalStudents, tvStatPresentToday, tvStatFeesCollected;
    private Button btnLogout;

    // Add Student Views
    private EditText etStudentName, etStudentRoll, etStudentClass, etStudentContact;
    private Button btnSaveStudent, btnCancelAddStudent;

    // View Students Views
    private ListView lvStudents;
    private Button btnBackViewStudents;

    // Attendance Views
    private Spinner spinnerAttendanceStudent, spinnerAttendanceStatus;
    private Button btnSaveAttendance, btnBackAttendance;
    private ListView lvAttendanceLog;

    // Fees Views
    private Spinner spinnerFeeStudent, spinnerFeeStatus;
    private EditText etFeeAmount;
    private Button btnSaveFee, btnBackFees;
    private ListView lvFeeLog;

    // Marks Views
    private Spinner spinnerMarksStudent;
    private EditText etMarksSubject, etMarksScore, etMarksMax;
    private Button btnSaveMarks, btnBackMarks;
    private ListView lvMarksLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init database
        dbHelper = new DatabaseHelper(this);
        prepopulateDummyDataIfEmpty();

        // Bind all UI Views
        initializeUI();

        // Setup action listeners
        setupListeners();
    }

    private void initializeUI() {
        // Layout containers
        layoutLogin = (LinearLayout) findViewById(R.id.layout_login);
        layoutDashboard = (LinearLayout) findViewById(R.id.layout_dashboard);
        layoutAddStudent = (LinearLayout) findViewById(R.id.layout_add_student);
        layoutViewStudents = (LinearLayout) findViewById(R.id.layout_view_students);
        layoutAttendance = (LinearLayout) findViewById(R.id.layout_attendance);
        layoutFees = (LinearLayout) findViewById(R.id.layout_fees);
        layoutMarks = (LinearLayout) findViewById(R.id.layout_marks);

        // Login screen components
        etUsername = (EditText) findViewById(R.id.et_username);
        etPassword = (EditText) findViewById(R.id.et_password);
        btnLogin = (Button) findViewById(R.id.btn_login);

        // Dashboard screen components
        tvStatTotalStudents = (TextView) findViewById(R.id.tv_stat_total_students);
        tvStatPresentToday = (TextView) findViewById(R.id.tv_stat_present_today);
        tvStatFeesCollected = (TextView) findViewById(R.id.tv_stat_fees_collected);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        // Add Student Form elements
        etStudentName = (EditText) findViewById(R.id.et_student_name);
        etStudentRoll = (EditText) findViewById(R.id.et_student_roll);
        etStudentClass = (EditText) findViewById(R.id.et_student_class);
        etStudentContact = (EditText) findViewById(R.id.et_student_contact);
        btnSaveStudent = (Button) findViewById(R.id.btn_save_student);
        btnCancelAddStudent = (Button) findViewById(R.id.btn_cancel_add_student);

        // View Directory
        lvStudents = (ListView) findViewById(R.id.lv_students);
        btnBackViewStudents = (Button) findViewById(R.id.btn_back_view_students);

        // Attendance screen
        spinnerAttendanceStudent = (Spinner) findViewById(R.id.spinner_attendance_student);
        spinnerAttendanceStatus = (Spinner) findViewById(R.id.spinner_attendance_status);
        btnSaveAttendance = (Button) findViewById(R.id.btn_save_attendance);
        lvAttendanceLog = (ListView) findViewById(R.id.lv_attendance_log);
        btnBackAttendance = (Button) findViewById(R.id.btn_back_attendance);

        // Fees
        spinnerFeeStudent = (Spinner) findViewById(R.id.spinner_fee_student);
        spinnerFeeStatus = (Spinner) findViewById(R.id.spinner_fee_status);
        etFeeAmount = (EditText) findViewById(R.id.et_fee_amount);
        btnSaveFee = (Button) findViewById(R.id.btn_save_fee);
        lvFeeLog = (ListView) findViewById(R.id.lv_fee_log);
        btnBackFees = (Button) findViewById(R.id.btn_back_fees);

        // Marks
        spinnerMarksStudent = (Spinner) findViewById(R.id.spinner_marks_student);
        etMarksSubject = (EditText) findViewById(R.id.et_marks_subject);
        etMarksScore = (EditText) findViewById(R.id.et_marks_score);
        etMarksMax = (EditText) findViewById(R.id.et_marks_max);
        btnSaveMarks = (Button) findViewById(R.id.btn_save_marks);
        lvMarksLog = (ListView) findViewById(R.id.lv_marks_log);
        btnBackMarks = (Button) findViewById(R.id.btn_back_marks);

        // Setup basic spinners options
        setupSpinnersStaticContent();
    }

    private void setupSpinnersStaticContent() {
        // Attendance Status: Present or Absent
        String[] statusOptions = {"Present", "Absent"};
        ArrayAdapter<String> adapterStatus = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, statusOptions);
        adapterStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAttendanceStatus.setAdapter(adapterStatus);

        // Fee Status: Paid or Pending
        String[] feeOptions = {"Paid", "Pending"};
        ArrayAdapter<String> adapterFee = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, feeOptions);
        adapterFee.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeeStatus.setAdapter(adapterFee);
    }

    private void setupListeners() {
        // Handle Login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etUsername.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();
                if (user.equals("admin") && pass.equals("password")) {
                    Toast.makeText(MainActivity.this, "लॉगिन सफल हुआ! स्वागत है एडमिन।", Toast.LENGTH_SHORT).show();
                    etUsername.setText("");
                    etPassword.setText("");
                    showLayout(layoutDashboard);
                    loadDashboardStats();
                } else {
                    Toast.makeText(MainActivity.this, "अमान्य लॉगिन क्रेडेंशियल!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Handle Logout button
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutLogin);
                Toast.makeText(MainActivity.this, "लॉगआउट सफल हुआ।", Toast.LENGTH_SHORT).show();
            }
        });

        // Dashboard Navigation Links
        findViewById(R.id.btn_nav_add_student).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutAddStudent);
            }
        });

        findViewById(R.id.btn_nav_view_students).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutViewStudents);
                loadStudentDirectoryList();
            }
        });

        findViewById(R.id.btn_nav_attendance).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutAttendance);
                refreshStudentSelectionSpinner(spinnerAttendanceStudent);
                loadAttendanceHistoryList();
            }
        });

        findViewById(R.id.btn_nav_fees).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutFees);
                refreshStudentSelectionSpinner(spinnerFeeStudent);
                loadFeeHistoryList();
            }
        });

        findViewById(R.id.btn_nav_marks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutMarks);
                refreshStudentSelectionSpinner(spinnerMarksStudent);
                loadMarksHistoryList();
            }
        });

        // Back Navigation handlers
        btnCancelAddStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutDashboard);
                loadDashboardStats();
            }
        });

        btnBackViewStudents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutDashboard);
                loadDashboardStats();
            }
        });

        btnBackAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutDashboard);
                loadDashboardStats();
            }
        });

        btnBackFees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutDashboard);
                loadDashboardStats();
            }
        });

        btnBackMarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLayout(layoutDashboard);
                loadDashboardStats();
            }
        });

        // Save Student logic
        btnSaveStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etStudentName.getText().toString().trim();
                String roll = etStudentRoll.getText().toString().trim();
                String grade = etStudentClass.getText().toString().trim();
                String contact = etStudentContact.getText().toString().trim();

                if (name.isEmpty() || roll.isEmpty() || grade.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(MainActivity.this, "कृपया सभी फ़ील्ड भरें!", Toast.LENGTH_SHORT).show();
                    return;
                }

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("roll", roll);
                values.put("grade", grade);
                values.put("contact", contact);

                long id = db.insert("students", null, values);
                if (id != -1) {
                    Toast.makeText(MainActivity.this, "छात्र सफलतापूर्वक जोड़ा गया!", Toast.LENGTH_SHORT).show();
                    etStudentName.setText("");
                    etStudentRoll.setText("");
                    etStudentClass.setText("");
                    etStudentContact.setText("");
                    showLayout(layoutDashboard);
                    loadDashboardStats();
                } else {
                    Toast.makeText(MainActivity.this, "त्रुटि! छात्र सहेजा नहीं जा सका।", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Delete student with confirmation inside Directory list view (Long press event)
        lvStudents.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                final int studentId = parseIdFromSelectedItemString(selectedItem);
                if (studentId == -1) return false;

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("हटाने की पुष्टि करें");
                builder.setMessage("क्या आप निश्चित रूप से इस छात्र का रिकॉर्ड हटाना चाहते हैं?");
                builder.setPositiveButton("हाँ, हटाएं", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("students", "id = ?", new String[]{String.valueOf(studentId)});
                        db.delete("attendance", "student_id = ?", new String[]{String.valueOf(studentId)});
                        db.delete("fees", "student_id = ?", new String[]{String.valueOf(studentId)});
                        db.delete("marks", "student_id = ?", new String[]{String.valueOf(studentId)});

                        Toast.makeText(MainActivity.this, "छात्र का रिकॉर्ड हटा दिया गया।", Toast.LENGTH_SHORT).show();
                        loadStudentDirectoryList();
                    }
                });
                builder.setNegativeButton("रद्द करें", null);
                builder.show();
                return true;
            } 
        });

        // Save Attendance entry
        btnSaveAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinnerAttendanceStudent.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "पहले छात्र को पंजीकृत करें!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedStudentText = spinnerAttendanceStudent.getSelectedItem().toString();
                int studentId = parseIdFromSelectedItemString(selectedStudentText);
                String status = spinnerAttendanceStatus.getSelectedItem().toString();
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                if (studentId == -1) return;

                SQLiteDatabase db = dbHelper.getWritableDatabase();

                // Remove previous attendance for today for this student if duplicate
                db.delete("attendance", "student_id = ? AND date = ?", new String[]{String.valueOf(studentId), todayDate});

                ContentValues values = new ContentValues();
                values.put("student_id", studentId);
                values.put("date", todayDate);
                values.put("status", status);

                long result = db.insert("attendance", null, values);
                if (result != -1) {
                    Toast.makeText(MainActivity.this, "उपस्थिति सुरक्षित की गई!", Toast.LENGTH_SHORT).show();
                    loadAttendanceHistoryList();
                } else {
                    Toast.makeText(MainActivity.this, "उपस्थिति सुरक्षित करने में विफल!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Save Fee log
        btnSaveFee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinnerFeeStudent.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "पहले छात्र जोड़ें!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String amountText = etFeeAmount.getText().toString().trim();
                if (amountText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "कृपया वैध राशि प्रविष्ट करें!", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountText);
                String selectedStudentText = spinnerFeeStudent.getSelectedItem().toString();
                int studentId = parseIdFromSelectedItemString(selectedStudentText);
                String status = spinnerFeeStatus.getSelectedItem().toString();
                String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                if (studentId == -1) return;

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("student_id", studentId);
                values.put("amount", amount);
                values.put("status", status);
                values.put("date", todayDate);

                long result = db.insert("fees", null, values);
                if (result != -1) {
                    Toast.makeText(MainActivity.this, "फीस रिकॉर्ड सुरक्षित किया गया!", Toast.LENGTH_SHORT).show();
                    etFeeAmount.setText("");
                    loadFeeHistoryList();
                } else {
                    Toast.makeText(MainActivity.this, "फीस प्रविष्टि विफल!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Save Performance Marks
        btnSaveMarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinnerMarksStudent.getSelectedItem() == null) {
                    Toast.makeText(MainActivity.this, "पहले छात्र जोड़ें!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String subject = etMarksSubject.getText().toString().trim();
                String scoreStr = etMarksScore.getText().toString().trim();
                String maxStr = etMarksMax.getText().toString().trim();

                if (subject.isEmpty() || scoreStr.isEmpty() || maxStr.isEmpty()) {
                    Toast.makeText(MainActivity.this, "सभी फ़ील्ड आवश्यक हैं!", Toast.LENGTH_SHORT).show();
                    return;
                }

                double score = Double.parseDouble(scoreStr);
                double maxMarks = Double.parseDouble(maxStr);

                if (score > maxMarks) {
                    Toast.makeText(MainActivity.this, "प्राप्त अंक अधिकतम से अधिक नहीं हो सकते!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedStudentText = spinnerMarksStudent.getSelectedItem().toString();
                int studentId = parseIdFromSelectedItemString(selectedStudentText);

                if (studentId == -1) return;

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("student_id", studentId);
                values.put("subject", subject);
                values.put("score", score);
                values.put("max_score", maxMarks);

                long result = db.insert("marks", null, values);
                if (result != -1) {
                    Toast.makeText(MainActivity.this, "मार्क्स रिकॉर्ड सुरक्षित किया गया!", Toast.LENGTH_SHORT).show();
                    etMarksSubject.setText("");
                    etMarksScore.setText("");
                    etMarksMax.setText("");
                    loadMarksHistoryList();
                } else {
                    Toast.makeText(MainActivity.this, "मार्क्स प्रविष्टि विफल!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLayout(LinearLayout activeLayout) {
        layoutLogin.setVisibility(View.GONE);
        layoutDashboard.setVisibility(View.GONE);
        layoutAddStudent.setVisibility(View.GONE);
        layoutViewStudents.setVisibility(View.GONE);
        layoutAttendance.setVisibility(View.GONE);
        layoutFees.setVisibility(View.GONE);
        layoutMarks.setVisibility(View.GONE);

        activeLayout.setVisibility(View.VISIBLE);
    }

    private int parseIdFromSelectedItemString(String selectedItemText) {
        try {
            if (selectedItemText != null && selectedItemText.contains("-")) {
                String[] parts = selectedItemText.split("-");
                return Integer.parseInt(parts[0].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Helper loader to update real-time statistics counters
    private void loadDashboardStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Total student count
        Cursor cStudents = db.rawQuery("SELECT COUNT(*) FROM students", null);
        int totalStudents = 0;
        if (cStudents.moveToFirst()) {
            totalStudents = cStudents.getInt(0);
        }
        cStudents.close();
        tvStatTotalStudents.setText(String.valueOf(totalStudents));

        // 2. Attendance Present today count
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor cAttendance = db.rawQuery("SELECT COUNT(*) FROM attendance WHERE date = ? AND status = 'Present'", new String[]{todayDate});
        int totalPresent = 0;
        if (cAttendance.moveToFirst()) {
            totalPresent = cAttendance.getInt(0);
        }
        cAttendance.close();
        tvStatPresentToday.setText(String.valueOf(totalPresent));

        // 3. Sum of collected fees
        Cursor cFees = db.rawQuery("SELECT SUM(amount) FROM fees WHERE status = 'Paid'", null);
        double totalFees = 0.0;
        if (cFees.moveToFirst()) {
            totalFees = cFees.getDouble(0);
        }
        cFees.close();
        tvStatFeesCollected.setText("₹" + Math.round(totalFees));
    }

    // Dynamic selection spinner data filler
    private void refreshStudentSelectionSpinner(Spinner spinner) {
        List<String> studentSpinnerList = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, roll FROM students ORDER BY name ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String roll = cursor.getString(2);
                studentSpinnerList.add(id + " - " + name + " (Roll: " + roll + ")");
            } while (cursor.moveToNext());
        }
        cursor.close();

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, studentSpinnerList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    // Directory Loader
    private void loadStudentDirectoryList() {
        List<String> studentsList = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, roll, grade, contact FROM students ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String roll = cursor.getString(2);
                String grade = cursor.getString(3);
                String contact = cursor.getString(4);
                studentsList.add(id + " - " + name + "\nRoll No: " + roll + " | Class: " + grade + "\nContact: " + contact);
            } while (cursor.moveToNext());
        } else {
            studentsList.add("कोई स्टूडेंट नहीं मिला। कृपया स्टूडेंट जोड़ें!");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, studentsList);
        lvStudents.setAdapter(adapter);
    }

    // Attendance list loader
    private void loadAttendanceHistoryList() {
        List<String> attendanceLogs = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String query = "SELECT s.name, a.status, a.date FROM attendance a JOIN students s ON a.student_id = s.id WHERE a.date = ? ORDER BY a.id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{todayDate});
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String status = cursor.getString(1);
                String dateStr = cursor.getString(2);
                attendanceLogs.add(name + " -> " + (status.equals("Present") ? "🟢 Present" : "🔴 Absent") + " (" + dateStr + ")");
            } while (cursor.moveToNext());
        } else {
            attendanceLogs.add("आज की उपस्थिति अभी दर्ज नहीं हुई है।");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, attendanceLogs);
        lvAttendanceLog.setAdapter(adapter);
    }

    // Fee list loader
    private void loadFeeHistoryList() {
        List<String> feeLogs = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT s.name, f.amount, f.status, f.date FROM fees f JOIN students s ON f.student_id = s.id ORDER BY f.id DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                double amount = cursor.getDouble(1);
                String status = cursor.getString(2);
                String date = cursor.getString(3);
                feeLogs.add(name + " - ₹" + amount + " (" + status + ") ऑन " + date);
            } while (cursor.moveToNext());
        } else {
            feeLogs.add("कोई फीस भुगतान इतिहास नहीं मिला।");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, feeLogs);
        lvFeeLog.setAdapter(adapter);
    }

    // Academic performance marks loader
    private void loadMarksHistoryList() {
        List<String> marksLogs = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT s.name, m.subject, m.score, m.max_score FROM marks m JOIN students s ON m.student_id = s.id ORDER BY m.id DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String subject = cursor.getString(1);
                double score = cursor.getDouble(2);
                double maxMarks = cursor.getDouble(3);
                double percentage = (score / maxMarks) * 100.0;
                marksLogs.add(name + " : " + subject + "\nप्राप्तांक: " + score + " / " + maxMarks + " (" + Math.round(percentage) + "%)");
            } while (cursor.moveToNext());
        } else {
            marksLogs.add("कोई परीक्षा मार्क्स प्रविष्टि नहीं मिली।");
        }
        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, marksLogs);
        lvMarksLog.setAdapter(adapter);
    }

    // Dummy seed records
    private void prepopulateDummyDataIfEmpty() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM students", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        if (count == 0) {
            // Add Student 1
            ContentValues s1 = new ContentValues();
            s1.put("name", "रमेश कुमार (Ramesh Kumar)");
            s1.put("roll", "101");
            s1.put("grade", "10th Class");
            s1.put("contact", "9876543210");
            long id1 = db.insert("students", null, s1);

            // Add Student 2
            ContentValues s2 = new ContentValues();
            s2.put("name", "प्रिया शर्मा (Priya Sharma)");
            s2.put("roll", "102");
            s2.put("grade", "10th Class");
            s2.put("contact", "8765432109");
            long id2 = db.insert("students", null, s2);

            // Add Student 3
            ContentValues s3 = new ContentValues();
            s3.put("name", "अमित वर्मा (Amit Verma)");
            s3.put("roll", "103");
            s3.put("grade", "9th Class");
            s3.put("contact", "7654321098");
            long id3 = db.insert("students", null, s3);

            // Setup dummy attendance for Ramesh
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            ContentValues attVal = new ContentValues();
            attVal.put("student_id", id1);
            attVal.put("date", today);
            attVal.put("status", "Present");
            db.insert("attendance", null, attVal);

            // Setup dummy fees collection
            ContentValues feeVal = new ContentValues();
            feeVal.put("student_id", id1);
            feeVal.put("amount", 1500.00);
            feeVal.put("status", "Paid");
            feeVal.put("date", today);
            db.insert("fees", null, feeVal);
        }
    }

    // SQL Helper Class definition
    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "student_records.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE students (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, roll TEXT, grade TEXT, contact TEXT);");
            db.execSQL("CREATE TABLE attendance (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, date TEXT, status TEXT);");
            db.execSQL("CREATE TABLE fees (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, amount REAL, status TEXT, date TEXT);");
            db.execSQL("CREATE TABLE marks (id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER, subject TEXT, score REAL, max_score REAL);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS students");
            db.execSQL("DROP TABLE IF EXISTS attendance");
            db.execSQL("DROP TABLE IF EXISTS fees");
            db.execSQL("DROP TABLE IF EXISTS marks");
            onCreate(db);
        }
    }
}