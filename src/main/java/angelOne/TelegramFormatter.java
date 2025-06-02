package angelOne;

import java.util.List;

public class TelegramFormatter {

    public static String formatStockAnalysis(List<BullishStock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return "📉 *IntradayPKbot:*\nNo bullish stocks found today.";
        }

        StringBuilder sb = new StringBuilder("📊 *Top 3 Bullish Stocks:*\n\n");

        for (BullishStock stock : stocks) {
            sb.append("🔹 ").append(stock.symbol).append("\n")
                    .append("Price: ₹").append(String.format("%.2f", stock.price)).append("\n")
                    .append("Entry: ₹").append(String.format("%.2f", stock.entry)).append("\n")
                    .append("Exit: ₹").append(String.format("%.2f", stock.exit)).append("\n")
                    .append("Stoploss: ₹").append(String.format("%.2f", stock.stoploss)).append("\n")
                    .append("RSI: ").append(String.format("%.2f", stock.rsi)).append("\n")
                    .append("ATR: ").append(String.format("%.2f", stock.atr)).append("\n\n");
        }

        BullishStock bestPick = stocks.get(0);
        sb.append("🔥 *Best Pick:* ").append(bestPick.symbol).append("\n")
                .append("Entry: ₹").append(String.format("%.2f", bestPick.entry)).append("\n")
                .append("Exit: ₹").append(String.format("%.2f", bestPick.exit)).append("\n")
                .append("Stoploss: ₹").append(String.format("%.2f", bestPick.stoploss));

        return sb.toString();
    }
}
