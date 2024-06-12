package com.metatrope.jact.dispatcher;

import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.message.MessageType;
import com.metatrope.jact.queue.MessageQueue;
import com.metatrope.jact.queue.QueueReceiver;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ReplyDispatcher<Q extends MessageQueue> implements Dispatcher, QueueReceiver {
    private static final Logger logger = LogManager.getLogger(ReplyDispatcher.class);

    private final Map<String, CompletableFuture<?>> awaitingReplies = new ConcurrentHashMap<>();
    private final Q messageQueue;

    public ReplyDispatcher(Q messageQueue) {
        this.messageQueue = messageQueue;
    }

    protected Q getMessageQueue() {
        return messageQueue;
    }

    public abstract ActorSystem getActorSystem();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CompletableFuture deliverReply(Envelope reply) {
        CompletableFuture futureReply = awaitingReplies.get(reply.getReplyToId());
        if (futureReply == null) {
            logger.error("{} -- Received a reply for an unknown request {} == {}", getActorSystem().getName(), reply.getReplyToId(), reply);
        } else {
            logger.trace("{} -- Received a reply for a request {} -- completing {}", getActorSystem().getName(), reply.getReplyToId(), futureReply.hashCode());
            futureReply.complete(reply.getPayload());
            awaitingReplies.remove(reply.getReplyToId());
        }
        return futureReply;
    }

    @Override
    public Envelope<?> take() {
        Envelope<?> envelope = messageQueue.take();
        if (envelope != null) {
            if (envelope.getMessageType() == MessageType.REPLY) {
                deliverReply(envelope);
                return null;
            }
        }
        return envelope;
    }

    @Override
    public <T, R> Future<R> dispatch(Envelope<T> envelope) {
        CompletableFuture<R> futureReply = null;
        if (envelope.isAsk()) {
            futureReply = new CompletableFuture<R>();
            logger.trace("Awaiting a reply: {} with Future {}", envelope.getMessageId(), futureReply.hashCode());
            awaitingReplies.put(envelope.getMessageId(), futureReply);
        }
        messageQueue.offer(envelope);
        return futureReply;
    }

}
