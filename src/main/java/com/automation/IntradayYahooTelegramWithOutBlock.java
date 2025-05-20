package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class IntradayYahooTelegramWithOutBlock {


    static String TELEGRAM_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

        static final String[] NIFTY_100 = {
                "RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS",
                "SBIN.NS", "BHARTIARTL.NS", "HINDUNILVR.NS", "ITC.NS", "KOTAKBANK.NS",
                "WIPRO.NS", "ULTRACEMCO.NS", "ASIANPAINT.NS", "LT.NS", "MARUTI.NS"
                // Add more if needed
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
            double entry, exit, stoploss, currentPrice;

            StockAnalysis(String symbol, double rsi, double atr, boolean bullish, double entry, double exit, double stoploss, double currentPrice) {
                this.symbol = symbol;
                this.rsi = rsi;
                this.atr = atr;
                this.bullish = bullish;
                this.entry = entry;
                this.exit = exit;
                this.stoploss = stoploss;
                this.currentPrice = currentPrice;
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
                Candle last = candles.get(candles.size() - 1);
                boolean bullish = rsi > 50 && last.close > last.open;

                if (bullish) {
                    double entry = last.close;
                    double stoploss = entry - atr;
                    double exit = entry + (atr * 2);
                    double currentPrice = last.close;
                    bullishStocks.add(new StockAnalysis(symbol, rsi, atr, true, entry, exit, stoploss, currentPrice));
                }

                Thread.sleep(2500); // Avoid rate-limit
            }

            bullishStocks.sort(Comparator.comparingDouble(a -> a.atr));

            if (bullishStocks.isEmpty()) {
                String msg = "⚠️ No stocks found with bullish signals.";
                System.out.println(msg);
                sendTelegramMessage(msg);
                return;
            }

            StringBuilder message = new StringBuilder("📊 *Top 3 Bullish Stocks:*\n");
            for (int i = 0; i < Math.min(3, bullishStocks.size()); i++) {
                StockAnalysis s = bullishStocks.get(i);
                message.append(String.format(
                        "\n🔹 *%s*\nPrice: ₹%.2f\nEntry: ₹%.2f\nExit: ₹%.2f\nStoploss: ₹%.2f\nRSI: %.2f\nATR: %.2f\n",
                        s.symbol, s.currentPrice, s.entry, s.exit, s.stoploss, s.rsi, s.atr
                ));
            }

            StockAnalysis best = bullishStocks.get(0);
            message.append(String.format(
                    "\n🔥 *Best Pick*: %s\nEntry: ₹%.2f\nExit: ₹%.2f\nStoploss: ₹%.2f\n",
                    best.symbol, best.entry, best.exit, best.stoploss
            ));

            System.out.println(message);
            sendTelegramMessage(message.toString());
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

        static void sendTelegramMessage(String message) {
            try {
                String urlString = String.format(
                        "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                        TELEGRAM_TOKEN, TELEGRAM_CHAT_ID, java.net.URLEncoder.encode(message, "UTF-8")
                );
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getInputStream().close(); // Fire and forget
            } catch (Exception e) {
                System.out.println("❌ Telegram message send failed: " + e.getMessage());
            }
        }
    }


