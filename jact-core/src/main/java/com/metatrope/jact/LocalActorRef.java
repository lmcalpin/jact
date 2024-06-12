package com.metatrope.jact;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageFactory;

import java.util.concurrent.Future;

import com.google.common.base.Preconditions;

/**
 * A reference to an <code>Actor</code> on a local <code>ActorSystem</code>.
 */
public class LocalActorRef<T, R> implements ActorRef<T, R> {
    private final String name;
    private final ActorSystem system;

    LocalActorRef(ActorSystem system, String name) {
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.contains("@"));
        this.name = name;
        this.system = system;
    }

    @Override
    public void tell(T payload) {
        Envelope<T> message = MessageFactory.tell(ActorContext.self(), this, payload);
        system.getLocalDispatcher().dispatch(message);
    }

    @Override
    public Future<R> ask(T payload) {
        Envelope<T> message = MessageFactory.ask(ActorContext.self(), this, payload);
        Future<R> reply = system.getLocalDispatcher().dispatch(message);
        return reply;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Future<R> forward() {
        Envelope<T> originalEnvelope = ActorContext.envelope();
        Envelope<T> forwarded = MessageFactory.forward(this, originalEnvelope);
        return system.getLocalDispatcher().dispatch(forwarded);
    }

    @Override
    public String toString() {
        return system.getName() + "/" + name;
    }
}
