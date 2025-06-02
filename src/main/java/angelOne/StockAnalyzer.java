package angelOne;

import java.util.*;

public class StockAnalyzer {
    public static List<BullishStock> analyzeStocks(List<String> symbols, AngelOneApiClient client) {
        List<BullishStock> bullishStocks = new ArrayList<>();

        for (String symbol : symbols) {
            List<Candle> candles = CandleFetcher.fetchCandles(symbol, client);
            if (candles.size() < 15) continue;

            double rsi = IndicatorUtils.calculateRSI(candles);
            double atr = IndicatorUtils.calculateATR(candles);
            double vwap = CandleUtils.calculateVWAP(candles);
            double close = candles.get(candles.size() - 1).getClose();

            if (rsi > 60 && close > vwap) {
                double entry = close;
                double exit = entry + (2 * atr);
                double stoploss = entry - (1.5 * atr);

                BullishStock stock = new BullishStock(symbol, close, rsi, atr, entry, exit, stoploss);
                bullishStocks.add(stock);
            }
        }

        // Sort by lowest ATR (safest)
        bullishStocks.sort(Comparator.comparingDouble(s -> s.atr));

        // Return top 3
        return bullishStocks.subList(0, Math.min(3, bullishStocks.size()));
    }
}
