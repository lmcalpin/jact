package com.metatrope.jact.remote.fake;

import com.metatrope.jact.ActorPath;
import com.metatrope.jact.ActorSystem;
import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.queue.BlockingMessageQueue;
import com.metatrope.jact.queue.QueueSender;
import com.metatrope.jact.remote.ServerEndpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeEndpoint implements ServerEndpoint {
    BlockingMessageQueue serverQueue = new BlockingMessageQueue();
    Map<String, QueueSender> replyQueues = new ConcurrentHashMap<>();
    ActorSystem actorSystem;

    @Override
    public void start(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }
    
    public void registerReplyQueue(String clientID, QueueSender q) {
        this.replyQueues.put(clientID, q);
    }

    @Override
    public Envelope<?> take() {
        return serverQueue.take();
    }

    @Override
    public <T> void send(Envelope<T> envelope) {
        String recipientID = envelope.getReceiverId();
        ActorPath actorPath = new ActorPath(actorSystem, recipientID);
        String targetActorSystem = actorPath.getActorSystemName();
        System.out.println("sending " + envelope + " to " + targetActorSystem + " --- [" + replyQueues.get(targetActorSystem) + "]");
        replyQueues.get(targetActorSystem).offer(envelope);
    }

    @Override
    public void close() {

    }
}
