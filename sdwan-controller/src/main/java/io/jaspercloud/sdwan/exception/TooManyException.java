package io.jaspercloud.sdwan.exception;

public class TooManyException extends RuntimeException {

    public TooManyException() {
    }

    public TooManyException(String message) {
        super(message);
    }

    public TooManyException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyException(Throwable cause) {
        super(cause);
    }
}
