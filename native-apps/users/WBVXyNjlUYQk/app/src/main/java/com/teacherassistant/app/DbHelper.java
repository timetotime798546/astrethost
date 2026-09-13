package com.teacherassistant.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "teacher_assistant.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    public static final String TABLE_STUDENTS = "students";
    public static final String TABLE_ATTENDANCE = "attendance";
    public static final String TABLE_ASSIGNMENTS = "assignments";
    public static final String TABLE_GRADES = "grades";
    public static final String TABLE_LESSONS = "lessons";

    // Common columns
    public static final String KEY_ID = "id";

    // STUDENTS columns
    public static final String KEY_STUDENT_NAME = "name";

    // ATTENDANCE columns
    public static final String KEY_ATT_STUDENT_ID = "student_id";
    public static final String KEY_ATT_DATE = "att_date";
    public static final String KEY_ATT_STATUS = "status"; // PRESENT / ABSENT

    // ASSIGNMENTS columns
    public static final String KEY_ASG_TITLE = "title";
    public static final String KEY_ASG_MAX = "max_points";

    // GRADES columns
    public static final String KEY_GRD_STUDENT_ID = "student_id";
    public static final String KEY_GRD_ASG_ID = "assignment_id";
    public static final String KEY_GRD_SCORE = "score";

    // LESSONS columns
    public static final String KEY_LES_TITLE = "title";
    public static final String KEY_LES_DATE = "les_date";
    public static final String KEY_LES_NOTES = "notes";
    public static final String KEY_LES_STATUS = "status"; // DRAFT / COMPLETED

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Students table
        String CREATE_STUDENTS_TABLE = "CREATE TABLE " + TABLE_STUDENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_STUDENT_NAME + " TEXT" + ")";
        db.execSQL(CREATE_STUDENTS_TABLE);

        // Create Attendance table
        String CREATE_ATTENDANCE_TABLE = "CREATE TABLE " + TABLE_ATTENDANCE + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ATT_STUDENT_ID + " INTEGER,"
                + KEY_ATT_DATE + " TEXT,"
                + KEY_ATT_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_ATTENDANCE_TABLE);

        // Create Assignments table
        String CREATE_ASSIGNMENTS_TABLE = "CREATE TABLE " + TABLE_ASSIGNMENTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_ASG_TITLE + " TEXT,"
                + KEY_ASG_MAX + " REAL" + ")";
        db.execSQL(CREATE_ASSIGNMENTS_TABLE);

        // Create Grades table
        String CREATE_GRADES_TABLE = "CREATE TABLE " + TABLE_GRADES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_GRD_STUDENT_ID + " INTEGER,"
                + KEY_GRD_ASG_ID + " INTEGER,"
                + KEY_GRD_SCORE + " REAL" + ")";
        db.execSQL(CREATE_GRADES_TABLE);

        // Create Lessons table
        String CREATE_LESSONS_TABLE = "CREATE TABLE " + TABLE_LESSONS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_LES_TITLE + " TEXT,"
                + KEY_LES_DATE + " TEXT,"
                + KEY_LES_NOTES + " TEXT,"
                + KEY_LES_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_LESSONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ASSIGNMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GRADES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LESSONS);
        onCreate(db);
    }

    // --- Students Operations ---
    public long insertStudent(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_STUDENT_NAME, name);
        long id = db.insert(TABLE_STUDENTS, null, values);
        return id;
    }

    public List<Models.Student> getAllStudents() {
        List<Models.Student> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_STUDENTS, null, null, null, null, null, KEY_STUDENT_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STUDENT_NAME));
                Models.Student s = new Models.Student(id, name);
                
                // Compute summary metrics dynamically for this student
                s.attendancePercentage = getStudentAttendanceRate(id);
                s.avgGrade = getStudentAverageGrade(id);

                list.add(s);
            }
            cursor.close();
        }
        return list;
    }

    public void deleteStudent(long studentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STUDENTS, KEY_ID + " = ?", new String[]{String.valueOf(studentId)});
        db.delete(TABLE_ATTENDANCE, KEY_ATT_STUDENT_ID + " = ?", new String[]{String.valueOf(studentId)});
        db.delete(TABLE_GRADES, KEY_GRD_STUDENT_ID + " = ?", new String[]{String.valueOf(studentId)});
    }

    // --- Attendance Operations ---
    public void saveAttendance(long studentId, String date, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Check if attendance for this student and date already recorded
        Cursor cursor = db.query(TABLE_ATTENDANCE, null, 
                KEY_ATT_STUDENT_ID + " = ? AND " + KEY_ATT_DATE + " = ?", 
                new String[]{String.valueOf(studentId), date}, null, null, null);
        
        ContentValues values = new ContentValues();
        values.put(KEY_ATT_STUDENT_ID, studentId);
        values.put(KEY_ATT_DATE, date);
        values.put(KEY_ATT_STATUS, status);

        if (cursor != null && cursor.getCount() > 0) {
            db.update(TABLE_ATTENDANCE, values, KEY_ATT_STUDENT_ID + " = ? AND " + KEY_ATT_DATE + " = ?", 
                    new String[]{String.valueOf(studentId), date});
        } else {
            db.insert(TABLE_ATTENDANCE, null, values);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public String getAttendanceStatus(long studentId, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, new String[]{KEY_ATT_STATUS}, 
                KEY_ATT_STUDENT_ID + " = ? AND " + KEY_ATT_DATE + " = ?", 
                new String[]{String.valueOf(studentId), date}, null, null, null);
        String status = "PRESENT"; // Default to PRESENT if not saved
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ATT_STATUS));
            }
            cursor.close();
        }
        return status;
    }

    private int getStudentAttendanceRate(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ATTENDANCE, null, KEY_ATT_STUDENT_ID + " = ?", 
                new String[]{String.valueOf(studentId)}, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return 100; // Assume 100% attendance if no records
        }
        int total = cursor.getCount();
        int presentCount = 0;
        while (cursor.moveToNext()) {
            String status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ATT_STATUS));
            if ("PRESENT".equalsIgnoreCase(status)) {
                presentCount++;
            }
        }
        cursor.close();
        return (int) (((double) presentCount / total) * 100);
    }

    // --- Assignments Operations ---
    public long insertAssignment(String title, double maxPoints) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ASG_TITLE, title);
        values.put(KEY_ASG_MAX, maxPoints);
        return db.insert(TABLE_ASSIGNMENTS, null, values);
    }

    public List<Models.Assignment> getAllAssignments() {
        List<Models.Assignment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ASSIGNMENTS, null, null, null, null, null, KEY_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ASG_TITLE));
                double max = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_ASG_MAX));
                list.add(new Models.Assignment(id, title, max));
            }
            cursor.close();
        }
        return list;
    }

    // --- Grades Operations ---
    public void saveGrade(long studentId, long assignmentId, double score) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_GRADES, null, 
                KEY_GRD_STUDENT_ID + " = ? AND " + KEY_GRD_ASG_ID + " = ?", 
                new String[]{String.valueOf(studentId), String.valueOf(assignmentId)}, null, null, null);
        
        ContentValues values = new ContentValues();
        values.put(KEY_GRD_STUDENT_ID, studentId);
        values.put(KEY_GRD_ASG_ID, assignmentId);
        values.put(KEY_GRD_SCORE, score);

        if (cursor != null && cursor.getCount() > 0) {
            db.update(TABLE_GRADES, values, KEY_GRD_STUDENT_ID + " = ? AND " + KEY_GRD_ASG_ID + " = ?", 
                    new String[]{String.valueOf(studentId), String.valueOf(assignmentId)});
        } else {
            db.insert(TABLE_GRADES, null, values);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public double getGradeScore(long studentId, long assignmentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_GRADES, new String[]{KEY_GRD_SCORE}, 
                KEY_GRD_STUDENT_ID + " = ? AND " + KEY_GRD_ASG_ID + " = ?", 
                new String[]{String.valueOf(studentId), String.valueOf(assignmentId)}, null, null, null);
        double score = -1; // -1 represents ungraded/empty
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                score = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_GRD_SCORE));
            }
            cursor.close();
        }
        return score;
    }

    private double getStudentAverageGrade(long studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT G." + KEY_GRD_SCORE + ", A." + KEY_ASG_MAX + " FROM " + TABLE_GRADES + " G"
                + " INNER JOIN " + TABLE_ASSIGNMENTS + " A ON G." + KEY_GRD_ASG_ID + " = A." + KEY_ID
                + " WHERE G." + KEY_GRD_STUDENT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(studentId)});
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return -1; // Represents no records
        }
        double sumEarned = 0;
        double sumMax = 0;
        while (cursor.moveToNext()) {
            double earned = cursor.getDouble(0);
            double max = cursor.getDouble(1);
            if (earned >= 0 && max > 0) {
                sumEarned += earned;
                sumMax += max;
            }
        }
        cursor.close();
        if (sumMax == 0) return 100.0;
        return (sumEarned / sumMax) * 100.0;
    }

    // --- Planner / Lessons Operations ---
    public long insertLesson(String title, String date, String notes, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LES_TITLE, title);
        values.put(KEY_LES_DATE, date);
        values.put(KEY_LES_NOTES, notes);
        values.put(KEY_LES_STATUS, status);
        return db.insert(TABLE_LESSONS, null, values);
    }

    public List<Models.Lesson> getAllLessons() {
        List<Models.Lesson> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LESSONS, null, null, null, null, null, KEY_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LES_TITLE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LES_DATE));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LES_NOTES));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LES_STATUS));
                list.add(new Models.Lesson(id, title, date, notes, status));
            }
            cursor.close();
        }
        return list;
    }

    public void updateLessonStatus(long lessonId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LES_STATUS, status);
        db.update(TABLE_LESSONS, values, KEY_ID + " = ?", new String[]{String.valueOf(lessonId)});
    }

    public void deleteLesson(long lessonId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LESSONS, KEY_ID + " = ?", new String[]{String.valueOf(lessonId)});
    }
}