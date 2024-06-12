package com.metatrope.jact.actors;

import com.metatrope.jact.Actor;
import com.metatrope.jact.ActorBehavior;

import java.util.function.Consumer;

/**
 * A simple <code>Actor</code> implementation that delegates processing to an
 * <copde>ActorBehavior</code> instance.
 */
public class DelegatingActor<T, R> extends Actor<T, R> {
    private ActorBehavior<T, R> initialBehavior;

    public DelegatingActor(ActorBehavior<T, R> behavior) {
        this.initialBehavior = behavior;
    }

    public DelegatingActor(Consumer<T> f) {
        this.initialBehavior = msg -> {
            f.accept(msg);
            return null;
        };
    }

    @Override
    public R onMessage(T message) throws Exception {
        return initialBehavior.onMessage(message);
    }
}
