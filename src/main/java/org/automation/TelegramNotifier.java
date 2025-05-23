package org.automation;

import java.io.IOException;
import java.util.List;
import okhttp3.*;

public class TelegramNotifier {
    private static final String BOT_TOKEN = "7809511890:AAEENJpQBNhT8wQnlEi7MSsaHwq8qd1QYPU";
    private static final String CHAT_ID = "1230484573";



    public static void sendStockSignal(List<StockSignal> topStocks) throws IOException {
        StringBuilder msg = new StringBuilder("Top 3 Bullish Stocks (Low ATR):\n\n");
        for (StockSignal stock : topStocks) {
            msg.append(String.format(
                "%s\nPrice: %.2f\nRSI: %.2f\nATR: %.2f\nVWAP: %.2f\n\n",
                stock.symbol, stock.lastPrice, stock.rsi, stock.atr, stock.vwap
            ));
        }

        String url = String.format("https://api.telegram.org/bot%s/sendMessage", BOT_TOKEN);
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("chat_id", CHAT_ID)
                .add("text", msg.toString())
                .build();

        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).execute();
    }
}