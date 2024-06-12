package com.metatrope.jact;

/**
 * An implementation of <code>ActorBehavior</code> can encapsulate the logic
 * that an {@link Actor} will employ when processing a message.
 * <code>Actor</code>s may also supply an <code>ActorBehavior</code> instance to
 * the <code>become</code> and <code>unbecome</code> methods to change their
 * behavior at runtime.
 */
@FunctionalInterface
public interface ActorBehavior<T, R> {
    R onMessage(T message) throws Exception;

    @SuppressWarnings("unchecked")
    default ActorRef<T, R> self() {
        return (ActorRef<T, R>) ActorContext.self();
    }

    default ActorRef<?, ?> sender() {
        return (ActorRef<?, ?>) ActorContext.sender();
    }
}
