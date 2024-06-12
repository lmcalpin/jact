package com.metatrope.jact;

import com.metatrope.jact.message.Envelope;

import java.util.function.Supplier;

/**
 * A ThreadLocal context that holds a reference to the current ActorRef during
 * message processing.
 */
public class ActorContext<T, R> {
    private static final ThreadLocal<ActorContext<?, ?>> CURRENT_CONTEXT = new ThreadLocal<>();

    private final ActorRef<?, ?> sender;
    private final ActorRef<T, R> recipient;
    private final Envelope<T> envelope;

    public ActorContext(ActorRef<?, ?> sender, ActorRef<T, R> recipient, Envelope<T> envelope) {
        this.sender = sender;
        this.recipient = recipient;
        this.envelope = envelope;
    }

    public ActorRef<?, ?> getSender() {
        return sender;
    }

    public ActorRef<T, R> getRecipient() {
        return recipient;
    }

    Envelope<T> getEnvelope() {
        return envelope;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> ActorRef<T, R> self() {
        if (CURRENT_CONTEXT.get() == null)
            return null;
        return (ActorRef<T, R>) CURRENT_CONTEXT.get().getRecipient();
    }

    @SuppressWarnings("unchecked")
    public static <T, R> ActorRef<T, R> sender() {
        if (CURRENT_CONTEXT.get() == null)
            return null;
        return (ActorRef<T, R>) CURRENT_CONTEXT.get().getSender();
    }

    @SuppressWarnings("unchecked")
    static <T> Envelope<T> envelope() {
        if (CURRENT_CONTEXT.get() == null)
            return null;
        return (Envelope<T>) CURRENT_CONTEXT.get().getEnvelope();
    }

    public static <T, R> R withContext(ActorSystem system, Envelope<T> envelope, Supplier<R> r) {
        ActorRef<T, R> currentActor = system.locateLocal(envelope.getReceiverId());
        ActorRef<?, ?> sender = envelope.getSenderId() != null ? system.tryLocateLocal(envelope.getSenderId()) : null;
        ActorContext<T, R> ctx = new ActorContext<T, R>(sender, currentActor, envelope);
        CURRENT_CONTEXT.set(ctx);
        try {
            return r.get();
        } finally {
            CURRENT_CONTEXT.set(null);
        }
    }
}
