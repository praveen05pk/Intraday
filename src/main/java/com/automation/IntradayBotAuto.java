package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class IntradayBotAuto {

    static String ALPHA_VANTAGE_API_KEY = "8RUNEM31A7UT0TL9";
    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

//    static String[] stockSymbols = {"RELIANCE.BSE", "TCS.BSE", "INFY.BSE", "HDFCBANK.BSE", "ICICIBANK.BSE",
//            "LT.BSE", "KOTAKBANK.BSE", "SBIN.BSE", "HINDUNILVR.BSE", "BAJFINANCE.BSE",
//            "BHARTIARTL.BSE", "ASIANPAINT.BSE", "AXISBANK.BSE", "MARUTI.BSE", "ULTRACEMCO.BSE",
//            "SUNPHARMA.BSE", "TITAN.BSE", "NTPC.BSE", "POWERGRID.BSE", "WIPRO.BSE"};

    static String[] stockSymbols = {"AAPL,MSFT"};

    public static void main(String[] args) throws Exception {
        List<StockMetrics> stockList = new ArrayList<>();

        for (String symbol : stockSymbols) {
            System.out.println("Fetching data for: " + symbol);
            Double price = fetchPrice(symbol);
            Double rsi = fetchRSI(symbol);
            Double macd = fetchMACD(symbol);
            Double atr = fetchATR(symbol);

            if (price != null && rsi != null && macd != null && atr != null) {
                stockList.add(new StockMetrics(symbol, price, rsi, macd, atr));
            } else {
                System.out.println("Skipped: " + symbol + " (missing data)");
            }
            Thread.sleep(15000); // 15s delay per API call to stay under free plan limit
        }

        // Filter stocks with RSI <70 and MACD > 0
        List<StockMetrics> shortlisted = new ArrayList<>();
        for (StockMetrics sm : stockList) {
            if (sm.rsi < 70 && sm.macd > 0) {
                shortlisted.add(sm);
            }
        }

        // Sort by lowest ATR
        shortlisted.sort(Comparator.comparingDouble(sm -> sm.atr));

        // Pick top 3
        List<StockMetrics> top3 = shortlisted.size() >= 3 ? shortlisted.subList(0, 3) : shortlisted;

        // Find best among 3 (highest MACD)
        StockMetrics bestStock = null;
        for (StockMetrics sm : top3) {
            if (bestStock == null || sm.macd > bestStock.macd) {
                bestStock = sm;
            }
        }

        // Build message
        StringBuilder message = new StringBuilder("Today's Intraday Picks:\n\n");
        int count = 1;
        for (StockMetrics sm : top3) {
            double target = sm.price + sm.atr * 1.5;
            double stoploss = sm.price - sm.atr;
            message.append(count).append("️⃣ ").append(sm.symbol).append("\n");
            message.append("Entry: ₹").append(String.format("%.2f", sm.price)).append("\n");
            message.append("Target: ₹").append(String.format("%.2f", target)).append("\n");
            message.append("Stop Loss: ₹").append(String.format("%.2f", stoploss)).append("\n\n");
            count++;
        }

        if (bestStock != null) {
            message.append("🏆 Best Pick: ").append(bestStock.symbol);
        } else {
            message.append("⚠️ No strong picks today.");
        }

        sendTelegramMessage(message.toString());
        System.out.println("Message sent:\n" + message);
    }

    static Double fetchPrice(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            JSONObject json = getJson(url);
            JSONObject quote = json.getJSONObject("Global Quote");
            return Double.parseDouble(quote.getString("05. price"));
        } catch (Exception e) {
            System.err.println("Price fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static Double fetchRSI(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=RSI&symbol=" + symbol + "&interval=15min&time_period=14&series_type=close&apikey=" + ALPHA_VANTAGE_API_KEY;
            JSONObject json = getJson(url);
            JSONObject technicals = json.getJSONObject("Technical Analysis: RSI");
            String lastKey = technicals.keys().next();
            return Double.parseDouble(technicals.getJSONObject(lastKey).getString("RSI"));
        } catch (Exception e) {
            System.err.println("RSI fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static Double fetchMACD(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=MACD&symbol=" + symbol + "&interval=15min&series_type=close&apikey=" + ALPHA_VANTAGE_API_KEY;
            JSONObject json = getJson(url);
            JSONObject technicals = json.getJSONObject("Technical Analysis: MACD");
            String lastKey = technicals.keys().next();
            return Double.parseDouble(technicals.getJSONObject(lastKey).getString("MACD"));
        } catch (Exception e) {
            System.err.println("MACD fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static Double fetchATR(String symbol) {
        try {
            String url = "https://www.alphavantage.co/query?function=ATR&symbol=" + symbol + "&interval=15min&time_period=14&apikey=" + ALPHA_VANTAGE_API_KEY;
            JSONObject json = getJson(url);
            JSONObject technicals = json.getJSONObject("Technical Analysis: ATR");
            String lastKey = technicals.keys().next();
            return Double.parseDouble(technicals.getJSONObject(lastKey).getString("ATR"));
        } catch (Exception e) {
            System.err.println("ATR fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static JSONObject getJson(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return new JSONObject(sb.toString());
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

    static class StockMetrics {
        String symbol;
        double price, rsi, macd, atr;
        StockMetrics(String symbol, double price, double rsi, double macd, double atr) {
            this.symbol = symbol;
            this.price = price;
            this.rsi = rsi;
            this.macd = macd;
            this.atr = atr;
        }
    }
}
