package angelOne;

import java.util.*;

public class CandleUtils {
    public static double calculateVWAP(List<Candle> candles) {
        double totalPV = 0, totalVolume = 0;
        for (Candle c : candles) {
            double typicalPrice = (c.getHigh() + c.getLow() + c.getClose()) / 3.0;
            totalPV += typicalPrice * c.getVolume();
            totalVolume += c.getVolume();
        }
        return totalVolume == 0 ? 0 : totalPV / totalVolume;
    }
}

