package com.moharajli.miniexchange.persistence;

public class RepositoryException extends RuntimeException {

    private final boolean retryable;

    public RepositoryException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
