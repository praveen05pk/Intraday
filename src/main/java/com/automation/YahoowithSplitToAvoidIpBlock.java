package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class YahoowithSplitToAvoidIpBlock {


        static final String[] NIFTY_100 = {
                "RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS",
                "SBIN.NS", "BHARTIARTL.NS", "HINDUNILVR.NS", "ITC.NS", "KOTAKBANK.NS",
                "WIPRO.NS", "ULTRACEMCO.NS", "ASIANPAINT.NS", "LT.NS", "MARUTI.NS"
                // Add more as needed
        };

        static class Candle {
            double open, high, low, close;
            Candle(double o, double h, double l, double c) {
                open = o; high = h; low = l; close = c;
            }
        }

        static class StockAnalysis {
            String symbol;
            double rsi, atr;
            boolean bullish;
            double entry, exit, stoploss;

            StockAnalysis(String symbol, double rsi, double atr, boolean bullish, double entry, double exit, double stoploss) {
                this.symbol = symbol;
                this.rsi = rsi;
                this.atr = atr;
                this.bullish = bullish;
                this.entry = entry;
                this.exit = exit;
                this.stoploss = stoploss;
            }
        }

        public static void main(String[] args) throws Exception {
            List<StockAnalysis> bullishStocks = new ArrayList<>();

            for (String symbol : NIFTY_100) {
                System.out.println("Fetching: " + symbol);
                List<Candle> candles = fetchCandles(symbol);
                if (candles == null || candles.size() < 14) {
                    System.out.println("⚠️ Skipped: " + symbol + " (not enough data)");
                    continue;
                }

                double rsi = calculateRSI(candles);
                double atr = calculateATR(candles);
                boolean bullish = rsi > 50 && candles.get(candles.size() - 1).close > candles.get(candles.size() - 1).open;

                if (bullish) {
                    double entry = candles.get(candles.size() - 1).close;
                    double stoploss = entry - atr;
                    double exit = entry + (atr * 2);
                    bullishStocks.add(new StockAnalysis(symbol, rsi, atr, true, entry, exit, stoploss));
                }
                Thread.sleep(2500); // Prevent rate limit
            }

            bullishStocks.sort(Comparator.comparingDouble(a -> a.atr));

            if (bullishStocks.size() == 0) {
                System.out.println("No stocks found with bullish signals.");
                return;
            }

            System.out.println("\nTop 3 Bullish Stocks:");
            for (int i = 0; i < Math.min(3, bullishStocks.size()); i++) {
                StockAnalysis s = bullishStocks.get(i);
                System.out.printf("%s: Entry=%.2f, Exit=%.2f, Stoploss=%.2f (RSI=%.2f, ATR=%.2f)\n",
                        s.symbol, s.entry, s.exit, s.stoploss, s.rsi, s.atr);
            }

            StockAnalysis best = bullishStocks.get(0);
            System.out.printf("\n🔥 Best Pick: %s (Entry=%.2f, Exit=%.2f, Stoploss=%.2f)\n",
                    best.symbol, best.entry, best.exit, best.stoploss);
        }

        static List<Candle> fetchCandles(String symbol) {
            int retries = 0;
            while (retries < 3) {
                try {
                    String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=20d";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 429) {
                        System.out.println("Error: Rate limit hit for " + symbol);
                        Thread.sleep((retries + 1) * 2000);
                        retries++;
                        continue;
                    } else if (responseCode != 200) {
                        System.out.println("Error fetching data for " + symbol);
                        return null;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray timestamps = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONArray("timestamp");
                    JSONObject indicators = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);

                    JSONArray opens = indicators.getJSONArray("open");
                    JSONArray highs = indicators.getJSONArray("high");
                    JSONArray lows = indicators.getJSONArray("low");
                    JSONArray closes = indicators.getJSONArray("close");

                    List<Candle> candles = new ArrayList<>();
                    for (int i = 0; i < timestamps.length(); i++) {
                        if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i)) continue;
                        candles.add(new Candle(
                                opens.getDouble(i),
                                highs.getDouble(i),
                                lows.getDouble(i),
                                closes.getDouble(i)
                        ));
                    }
                    return candles;

                } catch (Exception e) {
                    System.out.println("Exception for " + symbol + ": " + e.getMessage());
                    return null;
                }
            }
            return null;
        }

        static double calculateRSI(List<Candle> candles) {
            double gain = 0, loss = 0;
            for (int i = 1; i < 15; i++) {
                double diff = candles.get(i).close - candles.get(i - 1).close;
                if (diff > 0) gain += diff;
                else loss -= diff;
            }
            double rs = gain / (loss == 0 ? 1 : loss);
            return 100 - (100 / (1 + rs));
        }

        static double calculateATR(List<Candle> candles) {
            double atr = 0;
            for (int i = 1; i < candles.size(); i++) {
                Candle c = candles.get(i);
                Candle prev = candles.get(i - 1);
                double tr = Math.max(c.high - c.low,
                        Math.max(Math.abs(c.high - prev.close), Math.abs(c.low - prev.close)));
                atr += tr;
            }
            return atr / (candles.size() - 1);
        }
    }


