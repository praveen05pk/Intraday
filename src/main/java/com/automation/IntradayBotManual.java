package com.automation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class IntradayBotManual {

    // Replace with your tokens
    static String FINNHUB_API_KEY = "d0ahu29r01qm3l9lhmngd0ahu29r01qm3l9lhmo0";
    static String TELEGRAM_BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    static String TELEGRAM_CHAT_ID = "1230484573";

    public static void main(String[] args) throws Exception {
//        String[] stockSymbols = {"RELIANCE.NS", "TCS.NS", "INFY.NS", "HDFCBANK.NS", "ICICIBANK.NS"}; // sample Indian stocks
        String[] stockSymbols = {"AAPL", "MSFT"};
        StockInfo bestStock = null;

        for (String symbol : stockSymbols) {
            StockInfo info = fetchRecommendation(symbol);
            if (info != null) {
                System.out.println("Fetched: " + info);
                if (bestStock == null || info.buy > bestStock.buy) {
                    bestStock = info;
                }
            }
            Thread.sleep(1000); // avoid rate limit
        }

        if (bestStock != null) {
            String message = "Today's Best Intraday Stock Recommendation:\n"
                    + "Stock: " + bestStock.symbol + "\n"
                    + "Buy Recommendations: " + bestStock.buy + "\n"
                    + "Hold Recommendations: " + bestStock.hold + "\n"
                    + "Sell Recommendations: " + bestStock.sell + "\n"
                    + "Strong Buy: " + bestStock.strongBuy + "\n"
                    + "Strong Sell: " + bestStock.strongSell;

            sendTelegramMessage(message);
            System.out.println("Message sent to Telegram:\n" + message);
        } else {
            System.out.println("No recommendations found.");
        }
    }

    static StockInfo fetchRecommendation(String symbol) {
        try {
            String apiUrl = "https://finnhub.io/api/v1/stock/recommendation?symbol=" + symbol + "&token=" + FINNHUB_API_KEY;
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonSb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonSb.append(line);
            }
            reader.close();

            JSONArray arr = new JSONArray(jsonSb.toString());
            if (arr.length() > 0) {
                JSONObject latest = arr.getJSONObject(0); // get latest recommendation
                return new StockInfo(
                        symbol,
                        latest.getInt("buy"),
                        latest.getInt("hold"),
                        latest.getInt("sell"),
                        latest.getInt("strongBuy"),
                        latest.getInt("strongSell")
                );
            }
        } catch (Exception e) {
            System.err.println("Error fetching recommendation for " + symbol + ": " + e.getMessage());
        }
        return null;
    }

//    static void sendTelegramMessage(String msg) throws Exception {
//        String encodedMsg = URLEncoder.encode(msg, "UTF-8");
//        String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN + "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encodedMsg;
//
//        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
//        conn.setRequestMethod("GET");
//        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        String line;
//        while ((line = in.readLine()) != null) {
//            System.out.println(line);
//        }
//        in.close();
//    }
static void sendTelegramMessage(String msg) throws Exception {
    String encodedMsg = URLEncoder.encode(msg, "UTF-8");
    String urlStr = "https://api.telegram.org/bot" + TELEGRAM_BOT_TOKEN +
            "/sendMessage?chat_id=" + TELEGRAM_CHAT_ID + "&text=" + encodedMsg;

    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
    conn.setRequestMethod("GET");

    int responseCode = conn.getResponseCode();
    System.out.println("Telegram Response Code: " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    while ((line = in.readLine()) != null) {
        System.out.println(line);
    }
    in.close();
}


    static class StockInfo {
        String symbol;
        int buy;
        int hold;
        int sell;
        int strongBuy;
        int strongSell;

        public StockInfo(String symbol, int buy, int hold, int sell, int strongBuy, int strongSell) {
            this.symbol = symbol;
            this.buy = buy;
            this.hold = hold;
            this.sell = sell;
            this.strongBuy = strongBuy;
            this.strongSell = strongSell;
        }

        @Override
        public String toString() {
            return symbol + " - Buy: " + buy + ", Hold: " + hold + ", Sell: " + sell;
        }
    }
}
