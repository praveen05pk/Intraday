package angelOne;


import java.io.InputStream;
import java.util.*;

public class Main {


    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("keys.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find key.properties");
            }
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load key.properties", e);
        }
        // 1. Angel One Credentials

        String apiKey = props.getProperty("ANGELONE_API_KEY");
        String clientCode = props.getProperty("ANGELONE_CLIENT_CODE");
        String secretKey = props.getProperty("ANGELONE_SECRET_KEY");
        String totpSecret = props.getProperty("ANGELONE_TOTP_SECRET");


        // Generate bearer token using TokenGenerator
        TokenGenerator tokenGenerator = new TokenGenerator(apiKey, clientCode, secretKey, totpSecret);
        String bearerToken = tokenGenerator.generateAccessToken();

        System.out.println(bearerToken);

        // 2. Telegram Bot Credentials
        String telegramBotToken = props.getProperty("TELEGRAM_BOT_TOKEN");
        String telegramChatId = props.getProperty("TELEGRAM_CHAT_ID");

        TelegramNotifier telegramNotifier = new TelegramNotifier(telegramBotToken, telegramChatId);

        // 3. Stock Symbol-Token Map
        Map<String, String> symbolTokenMap = stockSymbolTokenMap.getHardcodedSymbolTokenMap();
        List<String> stocksToAnalyze = new ArrayList<>(symbolTokenMap.keySet());

        // 4. Angel One API Client
        AngelOneApiClient apiClient = new AngelOneApiClient(bearerToken, clientCode, symbolTokenMap);

        // 5. Stock Analysis
        List<BullishStock> bullishStocks = StockAnalyzer.analyzeStocks(stocksToAnalyze, apiClient);

        // 6. Telegram Message Formatting
        String message = TelegramFormatter.formatStockAnalysis(bullishStocks);
        telegramNotifier.sendMessage(message);
    }
}
