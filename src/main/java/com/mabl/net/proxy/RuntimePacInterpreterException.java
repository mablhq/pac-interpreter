package com.mabl.net.proxy;

public class RuntimePacInterpreterException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RuntimePacInterpreterException(final String message) {
        super(message);
    }

    public RuntimePacInterpreterException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
