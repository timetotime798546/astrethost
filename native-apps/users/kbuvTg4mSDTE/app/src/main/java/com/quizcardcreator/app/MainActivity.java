package com.quizcardcreator.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    // Style Parameters constants
    private static final int[] THEME_START_COLORS = {
        0xFF1E3C72, // Royal Blue
        0xFFFF416C, // Sunset Pink
        0xFF11998E, // Emerald Green
        0xFF30CFD0, // Ocean Teal
        0xFF232526, // Charcoal Black
        0xFFEF32D9, // Orchid Magic
        0xFFF12711, // Fire Mango
        0xFF3A6073  // Slate Grey
    };

    private static final int[] THEME_END_COLORS = {
        0xFF2A5298,
        0xFFFF4B2B,
        0xFF38EF7D,
        0xFF330867,
        0xFF414345,
        0xFF89FFFD,
        0xFFF5AF19,
        0xFF3A6073
    };

    private static final String[] THEME_NAMES = {
        "Navy", "Sunset", "Emerald", "Teal", "Obsidian", "Orchid", "Mango", "Slate"
    };

    private static final int[] FONT_COLORS = {
        0xFFFFFFFF, // Pure White
        0xFFFFFF8D, // Soft Yellow
        0xFFA7FFEB, // Mint Green
        0xFFE0F7FA, // Soft Ice
        0xFF212121  // Charcoal Dark
    };

    private static final int[] FONT_SIZES = {
        16, // Small
        20, // Medium
        24, // Large
        28  // Extra Large
    };

    private static final String[] FONT_SIZE_LABELS = {
        "S", "M", "L", "XL"
    };

    // Question Data Model
    public static class Question {
        String id;
        String questionText;
        String optionA;
        String optionB;
        String optionC;
        String optionD;
        int themeIndex;
        int fontColorIndex;
        int fontSizeIndex;

        public Question() {
            id = UUID.randomUUID().toString();
            questionText = "";
            optionA = "";
            optionB = "";
            optionC = "";
            optionD = "";
            themeIndex = 0;
            fontColorIndex = 0;
            fontSizeIndex = 1; // Medium default
        }
    }

    private List<Question> questionList;
    private Question currentDraft;
    private String editingQuestionId = null; // null means adding a new card

    // Android Views
    private ImageView cardPreview;
    private LinearLayout themeContainer;
    private LinearLayout colorContainer;
    private LinearLayout sizeContainer;
    
    private EditText editQuestion;
    private EditText editOptionA;
    private EditText editOptionB;
    private EditText editOptionC;
    private EditText editOptionD;

    private Button btnAddUpdate;
    private Button btnSaveCurrentImage;
    private Button btnExportAllPdf;
    private TextView btnClearAll;
    private TextView tvEditingIndicator;
    private TextView tvCountHeader;
    private LinearLayout questionsListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Map Views
        cardPreview = (ImageView) findViewById(R.id.card_preview);
        themeContainer = (LinearLayout) findViewById(R.id.theme_container);
        colorContainer = (LinearLayout) findViewById(R.id.color_container);
        sizeContainer = (LinearLayout) findViewById(R.id.size_container);

        editQuestion = (EditText) findViewById(R.id.edit_question);
        editOptionA = (EditText) findViewById(R.id.edit_option_a);
        editOptionB = (EditText) findViewById(R.id.edit_option_b);
        editOptionC = (EditText) findViewById(R.id.edit_option_c);
        editOptionD = (EditText) findViewById(R.id.edit_option_d);

        btnAddUpdate = (Button) findViewById(R.id.btn_add_update_question);
        btnSaveCurrentImage = (Button) findViewById(R.id.btn_save_current_image);
        btnExportAllPdf = (Button) findViewById(R.id.btn_export_all_pdf);
        btnClearAll = (TextView) findViewById(R.id.btn_clear_all);
        tvEditingIndicator = (TextView) findViewById(R.id.tv_editing_indicator);
        tvCountHeader = (TextView) findViewById(R.id.tv_count_header);
        questionsListLayout = (LinearLayout) findViewById(R.id.questions_list_layout);

        // Check Permissions
        requestStoragePermissions();

        // Initialize state
        questionList = new ArrayList<>();
        currentDraft = new Question();
        
        // Load existing saved questions
        loadQuestionsFromStorage();
        if (questionList.isEmpty()) {
            addSampleQuestions();
        }

        // Construct Selection Controllers dynamically
        buildThemeSelectors();
        buildColorSelectors();
        buildSizeSelectors();

        // Register Dynamic Watchers
        registerTextChangeListeners();

        // Set Button Listeners
        btnAddUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddUpdateCardClicked();
            }
        });

        btnSaveCurrentImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDraftAsImage();
            }
        });

        btnExportAllPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportAllToSinglePdf();
            }
        });

        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllQuestions();
            }
        });

        // Initial preview rendering
        triggerPreviewRefresh();
        rebuildQuestionsListUI();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                }, 100);
            }
        }
    }

    private void addSampleQuestions() {
        Question q1 = new Question();
        q1.questionText = "Which planet is known as the Red Planet in our Solar System?";
        q1.optionA = "Venus";
        q1.optionB = "Mars";
        q1.optionC = "Jupiter";
        q1.optionD = "Saturn";
        q1.themeIndex = 1; // Sunset
        q1.fontColorIndex = 0;
        q1.fontSizeIndex = 1;
        questionList.add(q1);

        Question q2 = new Question();
        q2.questionText = "What is the chemical symbol for Water?";
        q2.optionA = "O2";
        q2.optionB = "CO2";
        q2.optionC = "H2O";
        q2.optionD = "NaCl";
        q2.themeIndex = 3; // Teal
        q2.fontColorIndex = 1; // Yellow font
        q2.fontSizeIndex = 2; // Large
        questionList.add(q2);

        saveQuestionsToStorage();
    }

    private void buildThemeSelectors() {
        themeContainer.removeAllViews();
        int itemSize = dpToPx(38);
        int margin = dpToPx(6);

        for (int i = 0; i < THEME_START_COLORS.length; i++) {
            final int index = i;
            View view = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemSize, itemSize);
            params.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(params);

            // Create gradient circular drawable dynamically
            Paint circlePaint = new Paint();
            circlePaint.setAntiAlias(true);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    new int[]{THEME_START_COLORS[i], THEME_END_COLORS[i]}
            );
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            
            // Highlight selected theme
            if (currentDraft.themeIndex == i) {
                gd.setStroke(dpToPx(3), 0xFF00E676);
            } else {
                gd.setStroke(dpToPx(1), 0xFFB0BEC5);
            }

            view.setBackground(gd);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentDraft.themeIndex = index;
                    buildThemeSelectors(); // refresh stroke outline
                    triggerPreviewRefresh();
                }
            });

            themeContainer.addView(view);
        }
    }

    private void buildColorSelectors() {
        colorContainer.removeAllViews();
        int itemSize = dpToPx(34);
        int margin = dpToPx(6);

        for (int i = 0; i < FONT_COLORS.length; i++) {
            final int index = i;
            View view = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemSize, itemSize);
            params.setMargins(margin, margin, margin, margin);
            view.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(FONT_COLORS[i]);
            
            if (currentDraft.fontColorIndex == i) {
                gd.setStroke(dpToPx(3), 0xFF00E676);
            } else {
                gd.setStroke(dpToPx(1), 0xFF37474F);
            }

            view.setBackground(gd);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentDraft.fontColorIndex = index;
                    buildColorSelectors();
                    triggerPreviewRefresh();
                }
            });

            colorContainer.addView(view);
        }
    }

    private void buildSizeSelectors() {
        sizeContainer.removeAllViews();
        for (int i = 0; i < FONT_SIZES.length; i++) {
            final int index = i;
            TextView btn = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(4, 4, 4, 4);
            btn.setLayoutParams(params);
            btn.setText(FONT_SIZE_LABELS[i]);
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(8, 12, 8, 12);
            btn.setTextSize(14); // Corrected from 14sp to standard integer
            btn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            if (currentDraft.fontSizeIndex == i) {
                btn.setBackgroundColor(0xFF00838F);
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(0xFFEEEEEE);
                btn.setTextColor(0xFF37474F);
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentDraft.fontSizeIndex = index;
                    buildSizeSelectors();
                    triggerPreviewRefresh();
                }
            });

            sizeContainer.addView(btn);
        }
    }

    private void registerTextChangeListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentDraft.questionText = editQuestion.getText().toString();
                currentDraft.optionA = editOptionA.getText().toString();
                currentDraft.optionB = editOptionB.getText().toString();
                currentDraft.optionC = editOptionC.getText().toString();
                currentDraft.optionD = editOptionD.getText().toString();
                triggerPreviewRefresh();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        editQuestion.addTextChangedListener(watcher);
        editOptionA.addTextChangedListener(watcher);
        editOptionB.addTextChangedListener(watcher);
        editOptionC.addTextChangedListener(watcher);
        editOptionD.addTextChangedListener(watcher);
    }

    private void triggerPreviewRefresh() {
        Bitmap previewBitmap = renderQuestionCard(currentDraft, 640, 360); // 16:9 ratio downscaled for real-time visual UI
        cardPreview.setImageBitmap(previewBitmap);
    }

    // High fidelity rendering logic used for both Preview rendering, PNG saving, and high-quality PDF page rendering
    private Bitmap renderQuestionCard(Question q, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Scale factors to support arbitrary sizes (e.g., 1280x720 output and 640x360 preview)
        float scale = (float) width / 1280.0f;

        // 1. Draw Linear Gradient Background
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Shader shader = new LinearGradient(0, 0, width, height,
                THEME_START_COLORS[q.themeIndex],
                THEME_END_COLORS[q.themeIndex],
                Shader.TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null); // release shader

        // 2. Draw Artistic Background Waves
        Paint wavePaint = new Paint();
        wavePaint.setAntiAlias(true);
        wavePaint.setColor(Color.WHITE);
        wavePaint.setAlpha(15);
        canvas.drawCircle(width + (100 * scale), -100 * scale, 350 * scale, wavePaint);
        canvas.drawCircle(-100 * scale, height + (100 * scale), 250 * scale, wavePaint);

        // 3. Draw Outer Card Border
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAlpha(35);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4 * scale);
        canvas.drawRect(15 * scale, 15 * scale, width - (15 * scale), height - (15 * scale), borderPaint);

        // 4. Draw Header Badge text
        Paint badgePaint = new Paint();
        badgePaint.setAntiAlias(true);
        badgePaint.setColor(FONT_COLORS[q.fontColorIndex]);
        badgePaint.setAlpha(140);
        badgePaint.setTextSize(16 * scale);
        badgePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        canvas.drawText("QUIZ CARD PRO", 40 * scale, 45 * scale, badgePaint);

        // 5. Build and draw Wrapped Question text
        TextPaint qTextPaint = new TextPaint();
        qTextPaint.setAntiAlias(true);
        qTextPaint.setColor(FONT_COLORS[q.fontColorIndex]);
        qTextPaint.setTextSize(FONT_SIZES[q.fontSizeIndex] * scale * 1.4f);
        qTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        int horizontalPadding = (int) (40 * scale);
        int availableWidth = width - (horizontalPadding * 2);

        String questionDisplay = (q.questionText.trim().isEmpty()) ? "Your Question text will appear here..." : q.questionText;
        
        StaticLayout staticLayout = new StaticLayout(
                questionDisplay,
                qTextPaint,
                availableWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.15f,
                0.0f,
                false
        );

        canvas.save();
        int questionStartY = (int) (75 * scale);
        canvas.translate(horizontalPadding, questionStartY);
        staticLayout.draw(canvas);
        canvas.restore();

        // 6. Options rendering logic
        int qHeight = staticLayout.getHeight();
        int optionsStartY = questionStartY + qHeight + (int) (25 * scale);

        // Avoid layout crashes or overlap by ensuring a minimum dynamic start point
        int minimumExpectedHeight = (int) (220 * scale);
        if (optionsStartY < minimumExpectedHeight) {
            optionsStartY = minimumExpectedHeight;
        }

        int rowHeight = (int) (52 * scale);
        int rowGap = (int) (12 * scale);

        Paint pillPaint = new Paint();
        pillPaint.setAntiAlias(true);
        // Determine pill transparency
        if (FONT_COLORS[q.fontColorIndex] == 0xFF212121) {
            pillPaint.setColor(Color.BLACK);
            pillPaint.setAlpha(12);
        } else {
            pillPaint.setColor(Color.WHITE);
            pillPaint.setAlpha(40);
        }
        pillPaint.setStyle(Paint.Style.FILL);

        TextPaint optionTextPaint = new TextPaint();
        optionTextPaint.setAntiAlias(true);
        optionTextPaint.setColor(FONT_COLORS[q.fontColorIndex]);
        optionTextPaint.setTextSize(FONT_SIZES[q.fontSizeIndex] * scale * 1.1f);
        optionTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        String[] prefixes = {"A. ", "B. ", "C. ", "D. "};
        String[] contents = {q.optionA, q.optionB, q.optionC, q.optionD};
        String[] defaults = {"Option A text", "Option B text", "Option C text", "Option D text"};

        for (int i = 0; i < 4; i++) {
            int top = optionsStartY + i * (rowHeight + rowGap);
            if (top + rowHeight > height - (15 * scale)) {
                break; // safeguard to avoid rendering outside the image boundaries
            }

            RectF pillRect = new RectF(horizontalPadding, top, width - horizontalPadding, top + rowHeight);
            canvas.drawRoundRect(pillRect, 8 * scale, 8 * scale, pillPaint);

            String textValue = contents[i].trim().isEmpty() ? defaults[i] : contents[i];
            String textToDraw = prefixes[i] + textValue;

            // Simple text truncation logic for extra-long text strings
            if (textToDraw.length() > 65) {
                textToDraw = textToDraw.substring(0, 62) + "...";
            }

            canvas.drawText(
                    textToDraw,
                    horizontalPadding + (20 * scale),
                    top + (rowHeight / 2.0f) + (optionTextPaint.getTextSize() / 3.0f),
                    optionTextPaint
            );
        }

        return bitmap;
    }

    private void onAddUpdateCardClicked() {
        String qText = editQuestion.getText().toString().trim();
        if (qText.isEmpty()) {
            Toast.makeText(this, "Question text cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingQuestionId == null) {
            // Add Mode
            Question newCard = new Question();
            newCard.id = UUID.randomUUID().toString();
            copyFields(currentDraft, newCard);
            questionList.add(newCard);
            Toast.makeText(this, "Quiz card added to bundle!", Toast.LENGTH_SHORT).show();
        } else {
            // Edit Mode
            for (Question q : questionList) {
                if (q.id.equals(editingQuestionId)) {
                    copyFields(currentDraft, q);
                    break;
                }
            }
            editingQuestionId = null;
            tvEditingIndicator.setVisibility(View.GONE);
            btnAddUpdate.setText("Add Card");
            btnAddUpdate.setBackgroundColor(0xFF00838F);
            Toast.makeText(this, "Quiz card updated successfully!", Toast.LENGTH_SHORT).show();
        }

        // Reset inputs and load fresh default
        resetDraftInputForm();
        saveQuestionsToStorage();
        triggerPreviewRefresh();
        rebuildQuestionsListUI();
    }

    private void copyFields(Question src, Question dest) {
        dest.questionText = src.questionText;
        dest.optionA = src.optionA;
        dest.optionB = src.optionB;
        dest.optionC = src.optionC;
        dest.optionD = src.optionD;
        dest.themeIndex = src.themeIndex;
        dest.fontColorIndex = src.fontColorIndex;
        dest.fontSizeIndex = src.fontSizeIndex;
    }

    private void resetDraftInputForm() {
        editQuestion.setText("");
        editOptionA.setText("");
        editOptionB.setText("");
        editOptionC.setText("");
        editOptionD.setText("");
        
        currentDraft = new Question(); // Clear draft data
        buildThemeSelectors();
        buildColorSelectors();
        buildSizeSelectors();
    }

    private void loadDraftFromQuestion(Question q) {
        editQuestion.setText(q.questionText);
        editOptionA.setText(q.optionA);
        editOptionB.setText(q.optionB);
        editOptionC.setText(q.optionC);
        editOptionD.setText(q.optionD);

        currentDraft.questionText = q.questionText;
        currentDraft.optionA = q.optionA;
        currentDraft.optionB = q.optionB;
        currentDraft.optionC = q.optionC;
        currentDraft.optionD = q.optionD;
        currentDraft.themeIndex = q.themeIndex;
        currentDraft.fontColorIndex = q.fontColorIndex;
        currentDraft.fontSizeIndex = q.fontSizeIndex;

        buildThemeSelectors();
        buildColorSelectors();
        buildSizeSelectors();
        triggerPreviewRefresh();
    }

    private void saveDraftAsImage() {
        Bitmap highResBitmap = renderQuestionCard(currentDraft, 1280, 720); // 16:9 720p Image
        saveBitmapToGallery(highResBitmap, "QuizCard_" + System.currentTimeMillis());
    }

    private void saveBitmapToGallery(Bitmap bitmap, String title) {
        OutputStream fos = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QuizCardCreator");
            }

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri != null) {
                fos = getContentResolver().openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(this, "Saved Image to Pictures/QuizCardCreator", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to reserve media directory", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (Exception ignored) {}
        }
    }

    private void exportAllToSinglePdf() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "No quiz cards inside your list! Please add questions first.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        int pageWidth = 1280;
        int pageHeight = 720;

        try {
            for (int i = 0; i < questionList.size(); i++) {
                Question q = questionList.get(i);
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                
                Canvas canvas = page.getCanvas();
                
                // Draw card render logic directly onto high quality PDF Page Canvas
                Bitmap cardRender = renderQuestionCard(q, pageWidth, pageHeight);
                canvas.drawBitmap(cardRender, 0, 0, null);
                
                pdfDocument.finishPage(page);
            }

            String pdfName = "MergedQuizBundle_" + System.currentTimeMillis() + ".pdf";
            OutputStream fos = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, pdfName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/QuizCardCreator");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    fos = getContentResolver().openOutputStream(uri);
                }
            } else {
                File publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File subDir = new File(publicDir, "QuizCardCreator");
                if (!subDir.exists()) {
                    subDir.mkdirs();
                }
                File file = new File(subDir, pdfName);
                fos = new FileOutputStream(file);
            }

            if (fos != null) {
                pdfDocument.writeTo(fos);
                Toast.makeText(this, "PDF merged successfully into Downloads/QuizCardCreator!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Unable to create PDF output Stream", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF Compile Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    private void clearAllQuestions() {
        questionList.clear();
        saveQuestionsToStorage();
        rebuildQuestionsListUI();
        Toast.makeText(this, "Cleared all quiz cards!", Toast.LENGTH_SHORT).show();
    }

    private void rebuildQuestionsListUI() {
        questionsListLayout.removeAllViews();
        tvCountHeader.setText("SAVED CARDS (" + questionList.size() + ")");

        if (questionList.isEmpty()) {
            TextView placeholder = new TextView(this);
            placeholder.setText("No questions added yet. Construct using builder form above.");
            placeholder.setTextColor(0xFF90A4AE);
            placeholder.setTextSize(14); // Corrected from 14sp to standard integer
            placeholder.setGravity(Gravity.CENTER);
            placeholder.setPadding(0, 30, 0, 30);
            questionsListLayout.addView(placeholder);
            return;
        }

        for (int i = 0; i < questionList.size(); i++) {
            final Question q = questionList.get(i);
            final int listIndex = i;

            LinearLayout itemRoot = new LinearLayout(this);
            itemRoot.setOrientation(LinearLayout.VERTICAL);
            itemRoot.setBackgroundColor(Color.WHITE);
            itemRoot.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
            
            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rootParams.setMargins(0, 0, 0, dpToPx(10));
            itemRoot.setLayoutParams(rootParams);
            itemRoot.setElevation(dpToPx(2));

            // Horizontal layout for card details & quick representation
            LinearLayout metaRow = new LinearLayout(this);
            metaRow.setOrientation(LinearLayout.HORIZONTAL);
            metaRow.setGravity(Gravity.CENTER_VERTICAL);

            // Miniature Color Icon representing theme index chosen
            View thumbTheme = new View(this);
            LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dpToPx(18), dpToPx(18));
            thumbParams.setMargins(0, 0, dpToPx(8), 0);
            thumbTheme.setLayoutParams(thumbParams);
            android.graphics.drawable.GradientDrawable thumbGd = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    new int[]{THEME_START_COLORS[q.themeIndex], THEME_END_COLORS[q.themeIndex]}
            );
            thumbGd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            thumbTheme.setBackground(thumbGd);
            metaRow.addView(thumbTheme);

            TextView titleText = new TextView(this);
            titleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            titleText.setText("Card #" + (i + 1) + " - " + THEME_NAMES[q.themeIndex] + " (Size: " + FONT_SIZE_LABELS[q.fontSizeIndex] + ")");
            titleText.setTextColor(0xFF37474F);
            titleText.setTextSize(13); // Corrected from 13sp to standard integer
            titleText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            metaRow.addView(titleText);

            itemRoot.addView(metaRow);

            // Question summary string snippet
            TextView qPreviewText = new TextView(this);
            qPreviewText.setText(q.questionText);
            qPreviewText.setTextColor(0xFF546E7A);
            qPreviewText.setTextSize(14); // Corrected from 14sp to standard integer
            qPreviewText.setSingleLine(true);
            qPreviewText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            qPreviewText.setPadding(0, dpToPx(6), 0, dpToPx(10));
            itemRoot.addView(qPreviewText);

            // Bottom controls for this specific card
            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setGravity(Gravity.RIGHT);

            // Save individual item image button
            Button itemImageBtn = new Button(this);
            itemImageBtn.setText("Save Image");
            itemImageBtn.setTextSize(11); // Corrected from 11sp to standard integer
            itemImageBtn.setBackgroundColor(0xFF90A4AE);
            itemImageBtn.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams btnImgParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
            btnImgParams.setMargins(0, 0, dpToPx(8), 0);
            itemImageBtn.setLayoutParams(btnImgParams);
            itemImageBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap itemBmp = renderQuestionCard(q, 1280, 720);
                    saveBitmapToGallery(itemBmp, "QuizCard_" + (listIndex + 1) + "_" + System.currentTimeMillis());
                }
            });
            actionRow.addView(itemImageBtn);

            // Edit button
            Button itemEditBtn = new Button(this);
            itemEditBtn.setText("Edit");
            itemEditBtn.setTextSize(11); // Corrected from 11sp to standard integer
            itemEditBtn.setBackgroundColor(0xFF0288D1);
            itemEditBtn.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams btnEditParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
            btnEditParams.setMargins(0, 0, dpToPx(8), 0);
            itemEditBtn.setLayoutParams(btnEditParams);
            itemEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editingQuestionId = q.id;
                    tvEditingIndicator.setVisibility(View.VISIBLE);
                    btnAddUpdate.setText("Update Card");
                    btnAddUpdate.setBackgroundColor(0xFFE65100);
                    loadDraftFromQuestion(q);
                }
            });
            actionRow.addView(itemEditBtn);

            // Delete button
            Button itemDeleteBtn = new Button(this);
            itemDeleteBtn.setText("Delete");
            itemDeleteBtn.setTextSize(11); // Corrected from 11sp to standard integer
            itemDeleteBtn.setBackgroundColor(0xFFD32F2F);
            itemDeleteBtn.setTextColor(Color.WHITE);
            itemDeleteBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34)));
            itemDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (q.id.equals(editingQuestionId)) {
                        editingQuestionId = null;
                        tvEditingIndicator.setVisibility(View.GONE);
                        btnAddUpdate.setText("Add Card");
                        btnAddUpdate.setBackgroundColor(0xFF00838F);
                        resetDraftInputForm();
                    }
                    questionList.remove(listIndex);
                    saveQuestionsToStorage();
                    rebuildQuestionsListUI();
                    triggerPreviewRefresh();
                }
            });
            actionRow.addView(itemDeleteBtn);

            itemRoot.addView(actionRow);
            questionsListLayout.addView(itemRoot);
        }
    }

    // Persist list helper
    private void saveQuestionsToStorage() {
        try {
            SharedPreferences prefs = getSharedPreferences("quiz_cards_prefs", Context.MODE_PRIVATE);
            JSONArray array = new JSONArray();
            for (Question q : questionList) {
                JSONObject obj = new JSONObject();
                obj.put("id", q.id);
                obj.put("question", q.questionText);
                obj.put("optionA", q.optionA);
                obj.put("optionB", q.optionB);
                obj.put("optionC", q.optionC);
                obj.put("optionD", q.optionD);
                obj.put("themeIndex", q.themeIndex);
                obj.put("fontColorIndex", q.fontColorIndex);
                obj.put("fontSizeIndex", q.fontSizeIndex);
                array.put(obj);
            }
            prefs.edit().putString("saved_questions", array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadQuestionsFromStorage() {
        try {
            SharedPreferences prefs = getSharedPreferences("quiz_cards_prefs", Context.MODE_PRIVATE);
            String rawJson = prefs.getString("saved_questions", "[]");
            JSONArray array = new JSONArray(rawJson);
            questionList.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Question q = new Question();
                q.id = obj.optString("id", UUID.randomUUID().toString());
                q.questionText = obj.optString("question", "");
                q.optionA = obj.optString("optionA", "");
                q.optionB = obj.optString("optionB", "");
                q.optionC = obj.optString("optionC", "");
                q.optionD = obj.optString("optionD", "");
                q.themeIndex = obj.optInt("themeIndex", 0);
                q.fontColorIndex = obj.optInt("fontColorIndex", 0);
                q.fontSizeIndex = obj.optInt("fontSizeIndex", 1);
                questionList.add(q);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Utility Unit Converters
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}