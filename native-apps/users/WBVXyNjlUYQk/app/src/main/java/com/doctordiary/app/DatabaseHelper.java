package com.doctordiary.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "doctordiary.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE patients (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, age TEXT, gender TEXT, contact TEXT, history TEXT)");
        db.execSQL("CREATE TABLE appointments (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_name TEXT, app_date TEXT, app_time TEXT, purpose TEXT, status TEXT)");
        db.execSQL("CREATE TABLE prescriptions (id INTEGER PRIMARY KEY AUTOINCREMENT, patient_name TEXT, symptoms TEXT, diagnosis TEXT, medicines TEXT, advice TEXT, date_created TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS patients");
        db.execSQL("DROP TABLE IF EXISTS appointments");
        db.execSQL("DROP TABLE IF EXISTS prescriptions");
        onCreate(db);
    }
}