package com.metatrope.jact.queue;

import com.metatrope.jact.message.Envelope;

import java.util.ArrayDeque;
import java.util.Queue;

public class NonBlockingMessageQueue implements MessageQueue, QueueReceiver {
    private Queue<Envelope<?>> queue = new ArrayDeque<>();

    @Override
    public void offer(Envelope<?> envelope) {
        queue.offer(envelope);
    }

    @Override
    public Envelope<?> take() {
        return queue.poll();
    }

    @Override
    public void close() throws Exception {

    }
}
