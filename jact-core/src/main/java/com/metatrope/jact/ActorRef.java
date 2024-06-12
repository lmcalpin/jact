package com.metatrope.jact;

import java.util.concurrent.Future;

/**
 * A reference to an Actor.
 *
 * @author Lawrence McAlpin (admin@lmcalpin.com)
 */
public interface ActorRef<T, R> {
    public String getName();

    public void tell(T message);

    public Future<R> ask(T message);

    public Future<R> forward();
}
