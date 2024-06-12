package com.metatrope.jact;

import com.metatrope.jact.message.Envelope;

public abstract class SystemActor<T, R> extends Actor<Envelope<T>, R> {

    @Override
    public abstract R onMessage(Envelope<T> message) throws Exception;

    @Override
    boolean isSystemActor() {
        return true;
    }
}
