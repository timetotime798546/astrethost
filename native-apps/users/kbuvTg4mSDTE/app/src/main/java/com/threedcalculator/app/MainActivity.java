package com.threedcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {

    private ThreeDView threeDView;
    private Button btnZoomIn, btnZoomOut, btnResetView;
    private CheckBox cbAutoRotate;
    private TextView txtRenderStatus;

    // Tabs
    private Button tabShapes, tabFunctions;
    private LinearLayout panelShapes, panelFunctions;

    // Shape elements
    private Spinner spinnerShapes;
    private LinearLayout layoutInput1, layoutInput2;
    private TextView lblInput1, lblInput2;
    private EditText editInput1, editInput2;
    private Button btnCalculateShape;

    // Function elements
    private Spinner spinnerFunctions;
    private EditText editGridRes, editAmplitude;
    private Button btnPlotFunction;

    // Calculation display elements
    private TextView txtMainResults;
    private TextView txtFormulasInfo;
    private TextView txtStepDetails;

    private boolean isShapeTab = true;

    private static final String[] SHAPES_LIST = {
        "Cube", "Sphere", "Cylinder", "Cone", "Square Pyramid"
    };

    private static final String[] FUNCTIONS_LIST = {
        "Ripple: sin(√(x²+z²))",
        "Saddle: x² - z²",
        "Waves: cos(4x) * sin(4z)",
        "Paraboloid: x² + z²"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind interactive canvas
        threeDView = (ThreeDView) findViewById(R.id.threeDView);
        btnZoomIn = (Button) findViewById(R.id.btnZoomIn);
        btnZoomOut = (Button) findViewById(R.id.btnZoomOut);
        btnResetView = (Button) findViewById(R.id.btnResetView);
        cbAutoRotate = (CheckBox) findViewById(R.id.cbAutoRotate);
        txtRenderStatus = (TextView) findViewById(R.id.txtRenderStatus);

        // Bind Panels
        tabShapes = (Button) findViewById(R.id.tabShapes);
        tabFunctions = (Button) findViewById(R.id.tabFunctions);
        panelShapes = (LinearLayout) findViewById(R.id.panelShapes);
        panelFunctions = (LinearLayout) findViewById(R.id.panelFunctions);

        // Bind Shape tools
        spinnerShapes = (Spinner) findViewById(R.id.spinnerShapes);
        layoutInput1 = (LinearLayout) findViewById(R.id.layoutInput1);
        layoutInput2 = (LinearLayout) findViewById(R.id.layoutInput2);
        lblInput1 = (TextView) findViewById(R.id.lblInput1);
        lblInput2 = (TextView) findViewById(R.id.lblInput2);
        editInput1 = (EditText) findViewById(R.id.editInput1);
        editInput2 = (EditText) findViewById(R.id.editInput2);
        btnCalculateShape = (Button) findViewById(R.id.btnCalculateShape);

        // Bind Function tools
        spinnerFunctions = (Spinner) findViewById(R.id.spinnerFunctions);
        editGridRes = (EditText) findViewById(R.id.editGridRes);
        editAmplitude = (EditText) findViewById(R.id.editAmplitude);
        btnPlotFunction = (Button) findViewById(R.id.btnPlotFunction);

        // Bind Calculation display
        txtMainResults = (TextView) findViewById(R.id.txtMainResults);
        txtFormulasInfo = (TextView) findViewById(R.id.txtFormulasInfo);
        txtStepDetails = (TextView) findViewById(R.id.txtStepDetails);

        // Setup Spinners
        ArrayAdapter<String> shapesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SHAPES_LIST);
        shapesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShapes.setAdapter(shapesAdapter);

        ArrayAdapter<String> functionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, FUNCTIONS_LIST);
        functionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFunctions.setAdapter(functionsAdapter);

        // Setup Tab Switchers
        tabShapes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        tabFunctions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
            }
        });

        // Shape type selection behavior
        spinnerShapes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                configureInputFieldsForShape(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Zoom Click Handlers
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDView.zoomIn();
            }
        });

        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDView.zoomOut();
            }
        });

        btnResetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeDView.resetView();
            }
        });

        // Auto rotation behavior
        cbAutoRotate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                threeDView.setAutoRotate(isChecked);
            }
        });

        // Calculate Shape Actions
        btnCalculateShape.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performShapeCalculation();
            }
        });

        // Plot Function Actions
        btnPlotFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFunctionPlotting();
            }
        });

        // Launch with default state
        switchTab(true);
        performShapeCalculation();
    }

    private void switchTab(boolean shapesSelected) {
        isShapeTab = shapesSelected;
        if (shapesSelected) {
            tabShapes.setBackgroundColor(0xFF6200EE);
            tabShapes.setTextColor(0xFFFFFFFF);
            tabFunctions.setBackgroundColor(0xFFE0E0E0);
            tabFunctions.setTextColor(0xFF1E1E24);

            panelShapes.setVisibility(View.VISIBLE);
            panelFunctions.setVisibility(View.GONE);
            txtRenderStatus.setText("Mode: Solid Shapes");
            performShapeCalculation();
        } else {
            tabShapes.setBackgroundColor(0xFFE0E0E0);
            tabShapes.setTextColor(0xFF1E1E24);
            tabFunctions.setBackgroundColor(0xFF6200EE);
            tabFunctions.setTextColor(0xFFFFFFFF);

            panelShapes.setVisibility(View.GONE);
            panelFunctions.setVisibility(View.VISIBLE);
            txtRenderStatus.setText("Mode: Mathematical Plot");
            performFunctionPlotting();
        }
    }

    private void configureInputFieldsForShape(int position) {
        // 0: Cube, 1: Sphere, 2: Cylinder, 3: Cone, 4: Square Pyramid
        switch (position) {
            case 0: // Cube
                layoutInput1.setVisibility(View.VISIBLE);
                layoutInput2.setVisibility(View.GONE);
                lblInput1.setText("Side Length s (m)");
                editInput1.setText("5.0");
                break;
            case 1: // Sphere
                layoutInput1.setVisibility(View.VISIBLE);
                layoutInput2.setVisibility(View.GONE);
                lblInput1.setText("Radius r (m)");
                editInput1.setText("4.0");
                break;
            case 2: // Cylinder
                layoutInput1.setVisibility(View.VISIBLE);
                layoutInput2.setVisibility(View.VISIBLE);
                lblInput1.setText("Radius r (m)");
                lblInput2.setText("Height h (m)");
                editInput1.setText("3.0");
                editInput2.setText("8.0");
                break;
            case 3: // Cone
                layoutInput1.setVisibility(View.VISIBLE);
                layoutInput2.setVisibility(View.VISIBLE);
                lblInput1.setText("Radius r (m)");
                lblInput2.setText("Height h (m)");
                editInput1.setText("3.0");
                editInput2.setText("7.0");
                break;
            case 4: // Square Pyramid
                layoutInput1.setVisibility(View.VISIBLE);
                layoutInput2.setVisibility(View.VISIBLE);
                lblInput1.setText("Base Width w (m)");
                lblInput2.setText("Height h (m)");
                editInput1.setText("4.5");
                editInput2.setText("6.0");
                break;
        }
    }

    private void performShapeCalculation() {
        int shapeIndex = spinnerShapes.getSelectedItemPosition();

        float param1 = 1.0f;
        float param2 = 1.0f;

        try {
            String val1Str = editInput1.getText().toString().trim();
            if (!val1Str.isEmpty()) {
                param1 = Float.parseFloat(val1Str);
            }
            if (layoutInput2.getVisibility() == View.VISIBLE) {
                String val2Str = editInput2.getText().toString().trim();
                if (!val2Str.isEmpty()) {
                    param2 = Float.parseFloat(val2Str);
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numeric parameters.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (param1 <= 0 || param2 <= 0) {
            Toast.makeText(this, "Dimensions must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        double volume = 0;
        double surfaceArea = 0;
        String title = SHAPES_LIST[shapeIndex];

        // Perform ALL deterministic math calculations locally
        if (shapeIndex == 0) { // Cube
            volume = Math.pow(param1, 3);
            surfaceArea = 6 * Math.pow(param1, 2);

            threeDView.showCube(); // Draws standardized cube

            txtMainResults.setText(String.format(Locale.US,
                    "CUBE (Side = %.2fm)\nVolume = %.3f m³\nSurface Area = %.3f m²",
                    param1, volume, surfaceArea));

            txtFormulasInfo.setText("Volume V = s³ \nSurface Area A = 6s²");

            txtStepDetails.setText(String.format(Locale.US,
                    "Step Logic:\n1. V = %.2f³ = %.3f\n2. A = 6 * %.2f² = 6 * %.2f = %.3f",
                    param1, volume, param1, param1 * param1, surfaceArea));

        } else if (shapeIndex == 1) { // Sphere
            volume = (4.0 / 3.0) * Math.PI * Math.pow(param1, 3);
            surfaceArea = 4 * Math.PI * Math.pow(param1, 2);

            threeDView.showSphere(1.0f); // Render standard size sphere

            txtMainResults.setText(String.format(Locale.US,
                    "SPHERE (Radius = %.2fm)\nVolume = %.3f m³\nSurface Area = %.3f m²",
                    param1, volume, surfaceArea));

            txtFormulasInfo.setText("Volume V = ⁴⁄₃ π r³ \nSurface Area A = 4 π r²");

            txtStepDetails.setText(String.format(Locale.US,
                    "Step Logic:\n1. V = 1.333 * π * %.2f³ = %.3f\n2. A = 4 * π * %.2f² = %.3f",
                    param1, volume, param1, surfaceArea));

        } else if (shapeIndex == 2) { // Cylinder
            volume = Math.PI * Math.pow(param1, 2) * param2;
            surfaceArea = 2 * Math.PI * param1 * (param1 + param2);

            // Compute scaling relative aspect ratio to display properly in 3D frame
            float maxDim = Math.max(param1, param2);
            float rRatio = param1 / maxDim;
            float hRatio = param2 / maxDim;
            threeDView.showCylinder(rRatio, hRatio);

            txtMainResults.setText(String.format(Locale.US,
                    "CYLINDER (r = %.2fm, h = %.2fm)\nVolume = %.3f m³\nSurface Area = %.3f m²",
                    param1, param2, volume, surfaceArea));

            txtFormulasInfo.setText("Volume V = π r² h \nSurface Area A = 2πr(r + h)");

            txtStepDetails.setText(String.format(Locale.US,
                    "Step Logic:\n1. V = π * %.2f² * %.2f = %.3f\n2. A = 2 * π * %.2f * (%.2f + %.2f) = %.3f",
                    param1, param2, volume, param1, param1, param2, surfaceArea));

        } else if (shapeIndex == 3) { // Cone
            double slantHeight = Math.sqrt(Math.pow(param1, 2) + Math.pow(param2, 2));
            volume = (1.0 / 3.0) * Math.PI * Math.pow(param1, 2) * param2;
            surfaceArea = Math.PI * param1 * (param1 + slantHeight);

            float maxDim = Math.max(param1, param2);
            float rRatio = param1 / maxDim;
            float hRatio = param2 / maxDim;
            threeDView.showCone(rRatio, hRatio);

            txtMainResults.setText(String.format(Locale.US,
                    "CONE (r = %.2fm, h = %.2fm)\nSlant L = %.3fm\nVolume = %.3f m³\nSurface Area = %.3f m²",
                    param1, param2, slantHeight, volume, surfaceArea));

            txtFormulasInfo.setText("Slant Height L = √(r² + h²)\nVolume V = ¹⁄₃ π r² h\nSurface Area A = π r (r + L)");

            txtStepDetails.setText(String.format(Locale.US,
                    "Step Logic:\n1. L = √(%.2f² + %.2f²) = %.3f\n2. V = 0.333 * π * %.2f² * %.2f = %.3f\n3. A = π * %.2f * (%.2f + %.2f) = %.3f",
                    param1, param2, slantHeight, param1, param2, volume, param1, param1, slantHeight, surfaceArea));

        } else if (shapeIndex == 4) { // Square Pyramid
            double slantFaceHeight = Math.sqrt(Math.pow(param1 / 2.0, 2) + Math.pow(param2, 2));
            volume = (1.0 / 3.0) * Math.pow(param1, 2) * param2;
            surfaceArea = Math.pow(param1, 2) + 2 * param1 * slantFaceHeight;

            float maxDim = Math.max(param1, param2);
            float wRatio = param1 / maxDim;
            float hRatio = param2 / maxDim;
            threeDView.showPyramid(wRatio, hRatio);

            txtMainResults.setText(String.format(Locale.US,
                    "SQUARE PYRAMID (w = %.2fm, h = %.2fm)\nSlant Height = %.3fm\nVolume = %.3f m³\nSurface Area = %.3f m²",
                    param1, param2, slantFaceHeight, volume, surfaceArea));

            txtFormulasInfo.setText("Slant Height = √((w/2)² + h²)\nVolume V = ¹⁄₃ w² h\nSurface Area A = w² + 2w * (Slant Height)");

            txtStepDetails.setText(String.format(Locale.US,
                    "Step Logic:\n1. Slant = √((%.2f)² + %.2f²) = %.3f\n2. V = 0.333 * %.2f² * %.2f = %.3f\n3. A = %.2f² + 2 * %.2f * %.2f = %.3f",
                    param1 / 2.0f, param2, slantFaceHeight, param1, param2, volume, param1, param1, slantFaceHeight, surfaceArea));
        }
    }

    private void performFunctionPlotting() {
        int funcIndex = spinnerFunctions.getSelectedItemPosition();

        int resVal = 16;
        float amplitudeVal = 1.0f;

        try {
            String resStr = editGridRes.getText().toString().trim();
            if (!resStr.isEmpty()) {
                resVal = Integer.parseInt(resStr);
            }
            String ampStr = editAmplitude.getText().toString().trim();
            if (!ampStr.isEmpty()) {
                amplitudeVal = Float.parseFloat(ampStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid mathematical parameters.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resVal < 6) resVal = 6;
        if (resVal > 30) resVal = 30; // Limit rendering complexity to prevent sluggish FPS
        if (amplitudeVal < 0.1f) amplitudeVal = 0.1f;

        threeDView.showFunctionSurface(funcIndex, resVal, amplitudeVal);

        String funcLabel = FUNCTIONS_LIST[funcIndex];
        txtMainResults.setText(String.format(Locale.US,
                "PLOTTED SURFACE\n%s\nGrid points: %d x %d\nScale Ampl: %.2f",
                funcLabel, resVal + 1, resVal + 1, amplitudeVal));

        txtFormulasInfo.setText("Evaluated in 3D Cartesian coordinates x, z ∈ [-1.5, 1.5]\nHeight Y = f(x, z)");

        txtStepDetails.setText(String.format(Locale.US,
                "Step Logic:\n1. Divide domain range 3.0 into %d steps.\n2. Iterate through vertices and evaluate local mathematical value locally.\n3. Create dynamic wireframe matrix edges.",
                resVal));
    }
}