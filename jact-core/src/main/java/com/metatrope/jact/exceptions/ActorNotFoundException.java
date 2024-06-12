package com.metatrope.jact.exceptions;

public class ActorNotFoundException extends ActorException {
    private static final long serialVersionUID = 1L;

    public ActorNotFoundException() {
        super();
    }

    public ActorNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ActorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorNotFoundException(String message) {
        super(message);
    }

    public ActorNotFoundException(Throwable cause) {
        super(cause);
    }
}
