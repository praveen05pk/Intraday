package org.automation;

import java.util.*;
import java.util.stream.Collectors;
import org.json.*;

public class StockAnalyzer {
    public static List<StockSignal> analyzeAllStocks(Map<String, String> symbolToTokenMap, String accessToken) throws Exception {
        List<StockSignal> bullishStocks = new ArrayList<>();
        for (String symbol : symbolToTokenMap.keySet()) {
            String token = symbolToTokenMap.get(symbol);
            try {
                JSONArray json = CandleFetcher.fetch5MinCandles(accessToken, token);
                List<Candle> candles = CandleUtils.parseCandles(json);
                if (candles.size() < 15) continue;

                double rsi = IndicatorUtils.calculateRSI(candles, 14);
                double atr = IndicatorUtils.calculateATR(candles, 14);
                double vwap = IndicatorUtils.calculateVWAP(candles);
                double lastPrice = candles.get(candles.size() - 1).close;

                if (rsi > 60 && lastPrice > vwap) {
                    bullishStocks.add(new StockSignal(symbol, rsi, atr, vwap, lastPrice));
                }

                Thread.sleep(200);
            } catch (Exception e) {
                System.out.println("Error for " + symbol + ": " + e.getMessage());
            }
        }
        return bullishStocks.stream()
                .sorted(Comparator.comparingDouble(s -> s.atr))
                .limit(3)
                .collect(Collectors.toList());
    }
}