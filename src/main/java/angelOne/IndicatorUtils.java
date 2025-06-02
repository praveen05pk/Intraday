package angelOne;

import java.util.*;

public class IndicatorUtils {
    public static double calculateRSI(List<Candle> candles) {
        double gain = 0, loss = 0;
        for (int i = 1; i < candles.size(); i++) {
            double diff = candles.get(i).getClose() - candles.get(i - 1).getClose();
            if (diff > 0) gain += diff;
            else loss -= diff;
        }
        if (loss == 0) return 100;
        double rs = gain / loss;
        return 100 - (100 / (1 + rs));
    }

    public static double calculateATR(List<Candle> candles) {
        double atr = 0;
        for (int i = 1; i < candles.size(); i++) {
            double high = candles.get(i).getHigh();
            double low = candles.get(i).getLow();
            double prevClose = candles.get(i - 1).getClose();
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            atr += tr;
        }
        return candles.size() > 1 ? atr / (candles.size() - 1) : 0;
    }
}

