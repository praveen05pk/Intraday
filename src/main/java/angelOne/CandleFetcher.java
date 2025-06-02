package angelOne;

import java.util.*;

public class CandleFetcher {
    public static List<Candle> fetchCandles(String symbol, AngelOneApiClient client) {
        return client.getFiveMinCandles(symbol);
    }
}

