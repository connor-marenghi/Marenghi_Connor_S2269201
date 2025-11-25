package org.me.gcu.marenghi_connor_s2269201_1;


import java.io.Serializable;

public class CurrencyRate implements Serializable {
    private String currencyCode;
    private String currencyName;
    private double gbpToCurrency;
    private String pubDate;
    private String title;
    private String description;

    public CurrencyRate() { }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }

    public double getGbpToCurrency() { return gbpToCurrency; }
    public void setGbpToCurrency(double gbpToCurrency) { this.gbpToCurrency = gbpToCurrency; }

    public String getPubDate() { return pubDate; }
    public void setPubDate(String pubDate) { this.pubDate = pubDate; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


    @Override
    public String toString() {
        return currencyCode + "  " + gbpToCurrency + " (" + currencyName + ")";
    }
}
