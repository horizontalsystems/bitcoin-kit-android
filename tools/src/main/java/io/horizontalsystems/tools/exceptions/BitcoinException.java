package io.horizontalsystems.tools.exceptions;

/**
 * Base exception for bitcoin app.
 *
 * @author liaoxuefeng
 */
public class BitcoinException extends RuntimeException {

    public BitcoinException() {
    }

    public BitcoinException(String message) {
        super(message);
    }

    public BitcoinException(Throwable cause) {
        super(cause);
    }

    public BitcoinException(String message, Throwable cause) {
        super(message, cause);
    }

}

