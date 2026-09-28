package com.calculatorverifier.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private BackendApi api;

    // Login View Elements
    private LinearLayout loginContainer;
    private EditText loginEmail;
    private EditText loginPassword;
    private Button btnLogin;
    private TextView txtGoToRegister;
    private ProgressBar loginProgress;
    private TextView txtLoginError;

    // Register View Elements
    private LinearLayout registerContainer;
    private EditText registerEmail;
    private EditText registerPassword;
    private EditText registerConfirmPassword;
    private Button btnRegister;
    private TextView txtGoToLogin;
    private ProgressBar registerProgress;
    private TextView txtRegisterError;

    // Calculator View Elements
    private LinearLayout calculatorContainer;
    private TextView txtUserEmail;
    private Button btnLogout;
    private EditText editNumA;
    private EditText editNumB;
    private Spinner spinnerOp;
    private Button btnCalculate;
    private Button btnClear;
    private Button btnVerify;
    private TextView textResult;
    private TextView textVerifyStatus;
    
    // Verify Result elements
    private LinearLayout layoutVerifyResult;
    private TextView txtVerifyHeader;
    private TextView txtVerifyDetails;
    private ProgressBar verifyProgress;

    // State parameters
    private boolean isCalculated = false;
    private double calculatedA = 0.0;
    private double calculatedB = 0.0;
    private double calculatedResult = 0.0;
    private String currentLogic = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new BackendApi(this);

        initViews();
        setupSpinner();
        setupListeners();

        // Check authentication state
        String token = api.getToken();
        if (token != null && !token.isEmpty()) {
            showCalculator();
        } else {
            showLogin();
        }
    }

    private void initViews() {
        // Containers
        loginContainer = (LinearLayout) findViewById(R.id.loginContainer);
        registerContainer = (LinearLayout) findViewById(R.id.registerContainer);
        calculatorContainer = (LinearLayout) findViewById(R.id.calculatorContainer);

        // Login UI
        loginEmail = (EditText) findViewById(R.id.loginEmail);
        loginPassword = (EditText) findViewById(R.id.loginPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtGoToRegister = (TextView) findViewById(R.id.txtGoToRegister);
        loginProgress = (ProgressBar) findViewById(R.id.loginProgress);
        txtLoginError = (TextView) findViewById(R.id.txtLoginError);

        // Register UI
        registerEmail = (EditText) findViewById(R.id.registerEmail);
        registerPassword = (EditText) findViewById(R.id.registerPassword);
        registerConfirmPassword = (EditText) findViewById(R.id.registerConfirmPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        txtGoToLogin = (TextView) findViewById(R.id.txtGoToLogin);
        registerProgress = (ProgressBar) findViewById(R.id.registerProgress);
        txtRegisterError = (TextView) findViewById(R.id.txtRegisterError);

        // Calculator UI
        txtUserEmail = (TextView) findViewById(R.id.txtUserEmail);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        editNumA = (EditText) findViewById(R.id.editNumA);
        editNumB = (EditText) findViewById(R.id.editNumB);
        spinnerOp = (Spinner) findViewById(R.id.spinnerOp);
        btnCalculate = (Button) findViewById(R.id.btnCalculate);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnVerify = (Button) findViewById(R.id.btnVerify);
        textResult = (TextView) findViewById(R.id.textResult);
        textVerifyStatus = (TextView) findViewById(R.id.textVerifyStatus);
        
        // Verification Display card
        layoutVerifyResult = (LinearLayout) findViewById(R.id.layoutVerifyResult);
        txtVerifyHeader = (TextView) findViewById(R.id.txtVerifyHeader);
        txtVerifyDetails = (TextView) findViewById(R.id.txtVerifyDetails);
        verifyProgress = (ProgressBar) findViewById(R.id.verifyProgress);
    }

    private void setupSpinner() {
        String[] operations = { "Add (+)", "Subtract (-)", "Multiply (*)", "Divide (/)" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, operations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOp.setAdapter(adapter);
    }

    private void setupListeners() {
        // Switch to Register UI
        txtGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegister();
            }
        });

        // Switch to Login UI
        txtGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogin();
            }
        });

        // Login Action
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        // Register Action
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        // Logout Action
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogout();
            }
        });

        // Calculate Action
        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLocalCalculation();
            }
        });

        // Clear Action
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearCalculatorFields();
            }
        });

        // Verify Action
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performBackendVerification();
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.trim().length() > 3;
    }

    private void performLogin() {
        String email = loginEmail.getText().toString().trim();
        String password = loginPassword.getText().toString();

        if (email.isEmpty()) {
            showLoginError("Please enter email address");
            return;
        }
        if (!isValidEmail(email)) {
            showLoginError("Please enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            showLoginError("Please enter your password");
            return;
        }

        hideLoginError();
        setLoginLoading(true);

        api.login(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setLoginLoading(false);
                showCalculator();
            }

            @Override
            public void onFailure(String error) {
                setLoginLoading(false);
                showLoginError(error);
            }
        });
    }

    private void performRegistration() {
        final String email = registerEmail.getText().toString().trim();
        final String password = registerPassword.getText().toString();
        String confirmPassword = registerConfirmPassword.getText().toString();

        if (email.isEmpty()) {
            showRegisterError("Please enter email address");
            return;
        }
        if (!isValidEmail(email)) {
            showRegisterError("Please enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            showRegisterError("Please enter password");
            return;
        }
        if (password.length() < 6) {
            showRegisterError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showRegisterError("Passwords do not match");
            return;
        }

        hideRegisterError();
        setRegisterLoading(true);

        // Call POST /register which does NOT return token
        api.register(email, password, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                showRegisterStatus("Register success! Logging in...");
                
                // Chain to POST /login for auto-login as required
                api.login(email, password, new BackendApi.ApiCallback() {
                    @Override
                    public void onSuccess(JSONObject loginResponse) {
                        setRegisterLoading(false);
                        showCalculator();
                    }

                    @Override
                    public void onFailure(String error) {
                        setRegisterLoading(false);
                        showRegisterError("Auto-login failed: " + error + ". Please log in manually.");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                setRegisterLoading(false);
                showRegisterError(error);
            }
        });
    }

    private void performLogout() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        api.logout(new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                showLogin();
            }

            @Override
            public void onFailure(String error) {
                showLogin();
            }
        });
    }

    private void performLocalCalculation() {
        String rawA = editNumA.getText().toString().trim();
        String rawB = editNumB.getText().toString().trim();

        if (rawA.isEmpty() || rawB.isEmpty()) {
            Toast.makeText(this, "Please enter both numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double aVal = Double.parseDouble(rawA);
            double bVal = Double.parseDouble(rawB);
            int selectedIndex = spinnerOp.getSelectedItemPosition();

            double resultVal = 0;
            boolean errorOccurred = false;
            String errorMsg = "";

            switch (selectedIndex) {
                case 0: // Add
                    resultVal = aVal + bVal;
                    currentLogic = "calculator_add";
                    break;
                case 1: // Subtract
                    resultVal = aVal - bVal;
                    currentLogic = "calculator_subtract";
                    break;
                case 2: // Multiply
                    resultVal = aVal * bVal;
                    currentLogic = "calculator_multiply";
                    break;
                case 3: // Divide
                    if (bVal == 0) {
                        errorOccurred = true;
                        errorMsg = "Division by zero is not allowed";
                    } else {
                        resultVal = aVal / bVal;
                        currentLogic = "calculator_divide";
                    }
                    break;
            }

            if (errorOccurred) {
                textResult.setText(errorMsg);
                btnVerify.setEnabled(false);
                isCalculated = false;
                layoutVerifyResult.setVisibility(View.GONE);
                textVerifyStatus.setText("Calculation failed.");
            } else {
                calculatedA = aVal;
                calculatedB = bVal;
                calculatedResult = resultVal;
                isCalculated = true;

                textResult.setText("Local Result: " + resultVal);
                btnVerify.setEnabled(true);
                textVerifyStatus.setText("Local calculation done. Tap verify to validate authoritatively.");
                layoutVerifyResult.setVisibility(View.GONE);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show();
        }
    }

    private void performBackendVerification() {
        if (!isCalculated) {
            Toast.makeText(this, "Calculate a valid local result first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setVerifyLoading(true);
        textVerifyStatus.setText("Verifying result with Cloudflare Worker...");

        api.verify(currentLogic, calculatedA, calculatedB, calculatedResult, new BackendApi.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                setVerifyLoading(false);
                try {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        boolean verified = response.optBoolean("verified", false);
                        double expected = response.optDouble("expected", 0.0);
                        double received = response.optDouble("received", 0.0);

                        layoutVerifyResult.setVisibility(View.VISIBLE);
                        if (verified) {
                            txtVerifyHeader.setText("✓ Result verified");
                            layoutVerifyResult.setBackgroundColor(0xFFE6F4EA); // Light Green
                            txtVerifyHeader.setTextColor(0xFF137333); // Dark Green
                            txtVerifyDetails.setTextColor(0xFF137333);
                            textVerifyStatus.setText("Worker matching completed: Verified!");
                        } else {
                            txtVerifyHeader.setText("✗ Result mismatch");
                            layoutVerifyResult.setBackgroundColor(0xFFFCE8E6); // Light Red
                            txtVerifyHeader.setTextColor(0xFFC5221F); // Dark Red
                            txtVerifyDetails.setTextColor(0xFFC5221F);
                            textVerifyStatus.setText("Worker reports discrepancy in formula execution.");
                        }
                        txtVerifyDetails.setText("Expected: " + expected + " | Received: " + received);
                    } else {
                        textVerifyStatus.setText("Verification rejected: " + response.optString("message", "Unknown rejection"));
                        layoutVerifyResult.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    textVerifyStatus.setText("Error reading verification payload: " + e.getMessage());
                    layoutVerifyResult.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String error) {
                setVerifyLoading(false);
                textVerifyStatus.setText("Verification Network error: " + error);
                layoutVerifyResult.setVisibility(View.GONE);
            }
        });
    }

    private void showLogin() {
        loginContainer.setVisibility(View.VISIBLE);
        registerContainer.setVisibility(View.GONE);
        calculatorContainer.setVisibility(View.GONE);
        clearLoginFields();
    }

    private void showRegister() {
        loginContainer.setVisibility(View.GONE);
        registerContainer.setVisibility(View.VISIBLE);
        calculatorContainer.setVisibility(View.GONE);
        clearRegisterFields();
    }

    private void showCalculator() {
        loginContainer.setVisibility(View.GONE);
        registerContainer.setVisibility(View.GONE);
        calculatorContainer.setVisibility(View.VISIBLE);
        
        txtUserEmail.setText(api.getSavedEmail());
        clearCalculatorFields();
    }

    private void clearLoginFields() {
        loginEmail.setText("");
        loginPassword.setText("");
        hideLoginError();
        setLoginLoading(false);
    }

    private void clearRegisterFields() {
        registerEmail.setText("");
        registerPassword.setText("");
        registerConfirmPassword.setText("");
        hideRegisterError();
        setRegisterLoading(false);
    }

    private void clearCalculatorFields() {
        editNumA.setText("");
        editNumB.setText("");
        spinnerOp.setSelection(0);
        textResult.setText("Result: -");
        textVerifyStatus.setText("Enter numbers and click Calculate.");
        layoutVerifyResult.setVisibility(View.GONE);
        btnVerify.setEnabled(false);
        isCalculated = false;
        setVerifyLoading(false);
    }

    private void showLoginError(String message) {
        txtLoginError.setText(message);
        txtLoginError.setVisibility(View.VISIBLE);
    }

    private void hideLoginError() {
        txtLoginError.setVisibility(View.GONE);
    }

    private void showRegisterError(String message) {
        txtRegisterError.setText(message);
        txtRegisterError.setVisibility(View.VISIBLE);
    }

    private void showRegisterStatus(String status) {
        txtRegisterError.setText(status);
        txtRegisterError.setVisibility(View.VISIBLE);
    }

    private void hideRegisterError() {
        txtRegisterError.setVisibility(View.GONE);
    }

    private void setLoginLoading(boolean loading) {
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void setRegisterLoading(boolean loading) {
        registerProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private void setVerifyLoading(boolean loading) {
        verifyProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!loading && isCalculated);
    }
}