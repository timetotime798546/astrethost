package com.studentplanner.app.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.studentplanner.app.R;
import com.studentplanner.app.data.DatabaseHelper;
import com.studentplanner.app.models.Subject;

public class AddSubjectActivity extends Activity {

    private EditText subjectNameEditText;
    private Button saveSubjectButton;
    private DatabaseHelper dbHelper;
    private int subjectId = -1; // -1 for new subject, otherwise editing existing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subject);

        dbHelper = new DatabaseHelper(this);

        subjectNameEditText = findViewById(R.id.subject_name_edit_text);
        saveSubjectButton = findViewById(R.id.save_subject_button);

        // Check if editing an existing subject
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("subject_id")) {
            subjectId = extras.getInt("subject_id");
            String subjectName = extras.getString("subject_name");
            subjectNameEditText.setText(subjectName);
            saveSubjectButton.setText(R.string.edit_subject);
            setTitle(R.string.edit_subject); // Set activity title
        } else {
            setTitle(R.string.title_activity_add_subject); // Set activity title for new subject
        }

        saveSubjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSubject();
            }
        });
    }

    private void saveSubject() {
        String name = subjectNameEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Subject name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subjectId == -1) {
            // Add new subject
            Subject newSubject = new Subject(name);
            long id = dbHelper.addSubject(newSubject);
            if (id > 0) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add subject. Name might already exist.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing subject
            Subject existingSubject = new Subject(subjectId, name);
            int rowsAffected = dbHelper.updateSubject(existingSubject);
            if (rowsAffected > 0) {
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update subject.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}