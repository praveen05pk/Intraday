package org.automation;

import java.util.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.json.*;

public class CandleUtils {
    public static List<Candle> parseCandles(JSONArray candlesArray) {
        List<Candle> candles = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        for (int i = 0; i < candlesArray.length(); i++) {
            JSONArray c = candlesArray.getJSONArray(i);
            ZonedDateTime time = ZonedDateTime.parse(c.getString(0), formatter);
            candles.add(new Candle(time, c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getDouble(5)));
        }
        return candles;
    }
}