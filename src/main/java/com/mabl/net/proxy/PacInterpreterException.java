package com.mabl.net.proxy;

public class PacInterpreterException extends Exception {
    private static final long serialVersionUID = 1L;

    public PacInterpreterException(final String message) {
        super(message);
    }

    public PacInterpreterException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
