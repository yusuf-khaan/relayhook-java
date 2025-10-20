package com.app.relayhook.Integrations.Relayhook;


public class RelayhookException extends RuntimeException {

    public RelayhookException(String message) {
        super(message);
    }

    public RelayhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
