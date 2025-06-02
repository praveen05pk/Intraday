package angelOne;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class AngelOneApiClient {

//    private final String bearerToken;
//    private final String clientCode;
//    private final Map<String, String> symbolTokenMap;
//
//    public AngelOneApiClient(String bearerToken, String clientCode, Map<String, String> symbolTokenMap) {
//        this.bearerToken = bearerToken;
//        this.clientCode = clientCode;
//        this.symbolTokenMap = symbolTokenMap;
//    }
//
//    private static final String CANDLE_URL = "https://smartapi.angelbroking.com/historical/candle-data";
//
//    public List<Candle> getFiveMinCandles(String symbol) {
//        List<Candle> candles = new ArrayList<>();
//        try {
//            String token = symbolTokenMap.get(symbol);
//            if (token == null) {
//                System.err.println("Token not found for symbol: " + symbol);
//                return candles;
//            }
//
//            // IST time
//            ZoneId istZone = ZoneId.of("Asia/Kolkata");
//            ZonedDateTime now = ZonedDateTime.now(istZone);
//            ZonedDateTime start = now.withHour(9).withMinute(15).withSecond(0).withNano(0);
//            ZonedDateTime end = now;
//
//            String fromDate = start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//            String toDate = end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
//
//            JSONObject body = new JSONObject();
//            body.put("exchange", "NSE");
//            body.put("symboltoken", token);
//            body.put("interval", "5min");
//            body.put("fromdate", fromDate);
//            body.put("todate", toDate);
//
//            HttpURLConnection conn = (HttpURLConnection) new URL(CANDLE_URL).openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
//            conn.setRequestProperty("X-ClientCode", clientCode);
//            conn.setDoOutput(true);
//
//            OutputStream os = conn.getOutputStream();
//            os.write(body.toString().getBytes());
//            os.flush();
//            os.close();
//
//            int responseCode = conn.getResponseCode();
//            if (responseCode != 200) {
//                System.err.println("Failed to fetch candles for " + symbol + ": HTTP " + responseCode);
//                return candles;
//            }
//
//            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            StringBuilder jsonString = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                jsonString.append(line);
//            }
//
//            JSONObject response = new JSONObject(jsonString.toString());
//            if (!response.has("data")) {
//                System.err.println("No candle data received for " + symbol);
//                return candles;
//            }
//
//            JSONArray candleData = response.getJSONObject("data").getJSONArray("candles");
//
//            for (int i = 0; i < candleData.length(); i++) {
//                JSONArray row = candleData.getJSONArray(i);
//                String time = row.getString(0);
//                double open = row.getDouble(1);
//                double high = row.getDouble(2);
//                double low = row.getDouble(3);
//                double close = row.getDouble(4);
//                double volume = row.getDouble(5);
//
//                candles.add(new Candle(symbol, time, open, high, low, close, volume));
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return candles;
//    }

        private final SmartConnect smartConnect;
        private final Map<String, String> symbolTokenMap;

        public AngelOneApiClient(SmartConnect smartConnect, Map<String, String> symbolTokenMap) {
            this.smartConnect = smartConnect;
            this.symbolTokenMap = symbolTokenMap;
        }

        public List<Candle> getFiveMinCandles(String symbol) {
            List<Candle> candles = new ArrayList<>();
            try {
                String token = symbolTokenMap.get(symbol);
                if (token == null) {
                    System.err.println("Token not found for symbol: " + symbol);
                    return candles;
                }

                ZoneId istZone = ZoneId.of("Asia/Kolkata");
                ZonedDateTime now = ZonedDateTime.now(istZone);
                ZonedDateTime start = now.withHour(9).withMinute(15).withSecond(0).withNano(0);
                ZonedDateTime end = now;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String fromDate = formatter.format(start);
                String toDate = formatter.format(end);

                HistoricalDataParams params = new HistoricalDataParams();
                params.setExchange("NSE");
                params.setSymbolToken(token);
                params.setInterval("FIVE_MINUTE");
                params.setFrom(fromDate);
                params.setTo(toDate);

                Object responseObj = smartConnect.getHistoricalData(params);

                JSONObject responseJson = new JSONObject(responseObj.toString());
                JSONArray candleArray = responseJson.getJSONObject("data").getJSONArray("candles");

                for (int i = 0; i < candleArray.length(); i++) {
                    JSONArray row = candleArray.getJSONArray(i);
                    String time = row.getString(0);
                    double open = row.getDouble(1);
                    double high = row.getDouble(2);
                    double low = row.getDouble(3);
                    double close = row.getDouble(4);
                    double volume = row.getDouble(5);

                    candles.add(new Candle(symbol, time, open, high, low, close, volume));
                }

            } catch (Exception e) {
                System.err.println("Error fetching candles for " + symbol);
                e.printStackTrace();
            }

            return candles;
        }
    }

}
