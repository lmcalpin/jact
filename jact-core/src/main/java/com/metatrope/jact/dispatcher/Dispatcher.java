package com.metatrope.jact.dispatcher;

import com.metatrope.jact.message.Envelope;

import java.util.concurrent.Future;

public interface Dispatcher {
    <T, R> Future<R> dispatch(Envelope<T> message);
}