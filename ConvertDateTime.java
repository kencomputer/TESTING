import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConvertDateTime {
    private static final DateTimeFormatter INPUT_FORMAT = 
        DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss 'HKT' yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter OUTPUT_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Convert datetime from format 'Dy Mon DD HH24:MI:SS HKT YYYY' 
     * to 'YYYY-MM-DD HH24:MI:SS'
     * 
     * Example: 'Wed Jan 15 14:30:45 HKT 2025' -> '2025-01-15 14:30:45'
     */
    public static String convertDateTime(String dtString) {
        try {
            String trimmed = dtString.trim();
            // Remove surrounding quotes if present
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            LocalDateTime dt = LocalDateTime.parse(trimmed, INPUT_FORMAT);
            return OUTPUT_FORMAT.format(dt);
        } catch (Exception e) {
            System.err.println("Error converting '" + dtString + "': " + e.getMessage());
            return dtString;
        }
    }

    /**
     * Read CSV, convert DateTime column, and write to output CSV
     */
    public static boolean convertCsv(String inputFile, String outputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            String[] headers = null;
            int dateTimeIndex = -1;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Parse header
                    headers = parseCSVLine(line);
                    for (int i = 0; i < headers.length; i++) {
                        if ("DateTime".equalsIgnoreCase(headers[i].trim())) {
                            dateTimeIndex = i;
                            break;
                        }
                    }
                    writer.write(line);
                    writer.newLine();
                    isFirstLine = false;
                } else {
                    // Parse data line
                    String[] fields = parseCSVLine(line);
                    
                    // Convert DateTime field if exists
                    if (dateTimeIndex >= 0 && dateTimeIndex < fields.length) {
                        fields[dateTimeIndex] = "\"" + convertDateTime(fields[dateTimeIndex]) + "\"";
                    }
                    
                    // Write converted line
                    writer.write(formatCSVLine(fields));
                    writer.newLine();
                }
            }

            System.out.println("Conversion complete. Output saved to: " + outputFile);
            return true;

        } catch (FileNotFoundException e) {
            System.err.println("Error: Input file '" + inputFile + "' not found");
            return false;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Parse a CSV line, handling quoted fields
     */
    private static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                insideQuotes = !insideQuotes;
                current.append(ch);
            } else if (ch == ',' && !insideQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Format fields back into CSV line
     */
    private static String formatCSVLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(fields[i]);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String inputFile = args.length > 0 ? args[0] : "input.csv";
        String outputFile = args.length > 1 ? args[1] : "output.csv";

        convertCsv(inputFile, outputFile);
    }
}
