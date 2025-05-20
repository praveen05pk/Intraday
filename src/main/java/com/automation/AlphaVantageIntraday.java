package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;


public class AlphaVantageIntraday {


        static final String ALPHA_VANTAGE_API_KEY = "8RUNEM31A7UT0TL9";
    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

        static final String[] NIFTY_100 = {
                "RELIANCE.BSE", "TCS.BSE", "INFY.BSE", "HDFCBANK.BSE", "ICICIBANK.BSE",
                "SBIN.BSE", "BHARTIARTL.BSE", "HINDUNILVR.BSE", "ITC.BSE", "KOTAKBANK.BSE",
                "WIPRO.BSE", "ULTRACEMCO.BSE", "ASIANPAINT.BSE", "LT.BSE", "MARUTI.BSE"
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
            double entry, exit, stoploss, price;

            StockAnalysis(String symbol, double rsi, double atr, boolean bullish, double entry, double exit, double stoploss, double price) {
                this.symbol = symbol;
                this.rsi = rsi;
                this.atr = atr;
                this.bullish = bullish;
                this.entry = entry;
                this.exit = exit;
                this.stoploss = stoploss;
                this.price = price;
            }
        }

        public static void main(String[] args) throws Exception {
            List<StockAnalysis> bullishStocks = new ArrayList<>();

            for (String symbol : NIFTY_100) {
                System.out.println("Fetching: " + symbol);
                List<Candle> candles = fetchIntradayCandles(symbol);
                if (candles == null || candles.size() < 15) {
                    System.out.println("⚠️ Skipped: " + symbol + " (not enough data)");
                    continue;
                }

                double rsi = calculateRSI(candles);
                double atr = calculateATR(candles);
                Candle latest = candles.get(candles.size() - 1);
                boolean bullish = rsi > 50 && latest.close > latest.open;

                if (bullish) {
                    double entry = latest.close;
                    double stoploss = entry - atr;
                    double exit = entry + (atr * 2);
                    bullishStocks.add(new StockAnalysis(symbol, rsi, atr, true, entry, exit, stoploss, latest.close));
                }

                Thread.sleep(15000); // Alpha Vantage free tier: 5 requests/min
            }

            bullishStocks.sort(Comparator.comparingDouble(a -> a.atr));

            if (bullishStocks.isEmpty()) {
                sendTelegram("No bullish stocks found today.");
                return;
            }

            StringBuilder message = new StringBuilder("📊 *Top 3 Bullish Stocks:*\n");
            for (int i = 0; i < Math.min(3, bullishStocks.size()); i++) {
                StockAnalysis s = bullishStocks.get(i);
                message.append(String.format(
                        "🔹 *%s*\nPrice: ₹%.2f\nEntry: ₹%.2f\nExit: ₹%.2f\nStoploss: ₹%.2f\nRSI: %.2f\nATR: %.2f\n\n",
                        s.symbol, s.price, s.entry, s.exit, s.stoploss, s.rsi, s.atr
                ));
            }

            StockAnalysis best = bullishStocks.get(0);
            message.append(String.format(
                    "🔥 *Best Pick*: %s\nEntry: ₹%.2f\nExit: ₹%.2f\nStoploss: ₹%.2f",
                    best.symbol, best.entry, best.exit, best.stoploss
            ));

            sendTelegram(message.toString());
        }

        static List<Candle> fetchIntradayCandles(String symbol) {
            try {
                String function = "TIME_SERIES_INTRADAY";
                String interval = "5min";
                String urlStr = String.format(
                        "https://www.alphavantage.co/query?function=%s&symbol=%s&interval=%s&apikey=%s&outputsize=compact",
                        function, symbol, interval, ALPHA_VANTAGE_API_KEY
                );

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    json.append(line);
                }
                in.close();

                JSONObject response = new JSONObject(json.toString());
                if (!response.has("Time Series (5min)")) return null;

                JSONObject timeSeries = response.getJSONObject("Time Series (5min)");
                List<Candle> candles = new ArrayList<>();

                for (String timestamp : timeSeries.keySet()) {
                    JSONObject ohlc = timeSeries.getJSONObject(timestamp);
                    candles.add(new Candle(
                            ohlc.getDouble("1. open"),
                            ohlc.getDouble("2. high"),
                            ohlc.getDouble("3. low"),
                            ohlc.getDouble("4. close")
                    ));
                }

                Collections.reverse(candles); // oldest to latest
                return candles;

            } catch (Exception e) {
                System.out.println("Error fetching " + symbol + ": " + e.getMessage());
                return null;
            }
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
                double tr = Math.max(c.high - c.low, Math.max(Math.abs(c.high - prev.close), Math.abs(c.low - prev.close)));
                atr += tr;
            }
            return atr / (candles.size() - 1);
        }

        static void sendTelegram(String message) {
            try {
                String urlStr = String.format("https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=Markdown",
                        TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID, URLEncoder.encode(message, "UTF-8"));
                new URL(urlStr).openStream().close();
            } catch (Exception e) {
                System.out.println("Telegram error: " + e.getMessage());
            }
        }
    }


