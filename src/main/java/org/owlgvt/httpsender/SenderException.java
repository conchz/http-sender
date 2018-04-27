package org.owlgvt.httpsender;

public class SenderException extends RuntimeException {

    public SenderException(String message) {
        super(message);
    }

    public SenderException(Throwable cause) {
        super(cause);
    }

    public SenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
