package com.expenseconverterapp.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.HashMap;
import java.util.Map;

public class CurrencyConverterFragment extends Fragment {

    private EditText etAmountToConvert;
    private Spinner spinnerCurrency;
    private Button btnConvert;
    private TextView tvConversionResult;

    private static final Map<String, Double> RATES = new HashMap<>();
    static {
        RATES.put("EUR (Euro)", 0.92);
        RATES.put("GBP (British Pound)", 0.79);
        RATES.put("JPY (Japanese Yen)", 155.0);
        RATES.put("CAD (Canadian Dollar)", 1.37);
        RATES.put("AUD (Australian Dollar)", 1.50);
        RATES.put("INR (Indian Rupee)", 83.3);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_currency, container, false);

        etAmountToConvert = view.findViewById(R.id.etAmountToConvert);
        spinnerCurrency = view.findViewById(R.id.spinnerCurrency);
        btnConvert = view.findViewById(R.id.btnConvert);
        tvConversionResult = view.findViewById(R.id.tvConversionResult);

        String[] currencies = RATES.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerCurrency.setAdapter(adapter);

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertCurrency();
            }
        });

        return view;
    }

    private void convertCurrency() {
        String valStr = etAmountToConvert.getText().toString().trim();
        if (valStr.isEmpty()) {
            Toast.makeText(requireContext(), "Enter amount in USD", Toast.LENGTH_SHORT).show();
            return;
        }

        double usdAmount;
        try {
            usdAmount = Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedCurrency = (String) spinnerCurrency.getSelectedItem();
        Double rate = RATES.get(selectedCurrency);
        if (rate != null) {
            double converted = usdAmount * rate;
            String symbol = selectedCurrency.substring(0, 3);
            tvConversionResult.setText(String.format("Result: %.2f %s", converted, symbol));
        }
    }
}