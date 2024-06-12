package com.metatrope.jact.exceptions;

public class ActorBusyException extends ActorException {
    private static final long serialVersionUID = 1L;

    public ActorBusyException() {
        super();
    }

    public ActorBusyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ActorBusyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorBusyException(String message) {
        super(message);
    }

    public ActorBusyException(Throwable cause) {
        super(cause);
    }
}
