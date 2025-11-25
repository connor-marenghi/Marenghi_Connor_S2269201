/*  Starter project for Mobile Platform Development - 1st diet 25/26
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required URL and displays the
    raw data in a TextField
*/

//
// Name                 Connor Marenghi
// Student ID          S2269201
// Programme of Study   Software Development
//

// UPDATE THE PACKAGE NAME to include your Student Identifier
package org.me.gcu.marenghi_connor_s2269201_1;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View.OnClickListener;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextWatcher;
import android.text.Editable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.widget.AdapterView;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import android.widget.ListView;
import android.graphics.Color;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private TextView rawDataDisplay;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long autoRefreshInt = 300000;
    private Button startButton;
    private TextView summaryEur;
    private TextView summaryUsd;
    private TextView summaryJpy;
    private String url1="https://www.fx-exchange.com/gbp/rss.xml";
    private TextView lastUpdate;
    private EditText searchBox;
    private ArrayList<CurrencyRate> allRates = new ArrayList<>();
    private ArrayAdapter<CurrencyRate> listAdapter;
    private ListView rateListView;

    private String feedLastBuildDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lastUpdate = findViewById(R.id.lastUpdate);
        searchBox = (EditText) findViewById(R.id.searchBox);
        rawDataDisplay = (TextView)findViewById(R.id.rawDataDisplay);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(this);
        rateListView = findViewById(R.id.rateList);

        summaryEur = findViewById(R.id.summaryEur);
        summaryUsd = findViewById(R.id.summaryUsd);
        summaryJpy = findViewById(R.id.summaryJpy);

        autoRefreshHandler = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                startProgress();
                autoRefreshHandler.postDelayed(this,autoRefreshInt);
            }
        };



        listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                allRates
        );

        rateListView.setAdapter(listAdapter);
        rateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CurrencyRate selected = (CurrencyRate) parent.getItemAtPosition(position);

                Intent intent = new Intent(MainActivity.this, ConverterActivity.class);
                intent.putExtra("selectedRate", selected);
                startActivity(intent);
            }
        });
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                listAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



    }
    @Override
    protected void onResume() {
        super.onResume();
        startProgress();
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {

            autoRefreshHandler.post(autoRefreshRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    public void onClick(View aview)
    {
        startProgress();
    }
    public void startProgress()
    {
        if (!isNetworkAvailable()) {
            Toast.makeText(
                    this,
                    "No internet connection. Please connect to the internet to use this app.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        new Thread(new Task(url1)).start();
    }




    private class Task implements Runnable
    {
        private String url;
        public Task(String aurl){
            url = aurl;
        }
        @Override
        public void run() {
            URL aurl;
            URLConnection yc;
            BufferedReader in = null;
            String inputLine = "";
            StringBuilder result = new StringBuilder();
            Log.d("MyTask", "in run");
            try {
                aurl = new URL(url);
                yc = aurl.openConnection();
                in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    result.append(inputLine);
                }
                in.close();
            } catch (IOException e) {
                Log.e("MyTag", "Error fetching data", e);
            }
            final String xmlResult = result.toString();
            final ArrayList<CurrencyRate> parsedRates = parseXmlData(xmlResult);
            final String parsedLastBuildDate = feedLastBuildDate;
            int i = result.indexOf("<?");
            result = new StringBuilder(result.substring(i));
            i = result.indexOf("</rss>");
            result = new StringBuilder(result.substring(0, i + 6));
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rawDataDisplay.setText("");
                    allRates.clear();
                    allRates.addAll(parsedRates);
                    listAdapter.notifyDataSetChanged();

                    if (parsedLastBuildDate != null && !parsedLastBuildDate.isEmpty()) {
                        lastUpdate.setText("Last updated: " + parsedLastBuildDate);
                    }
                    updateSummaryView(parsedRates);
                }
            });
        }
        private ArrayList<CurrencyRate> parseXmlData(String dataToParse) {
            ArrayList<CurrencyRate> rates = new ArrayList<>();
            feedLastBuildDate = "";

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(dataToParse));

                int eventType = xpp.getEventType();
                CurrencyRate currentRate = null;
                String textValue = "";

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String tagName = xpp.getName();

                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if ("item".equalsIgnoreCase(tagName)) {
                                currentRate = new CurrencyRate();
                            }
                            break;

                        case XmlPullParser.TEXT:
                            textValue = xpp.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if ("lastBuildDate".equalsIgnoreCase(tagName)) {

                                feedLastBuildDate = textValue.trim();
                            }
                            else if (currentRate != null) {
                                if ("title".equalsIgnoreCase(tagName)) {
                                    currentRate.setTitle(textValue.trim());

                                    extractCodeAndNameFromTitle(currentRate, textValue);
                                } else if ("description".equalsIgnoreCase(tagName)) {
                                    currentRate.setDescription(textValue.trim());

                                    currentRate.setGbpToCurrency(extractRateFromDescription(textValue));
                                } else if ("pubDate".equalsIgnoreCase(tagName)) {
                                    currentRate.setPubDate(textValue.trim());
                                } else if ("item".equalsIgnoreCase(tagName)) {
                                    rates.add(currentRate);
                                    currentRate = null;
                                }
                            }
                            break;
                    }

                    eventType = xpp.next();
                }
            } catch (XmlPullParserException e) {
                Log.e("MyTag", "Parsing error " + e.toString());
            } catch (IOException e) {
                Log.e("MyTag", "IO error during parsing");
            }

            return rates;
        }






    }
    private void extractCodeAndNameFromTitle(CurrencyRate rate, String title) {
        int lastOpen = title.lastIndexOf('(');
        int lastClose = title.lastIndexOf(')');
        if (lastOpen != -1 && lastClose != -1 && lastClose > lastOpen) {
            String code = title.substring(lastOpen + 1, lastClose).trim();
            rate.setCurrencyCode(code);
            int slashIndex = title.indexOf('/');
            if (slashIndex != -1 && slashIndex < lastOpen) {
                String namePart = title.substring(slashIndex + 1, lastOpen).trim();
                rate.setCurrencyName(namePart);
            }
        }
    }


    private double extractRateFromDescription(String description) {

        try {
            int equalsIndex = description.indexOf('=');
            if (equalsIndex != -1) {
                String afterEquals = description.substring(equalsIndex + 1).trim();
                String[] parts = afterEquals.split("\\s+"); // split on whitespace
                return Double.parseDouble(parts[0]);
            }
        } catch (Exception e) {
            Log.e("MyTag", "Error parsing rate from description: " + description, e);
        }
        return 0.0;
    }
    private void updateSummaryView(List<CurrencyRate> rates) {
        CurrencyRate eur = null;
        CurrencyRate usd = null;
        CurrencyRate jpy = null;

        for (CurrencyRate r : rates) {
            if (r.getCurrencyCode() == null) continue;
            String code = r.getCurrencyCode().toUpperCase(Locale.UK);

            if ("EUR".equals(code)) eur = r;
            else if ("USD".equals(code)) usd = r;
            else if ("JPY".equals(code)) jpy = r;
        }

        setSummaryRow(summaryEur, eur);
        setSummaryRow(summaryUsd, usd);
        setSummaryRow(summaryJpy, jpy);
    }

    private void setSummaryRow(TextView tv, CurrencyRate rate) {
        if (tv == null) return;

        if (rate == null) {
            tv.setText("Not available");
            tv.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        String text = String.format(
                Locale.UK,
                "%s - %s   (1 GBP = %.4f %s)",
                rate.getCurrencyCode(),
                rate.getCurrencyName(),
                rate.getGbpToCurrency(),
                rate.getCurrencyCode()
        );
        tv.setText(text);

        int color = getColorForRate(rate.getGbpToCurrency());
        tv.setBackgroundColor(color);
    }

    private int getColorForRate(double rate) {
        if (rate < 1.0) {
            return ContextCompat.getColor(this, R.color.rate_very_strong);
        } else if (rate < 2.0) {
            return ContextCompat.getColor(this, R.color.rate_strong);
        } else if (rate < 5.0) {
            return ContextCompat.getColor(this, R.color.rate_weak);
        } else {
            return ContextCompat.getColor(this, R.color.rate_very_weak);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

}