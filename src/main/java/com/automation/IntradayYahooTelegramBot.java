package com.automation;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class IntradayYahooTelegramBot {

    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";


    static final List<String> NIFTY_100 = Arrays.asList(
            "RELIANCE.NS");
//    , "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS",
//            "SBIN.NS", "HINDUNILVR.NS", "KOTAKBANK.NS", "ITC.NS", "BHARTIARTL.NS",
//            // ... Add remaining NIFTY 100 symbols here
//            "WIPRO.NS", "ASIANPAINT.NS", "ULTRACEMCO.NS", "MARUTI.NS", "AXISBANK.NS"
//    );

    static class Candle {
        double open, high, low, close;
        String date;
    }

    static class StockStats {
        String symbol;
        double latestPrice;
        double atr;
        double rsi;
        double macd;

        public StockStats(String symbol, double price, double atr, double rsi, double macd) {
            this.symbol = symbol;
            this.latestPrice = price;
            this.atr = atr;
            this.rsi = rsi;
            this.macd = macd;
        }
    }

    public static void main(String[] args) throws Exception {
        List<StockStats> result = new ArrayList<>();

        for (String symbol : NIFTY_100) {
            try {
                System.out.println("Fetching: " + symbol);
                List<Candle> candles = fetchYahooCandles(symbol);
                if (candles == null || candles.size() < 14) {
                    System.out.println("\u26A0\uFE0F Skipped: " + symbol + " (not enough data)");
                    continue;
                }
                double latestPrice = candles.get(candles.size() - 1).close;
                if (latestPrice < 100 || latestPrice > 150) continue;

                double atr = calculateATR(candles);
                double rsi = calculateRSI(candles);
                double macd = calculateMACD(candles);

                result.add(new StockStats(symbol, latestPrice, atr, rsi, macd));
                Thread.sleep(2000); // rate limit protection
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        List<StockStats> top3 = result.stream()
                .sorted(Comparator.comparingDouble(s -> s.atr))
                .limit(3)
                .collect(Collectors.toList());

        if (top3.isEmpty()) {
            System.out.println("No stocks found in price range with sufficient data.");
            return;
        }

        StringBuilder message = new StringBuilder("\uD83D\uDCC8 Top 3 Intraday Picks (Price: ₹100-₹150):\n\n");
        for (StockStats s : top3) {
            message.append(String.format("%s\nPrice: ₹%.2f\nATR: %.2f\nRSI: %.2f\nMACD: %.2f\n\n",
                    s.symbol, s.latestPrice, s.atr, s.rsi, s.macd));
        }

        message.append("\u2728 Best Pick: ").append(top3.get(0).symbol).append("\nSuggested Entry: ₹")
                .append(String.format("%.2f", top3.get(0).latestPrice)).append("\nExit: ₹")
                .append(String.format("%.2f", top3.get(0).latestPrice * 1.02)).append("\nStop-Loss: ₹")
                .append(String.format("%.2f", top3.get(0).latestPrice * 0.98));

        sendTelegramMessage(message.toString());
        System.out.println("\u2705 Message sent to Telegram.");
    }

    static List<Candle> fetchYahooCandles(String symbol) throws Exception {
        String urlStr = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol +
                "?range=15d&interval=1d";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        JSONObject json = new JSONObject(sb.toString());
        JSONArray timestamps = json.getJSONObject("chart").getJSONArray("result")
                .getJSONObject(0).getJSONArray("timestamp");
        JSONObject indicators = json.getJSONObject("chart").getJSONArray("result")
                .getJSONObject(0).getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);

        JSONArray opens = indicators.getJSONArray("open");
        JSONArray highs = indicators.getJSONArray("high");
        JSONArray lows = indicators.getJSONArray("low");
        JSONArray closes = indicators.getJSONArray("close");

        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < closes.length(); i++) {
            if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i)) continue;
            Candle c = new Candle();
            c.open = opens.getDouble(i);
            c.high = highs.getDouble(i);
            c.low = lows.getDouble(i);
            c.close = closes.getDouble(i);
            candles.add(c);
        }
        return candles;
    }

    static double calculateATR(List<Candle> candles) {
        List<Double> trs = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);
            double tr = Math.max(curr.high - curr.low,
                    Math.max(Math.abs(curr.high - prev.close), Math.abs(curr.low - prev.close)));
            trs.add(tr);
        }
        return trs.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
    }

    static double calculateRSI(List<Candle> candles) {
        double gain = 0, loss = 0;
        for (int i = 1; i < 15; i++) {
            double diff = candles.get(i).close - candles.get(i - 1).close;
            if (diff > 0) gain += diff;
            else loss -= diff;
        }
        if (gain + loss == 0) return 50;
        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }

    static double calculateMACD(List<Candle> candles) {
        double ema12 = candles.get(0).close;
        double ema26 = candles.get(0).close;
        for (int i = 1; i < candles.size(); i++) {
            double close = candles.get(i).close;
            ema12 = close * (2.0 / 13) + ema12 * (1 - 2.0 / 13);
            ema26 = close * (2.0 / 27) + ema26 * (1 - 2.0 / 27);
        }
        return ema12 - ema26;
    }

    static void sendTelegramMessage(String msg) throws Exception {
        String encodedMsg = URLEncoder.encode(msg, "UTF-8");
        String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN +
                "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encodedMsg;

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while (in.readLine() != null) {}
        in.close();
    }
}
