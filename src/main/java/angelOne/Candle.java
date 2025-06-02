package angelOne;

public class Candle {
    private String symbol;
    private String time;
    private double open, high, low, close, volume;

    // Constructor + Getters
    public Candle(String symbol, String time, double open, double high, double low, double close, double volume) {
        this.symbol = symbol;
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public String getSymbol() { return symbol; }
    public String getTime() { return time; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public double getVolume() { return volume; }
}

