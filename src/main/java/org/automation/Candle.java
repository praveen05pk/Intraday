package org.automation;

import java.time.ZonedDateTime;

public class Candle {
    public ZonedDateTime time;
    public double open, high, low, close, volume;

    public Candle(ZonedDateTime time, double open, double high, double low, double close, double volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }
}