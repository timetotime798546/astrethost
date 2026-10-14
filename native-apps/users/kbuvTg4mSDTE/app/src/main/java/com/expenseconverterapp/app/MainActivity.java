package com.expenseconverterapp.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

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

        loadFragment(new ExpenseTrackerFragment());

        btnTabExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
                loadFragment(new ExpenseTrackerFragment());
            }
        });

        btnTabCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnTabCurrency.setBackgroundColor(getResources().getColor(R.color.accentColor));
                btnTabExpense.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
                loadFragment(new CurrencyConverterFragment());
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backendApi.clearToken();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}