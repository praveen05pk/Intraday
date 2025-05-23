package org.automation;

import java.util.*;

public class IndicatorUtils {
    public static double calculateRSI(List<Candle> candles, int period) {
        double gain = 0, loss = 0;
        for (int i = candles.size() - period; i < candles.size() - 1; i++) {
            double diff = candles.get(i + 1).close - candles.get(i).close;
            if (diff > 0) gain += diff;
            else loss -= diff;
        }
        double rs = (loss == 0) ? 100 : gain / loss;
        return 100 - (100 / (1 + rs));
    }

    public static double calculateATR(List<Candle> candles, int period) {
        double atr = 0;
        for (int i = candles.size() - period; i < candles.size(); i++) {
            Candle c = candles.get(i);
            Candle prev = candles.get(i - 1);
            double tr = Math.max(c.high - c.low, Math.max(Math.abs(c.high - prev.close), Math.abs(c.low - prev.close)));
            atr += tr;
        }
        return atr / period;
    }

    public static double calculateVWAP(List<Candle> candles) {
        double cumPV = 0, cumVolume = 0;
        for (Candle c : candles) {
            double typicalPrice = (c.high + c.low + c.close) / 3.0;
            cumPV += typicalPrice * c.volume;
            cumVolume += c.volume;
        }
        return cumPV / cumVolume;
    }
}