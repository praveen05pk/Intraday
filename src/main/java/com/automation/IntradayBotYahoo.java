package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class IntradayBotYahoo {

    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

    static String[] stockSymbols = {"RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS", "WIPRO.NS", "KOTAKBANK.NS", "LT.NS", "AXISBANK.NS", "SBIN.NS"};

    public static void main(String[] args) throws Exception {
        List<StockInfo> stockInfos = new ArrayList<>();

        for (String symbol : stockSymbols) {
            try {
                List<Double> closes = fetchClosePrices(symbol);
                List<Double> highs = fetchHighPrices(symbol);
                List<Double> lows = fetchLowPrices(symbol);

                if (closes.size() < 15) {
                    System.out.println("Skipped: " + symbol + " (not enough data)");
                    continue;
                }

                double rsi = calculateRSI(closes);
                double macd = calculateMACD(closes);
                double atr = calculateATR(highs, lows, closes);

                stockInfos.add(new StockInfo(symbol, rsi, macd, atr));

                System.out.println("Fetched: " + symbol + " | RSI: " + rsi + " | MACD: " + macd + " | ATR: " + atr);
                Thread.sleep(1500); // avoid hitting rate limit
            } catch (Exception e) {
                System.out.println("Error processing " + symbol + ": " + e.getMessage());
            }
        }

        // Rank stocks: prioritize RSI < 70, MACD > 0, and lower ATR
        stockInfos.sort(Comparator.comparing((StockInfo s) -> Math.abs(50 - s.rsi)) // RSI near 50
                .thenComparing(s -> -s.macd) // higher MACD
                .thenComparing(s -> s.atr)); // lower ATR

        List<StockInfo> top3 = stockInfos.stream().limit(3).toList();
        StockInfo best = top3.get(0);

        StringBuilder msg = new StringBuilder("Today's Best Intraday Picks:\n\n");
        for (int i = 0; i < top3.size(); i++) {
            StockInfo s = top3.get(i);
            double entry = fetchLastClosePrice(s.symbol);
            double stopLoss = entry - (s.atr * 1.5);
            double target = entry + (s.atr * 2);
            msg.append((i + 1) + ". " + s.symbol + "\n")
                    .append("Entry: ").append(String.format("%.2f", entry)).append("\n")
                    .append("Target: ").append(String.format("%.2f", target)).append("\n")
                    .append("StopLoss: ").append(String.format("%.2f", stopLoss)).append("\n\n");
        }

        msg.append("**Best Pick: ").append(best.symbol).append("**");

        sendTelegramMessage(msg.toString());
        System.out.println("Message sent:\n" + msg);
    }

    static JSONObject fetchYahooData(String symbol) throws Exception {
        long now = System.currentTimeMillis() / 1000;
        long past = now - (60 * 60 * 24 * 15);
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&period1=" + past + "&period2=" + now;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line; while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return new JSONObject(sb.toString());
    }

    static List<Double> fetchClosePrices(String symbol) throws Exception {
        JSONObject json = fetchYahooData(symbol);
        JSONArray arr = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                .getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) closes.add(arr.optDouble(i, Double.NaN));
        closes.removeIf(x -> Double.isNaN(x));
        return closes;
    }

    static List<Double> fetchHighPrices(String symbol) throws Exception {
        JSONObject json = fetchYahooData(symbol);
        JSONArray arr = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                .getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("high");
        List<Double> highs = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) highs.add(arr.optDouble(i, Double.NaN));
        highs.removeIf(x -> Double.isNaN(x));
        return highs;
    }

    static List<Double> fetchLowPrices(String symbol) throws Exception {
        JSONObject json = fetchYahooData(symbol);
        JSONArray arr = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                .getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("low");
        List<Double> lows = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) lows.add(arr.optDouble(i, Double.NaN));
        lows.removeIf(x -> Double.isNaN(x));
        return lows;
    }

    static double fetchLastClosePrice(String symbol) throws Exception {
        List<Double> closes = fetchClosePrices(symbol);
        return closes.get(closes.size() - 1);
    }

    static double calculateRSI(List<Double> closes) {
        int period = 14;
        if (closes.size() <= period) return 50;
        double gain = 0, loss = 0;
        for (int i = closes.size() - period; i < closes.size() - 1; i++) {
            double change = closes.get(i + 1) - closes.get(i);
            if (change > 0) gain += change;
            else loss -= change;
        }
        double rs = (loss == 0) ? 100 : gain / loss;
        return 100 - (100 / (1 + rs));
    }

    static double calculateMACD(List<Double> closes) {
        double ema12 = ema(closes, 12);
        double ema26 = ema(closes, 26);
        return ema12 - ema26;
    }

    static double ema(List<Double> prices, int period) {
        if (prices.size() < period) return prices.get(prices.size() - 1);
        double multiplier = 2.0 / (period + 1);
        double ema = prices.get(prices.size() - period);
        for (int i = prices.size() - period + 1; i < prices.size(); i++) {
            ema = ((prices.get(i) - ema) * multiplier) + ema;
        }
        return ema;
    }

    static double calculateATR(List<Double> highs, List<Double> lows, List<Double> closes) {
        int period = 14;
        List<Double> trs = new ArrayList<>();
        for (int i = 1; i < highs.size(); i++) {
            double tr = Math.max(highs.get(i) - lows.get(i), Math.max(Math.abs(highs.get(i) - closes.get(i - 1)), Math.abs(lows.get(i) - closes.get(i - 1))));
            trs.add(tr);
        }
        return trs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    static void sendTelegramMessage(String msg) throws Exception {
        String encoded = URLEncoder.encode(msg, "UTF-8");
        String url = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encoded + "&parse_mode=Markdown";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        new BufferedReader(new InputStreamReader(conn.getInputStream())).close();
    }

    static class StockInfo {
        String symbol;
        double rsi;
        double macd;
        double atr;
        StockInfo(String s, double r, double m, double a) { symbol = s; rsi = r; macd = m; atr = a; }
    }
}
