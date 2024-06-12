package com.metatrope.jact.exceptions;

public class ActorKilledException extends ActorException {
    private static final long serialVersionUID = 1L;

    public ActorKilledException() {
        super();
    }

    public ActorKilledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ActorKilledException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorKilledException(String message) {
        super(message);
    }

    public ActorKilledException(Throwable cause) {
        super(cause);
    }
}
