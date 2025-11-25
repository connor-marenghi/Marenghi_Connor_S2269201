package org.me.gcu.marenghi_connor_s2269201_1;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.appcompat.app.AppCompatActivity;
import android.location.Address;
import android.location.Geocoder;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class ConverterActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView currencyHeader;
    private TextView rateInfo;
    private EditText amountInput;
    private RadioGroup directionGroup;
    private RadioButton rbGbpToOther;

    private TextView resultText;
    private Button convertButton;
    private GoogleMap mMap;
    private CurrencyRate selectedRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);


        selectedRate = (CurrencyRate) getIntent().getSerializableExtra("selectedRate");
        if (selectedRate == null) {
            Toast.makeText(this, "No rate data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        currencyHeader = findViewById(R.id.currencyHeader);
        rateInfo       = findViewById(R.id.rateInfo);
        amountInput    = findViewById(R.id.amountInput);
        directionGroup = findViewById(R.id.directionGroup);
        rbGbpToOther   = findViewById(R.id.rbGbpToOther);

        resultText     = findViewById(R.id.resultText);
        convertButton  = findViewById(R.id.convertButton);


        String header = selectedRate.getCurrencyCode() + " - " + selectedRate.getCurrencyName();
        currencyHeader.setText(header);

        String rateText = "1 GBP = " + selectedRate.getGbpToCurrency() + " " + selectedRate.getCurrencyCode();
        rateInfo.setText(rateText);


        rbGbpToOther.setChecked(true);

        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConversion();
            }
        });
    }

    private void performConversion() {
        String amountStr = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            amountInput.setError("Enter an amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid number");
            return;
        }

        double rate = selectedRate.getGbpToCurrency();
        double result;
        String directionLabel;

        if (directionGroup.getCheckedRadioButtonId() == R.id.rbGbpToOther) {
            // GBP -> other
            result = amount * rate;
            directionLabel = "GBP → " + selectedRate.getCurrencyCode();
        } else {
            // other -> GBP
            result = amount / rate;
            directionLabel = selectedRate.getCurrencyCode() + " → GBP";
        }

        String formatted = String.format("%.2f", result);
        resultText.setText(directionLabel + " = " + formatted);
    }



    public void onMapReady(GoogleMap googleMap){
        mMap = googleMap;
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (selectedRate == null) {
            return;
        }

        // Build a location query based on currency information
        final String query = buildLocationQuery(selectedRate);
        if (query == null || query.trim().isEmpty()) {
            // Fallback: London
            LatLng fallback = new LatLng(51.5074, -0.1278);
            setMapMarker(fallback, "Unknown location");
            return;
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(ConverterActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocationName(query, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        final LatLng latLng = new LatLng(addr.getLatitude(), addr.getLongitude());
                        final String title = selectedRate.getCurrencyCode() + " - " + query;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setMapMarker(latLng, title);
                            }
                        });
                    } else {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LatLng fallback = new LatLng(51.5074, -0.1278);
                                setMapMarker(fallback, "Location not found: " + query);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LatLng fallback = new LatLng(51.5074, -0.1278);
                            setMapMarker(fallback, "Error finding location");
                        }
                    });
                }
            }
        }).start();
    }
    private void setMapMarker(LatLng point, String title) {
        if (mMap == null) return;

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(point).title(title));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 4.5f));
    }
    private String buildLocationQuery(CurrencyRate rate) {
        if (rate == null) return null;

        String code = rate.getCurrencyCode();
        String name = rate.getCurrencyName();

        if (code == null) code = "";
        if (name == null) name = "";

        code = code.toUpperCase(Locale.UK);


        switch (code) {
            case "EUR":
                return "European Union";
            case "USD":
                return "United States";
            case "GBP":
                return "United Kingdom";
        }


        if (name.toLowerCase(Locale.UK).contains("united arab emirates")) {
            return "United Arab Emirates";
        }


        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(" ");
                sb.append(parts[i]);
            }
            String guess = sb.toString().trim();
            if (!guess.isEmpty()) {
                return guess;
            }
        }


        return name;
    }

}