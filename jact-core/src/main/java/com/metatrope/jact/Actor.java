package com.metatrope.jact;

import com.metatrope.jact.dispatcher.Mailbox;
import com.metatrope.jact.exceptions.ActorException;
import com.metatrope.jact.message.Envelope;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for an {@link Actor}. An implementation must extend this class and
 * implement the <code>onMessage</code> method with the logic that the
 * {@link Actor} will perform when it receives a message.
 */
public abstract class Actor<T, R> implements ActorBehavior<T, R> {
    private static final Logger logger = LogManager.getLogger(Actor.class);

    private final Stack<ActorBehavior<T, R>> behaviorStack;
    private final AtomicBoolean started = new AtomicBoolean();
    private String name;
    private ActorSystem actorSystem;
    private LocalActorRef<T, R> self;
    private Mailbox<T, R> mailbox;
    private ActorProcessor<T, R> actorProcessor;

    public Actor() {
        this.behaviorStack = new Stack<>();
    }

    @Override
    public abstract R onMessage(T message) throws Exception;

    /**
     * Called by the <code>ActorProcessor</code> when we receive a message.
     */
    R processMessage(Envelope<T> envelope) throws Exception {
        if (!behaviorStack.isEmpty()) {
            return behaviorStack.peek().onMessage(envelope.getPayload());
        } else {
            return onMessage(envelope.getPayload());
        }
    }

    /**
     * Assume a different ActorBehavior for future messages.
     * 
     * @param behavior
     */
    public void become(ActorBehavior<T, R> behavior) {
        this.behaviorStack.push(behavior);
    }

    /**
     * Revert to a previous ActorBehavior for future messages.
     * 
     * @param behavior
     */
    public void unbecome() {
        this.behaviorStack.pop();
    }

    /**
     * @return the <code>ActorSystem</code> that this <code>Actor</code> is
     *         registered with
     */
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    /**
     * This method will cause the {@link Actor} to stop processing messages. Once
     * stopped, it can not be restarted.
     */
    public void stop() {
        actorSystem.unregister(this);
        actorSystem.getLocalDispatcher().unregister(getName());
        actorSystem = null;
        started.set(false);
    }

    public boolean isRunning() {
        return started.get();
    }

    public boolean isBusy() {
        if (actorProcessor == null) {
            return false;
        }
        return actorProcessor.isBusy();
    }

    /**
     * @return the <code>ActorProcessor</code> that this <code>Actor</code> is
     *         registered with
     */
    ActorProcessor<T, R> getActorProcessor() {
        return actorProcessor;
    }

    @Override
    public LocalActorRef<T, R> self() {
        return self;
    }

    /**
     * Called by the {@link ActorSystem} when the {@link Actor} is registered.
     * 
     * @param system
     *            an {@link ActorSystem} on which this {@link Actor} is
     *            registered.
     */
    void start(ActorSystem actorSystem, String name) {
        if (!isSystemActor()) {
            Preconditions.checkNotNull(name);
            Preconditions.checkArgument(name.matches("[a-zA-Z0-9-]+"), "Name must be alphanumeric only");
        }
        if (this.started.compareAndSet(false, true)) {
            this.actorSystem = actorSystem;
            this.name = name;
            this.self = new LocalActorRef<T, R>(actorSystem, name);
            this.mailbox = actorSystem.getLocalDispatcher().register(getName());
            this.actorProcessor = new ActorProcessor<>(actorSystem, this);
        } else {
            throw new ActorException("Actor already started");
        }
    }

    String getName() {
        return name;
    }

    boolean hasMail() {
        return mailbox.hasMail();
    }

    Mailbox<T, R> getMailbox() {
        return mailbox;
    }

    boolean isSystemActor() {
        return false;
    }
}
