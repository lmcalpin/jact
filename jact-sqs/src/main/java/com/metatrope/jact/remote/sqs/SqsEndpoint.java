package com.metatrope.jact.remote.sqs;

import com.metatrope.jact.ActorPath;
import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.queue.sqs.SqsMessageQueue;
import com.metatrope.jact.queue.sqs.SqsMessageQueueFactory;
import com.metatrope.jact.remote.ServerEndpoint;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SqsEndpoint implements ServerEndpoint {
    private static final Logger logger = LogManager.getLogger(SqsEndpoint.class);

    private ActorSystem actorSystem;
    private SqsMessageQueueFactory sqsMessageQueueFactory;
    private ConcurrentHashMap<String, SqsMessageQueue> sqsMessageQueues = new ConcurrentHashMap<>();
    private SqsMessageQueue sqsMessageQueue;
    
    public SqsEndpoint(SqsMessageQueueFactory sqsMessageQueueFactory) {
        this.sqsMessageQueueFactory = sqsMessageQueueFactory;
    }
    
    @Override
    public void start(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.sqsMessageQueue = sqsMessageQueueFactory.createSqsMessageQueue(actorSystem.getName());
    }

    @Override
    public Envelope<?> take() {
        Envelope<?> received = sqsMessageQueue.take();
        logger.debug("{} -- received from connected client {}", actorSystem.getName(), received);
        return received;
    }

    @Override
    public <T> void send(Envelope<T> envelope) {
        String recipientID = envelope.getReceiverId();
        ActorPath actorPath = new ActorPath(actorSystem, recipientID);
        String targetActorSystem = actorPath.getActorSystemName();
        SqsMessageQueue sqsMessageQueue = sqsMessageQueues.get(targetActorSystem);
        if (sqsMessageQueue == null) {
            sqsMessageQueue = sqsMessageQueueFactory.createSqsMessageQueue(targetActorSystem);
            sqsMessageQueues.put(targetActorSystem, sqsMessageQueue);
        }
        sqsMessageQueue.offer(envelope);
    }

    @Override
    public void close() {

    }

}
