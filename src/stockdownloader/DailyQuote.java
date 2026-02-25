package stockdownloader;

import java.time.LocalDate;

/**
 * Represents a daily stock quote with OHLCV data.
 * Fields: date, open, close, high, low, volume
 */
public class DailyQuote {
    private LocalDate date;
    private double open;
    private double close;
    private double high;
    private double low;
    private long volume;
    
    public DailyQuote(LocalDate date, double open, double close, double high, double low, long volume) {
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
    }
    
    // Getters
    public LocalDate getDate() {
        return date;
    }
    
    public double getOpen() {
        return open;
    }
    
    public double getClose() {
        return close;
    }
    
    public double getHigh() {
        return high;
    }
    
    public double getLow() {
        return low;
    }
    
    public long getVolume() {
        return volume;
    }
    
    @Override
    public String toString() {
        return "DailyQuote{" +
                "date=" + date +
                ", open=" + open +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                '}';
    }
}
