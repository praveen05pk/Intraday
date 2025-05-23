package org.automation;

import java.io.File;
import java.util.*;

public class Main {


        public static void main(String[] args) throws Exception {
            String apiKey = "aec1694a-9dd4-4c6a-aee2-3fef8312f355";
            String apiSecret = "wx35ce1me6";
            String redirectUri = "https://127.0.0.1/callback";

            String accessToken;

            File tokenFile = new File("token.csv");
            if (tokenFile.exists()) {
                Map<String, String> tokens = TokenManager.loadTokens();
                String refreshToken = tokens.get("refresh_token");
                accessToken = UpstoxAuth.refreshAccessToken(apiKey, apiSecret, refreshToken);
            } else {
                // Only for first time
                String authCode = "mRzHMp";
                accessToken = UpstoxAuth.getAccessToken(apiKey, apiSecret, authCode, redirectUri);
            }

            Map<String, String> symbolToTokenMap = symbolTokenMap.getHardcodedSymbolTokenMap(); // Your static map here
            List<StockSignal> topStocks = StockAnalyzer.analyzeAllStocks(symbolToTokenMap, accessToken);
            TelegramNotifier.sendStockSignal(topStocks);
        }
    }
