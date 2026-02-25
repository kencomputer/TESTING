package stockdownloader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * StockDataDownloader: Downloads historical daily stock data from Yahoo Finance
 * and exports it to CSV format.
 * 
 * Usage: java StockDataDownloader TICKER START_DATE END_DATE CSV_PATH [USE_PROXY] [PROXY_HOST] [PROXY_PORT] [PROXY_USERNAME] [PROXY_PASSWORD]
 * Example (no proxy): java StockDataDownloader AAPL 2024-01-01 2024-12-31 output.csv
 * Example (with proxy): java StockDataDownloader AAPL 2024-01-01 2024-12-31 output.csv Y proxy.example.com 8080 username password
 */
public class StockDataDownloader {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    
    public static void main(String[] argss) {
    	String [] args = {"AAPL", "2025-01-01", "2026-12-31" ,"D:\\eap\\eclipse\\eclipse-workspace\\output.csv", "Y", "haproxy.housingauthority.gov.hk", "8080", "KAMWAHLEUNG", "796SkkH3##"};
    	
    	main1(args);
    }
    
    public static void main1(String[] args) {
        try {
            // Validate input arguments
            if (args.length < 4) {
                throw new FlowException("Invalid number of arguments. Usage: StockDataDownloader TICKER START_DATE END_DATE CSV_PATH [USE_PROXY] [PROXY_HOST] [PROXY_PORT] [PROXY_USERNAME] [PROXY_PASSWORD]");
            }
            
            String ticker = args[0];
            String startDateStr = args[1];
            String endDateStr = args[2];
            String csvPath = args[3];
            
            // Check for proxy settings
            boolean useProxy = false;
            String proxyHost = null;
            int proxyPort = -1;
            String proxyUsername = null;
            String proxyPassword = null;
            
            if (args.length >= 5) {
                useProxy = args[4].equalsIgnoreCase("Y");
                
                if (useProxy && args.length >= 7) {
                    proxyHost = args[5];
                    try {
                        proxyPort = Integer.parseInt(args[6]);
                    } catch (NumberFormatException e) {
                        throw new FlowException("Invalid proxy port: " + args[6] + ". Port must be a valid integer");
                    }
                    
                    // Check for proxy credentials
                    if (args.length >= 9) {
                        proxyUsername = args[7];
                        proxyPassword = args[8];
                        System.out.println("Proxy credentials provided for user: " + proxyUsername);
                    }
                } else if (useProxy && args.length < 7) {
                    throw new FlowException("Proxy is enabled but proxy host and/or port are missing. Required: USE_PROXY PROXY_HOST PROXY_PORT [PROXY_USERNAME] [PROXY_PASSWORD]");
                }
            }
            
            // Initialize HttpClient with or without proxy
            if (useProxy) {
                HTTP_CLIENT = createProxyHttpClient(proxyHost, proxyPort, proxyUsername, proxyPassword);
                System.out.println("Proxy enabled: " + proxyHost + ":" + proxyPort);
            } else {
                HTTP_CLIENT = HttpClient.newHttpClient();
                System.out.println("Proxy disabled - using direct connection");
            }
            
            // Validate and process inputs
            ticker = validateAndNormalizeTicker(ticker);
            LocalDate startDate = parseDate(startDateStr);
            LocalDate endDate = parseDate(endDateStr);
            
            System.out.println("Downloading stock data for: " + ticker);
            System.out.println("Period: " + startDate + " to " + endDate);
            
            // Download data from Yahoo Finance
            List<DailyQuote> quotes = downloadStockData(ticker, startDate, endDate);
            
            if (quotes.isEmpty()) {
                throw new FlowException("No data retrieved for ticker: " + ticker);
            }
            
            // Sort by date (ascending)
            Collections.sort(quotes, (q1, q2) -> q1.getDate().compareTo(q2.getDate()));
            
            // Export to CSV
            exportToCSV(quotes, csvPath);
            
            System.out.println("Successfully downloaded " + quotes.size() + " records");
            System.out.println("CSV exported to: " + csvPath);
            
        } catch (FlowException e) {
            System.err.println("FLOW EXCEPTION: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Validates ticker and converts to uppercase if necessary.
     * Throws FlowException if ticker is empty or invalid.
     */
    private static String validateAndNormalizeTicker(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new FlowException("Ticker cannot be empty");
        }
        
        String normalizedTicker = ticker.trim().toUpperCase();
        
        // Validate that ticker contains only alphanumeric characters and hyphens
        if (!normalizedTicker.matches("^[A-Z0-9\\-]+$")) {
            throw new FlowException("Invalid ticker format: " + ticker + ". Ticker must contain only letters, numbers, and hyphens");
        }
        
        return normalizedTicker;
    }
    
    /**
     * Parses date string in yyyy-MM-dd format.
     * Throws FlowException if format is invalid.
     */
    private static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new FlowException("Invalid date format: " + dateStr + ". Expected format: yyyy-MM-dd");
        }
    }
    
    /**
     * Downloads historical daily stock data from Yahoo Finance.
     * Yahoo Finance CSV URL format: https://query1.finance.yahoo.com/v7/finance/download/TICKER
     */
    private static List<DailyQuote> downloadStockData(String ticker, LocalDate startDate, LocalDate endDate) {
        try {
            // Convert dates to Unix timestamps (Yahoo Finance uses epoch seconds)
            long startTimestamp = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
            long endTimestamp = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond();
            
            // Build Yahoo Finance URL
            String url = String.format(
                "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%d&period2=%d&interval=1d&events=history&includeAdjustedClose=false",
                ticker, startTimestamp, endTimestamp
            );
            
            System.out.println("Fetching data from Yahoo Finance...");
            
            // Make HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new FlowException("Failed to download data. HTTP Status: " + response.statusCode());
            }
            
            // Parse CSV response
            return parseYahooCSV(response.body());
            
        } catch (FlowException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowException("Error downloading stock data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses Yahoo Finance CSV format.
     * Expected columns: Date, Open, High, Low, Close, Adj Close, Volume
     */
    private static List<DailyQuote> parseYahooCSV(String csvContent) {
        List<DailyQuote> quotes = new ArrayList<>();
        String[] lines = csvContent.split("\n");
        
        if (lines.length < 2) {
            throw new FlowException("Invalid CSV response from Yahoo Finance");
        }
        
        // Skip header line
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            
            try {
                String[] parts = line.split(",");
                if (parts.length < 7) {
                    continue; // Skip malformed lines
                }
                
                LocalDate date = LocalDate.parse(parts[0].trim(), DATE_FORMATTER);
                double open = parseDouble(parts[1]);
                double high = parseDouble(parts[2]);
                double low = parseDouble(parts[3]);
                double close = parseDouble(parts[4]);
                // parts[5] is Adj Close - we skip it
                long volume = parseLong(parts[6]);
                
                quotes.add(new DailyQuote(date, open, close, high, low, volume));
                
            } catch (Exception e) {
                // Log and skip problematic lines
                System.err.println("Warning: Skipping malformed line: " + line);
            }
        }
        
        if (quotes.isEmpty()) {
            throw new FlowException("No valid data parsed from CSV response");
        }
        
        return quotes;
    }
    
    /**
     * Safely parse double value, handling "null" strings from Yahoo Finance.
     */
    private static double parseDouble(String value) {
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("null") || trimmed.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(trimmed);
    }
    
    /**
     * Safely parse long value, handling "null" strings from Yahoo Finance.
     */
    private static long parseLong(String value) {
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("null") || trimmed.isEmpty()) {
            return 0L;
        }
        return Long.parseLong(trimmed);
    }
    
    /**
     * Exports DailyQuote list to CSV file with columns: DATE, OPEN, CLOSE, HIGH, LOW, VOLUMN
     */
    private static void exportToCSV(List<DailyQuote> quotes, String csvPath) {
        try {
            // Validate path - ensure directory exists or can be created
            java.nio.file.Path path = Paths.get(csvPath);
            java.nio.file.Path parentDir = path.getParent();
            
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(path))) {
                // Write header
                writer.write("DATE,OPEN,CLOSE,HIGH,LOW,VOLUMN");
                writer.newLine();
                
                // Write data rows
                for (DailyQuote quote : quotes) {
                    String line = String.format("%s,%.2f,%.2f,%.2f,%.2f,%d",
                        quote.getDate().format(DATE_FORMATTER),
                        quote.getOpen(),
                        quote.getClose(),
                        quote.getHigh(),
                        quote.getLow(),
                        quote.getVolume()
                    );
                    writer.write(line);
                    writer.newLine();
                }
            }
            
        } catch (IOException e) {
            throw new FlowException("Error writing CSV file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FlowException("Unexpected error during CSV export: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates an HttpClient configured with a proxy.
     * Supports proxy authentication with username and password via system properties.
     * @param proxyHost the proxy host address
     * @param proxyPort the proxy port number
     * @param proxyUsername the proxy username (optional)
     * @param proxyPassword the proxy password (optional)
     * @return HttpClient configured with the specified proxy and optional authentication
     * @throws FlowException if proxy configuration fails
     */
    private static HttpClient createProxyHttpClient(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
        try {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxyHost, proxyPort);
            
            // Set system properties for proxy authentication
            if (proxyUsername != null && proxyPassword != null && !proxyUsername.isEmpty() && !proxyPassword.isEmpty()) {
                System.setProperty("java.net.useSystemProxies", "false");
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                System.setProperty("java.net.http.allowRestrictedHeaders", "host");
                
                // Encode credentials in Base64 for proxy authentication header
                String credentials = proxyUsername + ":" + proxyPassword;
                String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
                System.setProperty("http.proxyAuthorizationHeader", "Basic " + encodedCredentials);
                
                System.out.println("Proxy authentication enabled with username: " + proxyUsername);
            } else {
                System.out.println("Proxy authentication disabled - no credentials provided");
            }
            
            // Build and return HttpClient with proxy
            HttpClient proxyClient = HttpClient.newBuilder()
                .proxy(ProxySelector.of(proxyAddress))
                .build();
            
            return proxyClient;
            
        } catch (IllegalArgumentException e) {
            throw new FlowException("Invalid proxy configuration: proxyHost=" + proxyHost + ", proxyPort=" + proxyPort, e);
        } catch (Exception e) {
            throw new FlowException("Error creating proxy HttpClient: " + e.getMessage(), e);
        }
    }
}
