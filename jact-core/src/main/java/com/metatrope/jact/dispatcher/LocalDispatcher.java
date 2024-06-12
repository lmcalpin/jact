package com.metatrope.jact.dispatcher;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.exceptions.ActorException;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageType;
import com.metatrope.jact.queue.BlockingMessageQueue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalDispatcher extends ReplyDispatcher<BlockingMessageQueue> {
    private static final Logger logger = LogManager.getLogger(LocalDispatcher.class);

    private final ActorSystem actorSystem;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final ExecutorService executorService;
    private final Map<String, Mailbox<?, ?>> mailboxes = new ConcurrentHashMap<>();

    public LocalDispatcher(ActorSystem actorSystem) {
        super(new BlockingMessageQueue());
        this.actorSystem = actorSystem;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.executorService.submit(this::dispatcherLoop);
    }

    public <T, R> Mailbox<T, R> register(String name) {
        Mailbox<T, R> mailbox = new Mailbox<T, R>();
        mailboxes.put(name, mailbox);
        return mailbox;
    }

    public <T, R> void unregister(String name) {
        mailboxes.remove(name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void distributeEnvelopes() {
        try {
            Envelope envelope = take();
            if (envelope != null) {
                if (envelope.getMessageType() == MessageType.REPLY) {
                    logger.trace("{} == REPLY == {}", actorSystem.getName(), envelope);
                    super.deliverReply(envelope);
                } else {
                    logger.trace("{} == DELIVERED == {}", actorSystem.getName(), envelope);
                    Mailbox<?, ?> mailbox = mailboxes.get(envelope.getReceiverId());
                    if (mailbox == null) {
                        logger.error("{} == MISSING MAILBOX {}", actorSystem.getName(), envelope.getReceiverId());
                        System.exit(-1);
                    }
                    if (mailbox != null) {
                        mailbox.deliver(envelope);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Actor system failed", e);
            throw new ActorException("Actor system failed due to an unexpected error", e);
        }
    }

    public void close() {
        stopped.set(true);
        executorService.shutdownNow();
    }

    @Override
    public <T, R> Future<R> dispatch(Envelope<T> envelope) {
        logger.trace("{} == SENDING == `{}`", actorSystem.getName(), envelope);
        if (envelope.getReceiverId() == null && envelope.getMessageType() != MessageType.REPLY) {
            throw new ActorException("no recipient");
        }
        return super.dispatch(envelope);
    }

    /*
     * Look for new messages and deliver them to the appropriate Actor's mailbox.
     * This method will block until new messages are available.
     */
    private void dispatcherLoop() {
        while (!stopped.get()) {
            distributeEnvelopes();
        }
    }

    @Override
    public ActorSystem getActorSystem() {
        return actorSystem;
    }
}
