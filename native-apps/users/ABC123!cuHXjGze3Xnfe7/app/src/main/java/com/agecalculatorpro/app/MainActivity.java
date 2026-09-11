package com.agecalculatorpro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    // Tab buttons
    private Button btnTabAge, btnTabCompare, btnTabDate, btnTabProfiles, btnTabLeap;

    // Panels
    private ScrollView panelAgeCalc;
    private ScrollView panelCompare;
    private ScrollView panelDateCalc;
    private LinearLayout panelProfiles;
    private ScrollView panelLeap;

    // --- Panel 1 Age Calc Elements ---
    private Button btnDobPicker, btnTargetPicker, btnCalculateAge, btnClearAge, btnSaveCalculatedProfile;
    private LinearLayout layoutAgeResults;
    private TextView txtResultExactAge, txtResultNextBirthday, txtResultNextWeekday;
    private TextView statYears, statMonths, statWeeks, statDays, statHours, statMinutes, statSeconds;

    private Calendar calDob = null;
    private Calendar calTarget = null;

    // --- Panel 2 Compare Elements ---
    private EditText edtCompareP1Name, edtCompareP2Name;
    private Button btnCompareP1Dob, btnCompareP2Dob, btnCompareAction;
    private LinearLayout layoutCompareResults;
    private TextView txtCompareStatement, txtCompareDiffExact;
    private Calendar calCompareP1 = null;
    private Calendar calCompareP2 = null;

    // --- Panel 3 Date Calc Elements ---
    private Button btnDateCalcBase, btnDateCalcAction;
    private RadioGroup rgDateCalcOperation;
    private EditText edtCalcYears, edtCalcMonths, edtCalcWeeks, edtCalcDays;
    private LinearLayout layoutDateCalcResults;
    private TextView txtDateCalcResult;
    private Calendar calDateCalcBase = null;

    // --- Panel 4 Profiles Elements ---
    private Button btnProfilesAddNew;
    private TextView txtProfilesEmpty;
    private ListView listSavedProfiles;
    private ArrayList<UserProfile> userProfilesList = new ArrayList<UserProfile>();
    private ProfilesAdapter profilesAdapter;

    // --- Panel 5 Leap Year Elements ---
    private EditText edtLeapYear;
    private Button btnCheckLeap;
    private LinearLayout layoutLeapResults;
    private TextView txtLeapResult, txtLeapExplanation;

    // Date formatting helper
    private SimpleDateFormat dateDisplayFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
    private SimpleDateFormat fullDateDisplayFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);

    // Profile Model
    public static class UserProfile {
        String id;
        String name;
        int day;
        int month; // 0-indexed
        int year;

        public UserProfile(String id, String name, int day, int month, int year) {
            this.id = id;
            this.name = name;
            this.day = day;
            this.month = month;
            this.year = year;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layouts and set standard listeners
        initViews();
        setupNavigation();
        setupAgeCalculator();
        setupCompareCalculator();
        setupDateCalculator();
        setupSavedProfiles();
        setupLeapYearChecker();

        // Defaults
        resetAgeCalcInputs();
        resetDateCalcInputs();
    }

    private void initViews() {
        // Tab elements
        btnTabAge = (Button) findViewById(R.id.btn_tab_age);
        btnTabCompare = (Button) findViewById(R.id.btn_tab_compare);
        btnTabDate = (Button) findViewById(R.id.btn_tab_date);
        btnTabProfiles = (Button) findViewById(R.id.btn_tab_profiles);
        btnTabLeap = (Button) findViewById(R.id.btn_tab_leap);

        // Panels
        panelAgeCalc = (ScrollView) findViewById(R.id.panel_age_calc);
        panelCompare = (ScrollView) findViewById(R.id.panel_compare);
        panelDateCalc = (ScrollView) findViewById(R.id.panel_date_calc);
        panelProfiles = (LinearLayout) findViewById(R.id.panel_profiles);
        panelLeap = (ScrollView) findViewById(R.id.panel_leap);

        // Panel 1 Views
        btnDobPicker = (Button) findViewById(R.id.btn_dob_picker);
        btnTargetPicker = (Button) findViewById(R.id.btn_target_picker);
        btnCalculateAge = (Button) findViewById(R.id.btn_calculate_age);
        btnClearAge = (Button) findViewById(R.id.btn_clear_age);
        btnSaveCalculatedProfile = (Button) findViewById(R.id.btn_save_calculated_profile);
        layoutAgeResults = (LinearLayout) findViewById(R.id.layout_age_results);
        txtResultExactAge = (TextView) findViewById(R.id.txt_result_exact_age);
        txtResultNextBirthday = (TextView) findViewById(R.id.txt_result_next_birthday);
        txtResultNextWeekday = (TextView) findViewById(R.id.txt_result_next_weekday);

        statYears = (TextView) findViewById(R.id.stat_years);
        statMonths = (TextView) findViewById(R.id.stat_months);
        statWeeks = (TextView) findViewById(R.id.stat_weeks);
        statDays = (TextView) findViewById(R.id.stat_days);
        statHours = (TextView) findViewById(R.id.stat_hours);
        statMinutes = (TextView) findViewById(R.id.stat_minutes);
        statSeconds = (TextView) findViewById(R.id.stat_seconds);

        // Panel 2 Views
        edtCompareP1Name = (EditText) findViewById(R.id.edt_compare_p1_name);
        edtCompareP2Name = (EditText) findViewById(R.id.edt_compare_p2_name);
        btnCompareP1Dob = (Button) findViewById(R.id.btn_compare_p1_dob);
        btnCompareP2Dob = (Button) findViewById(R.id.btn_compare_p2_dob);
        btnCompareAction = (Button) findViewById(R.id.btn_compare_action);
        layoutCompareResults = (LinearLayout) findViewById(R.id.layout_compare_results);
        txtCompareStatement = (TextView) findViewById(R.id.txt_compare_statement);
        txtCompareDiffExact = (TextView) findViewById(R.id.txt_compare_diff_exact);

        // Panel 3 Views
        btnDateCalcBase = (Button) findViewById(R.id.btn_datecalc_base);
        rgDateCalcOperation = (RadioGroup) findViewById(R.id.rg_datecalc_operation);
        edtCalcYears = (EditText) findViewById(R.id.edt_calc_years);
        edtCalcMonths = (EditText) findViewById(R.id.edt_calc_months);
        edtCalcWeeks = (EditText) findViewById(R.id.edt_calc_weeks);
        edtCalcDays = (EditText) findViewById(R.id.edt_calc_days);
        btnDateCalcAction = (Button) findViewById(R.id.btn_datecalc_action);
        layoutDateCalcResults = (LinearLayout) findViewById(R.id.layout_datecalc_results);
        txtDateCalcResult = (TextView) findViewById(R.id.txt_datecalc_result);

        // Panel 4 Views
        btnProfilesAddNew = (Button) findViewById(R.id.btn_profiles_add_new);
        txtProfilesEmpty = (TextView) findViewById(R.id.txt_profiles_empty);
        listSavedProfiles = (ListView) findViewById(R.id.list_saved_profiles);

        // Panel 5 Views
        edtLeapYear = (EditText) findViewById(R.id.edt_leap_year);
        btnCheckLeap = (Button) findViewById(R.id.btn_check_leap);
        layoutLeapResults = (LinearLayout) findViewById(R.id.layout_leap_results);
        txtLeapResult = (TextView) findViewById(R.id.txt_leap_result);
        txtLeapExplanation = (TextView) findViewById(R.id.txt_leap_explanation);
    }

    private void setupNavigation() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset all navigation tabs text colors and styles
                btnTabAge.setTextColor(Color.parseColor("#B2DFDB"));
                btnTabCompare.setTextColor(Color.parseColor("#B2DFDB"));
                btnTabDate.setTextColor(Color.parseColor("#B2DFDB"));
                btnTabProfiles.setTextColor(Color.parseColor("#B2DFDB"));
                btnTabLeap.setTextColor(Color.parseColor("#B2DFDB"));

                // Hide all panels
                panelAgeCalc.setVisibility(View.GONE);
                panelCompare.setVisibility(View.GONE);
                panelDateCalc.setVisibility(View.GONE);
                panelProfiles.setVisibility(View.GONE);
                panelLeap.setVisibility(View.GONE);

                int id = view.getId();
                if (id == R.id.btn_tab_age) {
                    btnTabAge.setTextColor(Color.parseColor("#FFFFFF"));
                    panelAgeCalc.setVisibility(View.VISIBLE);
                } else if (id == R.id.btn_tab_compare) {
                    btnTabCompare.setTextColor(Color.parseColor("#FFFFFF"));
                    panelCompare.setVisibility(View.VISIBLE);
                } else if (id == R.id.btn_tab_date) {
                    btnTabDate.setTextColor(Color.parseColor("#FFFFFF"));
                    panelDateCalc.setVisibility(View.VISIBLE);
                } else if (id == R.id.btn_tab_profiles) {
                    btnTabProfiles.setTextColor(Color.parseColor("#FFFFFF"));
                    panelProfiles.setVisibility(View.VISIBLE);
                    refreshProfilesList();
                } else if (id == R.id.btn_tab_leap) {
                    btnTabLeap.setTextColor(Color.parseColor("#FFFFFF"));
                    panelLeap.setVisibility(View.VISIBLE);
                }
            }
        };

        btnTabAge.setOnClickListener(clickListener);
        btnTabCompare.setOnClickListener(clickListener);
        btnTabDate.setOnClickListener(clickListener);
        btnTabProfiles.setOnClickListener(clickListener);
        btnTabLeap.setOnClickListener(clickListener);
    }

    // ================= MAIN AGE CALCULATOR CODE =================

    private void setupAgeCalculator() {
        btnDobPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = calDob != null ? calDob.get(Calendar.YEAR) : 1995;
                int month = calDob != null ? calDob.get(Calendar.MONTH) : 0;
                int day = calDob != null ? calDob.get(Calendar.DAY_OF_MONTH) : 1;

                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int sYear, int sMonth, int sDay) {
                        calDob = Calendar.getInstance();
                        calDob.set(sYear, sMonth, sDay, 0, 0, 0);
                        calDob.set(Calendar.MILLISECOND, 0);
                        btnDobPicker.setText(dateDisplayFormat.format(calDob.getTime()));
                    }
                }, year, month, day);
                pickerDialog.show();
            }
        });

        btnTargetPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = calTarget.get(Calendar.YEAR);
                int month = calTarget.get(Calendar.MONTH);
                int day = calTarget.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int sYear, int sMonth, int sDay) {
                        calTarget = Calendar.getInstance();
                        calTarget.set(sYear, sMonth, sDay, 0, 0, 0);
                        calTarget.set(Calendar.MILLISECOND, 0);
                        btnTargetPicker.setText(dateDisplayFormat.format(calTarget.getTime()));
                    }
                }, year, month, day);
                pickerDialog.show();
            }
        });

        btnCalculateAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performMainAgeCalculation();
            }
        });

        btnClearAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetAgeCalcInputs();
            }
        });

        btnSaveCalculatedProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSaveProfileDialog(calDob);
            }
        });
    }

    private void resetAgeCalcInputs() {
        calDob = null;
        calTarget = Calendar.getInstance();
        calTarget.set(Calendar.HOUR_OF_DAY, 0);
        calTarget.set(Calendar.MINUTE, 0);
        calTarget.set(Calendar.SECOND, 0);
        calTarget.set(Calendar.MILLISECOND, 0);

        btnDobPicker.setText("Select Date of Birth");
        btnTargetPicker.setText("Today (" + dateDisplayFormat.format(calTarget.getTime()) + ")");
        layoutAgeResults.setVisibility(View.GONE);
    }

    private void performMainAgeCalculation() {
        if (calDob == null) {
            Toast.makeText(this, "Please select Date of Birth first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calDob.after(calTarget)) {
            Toast.makeText(this, "Date of birth cannot be after the Target Date!", Toast.LENGTH_LONG).show();
            return;
        }

        // 1. Calculate precise Years, Months, Days
        long[] preciseAge = calculatePreciseDifference(calDob, calTarget);
        long years = preciseAge[0];
        long months = preciseAge[1];
        long days = preciseAge[2];

        txtResultExactAge.setText(years + " Years, " + months + " Months, " + days + " Days");

        // 2. Next birthday details
        Calendar nextBday = Calendar.getInstance();
        nextBday.setTime(calDob.getTime());
        nextBday.set(Calendar.YEAR, calTarget.get(Calendar.YEAR));

        if (nextBday.before(calTarget) || nextBday.equals(calTarget)) {
            nextBday.add(Calendar.YEAR, 1);
        }

        long[] nextBdayDiff = calculatePreciseDifference(calTarget, nextBday);
        long nextMonths = nextBdayDiff[0] * 12 + nextBdayDiff[1];
        long nextDays = nextBdayDiff[2];

        String nextBdayStr = "Remaining: ";
        if (nextMonths > 0) {
            nextBdayStr += nextMonths + " Month" + (nextMonths > 1 ? "s " : " ");
        }
        nextBdayStr += nextDays + " Day" + (nextDays > 1 ? "s" : "");
        txtResultNextBirthday.setText(nextBdayStr);

        SimpleDateFormat weekdayFormat = new SimpleDateFormat("EEEE", Locale.US);
        txtResultNextWeekday.setText("Next Birthday falls on: " + weekdayFormat.format(nextBday.getTime()));

        // 3. Complete Total Stats metrics
        long totalDiffMillis = calTarget.getTimeInMillis() - calDob.getTimeInMillis();
        long totalDays = totalDiffMillis / (1000L * 60 * 60 * 24);

        DecimalFormat df = new DecimalFormat("#,###");

        statYears.setText(df.format(years) + " Years");
        statMonths.setText(df.format(years * 12 + months) + " Months");
        statWeeks.setText(df.format(totalDays / 7) + " Weeks, " + (totalDays % 7) + " Days");
        statDays.setText(df.format(totalDays) + " Days");
        statHours.setText(df.format(totalDays * 24L) + " Hours");
        statMinutes.setText(df.format(totalDays * 24L * 60L) + " Minutes");
        statSeconds.setText(df.format(totalDays * 24L * 60L * 60L) + " Seconds");

        layoutAgeResults.setVisibility(View.VISIBLE);
    }

    private long[] calculatePreciseDifference(Calendar startCal, Calendar endCal) {
        Calendar start = (Calendar) startCal.clone();
        Calendar end = (Calendar) endCal.clone();

        int years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        int months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH);
        int days = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);

        if (days < 0) {
            Calendar temp = (Calendar) end.clone();
            temp.add(Calendar.MONTH, -1);
            days += temp.getActualMaximum(Calendar.DAY_OF_MONTH);
            months--;
        }

        if (months < 0) {
            months += 12;
            years--;
        }

        return new long[] { years, months, days };
    }

    // ================= AGE COMPARISON CODE =================

    private void setupCompareCalculator() {
        btnCompareP1Dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = calCompareP1 != null ? calCompareP1.get(Calendar.YEAR) : 1995;
                int month = calCompareP1 != null ? calCompareP1.get(Calendar.MONTH) : 0;
                int day = calCompareP1 != null ? calCompareP1.get(Calendar.DAY_OF_MONTH) : 1;

                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int sYear, int sMonth, int sDay) {
                        calCompareP1 = Calendar.getInstance();
                        calCompareP1.set(sYear, sMonth, sDay, 0, 0, 0);
                        calCompareP1.set(Calendar.MILLISECOND, 0);
                        btnCompareP1Dob.setText(dateDisplayFormat.format(calCompareP1.getTime()));
                    }
                }, year, month, day);
                pickerDialog.show();
            }
        });

        btnCompareP2Dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = calCompareP2 != null ? calCompareP2.get(Calendar.YEAR) : 1995;
                int month = calCompareP2 != null ? calCompareP2.get(Calendar.MONTH) : 0;
                int day = calCompareP2 != null ? calCompareP2.get(Calendar.DAY_OF_MONTH) : 1;

                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int sYear, int sMonth, int sDay) {
                        calCompareP2 = Calendar.getInstance();
                        calCompareP2.set(sYear, sMonth, sDay, 0, 0, 0);
                        calCompareP2.set(Calendar.MILLISECOND, 0);
                        btnCompareP2Dob.setText(dateDisplayFormat.format(calCompareP2.getTime()));
                    }
                }, year, month, day);
                pickerDialog.show();
            }
        });

        btnCompareAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performAgeComparison();
            }
        });
    }

    private void performAgeComparison() {
        String name1 = edtCompareP1Name.getText().toString().trim();
        String name2 = edtCompareP2Name.getText().toString().trim();

        if (TextUtils.isEmpty(name1)) name1 = "Person A";
        if (TextUtils.isEmpty(name2)) name2 = "Person B";

        if (calCompareP1 == null || calCompareP2 == null) {
            Toast.makeText(this, "Please select Date of Birth for both individuals", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calCompareP1.equals(calCompareP2)) {
            txtCompareStatement.setText(name1 + " and " + name2 + " are exactly of same age.");
            txtCompareDiffExact.setText("Difference: 0 Days");
            layoutCompareResults.setVisibility(View.VISIBLE);
            return;
        }

        Calendar older, younger;
        String olderName, youngerName;

        if (calCompareP1.before(calCompareP2)) {
            older = calCompareP1;
            olderName = name1;
            younger = calCompareP2;
            youngerName = name2;
        } else {
            older = calCompareP2;
            olderName = name2;
            younger = calCompareP1;
            youngerName = name1;
        }

        long[] diff = calculatePreciseDifference(older, younger);
        long years = diff[0];
        long months = diff[1];
        long days = diff[2];

        txtCompareStatement.setText(olderName + " is older than " + youngerName + ".");
        txtCompareDiffExact.setText("Difference: " + years + " Years, " + months + " Months, " + days + " Days");
        layoutCompareResults.setVisibility(View.VISIBLE);
    }

    // ================= DATE ADD / SUBTRACT CALCULATOR =================

    private void setupDateCalculator() {
        btnDateCalcBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = calDateCalcBase != null ? calDateCalcBase.get(Calendar.YEAR) : Calendar.getInstance().get(Calendar.YEAR);
                int month = calDateCalcBase != null ? calDateCalcBase.get(Calendar.MONTH) : Calendar.getInstance().get(Calendar.MONTH);
                int day = calDateCalcBase != null ? calDateCalcBase.get(Calendar.DAY_OF_MONTH) : Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int sYear, int sMonth, int sDay) {
                        calDateCalcBase = Calendar.getInstance();
                        calDateCalcBase.set(sYear, sMonth, sDay, 0, 0, 0);
                        calDateCalcBase.set(Calendar.MILLISECOND, 0);
                        btnDateCalcBase.setText(dateDisplayFormat.format(calDateCalcBase.getTime()));
                    }
                }, year, month, day);
                pickerDialog.show();
            }
        });

        btnDateCalcAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performDateCalculation();
            }
        });
    }

    private void resetDateCalcInputs() {
        calDateCalcBase = Calendar.getInstance();
        btnDateCalcBase.setText("Today (" + dateDisplayFormat.format(calDateCalcBase.getTime()) + ")");
        edtCalcYears.setText("");
        edtCalcMonths.setText("");
        edtCalcWeeks.setText("");
        edtCalcDays.setText("");
        layoutDateCalcResults.setVisibility(View.GONE);
    }

    private void performDateCalculation() {
        if (calDateCalcBase == null) {
            Toast.makeText(this, "Please select a Base Date", Toast.LENGTH_SHORT).show();
            return;
        }

        int years = 0;
        int months = 0;
        int weeks = 0;
        int days = 0;

        try {
            String yStr = edtCalcYears.getText().toString();
            if (!TextUtils.isEmpty(yStr)) years = Integer.parseInt(yStr);

            String mStr = edtCalcMonths.getText().toString();
            if (!TextUtils.isEmpty(mStr)) months = Integer.parseInt(mStr);

            String wStr = edtCalcWeeks.getText().toString();
            if (!TextUtils.isEmpty(wStr)) weeks = Integer.parseInt(wStr);

            String dStr = edtCalcDays.getText().toString();
            if (!TextUtils.isEmpty(dStr)) days = Integer.parseInt(dStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number inputs format!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isAddition = rgDateCalcOperation.getCheckedRadioButtonId() == R.id.rb_add;
        int factor = isAddition ? 1 : -1;

        Calendar resultCal = (Calendar) calDateCalcBase.clone();
        resultCal.add(Calendar.YEAR, years * factor);
        resultCal.add(Calendar.MONTH, months * factor);
        resultCal.add(Calendar.WEEK_OF_YEAR, weeks * factor);
        resultCal.add(Calendar.DAY_OF_MONTH, days * factor);

        txtDateCalcResult.setText(fullDateDisplayFormat.format(resultCal.getTime()));
        layoutDateCalcResults.setVisibility(View.VISIBLE);
    }

    // ================= LOCAL SAVED PROFILES CODE =================

    private void setupSavedProfiles() {
        profilesAdapter = new ProfilesAdapter();
        listSavedProfiles.setAdapter(profilesAdapter);

        btnProfilesAddNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSaveProfileDialog(null);
            }
        });
    }

    private void promptSaveProfileDialog(final Calendar defaultCal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Birthday Profile");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        final EditText inputName = new EditText(this);
        inputName.setHint("Profile Name (e.g. John)");
        inputName.setSingleLine(true);
        layout.addView(inputName);

        final Button dateSelectorBtn = new Button(this);
        dateSelectorBtn.setGravity(android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL);
        dateSelectorBtn.setBackgroundResource(android.R.drawable.edit_text);

        final Calendar pickerCalendar = Calendar.getInstance();
        if (defaultCal != null) {
            pickerCalendar.setTime(defaultCal.getTime());
            dateSelectorBtn.setText(dateDisplayFormat.format(pickerCalendar.getTime()));
        } else {
            pickerCalendar.set(1995, 0, 1);
            dateSelectorBtn.setText("Select Date of Birth");
        }

        dateSelectorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog pickerDialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker dp, int y, int m, int d) {
                        pickerCalendar.set(y, m, d, 0, 0, 0);
                        dateSelectorBtn.setText(dateDisplayFormat.format(pickerCalendar.getTime()));
                    }
                }, pickerCalendar.get(Calendar.YEAR), pickerCalendar.get(Calendar.MONTH), pickerCalendar.get(Calendar.DAY_OF_MONTH));
                pickerDialog.show();
            }
        });

        TextView lblDate = new TextView(this);
        lblDate.setText("Birthdate Date:");
        lblDate.setPadding(0, 16, 0, 4);

        layout.addView(lblDate);
        layout.addView(dateSelectorBtn);

        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = inputName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(MainActivity.this, "Profile name required!", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveNewUserProfile(name, pickerCalendar);
                refreshProfilesList();
                Toast.makeText(MainActivity.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveNewUserProfile(String name, Calendar cal) {
        SharedPreferences prefs = getSharedPreferences("AgeCalcPreferences", Context.MODE_PRIVATE);
        String raw = prefs.getString("profiles_raw", "");

        String entry = name.replace("|", "").replace(";", "") + "|" +
                cal.get(Calendar.DAY_OF_MONTH) + "|" +
                cal.get(Calendar.MONTH) + "|" +
                cal.get(Calendar.YEAR);

        if (TextUtils.isEmpty(raw)) {
            raw = entry;
        } else {
            raw += ";" + entry;
        }

        prefs.edit().putString("profiles_raw", raw).apply();
    }

    private void loadAllSavedProfiles() {
        userProfilesList.clear();
        SharedPreferences prefs = getSharedPreferences("AgeCalcPreferences", Context.MODE_PRIVATE);
        String raw = prefs.getString("profiles_raw", "");

        if (TextUtils.isEmpty(raw)) return;

        String[] entries = raw.split(";");
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            if (TextUtils.isEmpty(entry)) continue;

            String[] parts = entry.split("\\|");
            if (parts.length == 4) {
                try {
                    String name = parts[0];
                    int day = Integer.parseInt(parts[1]);
                    int month = Integer.parseInt(parts[2]);
                    int year = Integer.parseInt(parts[3]);

                    UserProfile profile = new UserProfile(String.valueOf(i), name, day, month, year);
                    userProfilesList.add(profile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void refreshProfilesList() {
        loadAllSavedProfiles();
        if (userProfilesList.isEmpty()) {
            txtProfilesEmpty.setVisibility(View.VISIBLE);
            listSavedProfiles.setVisibility(View.GONE);
        } else {
            txtProfilesEmpty.setVisibility(View.GONE);
            listSavedProfiles.setVisibility(View.VISIBLE);
            profilesAdapter.notifyDataSetChanged();
        }
    }

    private void deleteProfileAtIndex(int index) {
        SharedPreferences prefs = getSharedPreferences("AgeCalcPreferences", Context.MODE_PRIVATE);
        loadAllSavedProfiles();

        if (index < 0 || index >= userProfilesList.size()) return;

        userProfilesList.remove(index);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < userProfilesList.size(); i++) {
            UserProfile p = userProfilesList.get(i);
            sb.append(p.name).append("|")
                    .append(p.day).append("|")
                    .append(p.month).append("|")
                    .append(p.year);
            if (i < userProfilesList.size() - 1) {
                sb.append(";");
            }
        }

        prefs.edit().putString("profiles_raw", sb.toString()).apply();
        refreshProfilesList();
        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
    }

    private class ProfilesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return userProfilesList.size();
        }

        @Override
        public Object getItem(int i) {
            return userProfilesList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            UserProfile profile = userProfilesList.get(position);

            Calendar dob = Calendar.getInstance();
            dob.set(profile.year, profile.month, profile.day, 0, 0, 0);
            dob.set(Calendar.MILLISECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            long[] age = calculatePreciseDifference(dob, today);
            String ageStr = age[0] + " Yrs, " + age[1] + " Mos, " + age[2] + " Days";

            // Next Birthday Countdown Calculation
            Calendar nextBday = Calendar.getInstance();
            nextBday.setTime(dob.getTime());
            nextBday.set(Calendar.YEAR, today.get(Calendar.YEAR));
            if (nextBday.before(today) || nextBday.equals(today)) {
                nextBday.add(Calendar.YEAR, 1);
            }

            long daysRemaining = (nextBday.getTimeInMillis() - today.getTimeInMillis()) / (1000L * 60 * 60 * 24);

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

            text1.setText(profile.name + " (" + dateDisplayFormat.format(dob.getTime()) + ")");
            text1.setTextColor(Color.parseColor("#004D40"));
            text1.setTextSize(16sp());

            text2.setText("Age: " + ageStr + "\nNext Birthday: " + daysRemaining + " days left");
            text2.setPadding(0, 4, 0, 4);

            // Double click or Long press delete prompt
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    AlertDialog.Builder delBuilder = new AlertDialog.Builder(MainActivity.this);
                    delBuilder.setTitle("Delete Profile");
                    delBuilder.setMessage("Are you sure you want to delete profile: " + userProfilesList.get(position).name + "?");
                    delBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int idx) {
                            deleteProfileAtIndex(position);
                        }
                    });
                    delBuilder.setNegativeButton("Cancel", null);
                    delBuilder.show();
                    return true;
                }
            });

            return convertView;
        }

        private float sp() {
            return 16.0f;
        }
    }

    // ================= LEAP YEAR CHECKER CODE =================

    private void setupLeapYearChecker() {
        btnCheckLeap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performLeapYearCheck();
            }
        });
    }

    private void performLeapYearCheck() {
        String yearStr = edtLeapYear.getText().toString().trim();
        if (TextUtils.isEmpty(yearStr)) {
            Toast.makeText(this, "Please enter a valid year value!", Toast.LENGTH_SHORT).show();
            return;
        }

        int year;
        try {
            year = Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Year input value", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isLeap = false;
        String explanation = "";

        // Standard Leap Year algorithm rule logic
        if (year % 4 == 0) {
            if (year % 100 == 0) {
                if (year % 400 == 0) {
                    isLeap = true;
                    explanation = year + " is divisible by 4, 100, and 400. This fits standard Gregorian Calendar Leap requirements.";
                } else {
                    isLeap = false;
                    explanation = year + " is divisible by 4 and 100, but not by 400. Therefore, it is NOT a leap year.";
                }
            } else {
                isLeap = true;
                explanation = year + " is divisible by 4, but is not divisible by 100. This is a standard leap year featuring 366 total days.";
            }
        } else {
            isLeap = false;
            explanation = year + " is not divisible by 4. Hence, it contains standard 365 days and is not a leap year.";
        }

        if (isLeap) {
            txtLeapResult.setText(year + " IS a Leap Year!");
            txtLeapResult.setTextColor(Color.parseColor("#00796B"));
        } else {
            txtLeapResult.setText(year + " is NOT a Leap Year.");
            txtLeapResult.setTextColor(Color.parseColor("#D32F2F"));
        }

        txtLeapExplanation.setText(explanation);
        layoutLeapResults.setVisibility(View.VISIBLE);
    }
}