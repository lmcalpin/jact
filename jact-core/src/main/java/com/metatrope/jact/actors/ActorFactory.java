package com.metatrope.jact.actors;

import com.metatrope.jact.Actor;
import com.metatrope.jact.ActorBehavior;

import java.util.function.Consumer;

public class ActorFactory {
    public static <T, R> Actor<T, R> create(ActorBehavior<T, R> behavior) {
        return new DelegatingActor<>(behavior);
    }

    public static <T> Actor<T, Void> create(Consumer<T> consumer) {
        return new DelegatingActor<>(consumer);
    }

}
