package org.automation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.*;
import org.json.JSONObject;

public class UpstoxAuth {



        // First-time login using auth code (manual)
        public static String getAccessToken(String apiKey, String apiSecret, String authCode, String redirectUri) throws Exception {

            String url = "https://api.upstox.com/v2/login/authorization/token";

            String data = "code=" + URLEncoder.encode(authCode, "UTF-8") +
                    "&client_id=" + URLEncoder.encode(apiKey, "UTF-8") +
                    "&client_secret=" + URLEncoder.encode(apiSecret, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                    "&grant_type=authorization_code";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes(data);
                out.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);

            if (responseCode == 401) {
                throw new RuntimeException("401 Unauthorized: Check your API credentials or auth code.");
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            System.out.println("Full Response: " + json.toString(4));

            if (!json.has("data")) {
                throw new RuntimeException("Access token response missing 'data': " + json.toString());
            }

            String accessToken = json.getJSONObject("data").getString("access_token");
            String refreshToken = json.getJSONObject("data").getString("refresh_token");

            TokenManager.saveTokens(accessToken, refreshToken);
            return accessToken;
        }

        // Daily auto-login using refresh token
        public static String refreshAccessToken(String apiKey, String apiSecret, String refreshToken) throws Exception {
            String url = "https://api.upstox.com/v2/login/refresh/token";

            String data = "refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") +
                    "&client_id=" + URLEncoder.encode(apiKey, "UTF-8") +
                    "&client_secret=" + URLEncoder.encode(apiSecret, "UTF-8") +
                    "&grant_type=refresh_token";

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                out.writeBytes(data);
                out.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Refresh Token Response Code: " + responseCode);

            if (responseCode != 200) {
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorStream.readLine()) != null) {
                    errorResponse.append(line);
                }
                System.out.println("Error Response: " + errorResponse.toString());
                throw new RuntimeException("Refresh failed. HTTP Code: " + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            System.out.println("Refreshed Response: " + json.toString(4));

            String newAccessToken = json.getJSONObject("data").getString("access_token");
            String newRefreshToken = json.getJSONObject("data").getString("refresh_token");

            TokenManager.saveTokens(newAccessToken, newRefreshToken);
            return newAccessToken;
        }
    }

