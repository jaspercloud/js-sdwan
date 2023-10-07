package io.jaspercloud.sdwan.exception;

public class ProcessCodeException extends RuntimeException {

    private int code;

    public int getCode() {
        return code;
    }

    public ProcessCodeException(int code) {
        this.code = code;
    }
}
