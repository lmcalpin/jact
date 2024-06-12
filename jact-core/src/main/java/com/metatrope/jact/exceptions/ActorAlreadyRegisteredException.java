package com.metatrope.jact.exceptions;

public class ActorAlreadyRegisteredException extends ActorException {
    private static final long serialVersionUID = 1L;

    public ActorAlreadyRegisteredException() {
        super();
    }

    public ActorAlreadyRegisteredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ActorAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorAlreadyRegisteredException(String message) {
        super(message);
    }

    public ActorAlreadyRegisteredException(Throwable cause) {
        super(cause);
    }
}
