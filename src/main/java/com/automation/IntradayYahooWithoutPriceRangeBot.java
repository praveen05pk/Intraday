package com.automation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class IntradayYahooWithoutPriceRangeBot {



    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";


        static final List<String> NIFTY_100 = Arrays.asList(
                "RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS",
                "SBIN.NS", "BHARTIARTL.NS", "HINDUNILVR.NS", "ITC.NS", "KOTAKBANK.NS",
                // Add all 100 NIFTY stock symbols here
                "WIPRO.NS", "ULTRACEMCO.NS", "ASIANPAINT.NS", "LT.NS", "MARUTI.NS"
        );

        public static void main(String[] args) throws Exception {
            List<StockData> analyzedStocks = new ArrayList<>();

            for (String symbol : NIFTY_100) {
                System.out.println("Fetching: " + symbol);
                try {
                    List<Candle> candles = fetchYahooData(symbol);
                    if (candles.size() < 14) {
                        System.out.println("⚠️ Skipped: " + symbol + " (not enough data)");
                        continue;
                    }

                    double lastClose = candles.get(candles.size() - 1).close;

                    double rsi = calculateRSI(candles);
                    double[] macdResult = calculateMACD(candles);
                    double macd = macdResult[0];
                    double signal = macdResult[1];
                    double atr = calculateATR(candles);

                    if (!Double.isNaN(rsi) && !Double.isNaN(macd) && !Double.isNaN(signal) && !Double.isNaN(atr)) {
                        analyzedStocks.add(new StockData(symbol, lastClose, rsi, macd, signal, atr));
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            // Filter bullish signals and sort by safest (lowest ATR)
            List<StockData> topPicks = analyzedStocks.stream()
                    .filter(s -> s.rsi > 50 && s.macd > s.signal)
                    .sorted(Comparator.comparingDouble(s -> s.atr))
                    .limit(3)
                    .toList();

            if (!topPicks.isEmpty()) {
                StringBuilder message = new StringBuilder("📈 Today's Top 3 Intraday Stock Picks (Safest First):\n\n");
                for (StockData s : topPicks) {
                    message.append("🔹 ").append(s.symbol).append("\n")
                            .append("Price: ₹").append(s.price).append("\n")
                            .append("RSI: ").append(String.format("%.2f", s.rsi)).append("\n")
                            .append("MACD: ").append(String.format("%.2f", s.macd)).append("\n")
                            .append("ATR: ").append(String.format("%.2f", s.atr)).append("\n")
                            .append("Entry: ₹").append(String.format("%.2f", s.price)).append("\n")
                            .append("Target: ₹").append(String.format("%.2f", s.price * 1.02)).append("\n")
                            .append("Stoploss: ₹").append(String.format("%.2f", s.price * 0.98)).append("\n\n");
                }
                message.append("🏆 Best Pick: ").append(topPicks.get(0).symbol);

                sendTelegramMessage(message.toString());
                System.out.println("✅ Message sent to Telegram.");
            } else {
                System.out.println("No stocks found with bullish signals.");
            }
        }

        static List<Candle> fetchYahooData(String symbol) throws Exception {
            long end = System.currentTimeMillis() / 1000;
            long start = end - (86400 * 30); // 30 days ago
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?period1=" + start + "&period2=" + end + "&interval=1d";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 429) {
                throw new RuntimeException("Rate limit hit for " + symbol);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonSb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) jsonSb.append(line);
            in.close();

            JSONObject obj = new JSONObject(jsonSb.toString());
            JSONArray timestamps = obj.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONArray("timestamp");
            JSONObject indicators = obj.getJSONObject("chart").getJSONArray("result").getJSONObject(0).getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);

            List<Candle> candles = new ArrayList<>();
            for (int i = 0; i < timestamps.length(); i++) {
                double open = indicators.getJSONArray("open").optDouble(i);
                double high = indicators.getJSONArray("high").optDouble(i);
                double low = indicators.getJSONArray("low").optDouble(i);
                double close = indicators.getJSONArray("close").optDouble(i);
                if (Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) continue;
                candles.add(new Candle(open, high, low, close));
            }

            return candles;
        }

        static double calculateRSI(List<Candle> candles) {
            int period = 14;
            if (candles.size() <= period) return Double.NaN;
            double gain = 0, loss = 0;
            for (int i = candles.size() - period; i < candles.size(); i++) {
                double change = candles.get(i).close - candles.get(i - 1).close;
                if (change >= 0) gain += change;
                else loss -= change;
            }
            double rs = gain / (loss == 0 ? 1 : loss);
            return 100 - (100 / (1 + rs));
        }

        static double[] calculateMACD(List<Candle> candles) {
            List<Double> closePrices = new ArrayList<>();
            for (Candle c : candles) closePrices.add(c.close);
            double ema12 = calculateEMA(closePrices, 12);
            double ema26 = calculateEMA(closePrices, 26);
            double macd = ema12 - ema26;
            double signal = calculateEMA(Collections.singletonList(macd), 9);
            return new double[]{macd, signal};
        }

        static double calculateEMA(List<Double> prices, int period) {
            if (prices.size() < period) return Double.NaN;
            double multiplier = 2.0 / (period + 1);
            double ema = prices.get(prices.size() - period);
            for (int i = prices.size() - period + 1; i < prices.size(); i++) {
                ema = (prices.get(i) - ema) * multiplier + ema;
            }
            return ema;
        }

        static double calculateATR(List<Candle> candles) {
            int period = 14;
            if (candles.size() <= period) return Double.NaN;
            double atr = 0;
            for (int i = candles.size() - period + 1; i < candles.size(); i++) {
                Candle c = candles.get(i);
                Candle prev = candles.get(i - 1);
                double tr = Math.max(c.high - c.low,
                        Math.max(Math.abs(c.high - prev.close), Math.abs(c.low - prev.close)));
                atr += tr;
            }
            return atr / period;
        }

        static void sendTelegramMessage(String msg) throws Exception {
            String encodedMsg = URLEncoder.encode(msg, "UTF-8");
            String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encodedMsg;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while (in.readLine() != null);
            in.close();
        }

        static class Candle {
            double open, high, low, close;
            Candle(double open, double high, double low, double close) {
                this.open = open; this.high = high; this.low = low; this.close = close;
            }
        }

        static class StockData {
            String symbol;
            double price, rsi, macd, signal, atr;

            StockData(String symbol, double price, double rsi, double macd, double signal, double atr) {
                this.symbol = symbol; this.price = price; this.rsi = rsi; this.macd = macd; this.signal = signal; this.atr = atr;
            }
        }
    }

