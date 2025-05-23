package org.automation;

import java.net.URI;
import java.net.http.*;
import org.json.*;

public class CandleFetcher {
    public static JSONArray fetch5MinCandles(String accessToken, String instrumentToken) throws Exception {
        String to = java.time.Instant.now().toString();
        String from = java.time.Instant.now().minus(java.time.Duration.ofDays(2)).toString();

        String url = String.format("https://api.upstox.com/v2/historical-candle/%s/interval/5minute?from=%s&to=%s",
                instrumentToken, from, to);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());
        return json.getJSONObject("data").getJSONArray("candles");
    }
}