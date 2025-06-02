package angelOne;

public class BullishStock {
    public String symbol;
    public double price;
    public double rsi;
    public double atr;
    public double entry;
    public double exit;
    public double stoploss;

    public BullishStock(String symbol, double price, double rsi, double atr, double entry, double exit, double stoploss) {
        this.symbol = symbol;
        this.price = price;
        this.rsi = rsi;
        this.atr = atr;
        this.entry = entry;
        this.exit = exit;
        this.stoploss = stoploss;
    }
}
