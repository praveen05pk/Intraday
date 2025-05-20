package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class IntradayYahooHoliday {


    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

    public static void main(String[] args) throws Exception {
        String[] stockSymbols = {"TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS", "RELIANCE.NS"};

        List<StockScore> scoredStocks = new ArrayList<>();

        for (String symbol : stockSymbols) {
            System.out.println("Fetching data for: " + symbol);
            StockData data = fetchStockDataYahoo(symbol);

            if (data == null) {
                System.out.println("Skipped: " + symbol + " (not enough data or fetch error)");
                continue;
            }

            // Simple scoring: prefer higher price increase and lower ATR
            double score = (data.priceChangePercent * 2) - data.atr;

            scoredStocks.add(new StockScore(symbol, data, score));
            Thread.sleep(1500); // avoid rate limits
        }

        if (scoredStocks.isEmpty()) {
            System.out.println("No stocks found with sufficient data.");
            return;
        }

        // Sort descending by score
        scoredStocks.sort((a, b) -> Double.compare(b.score, a.score));

        List<StockScore> top3 = scoredStocks.subList(0, Math.min(3, scoredStocks.size()));

        // Build Telegram message
        StringBuilder msg = new StringBuilder("📈 *Today's Top 3 Stock Picks (Yahoo Finance)*:\n\n");
        for (int i = 0; i < top3.size(); i++) {
            StockScore s = top3.get(i);
            msg.append(i + 1).append(". ").append(s.symbol).append("\n")
                    .append("   Price: ").append(s.data.currentPrice).append("\n")
                    .append("   Change: ").append(String.format("%.2f%%", s.data.priceChangePercent)).append("\n")
                    .append("   ATR: ").append(String.format("%.2f", s.data.atr)).append("\n")
                    .append("   Entry: ").append(String.format("%.2f", s.data.currentPrice)).append("\n")
                    .append("   Target: ").append(String.format("%.2f", s.data.currentPrice * 1.01)).append("\n")
                    .append("   Stoploss: ").append(String.format("%.2f", s.data.currentPrice * 0.99)).append("\n\n");
        }

        msg.append("✅ Best pick: ").append(top3.get(0).symbol);

        sendTelegramMessage(msg.toString());
        System.out.println("Sent message:\n" + msg);
    }

    static StockData fetchStockDataYahoo(String symbol) {
        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?interval=1d&range=1mo";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonSb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) jsonSb.append(line);
            reader.close();

            JSONObject json = new JSONObject(jsonSb.toString());
            JSONObject chart = json.getJSONObject("chart");
            JSONArray resultArr = chart.getJSONArray("result");
            if (resultArr.length() == 0) return null;

            JSONObject result = resultArr.getJSONObject(0);
            JSONArray timestamps = result.getJSONArray("timestamp");
            JSONObject indicators = result.getJSONObject("indicators");
            JSONObject quote = indicators.getJSONArray("quote").getJSONObject(0);
            JSONArray closes = quote.getJSONArray("close");

            if (closes.length() < 10) return null; // need at least 10 candles

            // Get last valid price (skip nulls)
            Double lastClose = null;
            for (int i = closes.length() - 1; i >= 0; i--) {
                if (!closes.isNull(i)) {
                    lastClose = closes.getDouble(i);
                    break;
                }
            }
            if (lastClose == null) return null;

            // Calculate price change %
            double prevClose = closes.getDouble(closes.length() - 2);
            double priceChangePercent = ((lastClose - prevClose) / prevClose) * 100;

            // Simple ATR (average of last 5 candle ranges)
            JSONArray highs = indicators.getJSONArray("quote").getJSONObject(0).getJSONArray("high");
            JSONArray lows = indicators.getJSONArray("quote").getJSONObject(0).getJSONArray("low");
            List<Double> ranges = new ArrayList<>();
            for (int i = closes.length() - 5; i < closes.length(); i++) {
                if (!highs.isNull(i) && !lows.isNull(i)) {
                    ranges.add(highs.getDouble(i) - lows.getDouble(i));
                }
            }
            double atr = ranges.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            return new StockData(lastClose, priceChangePercent, atr);

        } catch (Exception e) {
            System.out.println("Fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static void sendTelegramMessage(String msg) throws Exception {
        String encodedMsg = java.net.URLEncoder.encode(msg, "UTF-8");
        String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encodedMsg + "&parse_mode=Markdown";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while (in.readLine() != null) {}
        in.close();
    }

    static class StockData {
        double currentPrice;
        double priceChangePercent;
        double atr;
        StockData(double price, double changePct, double atr) {
            this.currentPrice = price;
            this.priceChangePercent = changePct;
            this.atr = atr;
        }
    }

    static class StockScore {
        String symbol;
        StockData data;
        double score;
        StockScore(String symbol, StockData data, double score) {
            this.symbol = symbol;
            this.data = data;
            this.score = score;
        }
    }
}
