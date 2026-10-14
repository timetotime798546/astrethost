package com.expenseconverterapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

    private Button btnTabExpense, btnTabCurrency, btnLogout;
    private BackendApi backendApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        backendApi = new BackendApi(this);

        btnTabExpense = findViewById(R.id.btnTabExpense);
        btnTabCurrency = findViewById(R.id.btnTabCurrency);
        btnLogout = findViewById(R.id.btnLogout);

        loadFragment(new ExpenseTrackerFragment(), true);

        btnTabExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
                loadFragment(new ExpenseTrackerFragment(), false);
            }
        });

        btnTabCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
                loadFragment(new CurrencyConverterFragment(), false);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearToken();
                startActivity(new Intent(MainActivity.javaToClass(), LoginActivity.class));
                finish();
            }
        });
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment, boolean isInitial) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private static Class<?> javaToClass() {
        return MainActivity.class;
    }
}