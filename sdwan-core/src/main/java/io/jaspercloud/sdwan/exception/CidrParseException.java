package io.jaspercloud.sdwan.exception;

public class CidrParseException extends RuntimeException {

    public CidrParseException() {
    }

    public CidrParseException(String message) {
        super(message);
    }

    public CidrParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public CidrParseException(Throwable cause) {
        super(cause);
    }
}
