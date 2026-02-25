package stockdownloader;

/**
 * Custom exception for stock data download flow errors.
 * Extends RuntimeException for unchecked exception handling.
 */
public class FlowException extends RuntimeException {
    
    public FlowException(String message) {
        super(message);
    }
    
    public FlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
