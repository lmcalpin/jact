package com.metatrope.jact.exceptions;

/**
 * Oh snap.
 */
public class ActorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ActorException() {
        super();
    }

    public ActorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ActorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorException(String message) {
        super(message);
    }

    public ActorException(Throwable cause) {
        super(cause);
    }
}
