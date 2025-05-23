package org.automation;

public class StockSignal {
    public String symbol;
    public double rsi;
    public double atr;
    public double vwap;
    public double lastPrice;

    public StockSignal(String symbol, double rsi, double atr, double vwap, double lastPrice) {
        this.symbol = symbol;
        this.rsi = rsi;
        this.atr = atr;
        this.vwap = vwap;
        this.lastPrice = lastPrice;
    }
}