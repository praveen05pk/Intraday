package com.automation;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class IntradayBotTwelveData {
    static String TWELVE_DATA_API_KEY = "73c17ea99c8c4814a7f215c944fe12e7";
    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";


    static String[] stockSymbols = {
            "RELIANCE.NSE"
    }; // Nifty 20

//    static String[] stockSymbols = {
//            "RELIANCE.NSE", "TCS.NSE", "INFY.NSE", "HDFCBANK.NSE", "ICICIBANK.NSE",
//            "WIPRO.NSE", "SBIN.NSE", "HCLTECH.NSE", "KOTAKBANK.NSE", "LT.NSE",
//            "AXISBANK.NSE", "BAJFINANCE.NSE", "HINDUNILVR.NSE", "ITC.NSE", "SUNPHARMA.NSE",
//            "POWERGRID.NSE", "NESTLEIND.NSE", "ADANIPORTS.NSE", "ASIANPAINT.NSE", "TITAN.NSE"
//    }; // Nifty 20


    public static void main(String[] args) throws Exception {
        List<StockScore> scoredStocks = new ArrayList<>();

        for (String symbol : stockSymbols) {
            TechnicalData data = fetchTechnicalIndicators(symbol);
            if (data != null && !data.isIncomplete()) {
                double score = calculateScore(data);
                scoredStocks.add(new StockScore(symbol, data, score));
                System.out.println("✅ " + symbol + " => Score: " + score);
            } else {
                System.out.println("⚠️ Skipped: " + symbol + " (missing data)");
            }
            Thread.sleep(1000); // avoid rate limiting
        }

        // Sort descending by score
        scoredStocks.sort((a, b) -> Double.compare(b.score, a.score));

        List<StockScore> top3 = scoredStocks.size() >= 3 ? scoredStocks.subList(0, 3) : scoredStocks;
        StockScore bestStock = top3.size() > 0 ? top3.get(0) : null;

        StringBuilder message = new StringBuilder();
        message.append("📈 *Today's Best Intraday Picks:*\n\n");
        int rank = 1;
        for (StockScore s : top3) {
            message.append(rank++).append(". ")
                    .append(s.symbol).append("\n")
                    .append("ATR: ").append(s.data.atr).append("\n")
                    .append("RSI: ").append(s.data.rsi).append("\n")
                    .append("MACD: ").append(s.data.macd).append("\n")
                    .append("Entry: ").append("CMP").append("\n")
                    .append("Exit: ").append("CMP + ATR").append("\n")
                    .append("StopLoss: ").append("CMP - ATR").append("\n\n");
        }
        if (bestStock != null) {
            message.append("🔥 *Recommended Best Pick:* ").append(bestStock.symbol);
        } else {
            message.append("No stocks met criteria today.");
        }

        sendTelegramMessage(message.toString());
        System.out.println("✅ Message sent to Telegram.");
    }

    static TechnicalData fetchTechnicalIndicators(String symbol) {
        try {
            double atr = fetchValue(symbol, "atr", "value");
            double rsi = fetchValue(symbol, "rsi", "value");
            double macd = fetchValue(symbol, "macd", "macd");
            if (Double.isNaN(atr) || Double.isNaN(rsi) || Double.isNaN(macd)) return null;
            return new TechnicalData(atr, rsi, macd);
        } catch (Exception e) {
            System.err.println("Fetch error for " + symbol + ": " + e.getMessage());
            return null;
        }
    }

    static double fetchValue(String symbol, String indicator, String key) {
        try {
            String url = "https://api.twelvedata.com/" + indicator +
                    "?symbol=" + symbol + "&interval=1day&apikey=" + TWELVE_DATA_API_KEY;
            JSONObject json = readJsonFromUrl(url);
            if (json.has(key)) return Double.parseDouble(json.getString(key));
            else if (json.has("values")) { // sometimes values is an array
                return Double.parseDouble(json.getJSONArray("values").getJSONObject(0).getString(key));
            } else {
                System.err.println(indicator + " fetch error: " + json);
                return Double.NaN;
            }
        } catch (Exception e) {
            System.err.println(indicator + " exception: " + e.getMessage());
            return Double.NaN;
        }
    }

    static JSONObject readJsonFromUrl(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        return new JSONObject(sb.toString());
    }

    static double calculateScore(TechnicalData data) {
        double rsiScore = (data.rsi >= 40 && data.rsi <= 60) ? 1 : 0; // prefer neutral RSI
        double macdScore = data.macd > 0 ? 1 : 0; // bullish macd
        double atrScore = 1 / (1 + data.atr); // prefer lower ATR
        return rsiScore + macdScore + atrScore;
    }

    static void sendTelegramMessage(String msg) throws Exception {
        String encodedMsg = URLEncoder.encode(msg, "UTF-8");
        String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN +
                "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&parse_mode=Markdown&text=" + encodedMsg;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while (in.readLine() != null) {}
        in.close();
    }

    static class TechnicalData {
        double atr, rsi, macd;
        TechnicalData(double atr, double rsi, double macd) {
            this.atr = atr; this.rsi = rsi; this.macd = macd;
        }
        boolean isIncomplete() {
            return Double.isNaN(atr) || Double.isNaN(rsi) || Double.isNaN(macd);
        }
    }

    static class StockScore {
        String symbol;
        TechnicalData data;
        double score;
        StockScore(String symbol, TechnicalData data, double score) {
            this.symbol = symbol; this.data = data; this.score = score;
        }
    }
}
