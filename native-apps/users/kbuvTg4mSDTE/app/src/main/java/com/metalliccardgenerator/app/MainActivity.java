package com.metalliccardgenerator.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

    private MetallicCardView metallicCardView;
    private EditText etShopName;
    private EditText etPhoneNumber;

    private Button btnGold;
    private Button btnSilver;
    private Button btnObsidian;
    private Button btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts and components
        metallicCardView = (MetallicCardView) findViewById(R.id.custom_metallic_card_view);
        etShopName = (EditText) findViewById(R.id.et_shop_name);
        etPhoneNumber = (EditText) findViewById(R.id.et_phone_number);

        btnGold = (Button) findViewById(R.id.btn_theme_gold);
        btnSilver = (Button) findViewById(R.id.btn_theme_silver);
        btnObsidian = (Button) findViewById(R.id.btn_theme_obsidian);
        btnReset = (Button) findViewById(R.id.btn_reset);

        // Bind event listeners for card texture triggers
        btnGold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                metallicCardView.setMetalTheme(MetallicCardView.THEME_GOLD);
            }
        });

        btnSilver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                metallicCardView.setMetalTheme(MetallicCardView.THEME_SILVER);
            }
        });

        btnObsidian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                metallicCardView.setMetalTheme(MetallicCardView.THEME_OBSIDIAN);
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etShopName.setText("Elite Horology Inc.");
                etPhoneNumber.setText("+1 (800) 555-0199");
                metallicCardView.resetAngles();
            }
        });

        // Add Text Watchers to update custom view automatically
        etShopName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                metallicCardView.setShopName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                metallicCardView.setPhoneNumber(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set initial default demo data values
        etShopName.setText("Elite Horology Inc.");
        etPhoneNumber.setText("+1 (800) 555-0199");
    }
}

/**
 * Custom High Performance Android View designed to render an interactive
 * 3D Visiting Card featuring dynamic metallic linear specular highlights
 * calculated depending on touch tilt angles. Uses pure Standard SDK 2D and 3D
 * Canvas primitives.
 */
class MetallicCardView extends View {

    public static final int THEME_GOLD = 0;
    public static final int THEME_SILVER = 1;
    public static final int THEME_OBSIDIAN = 2;

    private int currentTheme = THEME_GOLD;

    private String shopName = "Elite Horology Inc.";
    private String phoneNumber = "+1 (800) 555-0199";

    // 3D rotation values mapped to touch interactions
    private float rotX = -12f; // Slight tilt by default to highlight specular 3D depth
    private float rotY = 15f;

    // Last touch positions to compute dynamic rotation delta offsets
    private float lastTouchX;
    private float lastTouchY;

    // View component rendering objects instantiated at configuration steps to maintain Java 8 compliance
    private Camera camera;
    private Matrix matrix3D;
    private RectF cardBounds;
    private Paint cardBgPaint;
    private Paint borderPaint;
    private Paint chipPaint;
    private Paint dividerPaint;
    private Paint textTitlePaint;
    private Paint textTitleShadowPaint;
    private Paint textSubPaint;
    private Paint decorativeLinesPaint;

    // Physics Engine components for Idle Animation effects
    private boolean isUserTouching = false;
    private float idleTimeCount = 0f;
    private Runnable idleAnimationRunnable;

    public MetallicCardView(Context context) {
        super(context);
        init();
    }

    public MetallicCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MetallicCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        camera = new Camera();
        matrix3D = new Matrix();
        cardBounds = new RectF();

        // Card outer shell style setup
        cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBgPaint.setStyle(Paint.Style.FILL);

        // Card Gold/Silver Border styling setup
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3.5f);

        // Embedded Luxury Chip design
        chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chipPaint.setStyle(Paint.Style.FILL);

        // Structural Divider rules
        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setStrokeWidth(2.0f);

        // Bold embossed main shop layout text elements
        textTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTitlePaint.setTextSize(21f);
        textTitlePaint.setTextAlign(Paint.Align.CENTER);
        textTitlePaint.setFakeBoldText(true);

        textTitleShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textTitleShadowPaint.setTextSize(21f);
        textTitleShadowPaint.setTextAlign(Paint.Align.CENTER);
        textTitleShadowPaint.setFakeBoldText(true);

        // Subtitle contact info structure
        textSubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textSubPaint.setTextSize(13.5f);
        textSubPaint.setTextAlign(Paint.Align.CENTER);

        // Detailed premium design line accents
        decorativeLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        decorativeLinesPaint.setStyle(Paint.Style.STROKE);
        decorativeLinesPaint.setStrokeWidth(1.0f);

        // Auto sway runner setup to draw shiny metal movements when static
        idleAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUserTouching) {
                    idleTimeCount += 0.05f;
                    // Compute slight smooth sinus sway shifts on X and Y axis
                    rotY = (float) Math.sin(idleTimeCount) * 12f;
                    rotX = (float) Math.cos(idleTimeCount * 0.7f) * 8f - 5f;
                    invalidate();
                }
                // Cycle animation ticks
                postDelayed(this, 30);
            }
        };
        postDelayed(idleAnimationRunnable, 1000);
    }

    public void setShopName(String name) {
        this.shopName = (name != null && !name.trim().isEmpty()) ? name : "Elite Horology Inc.";
        invalidate();
    }

    public void setPhoneNumber(String phone) {
        this.phoneNumber = (phone != null && !phone.trim().isEmpty()) ? phone : "+1 (800) 555-0199";
        invalidate();
    }

    public void setMetalTheme(int theme) {
        this.currentTheme = theme;
        invalidate();
    }

    public void resetAngles() {
        this.rotX = -5f;
        this.rotY = 10f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isUserTouching = true;
                lastTouchX = x;
                lastTouchY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                // Scale values to convert dragging to rot angles
                rotY += dx * 0.35f;
                rotX -= dy * 0.35f;

                // Clamp angles to safeguard readable visual depth boundaries
                if (rotY > 50f) rotY = 50f;
                if (rotY < -50f) rotY = -50f;
                if (rotX > 45f) rotX = 45f;
                if (rotX < -45f) rotX = -45f;

                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isUserTouching = false;
                // Idle dynamic engine picks up transition later
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // Calculate card layout rectangle proportions in real-time
        float cardW = viewWidth * 0.88f;
        float cardH = cardW * 0.58f; // Golden ratio layout dimensions
        float left = (viewWidth - cardW) / 2f;
        float top = (viewHeight - cardH) / 2f;
        float right = left + cardW;
        float bottom = top + cardH;

        cardBounds.set(left, top, right, bottom);

        // Clear view space background safely using parent viewport styling rules
        canvas.save();

        // Prepare 3D Perspective Transformations
        camera.save();
        camera.setLocation(0f, 0f, -14f); // Scale target Z distance to guarantee perspective depth and limit clipping
        camera.rotateX(rotX);
        camera.rotateY(rotY);
        camera.getMatrix(matrix3D);
        camera.restore();

        // Target transformation coordinates around absolute view midpoint
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;
        matrix3D.preTranslate(-centerX, -centerY);
        matrix3D.postTranslate(centerX, centerY);
        canvas.concat(matrix3D);

        // Apply Premium Metal Color Shaders dependent on Theme choices
        int[] metallicGradients;
        int strokeHighlightColor;
        int chipCoreColor;
        int chipWiringColor;
        int textPrimaryColor;
        int textShadowColor;
        int decorLineColor;

        if (currentTheme == THEME_GOLD) {
            // Gold Linear speculates shifting reflections
            metallicGradients = new int[]{
                    0xFF8A6F27, // Matte Gold
                    0xFFD4AF37, // Bright Gold
                    0xFFF9E8A2, // Metallic highlight glow shine
                    0xFFC5A059, // Classic Gold
                    0xFF967B3B, // Deep Shadow Gold
                    0xFFF3E5AB, // High sheen reflection
                    0xFF7A6121  // Dark bronze base
            };
            strokeHighlightColor = 0xFFFCEEBC;
            chipCoreColor = 0xFFE5C060;
            chipWiringColor = 0xFF7A5F15;
            textPrimaryColor = 0xFF1C1300; // Intensely dark contrast
            textShadowColor = 0xFFF9E79F;  // Engraved gold emboss shadow
            decorLineColor = 0x667A5F15;
        } else if (currentTheme == THEME_SILVER) {
            // Titanium-Platinum silver alloy reflection
            metallicGradients = new int[]{
                    0xFF4A4E53, // Gunmetal Core
                    0xFF9E9E9E, // Solid Aluminum
                    0xFFEEEEEE, // Bright Specular Polish
                    0xFFCCCCCC, // Middle Platinum
                    0xFF7D8287, // Steel Core
                    0xFFF5F5F5, // Highlight reflection
                    0xFF3E4145  // Deep Shadow Steel
            };
            strokeHighlightColor = 0xFFFFFFFF;
            chipCoreColor = 0xFFD2D2D2;
            chipWiringColor = 0xFF424242;
            textPrimaryColor = 0xFF111315; // Dark titanium contrast
            textShadowColor = 0xFFFFFFFF;  // Polished clean steel highlight shadow
            decorLineColor = 0x664A4A4A;
        } else {
            // Obsidian & Carbon reflective finishes
            metallicGradients = new int[]{
                    0xFF0A0D12, // Dark space obsidian
                    0xFF22252A, // Polished coal
                    0xFF4F535B, // Graphite reflection
                    0xFF181B1F, // Raw basalt
                    0xFF0A0C0E, // Pitch black
                    0xFF3A3D42, // Steel specular highlight reflection
                    0xFF020203  // Edge space shadow
            };
            strokeHighlightColor = 0xFF717A84;
            chipCoreColor = 0xFFE0E0E0; // Platinum chip on black
            chipWiringColor = 0xFF3E4145;
            textPrimaryColor = 0xFFF1F1F5; // Pure silver foil lettering
            textShadowColor = 0xFF000000;  // Deep cast shadow
            decorLineColor = 0x448A909A;
        }

        // Calculate custom gradient displacement parameters relative to tilt angles to yield dynamic reflections
        float specShiftX = (rotY / 50f) * cardW;
        float specShiftY = (rotX / 45f) * cardH;

        LinearGradient dynamicMetallicShader = new LinearGradient(
                left - specShiftX, top - specShiftY,
                right + specShiftX, bottom + specShiftY,
                metallicGradients,
                null,
                Shader.TileMode.CLAMP
        );
        cardBgPaint.setShader(dynamicMetallicShader);

        // 1. Draw Card Base Shape (Rounded Rect representing actual 3.375 x 2.125 golden credit card ratio)
        float cardCornerRadius = cardH * 0.07f;
        canvas.drawRoundRect(cardBounds, cardCornerRadius, cardCornerRadius, cardBgPaint);

        // 2. Draw Premium Highlight Inner Borders
        borderPaint.setColor(strokeHighlightColor);
        RectF innerBorderBounds = new RectF(left + 8f, top + 8f, right - 8f, bottom - 8f);
        canvas.drawRoundRect(innerBorderBounds, cardCornerRadius - 3f, cardCornerRadius - 3f, borderPaint);

        // 3. Draw Elegantly Formulated Microchip Detail
        float chipW = cardW * 0.13f;
        float chipH = chipW * 0.75f;
        float chipX = left + cardW * 0.1f;
        float chipY = top + cardH * 0.28f;
        RectF chipBounds = new RectF(chipX, chipY, chipX + chipW, chipY + chipH);
        chipPaint.setColor(chipCoreColor);
        canvas.drawRoundRect(chipBounds, 6f, 6f, chipPaint);

        // Custom microchip wiring trace outlines inside chip block to enrich visual complexity
        decorativeLinesPaint.setColor(chipWiringColor);
        decorativeLinesPaint.setStrokeWidth(1.2f);
        // Chip dividers layout
        canvas.drawLine(chipBounds.left + chipW*0.33f, chipBounds.top, chipBounds.left + chipW*0.33f, chipBounds.bottom, decorativeLinesPaint);
        canvas.drawLine(chipBounds.left + chipW*0.66f, chipBounds.top, chipBounds.left + chipW*0.66f, chipBounds.bottom, decorativeLinesPaint);
        canvas.drawLine(chipBounds.left, chipBounds.top + chipH*0.5f, chipBounds.right, chipBounds.top + chipH*0.5f, decorativeLinesPaint);

        // 4. Draw Premium Geometric Watermark Outlines (Abstract Diamond and Crown structures representing premium grade)
        decorativeLinesPaint.setColor(decorLineColor);
        decorativeLinesPaint.setStrokeWidth(1.0f);
        Path decorPath = new Path();
        float designMidX = right - cardW * 0.22f;
        float designMidY = top + cardH * 0.45f;
        float designRadius = cardH * 0.25f;

        // Draw Nested Geometric Polygons for luxury brand feel
        decorPath.moveTo(designMidX, designMidY - designRadius);
        decorPath.lineTo(designMidX + designRadius * 0.8f, designMidY);
        decorPath.lineTo(designMidX, designMidY + designRadius);
        decorPath.lineTo(designMidX - designRadius * 0.8f, designMidY);
        decorPath.close();
        canvas.drawPath(decorPath, decorativeLinesPaint);

        decorPath.reset();
        decorPath.moveTo(designMidX, designMidY - designRadius * 0.6f);
        decorPath.lineTo(designMidX + designRadius * 0.5f, designMidY);
        decorPath.lineTo(designMidX, designMidY + designRadius * 0.6f);
        decorPath.lineTo(designMidX - designRadius * 0.5f, designMidY);
        decorPath.close();
        canvas.drawPath(decorPath, decorativeLinesPaint);

        // Draw structural line alignments on backgrounds
        canvas.drawLine(left + 20f, top + cardH * 0.68f, right - 20f, top + cardH * 0.68f, decorativeLinesPaint);

        // 5. Draw Dynamic Cardholder / Shop Texts with Embossed 3D Shadows
        // Generate shadow offset depending on tilts
        float shadowOffsetX = (rotY / 50f) * 2.5f;
        float shadowOffsetY = -(rotX / 45f) * 2.5f;

        // Render Title (Business/Shop Name)
        textTitleShadowPaint.setColor(textShadowColor);
        textTitlePaint.setColor(textPrimaryColor);

        float textX = centerX;
        float textY = top + cardH * 0.45f;

        // Draw shadow first (engraved depth offset)
        canvas.drawText(shopName, textX + shadowOffsetX, textY + shadowOffsetY, textTitleShadowPaint);
        // Draw primary face
        canvas.drawText(shopName, textX, textY, textTitlePaint);

        // Render "PREMIUM MERCHANT" Subtitle Badge Text
        textSubPaint.setColor(textPrimaryColor);
        textSubPaint.setTextSize(10f);
        textSubPaint.setFakeBoldText(true);
        textSubPaint.setLetterSpacing(0.25f);
        canvas.drawText("PREMIUM METALLIC CLUB MEMBER", textX, top + cardH * 0.57f, textSubPaint);

        // Render Phone/Contact Number in lower section
        textSubPaint.setTextSize(13.5f);
        textSubPaint.setFakeBoldText(false);
        textSubPaint.setLetterSpacing(0.08f);

        float phoneY = top + cardH * 0.83f;
        // Phone shadow
        textTitleShadowPaint.setTextSize(13.5f);
        textTitleShadowPaint.setFakeBoldText(false);
        textTitleShadowPaint.setLetterSpacing(0.08f);
        canvas.drawText(phoneNumber, textX + shadowOffsetX, phoneY + shadowOffsetY, textTitleShadowPaint);
        // Phone text
        canvas.drawText(phoneNumber, textX, phoneY, textSubPaint);

        // Reset tracking states to retain standard bounds
        canvas.restore();
    }
}