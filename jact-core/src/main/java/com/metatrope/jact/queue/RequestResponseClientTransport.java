package com.metatrope.jact.queue;

import com.metatrope.jact.message.Envelope;
import com.metatrope.jact.remote.Transport;

import java.util.Map;

public class RequestResponseClientTransport implements Transport {
    private QueueSender serverQueue;
    private MessageQueue replyQueue;

    public RequestResponseClientTransport(MessageQueue serverQueue, MessageQueue replyQueue) {
        this.serverQueue = serverQueue;
        this.replyQueue = replyQueue;
    }
    
    public QueueSender getServerQueue() {
        return serverQueue;
    }

    public MessageQueue getReplyQueue() {
        return replyQueue;
    }
    
    @Override
    public void offer(Envelope<?> envelope) {
        serverQueue.offer(envelope);
    }

    @Override
    public Envelope<?> take() {
        return replyQueue.take();
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Map<String, String> getConnectionProperties() {
        return null;
    }
}
